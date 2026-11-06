/*
 * Copyright (C) 2026 Jens Kutschke
 *
 * This file is part of j-lawyer.org.
 *
 * j-lawyer.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * j-lawyer.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with j-lawyer.org.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.jdimension.jlawyer.documents;

import com.jdimension.jlawyer.persistence.ServerSettingsBean;
import com.jdimension.jlawyer.persistence.ServerSettingsBeanFacadeLocal;
import com.jdimension.jlawyer.server.services.settings.ServerSettingsKeys;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.transform.BaseScript;
import groovy.transform.CompileStatic;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.InitialContext;
import org.apache.log4j.Logger;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
import org.codehaus.groovy.ast.expr.MethodReferenceExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.transform.stc.ExtensionMethodNode;
import org.codehaus.groovy.transform.stc.StaticTypesMarker;

/**
 * Evaluates the [[SCRIPT:...]] expressions of document templates.
 *
 * <p>Placeholder values are inserted as escaped, single-quoted string literals, so values from cases
 * and contacts can never become code. Each expression is compiled as a script extending the trusted
 * base class {@code SmartTemplateBase} (resource {@code /templates/smart/SmartTemplateBase.groovy}),
 * statically compiled and checked against an allow list: only the template functions and calls on
 * well-known value classes (strings, numbers, dates, formats) are allowed.</p>
 *
 * <p>Server setting {@link ServerSettingsKeys#SERVERCONF_TEMPLATES_SCRIPTCHECK} controls what happens
 * with expressions that do not pass the check: {@value #MODE_ENFORCE} (default) rejects them,
 * {@value #MODE_LOG} logs them and runs them as before.</p>
 *
 * @author jens
 */
public final class SmartTemplateEvaluator {

    private static final Logger log = Logger.getLogger(SmartTemplateEvaluator.class.getName());

    public static final String MODE_LOG = "log";
    public static final String MODE_ENFORCE = "enforce";

    private static final String BASE_CLASS = "SmartTemplateBase";
    private static final String BASE_CLASS_RESOURCE = "/templates/smart/SmartTemplateBase.groovy";
    private static final String PLACEHOLDER_OPEN = "{{";
    private static final String PLACEHOLDER_CLOSE = "}}";

    private static GroovyClassLoader baseLoader = null;

    // classes whose methods and constructors may be used (exact names)
    private static final Set<String> ALLOWED_CLASSES = new HashSet<>(Arrays.asList(
            "java.lang.String", "java.lang.CharSequence", "java.lang.Comparable",
            "java.lang.Integer", "java.lang.Long", "java.lang.Double", "java.lang.Float", "java.lang.Short",
            "java.lang.Byte", "java.lang.Number", "java.lang.Boolean", "java.lang.Character",
            "java.lang.Math", "java.lang.StrictMath",
            "java.math.BigDecimal", "java.math.BigInteger", "java.math.RoundingMode", "java.math.MathContext",
            "java.text.SimpleDateFormat", "java.text.DateFormat", "java.text.Format", "java.text.DecimalFormat",
            "java.text.NumberFormat", "java.text.DecimalFormatSymbols",
            "java.util.Date", "java.util.Calendar", "java.util.GregorianCalendar", "java.util.Locale",
            "java.util.Objects", "java.util.TimeZone",
            "groovy.lang.GString",
            "org.codehaus.groovy.runtime.StringGroovyMethods",
            BASE_CLASS, "templates.smart.SmartFunctions"));
    // packages whose classes may be used
    private static final List<String> ALLOWED_PACKAGES = Arrays.asList("java.time.");
    // methods of java.lang.Object that may be called
    private static final Set<String> ALLOWED_OBJECT_METHODS = new HashSet<>(Arrays.asList("equals", "toString", "hashCode", "compareTo"));
    // Groovy default methods (DefaultGroovyMethods) that may be called
    private static final Set<String> ALLOWED_GROOVY_DEFAULT_METHODS = new HashSet<>(Arrays.asList(
            "toString", "equals", "plus", "minus", "multiply", "div", "mod", "power", "intdiv", "compareTo",
            "abs", "round", "isCase", "asBoolean", "size", "getAt", "contains", "join"));
    // methods that are never allowed, on whatever class
    private static final Set<String> FORBIDDEN_METHODS = new HashSet<>(Arrays.asList(
            "getClass", "forName", "newInstance", "invokeMethod", "getMetaClass", "setMetaClass", "getProperty",
            "setProperty", "evaluate", "execute", "run", "wait", "notify", "notifyAll", "finalize", "exit",
            "getBinding", "setBinding"));
    // properties that are never allowed
    private static final Set<String> FORBIDDEN_PROPERTIES = new HashSet<>(Arrays.asList(
            "class", "metaClass", "binding", "properties", "classLoader", "declaredMethods", "declaredFields",
            "methods", "fields", "constructors", "delegate", "owner", "thisObject", "protectionDomain"));

    private SmartTemplateEvaluator() {
    }

    /**
     * Evaluates a script placeholder of a template. The check mode is read once
     * per document via {@link #readMode()}.
     *
     * @param caseId the case the document is created for, may be null
     * @param scriptContent the script, with or without the surrounding [[SCRIPT: and ]]
     * @param values placeholder values, keys in the form {{NAME}}
     * @param formsPrefixes prefixes of the forms of the case
     * @param mode {@value #MODE_LOG} or {@value #MODE_ENFORCE}; anything other than {@value #MODE_LOG} enforces the check
     * @return the result of the script, or "SCRIPTFEHLER: ..." if it fails or is not allowed
     */
    public static String evaluate(String caseId, String scriptContent, Map<String, Object> values, List<String> formsPrefixes, String mode) {
        String script = prepareScript(scriptContent, values);

        StringBuilder allFormPrefixes = new StringBuilder();
        if (formsPrefixes != null) {
            for (String fp : formsPrefixes) {
                allFormPrefixes.append("-").append(fp).append("-");
            }
        }

        Class<?> scriptClass;
        try {
            scriptClass = compileChecked(script);
        } catch (ScriptCheckException ex) {
            if (!MODE_LOG.equals(mode)) {
                log.warn("template script rejected: " + ex.getMessage() + " - script: " + script);
                return "SCRIPTFEHLER: Skript nicht zulässig (" + ex.getMessage() + ")";
            }
            log.warn("template script does not pass the allow list and will be rejected once the check is enforced: " + ex.getMessage() + " - script: " + script);
            try {
                scriptClass = compileUnchecked(script);
            } catch (Throwable t) {
                return scriptError(script, t);
            }
        } catch (Throwable t) {
            return scriptError(script, t);
        }

        try {
            Script instance = (Script) scriptClass.getDeclaredConstructor().newInstance();
            scriptClass.getMethod("setCaseId", String.class).invoke(instance, caseId == null ? "" : caseId);
            scriptClass.getMethod("setAllFormPrefixes", String.class).invoke(instance, allFormPrefixes.toString());
            Object result = instance.run();
            return result == null ? null : result.toString();
        } catch (Throwable t) {
            return scriptError(script, t);
        }
    }

    private static String scriptError(String script, Throwable t) {
        Throwable cause = t;
        while (cause instanceof java.lang.reflect.InvocationTargetException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        log.error("Unable to evaluate smart template script", cause);
        log.error("****************************************");
        log.error(script);
        log.error("****************************************");
        return "SCRIPTFEHLER: " + cause.getMessage();
    }

    /**
     * Strips the [[SCRIPT: ]] markers and replaces placeholder names by their values as escaped,
     * single-quoted string literals (no interpolation, so values can never become code).
     *
     * @param scriptContent the script
     * @param values placeholder values, keys in the form {{NAME}}
     * @return the script ready for compilation
     */
    static String prepareScript(String scriptContent, Map<String, Object> values) {
        if (scriptContent.startsWith("[[SCRIPT:")) {
            scriptContent = scriptContent.substring(9);
        }
        if (scriptContent.endsWith("]]")) {
            scriptContent = scriptContent.substring(0, scriptContent.length() - 2);
        }
        scriptContent = scriptContent.trim();
        // typographic quotes written by office suites - before inserting values, so values are not changed
        scriptContent = scriptContent.replace("„", "\"");
        scriptContent = scriptContent.replace("“", "\"");

        if (values != null) {
            // longest keys first, e.g. to prevent MANDANT_ORT from replacing a part of MANDANT_ORTSTEIL
            List<String> keys = new ArrayList<>(values.keySet());
            Collections.sort(keys, (k1, k2) -> Integer.compare(k2 == null ? 0 : k2.length(), k1 == null ? 0 : k1.length()));
            for (String key : keys) {
                if (key != null && key.startsWith(PLACEHOLDER_OPEN) && key.endsWith(PLACEHOLDER_CLOSE)) {
                    Object value = values.get(key);
                    String name = key.substring(2, key.length() - 2);
                    if (name.isEmpty()) {
                        continue;
                    }
                    scriptContent = scriptContent.replaceAll(Pattern.quote(name), Matcher.quoteReplacement(toLiteral(value == null ? "" : value.toString())));
                }
            }
        }
        return scriptContent;
    }

    /**
     * @param value a string
     * @return the value as a single-quoted Groovy string literal (single quotes do not interpolate)
     */
    static String toLiteral(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('\'');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        sb.append('\'');
        return sb.toString();
    }

    private static synchronized GroovyClassLoader getBaseLoader() throws Exception {
        if (baseLoader == null) {
            StringBuilder sb = new StringBuilder();
            try (InputStream is = SmartTemplateEvaluator.class.getResourceAsStream(BASE_CLASS_RESOURCE);
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append(System.lineSeparator());
                }
            }
            GroovyClassLoader gcl = new GroovyClassLoader(SmartTemplateEvaluator.class.getClassLoader());
            gcl.parseClass(sb.toString(), BASE_CLASS + ".groovy");
            baseLoader = gcl;
        }
        return baseLoader;
    }

    /**
     * Compiles a script statically and checks it against the allow list.
     */
    static Class<?> compileChecked(String script) throws Exception {
        StructureCheck structureCheck = new StructureCheck();
        AllowListCheck allowListCheck = new AllowListCheck();
        CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass(BASE_CLASS);
        // the order matters: the structure is checked before any transformation runs, the allow list
        // after static compilation has resolved every call
        config.addCompilationCustomizers(structureCheck, new ASTTransformationCustomizer(CompileStatic.class), allowListCheck);
        try {
            // a new loader per script, so compiled scripts can be garbage collected
            GroovyClassLoader gcl = new GroovyClassLoader(getBaseLoader(), config);
            return gcl.parseClass(script, "TemplateScript.groovy");
        } catch (Throwable t) {
            String violation = structureCheck.violation != null ? structureCheck.violation : allowListCheck.violation;
            if (violation != null) {
                throw new ScriptCheckException(violation);
            }
            // not statically compilable (e.g. dynamic code) - counts as not allowed as well
            throw new ScriptCheckException("nicht prüfbar: " + firstLine(t.getMessage()));
        }
    }

    /**
     * Compiles a script without any check, as before the allow list existed.
     */
    private static Class<?> compileUnchecked(String script) throws Exception {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass(BASE_CLASS);
        GroovyClassLoader gcl = new GroovyClassLoader(getBaseLoader(), config);
        return gcl.parseClass(script, "TemplateScript.groovy");
    }

    private static String firstLine(String s) {
        if (s == null) {
            return "";
        }
        String[] lines = s.split("\\R");
        for (String l : lines) {
            if (!l.trim().isEmpty() && !l.startsWith("startup failed")) {
                return l.trim();
            }
        }
        return s.trim();
    }

    /**
     * Reads the check mode from the server settings. Callers read it once per
     * document and pass it to every script of that document.
     *
     * @return {@link #MODE_LOG} if configured, {@link #MODE_ENFORCE} otherwise (also if the setting
     * is missing or cannot be read)
     */
    public static String readMode() {
        try {
            InitialContext ic = new InitialContext();
            ServerSettingsBeanFacadeLocal settings = (ServerSettingsBeanFacadeLocal) ic.lookup("java:global/j-lawyer-server/j-lawyer-server-ejb/ServerSettingsBeanFacade!com.jdimension.jlawyer.persistence.ServerSettingsBeanFacadeLocal");
            ServerSettingsBean b = settings.find(ServerSettingsKeys.SERVERCONF_TEMPLATES_SCRIPTCHECK);
            if (b != null && b.getSettingValue() != null && MODE_LOG.equalsIgnoreCase(b.getSettingValue().trim())) {
                return MODE_LOG;
            }
        } catch (Throwable t) {
            log.warn("unable to read the template script check mode - using " + MODE_ENFORCE, t);
        }
        return MODE_ENFORCE;
    }

    static boolean isAllowedClass(String className) {
        if (className == null) {
            return false;
        }
        if (ALLOWED_CLASSES.contains(className)) {
            return true;
        }
        for (String p : ALLOWED_PACKAGES) {
            if (className.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Thrown when a script does not pass the allow list.
     */
    static final class ScriptCheckException extends Exception {

        ScriptCheckException(String message) {
            super(message);
        }
    }

    /**
     * Rejects everything that is not a plain expression: imports, packages, class and method
     * definitions and annotations (which could run code at compile time). Runs before any AST
     * transformation.
     */
    private static final class StructureCheck extends CompilationCustomizer {

        private String violation = null;

        StructureCheck() {
            super(CompilePhase.CONVERSION);
        }

        @Override
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
            ModuleNode module = source.getAST();
            if (module.getPackage() != null) {
                fail("Package-Angabe");
            }
            if (!module.getImports().isEmpty() || !module.getStarImports().isEmpty()
                    || !module.getStaticImports().isEmpty() || !module.getStaticStarImports().isEmpty()) {
                fail("Import");
            }
            if (module.getClasses().size() != 1 || !module.getClasses().get(0).isScript()) {
                fail("Klassendefinition");
            }
            if (!module.getMethods().isEmpty()) {
                fail("Methodendefinition");
            }
            final StructureCheck check = this;
            ClassCodeVisitorSupport annotations = new ClassCodeVisitorSupport() {
                @Override
                protected SourceUnit getSourceUnit() {
                    return source;
                }

                @Override
                public void visitAnnotations(AnnotatedNode node) {
                    for (AnnotationNode an : node.getAnnotations()) {
                        // the compiler itself adds @BaseScript (without a source position) to the script
                        // class for the configured script base class - everything else is rejected
                        boolean generatedBaseScript = node == classNode && an.getLineNumber() < 1
                                && BaseScript.class.getName().equals(an.getClassNode().getName());
                        if (!generatedBaseScript) {
                            check.fail("Annotation");
                        }
                    }
                    super.visitAnnotations(node);
                }
            };
            annotations.visitClass(classNode);
        }

        private void fail(String what) {
            this.violation = what + " nicht erlaubt";
            throw new SecurityException(this.violation);
        }
    }

    /**
     * Checks every call of the (statically compiled) script against the allow list. Runs after
     * static compilation, so the target of every call is known.
     */
    private static final class AllowListCheck extends CompilationCustomizer {

        private String violation = null;

        AllowListCheck() {
            super(CompilePhase.INSTRUCTION_SELECTION);
        }

        @Override
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
            if (!classNode.isScript()) {
                return;
            }
            final AllowListCheck check = this;
            ClassCodeVisitorSupport visitor = new ClassCodeVisitorSupport() {
                @Override
                protected SourceUnit getSourceUnit() {
                    return source;
                }

                @Override
                public void visitMethodCallExpression(MethodCallExpression call) {
                    MethodNode target = call.getMethodTarget();
                    if (target == null) {
                        target = call.getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
                    }
                    check.checkMethod(target, call.getMethodAsString());
                    super.visitMethodCallExpression(call);
                }

                @Override
                public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                    MethodNode target = call.getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
                    if (target != null) {
                        check.checkMethod(target, call.getMethod());
                    } else {
                        check.checkClassMethod(call.getOwnerType().getName(), call.getMethod());
                    }
                    super.visitStaticMethodCallExpression(call);
                }

                @Override
                public void visitConstructorCallExpression(ConstructorCallExpression call) {
                    if (!call.isSpecialCall() && !isAllowedClass(call.getType().getName())) {
                        check.fail("Objekterzeugung von " + call.getType().getName());
                    }
                    super.visitConstructorCallExpression(call);
                }

                @Override
                public void visitPropertyExpression(PropertyExpression expression) {
                    String name = expression.getPropertyAsString();
                    if (name == null || FORBIDDEN_PROPERTIES.contains(name)) {
                        check.fail("Zugriff auf " + name);
                    }
                    if (expression.getObjectExpression() instanceof ClassExpression
                            && !isAllowedClass(expression.getObjectExpression().getType().getName())) {
                        check.fail("Zugriff auf " + expression.getObjectExpression().getType().getName());
                    }
                    super.visitPropertyExpression(expression);
                }

                @Override
                public void visitAttributeExpression(AttributeExpression expression) {
                    check.fail("Feldzugriff");
                }

                @Override
                public void visitMethodPointerExpression(MethodPointerExpression expression) {
                    check.fail("Methodenreferenz");
                }

                @Override
                public void visitMethodReferenceExpression(MethodReferenceExpression expression) {
                    check.fail("Methodenreferenz");
                }
            };
            // only the expression itself (the run method) is user code - the rest is generated
            for (MethodNode m : classNode.getDeclaredMethods("run")) {
                if (m.getCode() != null) {
                    m.getCode().visit(visitor);
                }
            }
        }

        private void checkMethod(MethodNode target, String name) {
            if (target == null) {
                fail("nicht auflösbarer Aufruf " + name);
                return;
            }
            if (target instanceof ExtensionMethodNode) {
                target = ((ExtensionMethodNode) target).getExtensionMethodNode();
            }
            checkClassMethod(target.getDeclaringClass().getName(), target.getName());
        }

        private void checkClassMethod(String className, String methodName) {
            if (FORBIDDEN_METHODS.contains(methodName)) {
                fail("Aufruf von " + methodName);
            }
            if ("java.lang.Object".equals(className)) {
                if (!ALLOWED_OBJECT_METHODS.contains(methodName)) {
                    fail("Aufruf von Object." + methodName);
                }
                return;
            }
            if ("org.codehaus.groovy.runtime.DefaultGroovyMethods".equals(className)) {
                if (!ALLOWED_GROOVY_DEFAULT_METHODS.contains(methodName)) {
                    fail("Aufruf von " + methodName);
                }
                return;
            }
            if (!isAllowedClass(className)) {
                fail("Aufruf von " + className + "." + methodName);
            }
        }

        private void fail(String what) {
            if (this.violation == null) {
                this.violation = what + " nicht erlaubt";
            }
            throw new SecurityException(this.violation);
        }
    }
}

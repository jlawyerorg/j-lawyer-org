/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 *
 */
package com.jdimension.jlawyer.client.launcher;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * Optional LibreOffice UNO launcher/monitor.
 *
 * This class deliberately avoids compile-time UNO imports. Java UNO support is
 * not present in every LibreOffice installation, so all UNO classes are loaded
 * reflectively and callers can fall back to lock-file monitoring on any error.
 */
public class JavaUnoOfficeMonitor {

    private static final Logger log = Logger.getLogger(JavaUnoOfficeMonitor.class.getName());
    private static final String PROP_UNO_CLASSPATH = "jlawyer.uno.classpath";
    private static final String PROP_UNO_EXTRA_ARGS = "jlawyer.uno.extraArgs";
    private static final String PROP_UNO_PORT = "jlawyer.uno.port";
    private static final String ENV_UNO_CLASSPATH = "JLAWYER_UNO_CLASSPATH";
    private static final int MIN_WINDOW_WIDTH = 800;
    private static final int MIN_WINDOW_HEIGHT = 600;
    private static final int DEFAULT_WINDOW_WIDTH = 1200;
    private static final int DEFAULT_WINDOW_HEIGHT = 900;

    private JavaUnoOfficeMonitor() {
    }

    public static boolean launch(String officeBinary, String documentPath, ObservedOfficeDocument document) {
        URLClassLoader unoClassLoader = null;
        Process process = null;
        boolean componentLoaded = false;
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            List<URL> unoJars = findUnoJars(officeBinary);
            if (unoJars.isEmpty()) {
                log.debug("Java UNO jars not found - falling back to lock-file monitoring");
                return false;
            }

            unoClassLoader = new URLClassLoader(unoJars.toArray(new URL[unoJars.size()]), JavaUnoOfficeMonitor.class.getClassLoader());
            Thread.currentThread().setContextClassLoader(unoClassLoader);
            UnoClasses uno = new UnoClasses(unoClassLoader);

            int port = findFreePort();
            process = startOfficeProcess(officeBinary, port);
            drain(process.getInputStream());

            Object localContext = uno.bootstrap.getMethod("createInitialComponentContext", java.util.Hashtable.class).invoke(null, new Object[]{null});
            Object localServiceManager = invoke(localContext, "getServiceManager");
            Object resolverObject = invoke(localServiceManager, "createInstanceWithContext", "com.sun.star.bridge.UnoUrlResolver", localContext);
            Object resolver = queryInterface(uno, uno.xUnoUrlResolver, resolverObject);
            Object remoteContext = connect(uno, resolver, port, process);

            Object remoteServiceManager = invoke(remoteContext, "getServiceManager");
            Object desktopObject = invoke(remoteServiceManager, "createInstanceWithContext", "com.sun.star.frame.Desktop", remoteContext);
            Object loader = queryInterface(uno, uno.xComponentLoader, desktopObject);
            Object propertyValues = Array.newInstance(uno.propertyValue, 0);
            Object component = invoke(loader, "loadComponentFromURL", toFileUrl(documentPath), "_blank", Integer.valueOf(0), propertyValues);
            if (component == null) {
                destroyQuietly(process);
                log.debug("UNO returned no document component - falling back to lock-file monitoring");
                return false;
            }
            componentLoaded = true;

            normalizeDocumentWindow(uno, component);
            List<Object> listeners = registerListeners(uno, component, document);
            document.setUnoMonitorSession(new MonitorSession(unoClassLoader, process, resolver, remoteContext, component, listeners));
            document.setUnoMonitoringActive(true);
            log.debug("UNO monitoring active for " + documentPath);
            return true;

        } catch (Throwable t) {
            if (componentLoaded) {
                log.warn("UNO opened document but monitoring is unavailable. Continuing with lock-file monitoring: " + t.getMessage(), t);
                return true;
            } else {
                destroyQuietly(process);
                log.debug("UNO monitoring unavailable - falling back to lock-file monitoring: " + t.getMessage(), t);
                return false;
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    private static List<Object> registerListeners(UnoClasses uno, Object component, ObservedOfficeDocument document) throws Exception {
        List<Object> listeners = new ArrayList<>();

        Object eventBroadcaster = queryInterface(uno, uno.xEventBroadcaster, component);
        if (eventBroadcaster != null) {
            Object eventListener = Proxy.newProxyInstance(uno.loader, new Class[]{uno.xDocumentEventListener}, new DocumentEventHandler(document));
            invoke(eventBroadcaster, "addEventListener", eventListener);
            listeners.add(eventListener);
        }

        Object closeBroadcaster = queryInterface(uno, uno.xCloseBroadcaster, component);
        if (closeBroadcaster != null) {
            Object closeListener = Proxy.newProxyInstance(uno.loader, new Class[]{uno.xCloseListener}, new CloseEventHandler(document));
            invoke(closeBroadcaster, "addCloseListener", closeListener);
            listeners.add(closeListener);
        }

        Object modifyBroadcaster = queryInterface(uno, uno.xModifyBroadcaster, component);
        if (modifyBroadcaster != null) {
            Object modifyListener = Proxy.newProxyInstance(uno.loader, new Class[]{uno.xModifyListener}, new ModifyEventHandler(document));
            invoke(modifyBroadcaster, "addModifyListener", modifyListener);
            listeners.add(modifyListener);
        }
        return listeners;
    }

    /**
     * Temporary workaround for a LibreOffice defect: LibreOffice can persist a corrupted
     * "restored" window geometry (ooSetupFactoryWindowAttributes) after a maximized window
     * is closed, leaving a freshly opened document window tiny (a barely-visible sliver) or
     * positioned off-screen (see issue #3483). The root cause lives in LibreOffice; this
     * mitigation should be removable once that is fixed upstream. The bad geometry is
     * restored asynchronously, so this retries a few times and only forces usable bounds
     * when the window is actually unusable.
     */
    private static void normalizeDocumentWindow(UnoClasses uno, Object component) {
        for (int attempt = 0; attempt < 5; attempt++) {
            normalizeDocumentWindowOnce(uno, component);
            if (attempt < 4) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static void normalizeDocumentWindowOnce(UnoClasses uno, Object component) {
        try {
            Object model = queryInterface(uno, uno.xModel, component);
            if (model == null) {
                return;
            }
            Object controller = invoke(model, "getCurrentController");
            if (controller == null) {
                return;
            }
            Object frame = invoke(controller, "getFrame");
            if (frame == null) {
                return;
            }
            Object xFrame = queryInterface(uno, uno.xFrame, frame);
            if (xFrame == null) {
                return;
            }
            Object containerWindow = queryInterface(uno, uno.xWindow, invoke(xFrame, "getContainerWindow"));
            if (containerWindow == null) {
                return;
            }

            invoke(containerWindow, "setVisible", Boolean.TRUE);
            Rectangle currentBounds = getUnoWindowBounds(containerWindow);
            if (!isWindowUsable(currentBounds)) {
                Rectangle safeBounds = getSafeWindowBounds();
                invoke(containerWindow, "setPosSize",
                        Integer.valueOf(safeBounds.x),
                        Integer.valueOf(safeBounds.y),
                        Integer.valueOf(safeBounds.width),
                        Integer.valueOf(safeBounds.height),
                        getPosSizeAllFlag(uno));
                log.debug("Normalized LibreOffice document window from " + currentBounds + " to " + safeBounds);
            }
            invoke(containerWindow, "setFocus");
        } catch (Throwable t) {
            log.debug("Could not normalize LibreOffice document window: " + t.getMessage(), t);
        }
    }

    private static Rectangle getUnoWindowBounds(Object window) throws Exception {
        Object bounds = invoke(window, "getPosSize");
        return new Rectangle(
                getIntField(bounds, "X"),
                getIntField(bounds, "Y"),
                getIntField(bounds, "Width"),
                getIntField(bounds, "Height"));
    }

    private static int getIntField(Object object, String name) throws Exception {
        Field field = object.getClass().getField(name);
        return ((Number) field.get(object)).intValue();
    }

    private static Object getPosSizeAllFlag(UnoClasses uno) throws Exception {
        Field field = uno.posSize.getField("POSSIZE");
        return Short.valueOf(((Number) field.get(null)).shortValue());
    }

    private static boolean isWindowUsable(Rectangle bounds) {
        if (bounds == null || bounds.width < MIN_WINDOW_WIDTH || bounds.height < MIN_WINDOW_HEIGHT) {
            return false;
        }

        Rectangle screenBounds = getVirtualScreenBounds();
        if (screenBounds == null) {
            return true;
        }
        Rectangle visibleBounds = screenBounds.intersection(bounds);
        return visibleBounds.width >= MIN_WINDOW_WIDTH / 2 && visibleBounds.height >= MIN_WINDOW_HEIGHT / 2;
    }

    private static Rectangle getSafeWindowBounds() {
        Rectangle screenBounds = getVirtualScreenBounds();
        if (screenBounds == null) {
            return new Rectangle(100, 100, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
        }

        int width = Math.min(DEFAULT_WINDOW_WIDTH, Math.max(MIN_WINDOW_WIDTH, screenBounds.width - 100));
        int height = Math.min(DEFAULT_WINDOW_HEIGHT, Math.max(MIN_WINDOW_HEIGHT, screenBounds.height - 100));
        int x = screenBounds.x + Math.max(50, (screenBounds.width - width) / 2);
        int y = screenBounds.y + Math.max(50, (screenBounds.height - height) / 2);
        return new Rectangle(x, y, width, height);
    }

    private static Rectangle getVirtualScreenBounds() {
        try {
            GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle bounds = null;
            for (GraphicsDevice device : environment.getScreenDevices()) {
                Rectangle deviceBounds = device.getDefaultConfiguration().getBounds();
                bounds = bounds == null ? new Rectangle(deviceBounds) : bounds.union(deviceBounds);
            }
            return bounds;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object connect(UnoClasses uno, Object resolver, int port, Process process) throws Exception {
        Method resolve = uno.xUnoUrlResolver.getMethod("resolve", String.class);
        Throwable lastError = null;
        for (int i = 0; i < 50; i++) {
            try {
                Object remoteContext = resolve.invoke(resolver, "uno:socket,host=localhost,port=" + port + ";urp;StarOffice.ComponentContext");
                return queryInterface(uno, uno.xComponentContext, remoteContext);
            } catch (Throwable t) {
                lastError = t;
                if (i >= 5 && process != null && !process.isAlive()) {
                    throw new IllegalStateException("LibreOffice process exited before UNO accepted connections", lastError);
                }
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("Could not connect to LibreOffice UNO", lastError);
    }

    private static Process startOfficeProcess(String officeBinary, int port) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(officeBinary);
        command.add("--norestore");
        command.add("--nofirststartwizard");
        command.add("--accept=socket,host=localhost,port=" + port + ";urp;");
        for (String extraArg : getExtraArgs()) {
            command.add(extraArg);
        }
        return new ProcessBuilder(command).redirectErrorStream(true).start();
    }

    private static List<String> getExtraArgs() {
        List<String> args = new ArrayList<>();
        String configured = System.getProperty(PROP_UNO_EXTRA_ARGS, "");
        if (configured.trim().isEmpty()) {
            return args;
        }
        for (String part : configured.split(",")) {
            String arg = part.trim();
            if (!arg.isEmpty()) {
                args.add(arg);
            }
        }
        return args;
    }

    private static List<URL> findUnoJars(String officeBinary) throws Exception {
        Set<File> files = new LinkedHashSet<>();
        addConfiguredClasspath(files, System.getProperty(PROP_UNO_CLASSPATH));
        addConfiguredClasspath(files, System.getenv(ENV_UNO_CLASSPATH));

        File binary = new File(officeBinary);
        if (binary.isAbsolute()) {
            File parent = binary.getParentFile();
            if (parent != null) {
                addJarDirectory(files, parent);
                addJarDirectory(files, new File(parent, "classes"));
                File appRoot = parent.getParentFile();
                if (appRoot != null) {
                    addJarDirectory(files, new File(appRoot, "Resources/java"));
                }
            }
        }

        addJarDirectory(files, new File("/usr/lib/libreoffice/program/classes"));
        addJarDirectory(files, new File("/usr/lib/libreoffice/program"));
        addJarDirectory(files, new File("/usr/share/java"));
        addJarDirectory(files, new File("/Applications/LibreOffice.app/Contents/Resources/java"));
        addJarDirectory(files, new File("C:\\Program Files\\LibreOffice\\program\\classes"));
        addJarDirectory(files, new File("C:\\Program Files (x86)\\LibreOffice\\program\\classes"));

        List<URL> urls = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                urls.add(file.toURI().toURL());
            }
        }
        return urls;
    }

    private static void addConfiguredClasspath(Set<File> files, String classpath) {
        if (classpath == null || classpath.trim().isEmpty()) {
            return;
        }
        for (String part : classpath.split(File.pathSeparator)) {
            File file = new File(part.trim());
            if (file.isDirectory()) {
                addJarDirectory(files, file);
            } else if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
                files.add(file);
            }
        }
    }

    private static void addJarDirectory(Set<File> files, File directory) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        File[] jarFiles = directory.listFiles((File file) -> file.isFile() && isUnoJar(file));
        if (jarFiles == null) {
            return;
        }
        for (File jarFile : jarFiles) {
            files.add(jarFile);
        }
    }

    private static boolean isUnoJar(File file) {
        String name = file.getName().toLowerCase();
        return name.equals("juh.jar")
                || name.equals("jurt.jar")
                || name.equals("ridl.jar")
                || name.equals("unoil.jar")
                || name.equals("unoloader.jar")
                || name.equals("libreoffice.jar")
                || name.equals("officebean.jar")
                || name.startsWith("unoil-")
                || name.startsWith("libreoffice-");
    }

    private static Object queryInterface(UnoClasses uno, Class<?> iface, Object object) throws Exception {
        return uno.unoRuntime.getMethod("queryInterface", Class.class, Object.class).invoke(null, iface, object);
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        Method method = findMethod(target.getClass(), methodName, args);
        return method.invoke(target, args);
    }

    private static Method findMethod(Class<?> type, String methodName, Object[] args) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterTypes().length != args.length) {
                continue;
            }
            return method;
        }
        throw new IllegalArgumentException("Method not found: " + methodName);
    }

    private static String toFileUrl(String path) {
        return new File(path).toURI().toString();
    }

    private static int findFreePort() throws Exception {
        String configuredPort = System.getProperty(PROP_UNO_PORT, "").trim();
        if (!configuredPort.isEmpty()) {
            return Integer.parseInt(configuredPort);
        }
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void drain(final InputStream stream) {
        Thread drainThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            try {
                while (stream.read(buffer) >= 0) {
                    // drain process output
                }
            } catch (Throwable t) {
                // ignore process output drain failures
            }
        }, "j-lawyer-uno-output-drain");
        drainThread.setDaemon(true);
        drainThread.start();
    }

    private static void destroyQuietly(Process process) {
        if (process != null) {
            try {
                process.destroy();
            } catch (Throwable t) {
                // ignore cleanup failures
            }
        }
    }

    public static class MonitorSession {

        private final URLClassLoader classLoader;
        private final Process process;
        private final Object resolver;
        private final Object remoteContext;
        private final Object component;
        private final List<Object> listeners;

        MonitorSession(URLClassLoader classLoader, Process process, Object resolver, Object remoteContext, Object component, List<Object> listeners) {
            this.classLoader = classLoader;
            this.process = process;
            this.resolver = resolver;
            this.remoteContext = remoteContext;
            this.component = component;
            this.listeners = listeners;
        }

        public void release() {
            try {
                classLoader.close();
            } catch (Throwable t) {
                log.debug("Could not release UNO classloader: " + t.getMessage(), t);
            }
        }

        public void abort() {
            destroyQuietly(process);
            release();
        }
    }

    private static String getDocumentEventName(Object event) {
        try {
            Field eventName = event.getClass().getField("EventName");
            Object value = eventName.get(event);
            return value == null ? "" : value.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    private static boolean isCloseEventName(String eventName) {
        return "OnUnload".equals(eventName) || "OnClose".equals(eventName);
    }

    private static Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        if ("hashCode".equals(method.getName()) && method.getParameterTypes().length == 0) {
            return Integer.valueOf(System.identityHashCode(proxy));
        }
        if ("equals".equals(method.getName()) && args != null && args.length == 1) {
            return Boolean.valueOf(proxy == args[0]);
        }
        if ("toString".equals(method.getName()) && method.getParameterTypes().length == 0) {
            return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
        }
        return null;
    }

    private static class UnoClasses {

        private final ClassLoader loader;
        private final Class<?> bootstrap;
        private final Class<?> unoRuntime;
        private final Class<?> propertyValue;
        private final Class<?> xUnoUrlResolver;
        private final Class<?> xComponentContext;
        private final Class<?> xComponentLoader;
        private final Class<?> xEventBroadcaster;
        private final Class<?> xDocumentEventListener;
        private final Class<?> xCloseBroadcaster;
        private final Class<?> xCloseListener;
        private final Class<?> xModifyBroadcaster;
        private final Class<?> xModifyListener;
        private final Class<?> xModel;
        private final Class<?> xFrame;
        private final Class<?> xWindow;
        private final Class<?> posSize;

        UnoClasses(ClassLoader loader) throws Exception {
            this.loader = loader;
            this.bootstrap = Class.forName("com.sun.star.comp.helper.Bootstrap", true, loader);
            this.unoRuntime = Class.forName("com.sun.star.uno.UnoRuntime", true, loader);
            this.propertyValue = Class.forName("com.sun.star.beans.PropertyValue", true, loader);
            this.xUnoUrlResolver = Class.forName("com.sun.star.bridge.XUnoUrlResolver", true, loader);
            this.xComponentContext = Class.forName("com.sun.star.uno.XComponentContext", true, loader);
            this.xComponentLoader = Class.forName("com.sun.star.frame.XComponentLoader", true, loader);
            this.xEventBroadcaster = Class.forName("com.sun.star.document.XEventBroadcaster", true, loader);
            this.xDocumentEventListener = Class.forName("com.sun.star.document.XEventListener", true, loader);
            this.xCloseBroadcaster = Class.forName("com.sun.star.util.XCloseBroadcaster", true, loader);
            this.xCloseListener = Class.forName("com.sun.star.util.XCloseListener", true, loader);
            this.xModifyBroadcaster = Class.forName("com.sun.star.util.XModifyBroadcaster", true, loader);
            this.xModifyListener = Class.forName("com.sun.star.util.XModifyListener", true, loader);
            this.xModel = Class.forName("com.sun.star.frame.XModel", true, loader);
            this.xFrame = Class.forName("com.sun.star.frame.XFrame", true, loader);
            this.xWindow = Class.forName("com.sun.star.awt.XWindow", true, loader);
            this.posSize = Class.forName("com.sun.star.awt.PosSize", true, loader);
        }
    }

    private static class DocumentEventHandler implements InvocationHandler {

        private final ObservedOfficeDocument document;

        DocumentEventHandler(ObservedOfficeDocument document) {
            this.document = document;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }
            if ("notifyEvent".equals(method.getName()) && args != null && args.length > 0) {
                String eventName = getDocumentEventName(args[0]);
                log.debug("UNO document event " + eventName + " for " + document.getName());
                if (isCloseEventName(eventName)) {
                    document.markUnoCloseConfirmed();
                }
            }
            return null;
        }
    }

    private static class CloseEventHandler implements InvocationHandler {

        private final ObservedOfficeDocument document;

        CloseEventHandler(ObservedOfficeDocument document) {
            this.document = document;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }
            if ("notifyClosing".equals(method.getName())) {
                log.debug("UNO close confirmed for " + document.getName());
                document.markUnoCloseConfirmed();
            }
            return null;
        }
    }

    private static class ModifyEventHandler implements InvocationHandler {

        private final ObservedOfficeDocument document;

        ModifyEventHandler(ObservedOfficeDocument document) {
            this.document = document;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }
            if ("modified".equals(method.getName())) {
                log.debug("UNO modify event for " + document.getName());
            }
            return null;
        }
    }
}

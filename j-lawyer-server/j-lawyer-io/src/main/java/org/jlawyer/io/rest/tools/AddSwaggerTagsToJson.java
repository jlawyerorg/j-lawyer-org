package org.jlawyer.io.rest.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Post-processes the generated swagger.json to inject Swagger tags based on
 * URL path segments. This groups endpoints by category in the Swagger UI.
 *
 * @author jens
 */
public class AddSwaggerTagsToJson {

    private static final Map<String, String> TAG_MAP = new HashMap<>();

    static {
        TAG_MAP.put("cases", "Cases");
        TAG_MAP.put("contacts", "Contacts");
        TAG_MAP.put("calendars", "Calendar");
        TAG_MAP.put("forms", "Forms");
        TAG_MAP.put("security", "Security");
        TAG_MAP.put("configuration", "Configuration");
        TAG_MAP.put("email", "Email");
        TAG_MAP.put("invoices", "Invoices");
        TAG_MAP.put("messages", "Messaging");
        TAG_MAP.put("reports", "Reports");
        TAG_MAP.put("webhooks", "WebHooks");
        TAG_MAP.put("databuckets", "DataBuckets");
        TAG_MAP.put("templates", "Templates");
        TAG_MAP.put("administration", "Administration");
        TAG_MAP.put("bea", "beA");
        TAG_MAP.put("timesheets", "Timesheets");
    }

    // matches a path key line like:   "/v7/email/list": {
    private static final Pattern PATH_PATTERN = Pattern.compile("^(\\s*)\"(/[^\"]+)\"\\s*:\\s*\\{\\s*$");
    // matches an HTTP method line like:   "get": {
    private static final Pattern METHOD_PATTERN = Pattern.compile("^(\\s*)\"(get|put|post|delete|patch|head|options)\"\\s*:\\s*\\{\\s*$");

    public static void main(String[] args) {

        System.out.println("Adding Swagger Tags to " + args[0]);

        try {
            File f = new File(args[0]);
            String json = readFileAsString(f);
            String[] lines = json.split("\\r?\\n");

            StringBuilder sb = new StringBuilder();
            TreeSet<String> usedTags = new TreeSet<>();
            String currentTag = null;
            boolean inPaths = false;
            int pathsDepth = -1;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                // detect "paths": {
                if (!inPaths && line.matches("\\s*\"paths\"\\s*:\\s*\\{\\s*")) {
                    inPaths = true;
                    pathsDepth = getIndent(line);
                    sb.append(line).append("\n");
                    continue;
                }

                // detect end of paths block
                if (inPaths && line.matches("\\s*\\}.*") && getIndent(line) == pathsDepth) {
                    inPaths = false;
                    currentTag = null;
                    sb.append(line).append("\n");
                    continue;
                }

                if (inPaths) {
                    // check for path key
                    Matcher pathMatcher = PATH_PATTERN.matcher(line);
                    if (pathMatcher.matches()) {
                        String path = pathMatcher.group(2);
                        currentTag = resolveTag(path);
                        if (currentTag != null) {
                            usedTags.add(currentTag);
                        }
                        sb.append(line).append("\n");
                        continue;
                    }

                    // check for HTTP method - inject tags after the opening brace
                    Matcher methodMatcher = METHOD_PATTERN.matcher(line);
                    if (methodMatcher.matches() && currentTag != null) {
                        String indent = methodMatcher.group(1);
                        sb.append(line).append("\n");
                        // peek ahead: if next non-empty line is "description", insert tags before it
                        // simply insert tags as first property of the operation
                        sb.append(indent).append("    \"tags\": [\"").append(currentTag).append("\"],\n");
                        continue;
                    }
                }

                sb.append(line).append("\n");
            }

            // insert top-level "tags" array before "paths"
            String result = sb.toString();
            StringBuilder tagsArray = new StringBuilder();
            tagsArray.append("    \"tags\": [\n");
            int tagCount = 0;
            for (String tag : usedTags) {
                tagCount++;
                tagsArray.append("        {\"name\": \"").append(tag).append("\"}");
                if (tagCount < usedTags.size()) {
                    tagsArray.append(",");
                }
                tagsArray.append("\n");
            }
            tagsArray.append("    ],\n    \"paths\": {");
            result = result.replaceFirst("    \"paths\":\\s*\\{", tagsArray.toString());

            FileWriter fw = new FileWriter(f);
            fw.write(result);
            fw.close();

            System.out.println("Added " + usedTags.size() + " tags: " + usedTags);

        } catch (Throwable t) {
            System.err.println("Unable to add Swagger Tags to " + args[0] + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static int getIndent(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Extracts the tag name from a path like /v7/email/... by taking the
     * second segment and mapping it via TAG_MAP. Falls back to capitalizing
     * the segment if no explicit mapping exists.
     */
    private static String resolveTag(String path) {
        String[] segments = path.split("/");
        // segments[0] = "", segments[1] = "v1", segments[2] = "segment"
        if (segments.length < 3) {
            return null;
        }
        String segment = segments[2].toLowerCase();
        if (TAG_MAP.containsKey(segment)) {
            return TAG_MAP.get(segment);
        }
        // fallback: capitalize first letter
        return segment.substring(0, 1).toUpperCase() + segment.substring(1);
    }

    private static String readFileAsString(File file) throws Exception {
        try (FileReader fileReader = new FileReader(file); BufferedReader br = new BufferedReader(fileReader)) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            return sb.toString();
        }
    }
}

package com.jdimension.jlawyer.client.editors.webview;

import com.jdimension.jlawyer.client.mail.EditorImplementation;
import com.jdimension.jlawyer.server.utils.ContentTypes;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * HTML editor implementation using SunEditor embedded in JavaFX WebView.
 * This is a proof-of-concept replacement for the SHEF-based HtmlEditorPanel.
 *
 * @author j-lawyer.org
 */
public class WebViewHtmlEditorPanel extends JPanel implements EditorImplementation {

    private static final Logger log = Logger.getLogger(WebViewHtmlEditorPanel.class.getName());

    private final JFXPanel jfxPanel;
    private WebView webView;
    private WebEngine webEngine;
    private JSObject editorAPI;
    private volatile boolean editorReady = false;
    private volatile boolean disposed = false;
    private Runnable onEditorReadyCallback = null;

    // Resource paths
    private static final String TEMPLATE_PATH = "/resources/suneditor/suneditor-template.html";
    private static final String SUNEDITOR_JS_PATH = "/resources/suneditor/js/suneditor.min.js";
    private static final String SUNEDITOR_CSS_PATH = "/resources/suneditor/css/suneditor.min.css";
    private static final String SUNEDITOR_LANG_DE_PATH = "/resources/suneditor/lang/de.js";

    /**
     * Creates a new WebViewHtmlEditorPanel.
     * Initializes the JavaFX WebView and loads the SunEditor.
     */
    public WebViewHtmlEditorPanel() {
        super(new BorderLayout());

        // Create JFXPanel (bridge between Swing and JavaFX)
        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);

        // Set minimum and preferred size to ensure panel is visible even with absolute positioning
        // This is critical for HtmlPanel which uses setBounds() with potentially 0x0 parent
        setMinimumSize(new Dimension(100, 100));
        setPreferredSize(new Dimension(800, 600));

        // Prevent JavaFX from exiting when last window is closed
        Platform.setImplicitExit(false);

        // Add ComponentListener to handle size changes
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                // Force repaint when size changes
                int newWidth = getWidth();
                int newHeight = getHeight();
                if (newWidth > 0 && newHeight > 0) {
                    SwingUtilities.invokeLater(() -> {
                        revalidate();
                        repaint();
                    });
                }
            }
        });

        // Initialize JavaFX components on JavaFX Application Thread
        Platform.runLater(this::initializeFX);
    }

    /**
     * Override setBounds to handle 0x0 sizing from absolute positioning in HtmlPanel.
     * When HtmlPanel calls setBounds(0,0,0,0), we try to find a reasonable size.
     */
    @Override
    public void setBounds(int x, int y, int width, int height) {
        // If being set to 0x0, try to get size from grandparent or use default
        if (width == 0 || height == 0) {
            Container parent = getParent();
            if (parent != null) {
                Container grandparent = parent.getParent();
                if (grandparent != null && grandparent.getWidth() > 0 && grandparent.getHeight() > 0) {
                    width = grandparent.getWidth();
                    height = grandparent.getHeight();
                } else if (getPreferredSize() != null) {
                    width = Math.max(width, getPreferredSize().width);
                    height = Math.max(height, getPreferredSize().height);
                }
            }
        }

        super.setBounds(x, y, width, height);
    }

    /**
     * Initializes JavaFX components (must run on JavaFX Application Thread).
     */
    private void initializeFX() {
        try {
            // Create WebView
            webView = new WebView();
            webView.setZoom(1.25);  // Scale editor to 125%
            webEngine = webView.getEngine();

            // Enable JavaScript
            webEngine.setJavaScriptEnabled(true);

            // Set up state change listener
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    onPageLoaded();
                } else if (newState == Worker.State.FAILED) {
                    log.error("Failed to load SunEditor page");
                    editorReady = false;
                }
            });
            
            webView.setOnScroll(e -> {
                if (e.isControlDown()) {
                    double delta = e.getDeltaY();
                    double zoom = webView.getZoom();
                    if (delta > 0) {
                        webView.setZoom(zoom + 0.1);
                    } else {
                        webView.setZoom(Math.max(0.1, zoom - 0.1));
                    }
                    e.consume();
                }
            });

            // Create scene and set it to jfxPanel
            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);

            // Load the editor HTML
            loadEditor();

        } catch (Exception e) {
            log.error("Error initializing JavaFX WebView", e);
        }
    }

    /**
     * Loads the SunEditor HTML template and resources.
     */
    private void loadEditor() {
        try {
            // Load HTML template
            String htmlContent = loadResourceAsString(TEMPLATE_PATH);

            // Load JavaScript and CSS as data URLs for offline operation
            String suneditorJs = loadResourceAsString(SUNEDITOR_JS_PATH);
            String suneditorCss = loadResourceAsString(SUNEDITOR_CSS_PATH);
            String langDe = loadResourceAsString(SUNEDITOR_LANG_DE_PATH);

            // Replace placeholders with inline content (for offline operation)
            htmlContent = htmlContent.replace("SUNEDITOR_CSS_PATH",
                "data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(suneditorCss.getBytes(StandardCharsets.UTF_8)));
            htmlContent = htmlContent.replace("SUNEDITOR_JS_PATH",
                "data:text/javascript;base64," + java.util.Base64.getEncoder().encodeToString(suneditorJs.getBytes(StandardCharsets.UTF_8)));
            htmlContent = htmlContent.replace("SUNEDITOR_LANG_PATH",
                "data:text/javascript;base64," + java.util.Base64.getEncoder().encodeToString(langDe.getBytes(StandardCharsets.UTF_8)));

            // Load the HTML content
            webEngine.loadContent(htmlContent);

        } catch (Exception e) {
            log.error("Error loading SunEditor resources", e);
        }
    }

    /**
     * Loads a resource file as a string.
     */
    private String loadResourceAsString(String path) throws Exception {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new Exception("Resource not found: " + path);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Called when the page has finished loading.
     */
    private void onPageLoaded() {
        try {
            // Get reference to JavaScript editorAPI object
            JSObject window = (JSObject) webEngine.executeScript("window");
            editorAPI = (JSObject) webEngine.executeScript("window.editorAPI");

            if (editorAPI == null) {
                log.warn("editorAPI is null - JavaScript may not have initialized correctly");
            }

            // Set up Java callback connector
            window.setMember("javaConnector", new JavaScriptConnector());

            log.info("SunEditor loaded successfully");

        } catch (Exception e) {
            log.error("Error accessing JavaScript API", e);
        }
    }

    /**
     * Waits for the editor to be ready (blocking with timeout).
     */
    private void waitForEditor() {
        int attempts = 0;
        while (!editorReady && attempts < 100) {  // 10 seconds timeout
            try {
                Thread.sleep(100);
                attempts++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!editorReady) {
            log.warn("Editor not ready after timeout");
        }
    }

    /**
     * Executes JavaScript and returns the result (thread-safe).
     */
    private Object executeScript(String script) {
        if (!editorReady) {
            waitForEditor();
        }

        CompletableFuture<Object> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            try {
                Object result = webEngine.executeScript(script);
                future.complete(result);
            } catch (Exception e) {
                log.error("Error executing script: " + script, e);
                future.completeExceptionally(e);
            }
        });

        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Timeout or error waiting for script execution", e);
            return null;
        }
    }

    /**
     * Calls a JavaScript API method (thread-safe).
     */
    private Object callEditorAPI(String methodName, Object... args) {
        if (!editorReady) {
            waitForEditor();
        }

        CompletableFuture<Object> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            try {
                Object result = editorAPI.call(methodName, args);
                future.complete(result);
            } catch (Exception e) {
                log.error("Error calling editorAPI." + methodName, e);
                future.completeExceptionally(e);
            }
        });

        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Timeout or error calling editorAPI." + methodName, e);
            return null;
        }
    }

    // ========== EditorImplementation Interface Methods ==========

    @Override
    public String getContentType() {
        return ContentTypes.TEXT_HTML;
    }

    @Override
    public String getText() {
        Object result = callEditorAPI("getText");
        return result != null ? result.toString() : "";
    }

    @Override
    public void setText(String text) {
        if (text == null) {
            text = "";
        }

        final String finalText = text;
        callEditorAPI("setText", finalText);

        // Force UI update after setting content to ensure it becomes visible
        SwingUtilities.invokeLater(() -> {
            // Update size if parent has been laid out
            Container parent = this.getParent();
            if (parent != null && parent.getWidth() > 0 && parent.getHeight() > 0) {
                int width = parent.getWidth();
                int height = parent.getHeight();
                if (this.getWidth() != width || this.getHeight() != height) {
                    this.setBounds(0, 0, width, height);
                }
            }

            this.revalidate();
            this.repaint();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
        });
    }

    @Override
    public void insert(String text, int pos) {
        if (text == null) {
            text = "";
        }
        final String finalText = text;
        callEditorAPI("insert", finalText, pos);
    }

    @Override
    public void setCaretPosition(int pos) {
        callEditorAPI("setCaretPosition", pos);
    }

    @Override
    public int getCaretPosition() {
        Object result = callEditorAPI("getCaretPosition");
        if (result instanceof Number) {
            return ((Number) result).intValue();
        }
        return 0;
    }

    @Override
    public String getSelectedText() {
        Object result = callEditorAPI("getSelectedText");
        return result != null ? result.toString() : "";
    }

    // ========== JavaScript Connector Class ==========

    /**
     * Java object exposed to JavaScript for callbacks.
     */
    public class JavaScriptConnector {
        /**
         * Called by JavaScript when the SunEditor is ready.
         */
        public void onEditorReady() {
            editorReady = true;
            log.info("SunEditor is ready");

            // Force UI update on EDT (Event Dispatch Thread)
            SwingUtilities.invokeLater(() -> {
                // Update size based on parent (important for absolute positioning in HtmlPanel)
                Container parent = WebViewHtmlEditorPanel.this.getParent();
                if (parent != null) {
                    int width = parent.getWidth();
                    int height = parent.getHeight();
                    WebViewHtmlEditorPanel.this.setBounds(0, 0, width, height);
                }

                // Revalidate and repaint this panel AND parent containers
                WebViewHtmlEditorPanel.this.revalidate();
                WebViewHtmlEditorPanel.this.repaint();

                // Also update parent container (critical for visibility)
                if (parent != null) {
                    parent.revalidate();
                    parent.repaint();
                }

                // Notify callback if set
                if (onEditorReadyCallback != null) {
                    onEditorReadyCallback.run();
                }
            });
        }

        /**
         * Called by JavaScript to log messages to Java console.
         * This is useful for debugging paste and other JavaScript operations.
         */
        public void log(String message) {
            log.info("[JS] " + message);
        }

        /**
         * Get HTML from system clipboard.
         * JavaFX WebView cannot access HTML clipboard data directly,
         * so we use Java's AWT Clipboard API instead.
         *
         * @return HTML string from clipboard, or null if not available
         */
        public String getClipboardHTML() {
            try {
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                java.awt.datatransfer.Clipboard clipboard = toolkit.getSystemClipboard();

                // Try to get HTML flavor first
                java.awt.datatransfer.DataFlavor htmlFlavor = new java.awt.datatransfer.DataFlavor("text/html; class=java.lang.String");

                if (clipboard.isDataFlavorAvailable(htmlFlavor)) {
                    String html = (String) clipboard.getData(htmlFlavor);
                    log.info("Got HTML from system clipboard: " + html.length() + " chars");

                    // Parse Windows HTML Format if present
                    html = parseWindowsHtmlFormat(html);

                    return html;
                }

                // Also check for alternate HTML flavors
                java.awt.datatransfer.DataFlavor[] flavors = clipboard.getAvailableDataFlavors();
                log.info("Available clipboard flavors: " + flavors.length);

                for (java.awt.datatransfer.DataFlavor flavor : flavors) {
                    log.info("  - " + flavor.getMimeType());

                    // Look for any text/html flavor
                    if (flavor.getMimeType().contains("text/html") ||
                        flavor.getMimeType().contains("application/x-java-jvm-local-objectref")) {

                        try {
                            Object data = clipboard.getData(flavor);
                            if (data instanceof String) {
                                log.info("Found HTML in flavor: " + flavor.getMimeType());
                                String html = (String) data;
                                // Parse Windows HTML Format if present
                                html = parseWindowsHtmlFormat(html);
                                return html;
                            } else if (data instanceof java.io.InputStream) {
                                // Read from input stream
                                java.io.InputStream is = (java.io.InputStream) data;
                                StringBuilder sb = new StringBuilder();
                                int ch;
                                while ((ch = is.read()) != -1) {
                                    sb.append((char) ch);
                                }
                                log.info("Read HTML from stream: " + sb.length() + " chars");
                                String html = sb.toString();
                                // Parse Windows HTML Format if present
                                html = parseWindowsHtmlFormat(html);
                                return html;
                            }
                        } catch (Exception e) {
                            log.warn("Could not read flavor " + flavor.getMimeType() + ": " + e.getMessage());
                        }
                    }
                }

                log.info("No HTML flavor found in clipboard");
                return null;

            } catch (Exception e) {
                log.error("Error reading clipboard HTML", e);
                return null;
            }
        }

        /**
         * Parses Windows HTML Format clipboard data and extracts the actual HTML fragment.
         *
         * Windows uses a special "HTML Format" specification when copying HTML to the clipboard.
         * This format includes metadata headers like:
         * <pre>
         * Version:1.0
         * StartHTML:0000000254
         * EndHTML:0000041346
         * StartFragment:0000039489
         * EndFragment:0000041306
         * SourceURL:file:///...
         * </pre>
         *
         * The StartFragment and EndFragment values are byte offsets indicating where the
         * actual content fragment is located. Only this fragment should be pasted.
         *
         * @param html The raw HTML string from clipboard (may include Windows HTML Format headers)
         * @return The cleaned HTML (without metadata headers), or the original string if not Windows format
         */
        private String parseWindowsHtmlFormat(String html) {
            if (html == null || html.isEmpty()) {
                return html;
            }

            // Check for Windows HTML Format header
            if (!html.startsWith("Version:")) {
                // Not Windows HTML Format - return unchanged
                return html;
            }

            try {
                int startFragmentPos = -1;
                int endFragmentPos = -1;

                // Parse header lines to extract byte offsets
                String[] lines = html.split("\\r?\\n");

                // Look for StartFragment and EndFragment headers (usually in first 10 lines)
                for (int i = 0; i < Math.min(lines.length, 10); i++) {
                    String line = lines[i];

                    if (line.startsWith("StartFragment:")) {
                        String value = line.substring("StartFragment:".length()).trim();
                        startFragmentPos = Integer.parseInt(value);
                        log.info("Found StartFragment: " + startFragmentPos);
                    } else if (line.startsWith("EndFragment:")) {
                        String value = line.substring("EndFragment:".length()).trim();
                        endFragmentPos = Integer.parseInt(value);
                        log.info("Found EndFragment: " + endFragmentPos);
                    }
                }

                // Extract fragment using byte offsets if both are present
                if (startFragmentPos > 0 && endFragmentPos > startFragmentPos) {
                    // IMPORTANT: Offsets are byte positions, not character positions
                    // We need to work with the UTF-8 byte representation
                    byte[] bytes = html.getBytes(StandardCharsets.UTF_8);

                    if (endFragmentPos <= bytes.length) {
                        // Extract the fragment bytes
                        byte[] fragment = Arrays.copyOfRange(bytes, startFragmentPos, endFragmentPos);
                        String result = new String(fragment, StandardCharsets.UTF_8);

                        log.info("Successfully extracted Windows HTML Format fragment: " +
                                 result.length() + " chars (was " + html.length() + " chars)");

                        return result;
                    } else {
                        log.warn("EndFragment offset (" + endFragmentPos + ") exceeds byte array length (" +
                                bytes.length + ") - returning original HTML");
                    }
                } else {
                    log.warn("Could not find valid StartFragment/EndFragment offsets - returning original HTML");
                }

            } catch (Exception e) {
                log.error("Error parsing Windows HTML Format: " + e.getMessage(), e);
            }

            // If anything goes wrong, return the original HTML
            return html;
        }
    }

    // ========== Utility Methods ==========

    /**
     * Gets the underlying WebView (for testing/debugging).
     */
    public WebView getWebView() {
        return webView;
    }

    /**
     * Checks if the editor is ready.
     */
    public boolean isEditorReady() {
        return editorReady;
    }

    /**
     * Sets a callback to be invoked when the editor is fully initialized and ready.
     * The callback will be executed on the Event Dispatch Thread (EDT).
     * This is useful for updating the UI after the editor has loaded asynchronously.
     *
     * Example usage:
     * <pre>
     * editorPanel.setOnEditorReadyCallback(() -> {
     *     // Revalidate the parent container to ensure proper layout
     *     Container parent = editorPanel.getParent();
     *     if (parent != null) {
     *         parent.revalidate();
     *         parent.repaint();
     *     }
     * });
     * </pre>
     *
     * @param callback Runnable to execute when editor is ready (will run on EDT)
     */
    public void setOnEditorReadyCallback(Runnable callback) {
        this.onEditorReadyCallback = callback;
    }

    /**
     * Cleanup resources when the editor is being closed/disposed.
     * IMPORTANT: This method MUST be called manually when closing dialogs/windows containing this editor.
     * Automatic cleanup was attempted but conflicts with complex UI layouts (CardLayout, TabbedPane).
     * Safe to call multiple times - will only execute cleanup once.
     */
    public void dispose() {
        // Prevent multiple dispose calls
        if (disposed) {
            log.debug("dispose() called but already disposed - ignoring");
            return;
        }

        disposed = true;
        log.info("Disposing WebViewHtmlEditorPanel - cleaning up resources");

        editorReady = false;

        // Cleanup must happen on JavaFX Application Thread
        Platform.runLater(() -> {
            try {
                if (webEngine != null) {
                    // Stop loading any pending resources
                    webEngine.getLoadWorker().cancel();

                    // Clear the page content to free memory
                    webEngine.loadContent("");

                    // Execute JavaScript cleanup (destroy editor instance)
                    try {
                        webEngine.executeScript("if (window.editor) { window.editor.destroy(); window.editor = null; }");
                        webEngine.executeScript("if (window.editorAPI) { window.editorAPI = null; }");
                    } catch (Exception e) {
                        log.warn("Error during JavaScript cleanup: " + e.getMessage());
                    }

                    log.info("WebEngine cleaned up");
                }

                if (webView != null) {
                    // Remove the WebView from scene
                    webView.getEngine().getLoadWorker().cancel();
                    log.info("WebView cleaned up");
                }

                // Clear references
                webEngine = null;
                webView = null;
                editorAPI = null;

            } catch (Exception e) {
                log.error("Error during JavaFX cleanup", e);
            }
        });

        // Remove all components from the panel
        this.removeAll();

        log.info("WebViewHtmlEditorPanel disposed successfully");
    }
}

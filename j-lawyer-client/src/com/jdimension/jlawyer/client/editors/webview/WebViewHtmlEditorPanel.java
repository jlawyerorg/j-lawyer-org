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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.jdimension.jlawyer.client.utils.SystemUtils;

/**
 * HTML editor implementation using SunEditor embedded in JavaFX WebView.
 * This is a proof-of-concept replacement for the SHEF-based HtmlEditorPanel.
 *
 * <h2>Architecture Overview</h2>
 * This component bridges three different UI technologies:
 * <ul>
 *   <li><b>Swing</b> - The main application UI framework (runs on EDT - Event Dispatch Thread)</li>
 *   <li><b>JavaFX</b> - Used for WebView rendering (runs on JavaFX Application Thread)</li>
 *   <li><b>JavaScript/SunEditor</b> - The actual HTML editor running inside the WebView</li>
 * </ul>
 *
 * <h2>macOS Threading Problem</h2>
 * On macOS, there is a critical threading issue that can cause application freezes (deadlocks).
 * The problem occurs because:
 * <ol>
 *   <li>Swing's EDT, JavaFX Application Thread, and macOS AppKit thread share native resources</li>
 *   <li>When the EDT calls a blocking method that waits for JavaFX (e.g., {@code CompletableFuture.get()}),
 *       it can deadlock with the AppKit thread</li>
 *   <li>Similarly, {@code Thread.sleep()} on the EDT (used in {@link #waitForEditor()}) blocks the UI</li>
 *   <li>Clipboard access from JavaFX thread can also cause deadlocks with AppKit</li>
 * </ol>
 *
 * <h2>Solution: Asynchronous Content Caching</h2>
 * To avoid deadlocks on macOS, this class implements a content caching mechanism:
 * <ol>
 *   <li><b>JavaScript onChange Handler</b>: The SunEditor in {@code suneditor-template.html} registers
 *       an {@code onChange} callback that notifies Java whenever content changes (with 300ms debounce)</li>
 *   <li><b>Java Content Cache</b>: The {@link #cachedEditorContent} field stores the latest editor content,
 *       updated by {@link JavaScriptConnector#onContentChanged(String)}</li>
 *   <li><b>Non-blocking getText()</b>: On macOS, when called from EDT, {@link #getText()} returns the
 *       cached content immediately instead of blocking to query the editor synchronously</li>
 *   <li><b>Async Clipboard Access</b>: Clipboard operations run in a background thread
 *       ({@link #clipboardExecutor}) to avoid AppKit deadlocks</li>
 * </ol>
 *
 * <h2>Java-JavaScript Interaction</h2>
 * Communication between Java and JavaScript happens through:
 * <ul>
 *   <li><b>Java → JavaScript</b>: Via {@link #editorAPI} (JSObject) calling methods defined in
 *       {@code window.editorAPI} in the HTML template</li>
 *   <li><b>JavaScript → Java</b>: Via {@code window.javaConnector} which references
 *       {@link JavaScriptConnector} instance, allowing JavaScript to call Java methods like
 *       {@link JavaScriptConnector#onEditorReady()}, {@link JavaScriptConnector#onContentChanged(String)},
 *       and {@link JavaScriptConnector#getClipboardHTML()}</li>
 * </ul>
 *
 * <h2>Key Files</h2>
 * <ul>
 *   <li>{@code suneditor-template.html} - HTML template containing:
 *     <ul>
 *       <li>SunEditor initialization and configuration</li>
 *       <li>{@code editor.onChange} handler that calls {@code javaConnector.onContentChanged()}</li>
 *       <li>{@code editor.onload} handler that calls {@code javaConnector.onEditorReady()}</li>
 *       <li>Custom paste handler that preserves formatting and line breaks</li>
 *       <li>{@code window.editorAPI} object exposing editor methods to Java</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Platform-Specific Behavior</h2>
 * <ul>
 *   <li><b>macOS</b>: Uses cached content for {@link #getText()}, {@link #getCaretPosition()},
 *       {@link #getSelectedText()} when called from EDT to prevent deadlocks</li>
 *   <li><b>Windows/Linux</b>: Uses synchronous calls to the editor as these platforms don't have
 *       the same threading constraints</li>
 * </ul>
 *
 * @author j-lawyer.org
 * @see JavaScriptConnector
 * @see #cachedEditorContent
 * @see #getText()
 */
public class WebViewHtmlEditorPanel extends JPanel implements EditorImplementation {

    private static final Logger log = Logger.getLogger(WebViewHtmlEditorPanel.class.getName());

    /** JFXPanel that hosts the JavaFX WebView within Swing. */
    private final JFXPanel jfxPanel;

    /** The JavaFX WebView component that renders the HTML editor. */
    private WebView webView;

    /** WebEngine that executes JavaScript and manages the web content. */
    private WebEngine webEngine;

    /**
     * JavaScript object reference to {@code window.editorAPI} in the HTML template.
     * Used to call editor methods from Java (e.g., getText, setText, insert).
     */
    private JSObject editorAPI;

    /**
     * Flag indicating whether the SunEditor has finished initialization.
     * Set to true when {@link JavaScriptConnector#onEditorReady()} is called from JavaScript.
     * Volatile to ensure visibility across threads.
     */
    private volatile boolean editorReady = false;

    /** Flag indicating whether this panel has been disposed. */
    private volatile boolean disposed = false;

    /** Optional callback to execute when the editor becomes ready. */
    private Runnable onEditorReadyCallback = null;

    /**
     * Content to be set when the editor becomes ready.
     * Used for non-blocking initialization: if {@link #setText(String)} is called before
     * the editor is ready, the content is stored here and applied in {@link JavaScriptConnector#onEditorReady()}.
     */
    private String pendingContent = null;

    /**
     * Caret position to be set when the editor becomes ready.
     * Similar to {@link #pendingContent}, this enables non-blocking caret positioning.
     */
    private int pendingCaretPosition = -1;

    /**
     * Cache for editor content, updated asynchronously by JavaScript's onChange handler.
     * <p>
     * <b>macOS Threading Solution:</b> On macOS, calling blocking methods from EDT causes deadlocks.
     * Instead of synchronously querying the editor, we maintain this cache which is updated
     * by the JavaScript {@code editor.onChange} callback (with 300ms debounce) calling
     * {@link JavaScriptConnector#onContentChanged(String)}.
     * </p>
     * <p>
     * When {@link #getText()} is called from EDT on macOS, it returns this cached value
     * immediately instead of blocking. The cache is also updated by:
     * <ul>
     *   <li>{@link #setText(String)} - immediately when content is set from Java</li>
     *   <li>{@link JavaScriptConnector#onEditorReady()} - when pending content is applied</li>
     * </ul>
     * </p>
     *
     * @see JavaScriptConnector#onContentChanged(String)
     * @see #getText()
     */
    private volatile String cachedEditorContent = null;

    /**
     * Cache for clipboard HTML content to avoid EDT/JavaFX/AppKit deadlock.
     * Updated asynchronously by {@link #refreshClipboardCacheInBackground()}.
     */
    private volatile String cachedClipboardHTML = null;

    /** Timestamp when {@link #cachedClipboardHTML} was last updated. */
    private volatile long clipboardCacheTimestamp = 0;

    /** Time-to-live for clipboard cache in milliseconds. */
    private static final long CLIPBOARD_CACHE_TTL_MS = 2000;

    /**
     * Background executor for clipboard operations.
     * <p>
     * On macOS, accessing the system clipboard from JavaFX thread can cause deadlocks
     * with the AppKit thread. This executor runs clipboard access in a separate daemon
     * thread to avoid blocking any UI thread.
     * </p>
     */
    private static final ExecutorService clipboardExecutor =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ClipboardCache-Worker");
            t.setDaemon(true);
            return t;
        });

    /** Flag to prevent multiple concurrent clipboard refresh operations. */
    private volatile boolean clipboardRefreshInProgress = false;

    // Resource paths
    private static final String TEMPLATE_PATH = "/resources/suneditor/suneditor-template.html";
    private static final String SUNEDITOR_JS_PATH = "/resources/suneditor/js/suneditor.min.js";
    private static final String SUNEDITOR_CSS_PATH = "/resources/suneditor/css/suneditor.min.css";
    private static final String SUNEDITOR_LANG_DE_PATH = "/resources/suneditor/lang/de.js";

    // Font resource paths
    private static final String FONTS_BASE_PATH = "/resources/suneditor/fonts/";
    private static final String[][] FONT_FILES = {
        {"FONT_LIBERATION_SANS_REGULAR", "LiberationSans-Regular.woff2"},
        {"FONT_LIBERATION_SANS_BOLD", "LiberationSans-Bold.woff2"},
        {"FONT_LIBERATION_SANS_ITALIC", "LiberationSans-Italic.woff2"},
        {"FONT_LIBERATION_SANS_BOLDITALIC", "LiberationSans-BoldItalic.woff2"},
        {"FONT_LIBERATION_SERIF_REGULAR", "LiberationSerif-Regular.woff2"},
        {"FONT_LIBERATION_SERIF_BOLD", "LiberationSerif-Bold.woff2"},
        {"FONT_LIBERATION_SERIF_ITALIC", "LiberationSerif-Italic.woff2"},
        {"FONT_LIBERATION_SERIF_BOLDITALIC", "LiberationSerif-BoldItalic.woff2"},
        {"FONT_LIBERATION_MONO_REGULAR", "LiberationMono-Regular.woff2"},
        {"FONT_LIBERATION_MONO_BOLD", "LiberationMono-Bold.woff2"},
        {"FONT_LIBERATION_MONO_ITALIC", "LiberationMono-Italic.woff2"},
        {"FONT_LIBERATION_MONO_BOLDITALIC", "LiberationMono-BoldItalic.woff2"}
    };

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

            // Add focus listener to proactively cache clipboard when editor gains focus
            // This prevents deadlock when paste event tries to access clipboard while EDT is blocked
            // CRITICAL: On macOS, clipboard access from EDT can cause deadlock with AppKit thread
            // Solution: Use background thread that doesn't block either EDT or JavaFX thread
            webView.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (isFocused) {
                    triggerAsyncClipboardRefresh();
                }
            });

            // Create scene and set it to jfxPanel
            Scene scene = new Scene(webView);

            // WORKAROUND: Prevent macOS crash when pressing Caps Lock in WebView
            // JavaFX WebView on macOS crashes when handling certain modifier keys
            // that have no "real" keycode. This filter consumes these events before
            // they reach the WebView's native event handling.
            // See: https://bugs.openjdk.org/browse/JDK-8322703
            // Fixed in JavaFX 17.0.11+ / 22+ / 23+ - this workaround can be removed
            // once the minimum JavaFX version is updated accordingly.
            if (SystemUtils.isMacOs()) {
                scene.addEventFilter(javafx.scene.input.KeyEvent.ANY, event -> {
                    // Only filter CAPS lock, not UNDEFINED - filtering UNDEFINED blocks all typing
                    // on some macOS configurations where normal key events are reported as UNDEFINED
                    if (event.getCode() == javafx.scene.input.KeyCode.CAPS) {
                        event.consume();
                    }
                });
            }

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

            // Load fonts and replace placeholders with data URLs
            for (String[] fontDef : FONT_FILES) {
                String placeholder = fontDef[0];
                String filename = fontDef[1];
                try {
                    byte[] fontData = loadResourceAsBytes(FONTS_BASE_PATH + filename);
                    String dataUrl = "data:font/woff2;base64," + java.util.Base64.getEncoder().encodeToString(fontData);
                    htmlContent = htmlContent.replace(placeholder, dataUrl);
                } catch (Exception e) {
                    log.warn("Could not load font: " + filename + " - " + e.getMessage());
                }
            }

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
     * Loads a resource file as a byte array.
     */
    private byte[] loadResourceAsBytes(String path) throws Exception {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new Exception("Resource not found: " + path);
        }

        try (is) {
            return is.readAllBytes();
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

            // Check if editor.onload already fired before javaConnector was set
            // This handles the race condition where SunEditor initializes before
            // the Java-JavaScript bridge is established
            Object jsEditorReady = webEngine.executeScript("window.editorReady === true");
            if (Boolean.TRUE.equals(jsEditorReady) && !editorReady) {
                log.info("Editor was already ready, triggering onEditorReady manually");
                new JavaScriptConnector().onEditorReady();
            } else if (!editorReady) {
                // Schedule a fallback check in case editor.onload doesn't fire
                // This can happen if SunEditor initialization fails silently
                scheduleEditorReadyFallback();
            }

        } catch (Exception e) {
            log.error("Error accessing JavaScript API", e);
        }
    }

    /**
     * Schedules a fallback check for editor readiness.
     * If editor.onload doesn't fire within 500ms, we check the JavaScript state
     * and trigger onEditorReady manually if the editor API is available.
     */
    private void scheduleEditorReadyFallback() {
        new Thread(() -> {
            try {
                Thread.sleep(500);
                if (!editorReady && !disposed) {
                    Platform.runLater(() -> {
                        try {
                            // Check if editorAPI is available even though onload didn't fire
                            Object apiAvailable = webEngine.executeScript("window.editorAPI !== null && window.editorAPI !== undefined");

                            if (Boolean.TRUE.equals(apiAvailable) && !editorReady) {
                                log.info("Editor onload did not fire, but API is available - triggering onEditorReady via fallback");
                                // Re-fetch editorAPI reference
                                editorAPI = (JSObject) webEngine.executeScript("window.editorAPI");
                                new JavaScriptConnector().onEditorReady();
                            }
                        } catch (Exception e) {
                            log.error("Error in editor ready fallback", e);
                        }
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "EditorReadyFallback").start();
    }

    /**
     * Waits for the editor to be ready (blocking with timeout).
     * <p>
     * <b>WARNING:</b> This method uses {@code Thread.sleep()} and will block the calling thread.
     * On macOS, this MUST NOT be called from EDT as it will cause UI freeze. The macOS-specific
     * code paths in {@link #getText()}, {@link #callEditorAPI(String, Object...)}, and
     * {@link #executeScript(String)} check for EDT and return cached/default values instead
     * of calling this method.
     * </p>
     * <p>
     * This method should only be used for getter methods that need to return a value on
     * non-macOS platforms. Setter methods should use the non-blocking {@link #pendingContent}
     * pattern instead.
     * </p>
     *
     * @see #pendingContent
     * @see #pendingCaretPosition
     */
    private void waitForEditor() {
        int attempts = 0;
        while (!editorReady && attempts < 30) {  // 3 seconds timeout (reduced from 10s)
            try {
                Thread.sleep(100);
                attempts++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!editorReady) {
            log.warn("Editor not ready after timeout (" + attempts * 100 + "ms)");
        }
    }

    /**
     * Executes JavaScript and returns the result (thread-safe).
     * <p>
     * This method schedules script execution on the JavaFX Application Thread via
     * {@link Platform#runLater(Runnable)} and waits for the result using a
     * {@link CompletableFuture} with a 5-second timeout.
     * </p>
     * <p>
     * <b>CRITICAL - macOS Deadlock Prevention:</b> On macOS, calling this from EDT can cause
     * deadlock because JavaFX and AWT share the same AppKit main thread for native operations.
     * When the EDT waits on {@code future.get()}, and the JavaFX thread needs AppKit resources
     * that are held by EDT, both threads deadlock. Therefore, if called from EDT on macOS,
     * this method will log a warning and return null immediately.
     * </p>
     *
     * @param script the JavaScript code to execute
     * @return the result of the JavaScript execution, or null if on macOS/EDT or on error
     */
    private Object executeScript(String script) {
        // CRITICAL: Check macOS/EDT FIRST, before any blocking code (including waitForEditor)
        if (SystemUtils.isMacOs() && SwingUtilities.isEventDispatchThread()) {
            log.warn("executeScript called from EDT on macOS - this can cause deadlock! Returning null.");
            return null;
        }

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
     * Calls a method on {@link #editorAPI} (the JavaScript {@code window.editorAPI} object).
     * <p>
     * This method schedules the API call on the JavaFX Application Thread and waits for
     * the result using a {@link CompletableFuture} with a 5-second timeout.
     * </p>
     * <p>
     * <b>CRITICAL - macOS Deadlock Prevention:</b> Same as {@link #executeScript(String)},
     * this method can cause deadlock on macOS when called from EDT. The check for macOS/EDT
     * is performed FIRST, before any blocking code including {@link #waitForEditor()}.
     * </p>
     *
     * @param methodName the name of the method to call on editorAPI
     * @param args the arguments to pass to the method
     * @return the result of the method call, or null if on macOS/EDT or on error
     * @see #callEditorAPIAsync(String, Object...)
     */
    private Object callEditorAPI(String methodName, Object... args) {
        // CRITICAL: Check macOS/EDT FIRST, before any blocking code (including waitForEditor)
        if (SystemUtils.isMacOs() && SwingUtilities.isEventDispatchThread()) {
            log.warn("callEditorAPI(" + methodName + ") called from EDT on macOS - this can cause deadlock! Returning null.");
            return null;
        }

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

    /**
     * Calls a method on {@link #editorAPI} asynchronously (fire-and-forget, non-blocking).
     * <p>
     * Unlike {@link #callEditorAPI(String, Object...)}, this method does not wait for a result.
     * It schedules the call on the JavaFX Application Thread and returns immediately.
     * This is safe to call from any thread, including EDT on macOS.
     * </p>
     * <p>
     * Use this for setter methods where we don't need to wait for a result (e.g., setText, insert).
     * </p>
     *
     * @param methodName the name of the method to call on editorAPI
     * @param args the arguments to pass to the method
     */
    private void callEditorAPIAsync(String methodName, Object... args) {
        Platform.runLater(() -> {
            try {
                if (editorAPI != null) {
                    editorAPI.call(methodName, args);
                }
            } catch (Exception e) {
                log.error("Error calling editorAPI." + methodName + " asynchronously", e);
            }
        });
    }

    // ========== EditorImplementation Interface Methods ==========

    @Override
    public String getContentType() {
        return ContentTypes.TEXT_HTML;
    }

    /**
     * Returns the current HTML content of the editor.
     * <p>
     * <b>macOS Threading Behavior:</b> When called from EDT on macOS, this method returns
     * the cached content from {@link #cachedEditorContent} instead of querying the editor
     * synchronously. This prevents deadlocks. The cache is kept up-to-date by:
     * <ul>
     *   <li>JavaScript's {@code editor.onChange} handler (300ms debounce) calling
     *       {@link JavaScriptConnector#onContentChanged(String)}</li>
     *   <li>{@link #setText(String)} updating the cache immediately</li>
     *   <li>{@link JavaScriptConnector#onEditorReady()} initializing the cache</li>
     * </ul>
     * </p>
     * <p>
     * <b>Fallback Chain on macOS/EDT:</b>
     * <ol>
     *   <li>Return {@link #cachedEditorContent} if available</li>
     *   <li>Return {@link #pendingContent} if editor not yet ready</li>
     *   <li>Return empty string as last resort</li>
     * </ol>
     * </p>
     * <p>
     * On Windows/Linux or when not called from EDT, the content is fetched synchronously
     * from the editor via {@link #callEditorAPI(String, Object...)}.
     * </p>
     *
     * @return the current HTML content of the editor
     */
    @Override
    public String getText() {
        // CRITICAL: On macOS, calling blocking methods from EDT causes deadlock.
        // Check this FIRST, before any potentially blocking code.
        // The cache is kept up-to-date by JavaScript's onChange handler (300ms debounce).
        if (SystemUtils.isMacOs() && SwingUtilities.isEventDispatchThread()) {
            log.debug("getText() called from EDT on macOS - returning cached content to avoid deadlock");
            if (cachedEditorContent != null) {
                return cachedEditorContent;
            }
            if (pendingContent != null) {
                return pendingContent;
            }
            log.warn("getText() on macOS/EDT with no cached content - returning empty string. This may indicate a race condition.");
            return "";
        }

        // If panel is disposed, return cached/pending content or empty string to avoid blocking
        // This prevents TimeoutException during UI teardown
        if (disposed) {
            if (cachedEditorContent != null) {
                return cachedEditorContent;
            }
            return pendingContent != null ? pendingContent : "";
        }

        // If editor is not ready yet, return pending content if available
        // This prevents data loss when the panel is closed before editor initialization completes
        if (!editorReady) {
            if (pendingContent != null) {
                return pendingContent;
            }
            // Editor not ready and no pending content - wait for it
            waitForEditor();
        }

        Object result = callEditorAPI("getText");
        String text = result != null ? result.toString() : "";
        // Update cache with fresh content, but only if we got actual content.
        // Prevents overwriting valid cache with empty string on transient failures.
        if (text != null && !text.isEmpty()) {
            cachedEditorContent = text;
        }
        return text;
    }

    @Override
    public void setText(String text) {
        if (text == null) {
            text = "";
        }

        final String finalText = text;

        // Update cache immediately so getText() returns correct value on macOS
        this.cachedEditorContent = finalText;

        if (!editorReady) {
            // Store content to set when editor becomes ready (non-blocking)
            this.pendingContent = finalText;
            return;
        }

        // Editor is ready, set content asynchronously (non-blocking)
        callEditorAPIAsync("setText", finalText);

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
        // Use async version to avoid blocking - insert doesn't need return value
        callEditorAPIAsync("insert", finalText, pos);
        // Note: Do NOT invalidate cache here - the JavaScript onChange handler
        // will fire and update the cache with the correct merged content.
        // Setting cachedEditorContent = null here causes empty emails when
        // insert() is followed by getText() before onChange fires.
    }

    @Override
    public void setCaretPosition(int pos) {
        if (!editorReady) {
            // Store position to set when editor becomes ready (non-blocking)
            this.pendingCaretPosition = pos;
            return;
        }
        callEditorAPIAsync("setCaretPosition", pos);
    }

    @Override
    public int getCaretPosition() {
        // On macOS from EDT, return 0 to avoid deadlock - caret position is not critical
        if (SystemUtils.isMacOs() && SwingUtilities.isEventDispatchThread()) {
            log.debug("getCaretPosition() called from EDT on macOS - returning 0 to avoid deadlock");
            return 0;
        }
        Object result = callEditorAPI("getCaretPosition");
        if (result instanceof Number) {
            return ((Number) result).intValue();
        }
        return 0;
    }

    @Override
    public String getSelectedText() {
        // On macOS from EDT, return empty string to avoid deadlock
        if (SystemUtils.isMacOs() && SwingUtilities.isEventDispatchThread()) {
            log.debug("getSelectedText() called from EDT on macOS - returning empty string to avoid deadlock");
            return "";
        }
        Object result = callEditorAPI("getSelectedText");
        return result != null ? result.toString() : "";
    }

    // ========== Clipboard Caching ==========

    /**
     * Triggers an asynchronous clipboard refresh in a background thread.
     * This method is safe to call from any thread (JavaFX, EDT, or other).
     *
     * The background approach prevents deadlocks on macOS where:
     * - JavaFX thread waits for EDT (via SwingUtilities.invokeLater)
     * - EDT waits for AppKit thread (via Toolkit.getSystemClipboard)
     * - AppKit thread may be waiting for JavaFX (native event loop)
     */
    private void triggerAsyncClipboardRefresh() {
        // Skip if refresh is already in progress
        if (clipboardRefreshInProgress) {
            log.debug("Clipboard refresh already in progress, skipping");
            return;
        }

        clipboardExecutor.submit(() -> {
            clipboardRefreshInProgress = true;
            try {
                refreshClipboardCacheInBackground();
            } finally {
                clipboardRefreshInProgress = false;
            }
        });
    }

    /**
     * Performs clipboard refresh in background thread.
     * This method accesses the system clipboard directly without going through EDT,
     * which is safe because we only read (not write) and use volatile fields for the cache.
     *
     * Note: On some platforms, clipboard access outside EDT may have limitations,
     * but for reading HTML content this approach is reliable and avoids deadlocks.
     */
    private void refreshClipboardCacheInBackground() {
        try {
            // Access clipboard directly from background thread
            // This is safe for read operations and avoids EDT/AppKit deadlock on macOS
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Clipboard clipboard = toolkit.getSystemClipboard();

            String html = readHtmlFromClipboard(clipboard);

            if (html != null) {
                cachedClipboardHTML = parseWindowsHtmlFormat(html);
                clipboardCacheTimestamp = System.currentTimeMillis();
                log.info("Background cached HTML from clipboard: " + cachedClipboardHTML.length() + " chars");
            } else {
                cachedClipboardHTML = null;
                clipboardCacheTimestamp = System.currentTimeMillis();
                log.info("No HTML flavor found in clipboard (background refresh)");
            }

        } catch (Exception e) {
            log.warn("Error in background clipboard refresh: " + e.getMessage());
            // Don't update cache on error - keep stale value if available
        }
    }

    /**
     * Reads HTML content from clipboard, trying multiple flavors.
     * This method is thread-safe for read operations.
     *
     * @param clipboard The system clipboard
     * @return HTML content or null if not available
     */
    private String readHtmlFromClipboard(Clipboard clipboard) {
        try {
            // Try standard HTML flavor first
            DataFlavor htmlFlavor = new DataFlavor("text/html; class=java.lang.String");
            if (clipboard.isDataFlavorAvailable(htmlFlavor)) {
                String html = (String) clipboard.getData(htmlFlavor);
                log.info("Read HTML from clipboard (standard flavor): " + html.length() + " chars");
                return html;
            }

            // Check all available flavors for HTML content
            DataFlavor[] flavors = clipboard.getAvailableDataFlavors();
            log.debug("Standard HTML flavor not available, checking " + flavors.length + " alternate flavors");

            for (DataFlavor flavor : flavors) {
                String mimeType = flavor.getMimeType();

                if (mimeType.contains("text/html")) {
                    try {
                        Object data = clipboard.getData(flavor);
                        if (data instanceof String) {
                            log.info("Read HTML from flavor " + mimeType);
                            return (String) data;
                        } else if (data instanceof java.io.InputStream) {
                            java.io.InputStream is = (java.io.InputStream) data;
                            StringBuilder sb = new StringBuilder();
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                            }
                            is.close();
                            log.info("Read HTML from stream flavor " + mimeType);
                            return sb.toString();
                        } else if (data instanceof java.io.Reader) {
                            java.io.Reader reader = (java.io.Reader) data;
                            StringBuilder sb = new StringBuilder();
                            char[] buffer = new char[4096];
                            int charsRead;
                            while ((charsRead = reader.read(buffer)) != -1) {
                                sb.append(buffer, 0, charsRead);
                            }
                            reader.close();
                            log.info("Read HTML from reader flavor " + mimeType);
                            return sb.toString();
                        }
                    } catch (Exception e) {
                        log.warn("Could not read flavor " + mimeType + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Error reading clipboard: " + e.getMessage());
        }

        return null;
    }

    /**
     * Refreshes the clipboard cache synchronously.
     *
     * WARNING: On macOS, calling this from EDT while JavaFX thread is waiting
     * can cause a deadlock. Prefer triggerAsyncClipboardRefresh() instead.
     */
    private void refreshClipboardCache() {
        // If not on EDT, use background refresh to avoid potential issues
        if (!SwingUtilities.isEventDispatchThread()) {
            if (SystemUtils.isMacOs()) {
                // On macOS, prefer background refresh to avoid potential deadlock
                log.debug("refreshClipboardCache called off-EDT on macOS - using background refresh instead");
                log.error("TODO: re-enable triggerAsyncClipboardRefresh on macOS");
                triggerAsyncClipboardRefresh();
                return;
            }
            SwingUtilities.invokeLater(this::refreshClipboardCache);
            return;
        }

        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Clipboard clipboard = toolkit.getSystemClipboard();

            String html = readHtmlFromClipboard(clipboard);

            if (html != null) {
                cachedClipboardHTML = parseWindowsHtmlFormat(html);
                clipboardCacheTimestamp = System.currentTimeMillis();
                log.info("EDT cached HTML from clipboard: " + cachedClipboardHTML.length() + " chars");
            } else {
                cachedClipboardHTML = null;
                clipboardCacheTimestamp = System.currentTimeMillis();
                log.info("No HTML flavor found in clipboard for caching");
            }

        } catch (Exception e) {
            log.warn("Error caching clipboard: " + e.getMessage());
            cachedClipboardHTML = null;
            clipboardCacheTimestamp = System.currentTimeMillis();
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
                    byte[] fragment = java.util.Arrays.copyOfRange(bytes, startFragmentPos, endFragmentPos);
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

    // ========== JavaScript Connector Class ==========

    /**
     * Java object exposed to JavaScript for callbacks.
     * <p>
     * This class is instantiated and registered with the WebEngine as {@code window.javaConnector}
     * (see the state change listener in constructor). JavaScript code in {@code suneditor-template.html}
     * can then call methods on this object.
     * </p>
     *
     * <h3>Key Methods Called from JavaScript:</h3>
     * <ul>
     *   <li>{@link #onEditorReady()} - Called by {@code editor.onload} when SunEditor initialization completes</li>
     *   <li>{@link #onContentChanged(String)} - Called by {@code editor.onChange} (with 300ms debounce)
     *       whenever the editor content changes. This is critical for maintaining the content cache on macOS.</li>
     *   <li>{@link #getClipboardHTML()} - Called by the custom paste handler to get HTML from system clipboard</li>
     *   <li>{@link #log(String)} - Called for debug logging from JavaScript</li>
     * </ul>
     *
     * <h3>Threading Context:</h3>
     * All methods in this class are called from the JavaFX Application Thread (since they're invoked
     * from JavaScript running in the WebView). When accessing shared state like {@link #cachedEditorContent},
     * the volatile keyword ensures visibility across threads.
     *
     * @see #cachedEditorContent
     * @see WebViewHtmlEditorPanel
     */
    public class JavaScriptConnector {
        /**
         * Called by JavaScript when the SunEditor is ready.
         * <p>
         * This method is invoked from {@code editor.onload} in {@code suneditor-template.html}.
         * It sets {@link #editorReady} to true and applies any pending content/caret position
         * that was set via {@link #setText(String)} or {@link #setCaretPosition(int)} before
         * the editor was ready.
         * </p>
         * <p>
         * Additionally, this method initializes {@link #cachedEditorContent} to ensure
         * {@link #getText()} returns the correct value on macOS even before the JavaScript
         * {@code onChange} handler fires.
         * </p>
         */
        public void onEditorReady() {
            editorReady = true;
            log.info("SunEditor is ready");

            // Apply any pending content that was set before editor was ready
            // Note: pendingCaretPosition is applied AFTER content is set (in same Platform.runLater block)
            // to ensure content is loaded before positioning the caret
            if (pendingContent != null || pendingCaretPosition >= 0) {
                final String contentToSet = pendingContent;
                final int caretToSet = pendingCaretPosition;
                pendingContent = null;
                pendingCaretPosition = -1;

                Platform.runLater(() -> {
                    try {
                        if (contentToSet != null && editorAPI != null) {
                            log.info("Applying pending content (" + contentToSet.length() + " chars)");
                            editorAPI.call("setText", contentToSet);
                            // Update cache immediately after setting content
                            // This ensures getText() returns correct value on macOS before onChange fires
                            cachedEditorContent = contentToSet;
                        }
                        // Caret position is set after content in the same runLater block,
                        // ensuring sequential execution on the JavaFX thread
                        if (caretToSet >= 0 && editorAPI != null) {
                            log.info("Applying pending caret position: " + caretToSet);
                            editorAPI.call("setCaretPosition", caretToSet);
                        }
                    } catch (Exception e) {
                        log.error("Error applying pending content/caret", e);
                    }
                });
            } else {
                // No pending content - initialize cache from editor's current content
                // This handles the case where editor is opened without setText() being called first
                Platform.runLater(() -> {
                    try {
                        if (editorAPI != null) {
                            Object result = editorAPI.call("getText");
                            if (result != null) {
                                cachedEditorContent = result.toString();
                                log.debug("Initialized content cache on editor ready: " + cachedEditorContent.length() + " chars");
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Could not initialize content cache on editor ready", e);
                    }
                });
            }

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
         * Called by JavaScript when editor content changes.
         * <p>
         * This method is invoked by the {@code editor.onChange} handler in {@code suneditor-template.html}
         * with a 300ms debounce to avoid flooding Java with updates during rapid typing.
         * </p>
         * <p>
         * <b>macOS Content Cache:</b> This method is the primary mechanism for keeping
         * {@link #cachedEditorContent} in sync with the actual editor content. On macOS,
         * {@link #getText()} cannot synchronously query the editor from EDT (it would cause deadlock),
         * so it relies on this cached value. The cache is updated:
         * <ul>
         *   <li>By this method (called from JavaScript every 300ms during editing)</li>
         *   <li>By {@link #setText(String)} when content is set from Java</li>
         *   <li>By {@link #onEditorReady()} when pending content is applied</li>
         * </ul>
         * </p>
         *
         * @param content The current HTML content of the editor
         * @see #cachedEditorContent
         * @see #getText()
         */
        public void onContentChanged(String content) {
            cachedEditorContent = content;
            log.debug("Editor content cache updated: " + (content != null ? content.length() : 0) + " chars");
        }

        /**
         * Get HTML from system clipboard using cached data with fallback to background refresh.
         * <p>
         * This method is called from the custom paste handler in {@code suneditor-template.html}
         * when the user pastes content into the editor.
         * </p>
         * <p>
         * <b>macOS Deadlock Prevention:</b> This method is called from the JavaFX thread
         * (via JavaScript bridge). On macOS, we must NOT wait for EDT synchronously, as this
         * can cause deadlock with the AppKit thread. Instead, we:
         * <ol>
         *   <li>Return cached clipboard content if available and fresh (within {@link #CLIPBOARD_CACHE_TTL_MS})</li>
         *   <li>Trigger an async refresh via {@link #triggerAsyncClipboardRefresh()} for future pastes</li>
         *   <li>Return stale cache or null rather than blocking</li>
         * </ol>
         * </p>
         * <p>
         * The clipboard cache is refreshed in background via {@link #clipboardExecutor} when the
         * editor gains focus (see focus listener in constructor).
         * </p>
         *
         * @return HTML string from clipboard, or null if not available
         * @see #cachedClipboardHTML
         * @see #refreshClipboardCacheInBackground()
         */
        public String getClipboardHTML() {
            // Return cached value if fresh enough
            long age = System.currentTimeMillis() - clipboardCacheTimestamp;
            if (age < CLIPBOARD_CACHE_TTL_MS && cachedClipboardHTML != null) {
                log.info("Returning cached clipboard HTML: " + cachedClipboardHTML.length() + " chars (age=" + age + "ms)");
                return cachedClipboardHTML;
            }

            // Cache is stale or empty
            log.info("Clipboard cache stale or empty (age=" + age + "ms)");

            // On macOS, never wait for EDT - return stale cache and trigger async refresh
            if (SystemUtils.isMacOs()) {
                log.info("macOS detected - triggering async refresh, returning cached value to avoid deadlock");
                triggerAsyncClipboardRefresh();

                // Return whatever we have (may be stale or null)
                if (cachedClipboardHTML != null) {
                    log.info("Returning stale cached clipboard HTML: " + cachedClipboardHTML.length() + " chars");
                } else {
                    log.info("No cached clipboard HTML available");
                }
                return cachedClipboardHTML;
            }

            // On Windows/Linux, we can try a quick synchronous refresh with short timeout
            // This is less risky as there's no AppKit thread involvement
            log.info("Non-macOS platform - attempting sync refresh with timeout");

            CompletableFuture<String> future = new CompletableFuture<>();

            SwingUtilities.invokeLater(() -> {
                try {
                    refreshClipboardCache();
                    future.complete(cachedClipboardHTML);
                } catch (Exception e) {
                    log.warn("Error during sync clipboard refresh: " + e.getMessage());
                    future.complete(null);
                }
            });

            try {
                // Wait up to 200ms for clipboard refresh - short enough to not freeze UI
                String result = future.get(200, TimeUnit.MILLISECONDS);
                if (result != null) {
                    log.info("Got HTML from sync clipboard refresh: " + result.length() + " chars");
                    return result;
                }
            } catch (java.util.concurrent.TimeoutException e) {
                log.info("Clipboard refresh timed out, returning cached value");
            } catch (Exception e) {
                log.warn("Error waiting for clipboard refresh: " + e.getMessage());
            }

            // Return whatever we have (may be null or stale)
            if (cachedClipboardHTML != null) {
                log.info("Returning stale cached clipboard HTML: " + cachedClipboardHTML.length() + " chars");
            } else {
                log.info("No HTML flavor found in clipboard cache");
            }
            return cachedClipboardHTML;
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

                    // Execute JavaScript cleanup BEFORE clearing content
                    try {
                        // SunEditor's destroy method - check if editor and method exist
                        webEngine.executeScript(
                            "if (window.editor && typeof window.editor.destroy === 'function') { " +
                            "  window.editor.destroy(); " +
                            "  window.editor = null; " +
                            "} " +
                            "if (window.editorAPI) { window.editorAPI = null; }"
                        );
                    } catch (Exception e) {
                        // Ignore - page might already be unloaded or editor not initialized
                        log.debug("JavaScript cleanup skipped: " + e.getMessage());
                    }

                    // Clear the page content to free memory
                    webEngine.loadContent("");

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

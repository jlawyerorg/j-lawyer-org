# Design: WebView HTML Editor Integration

## Overview

This design document describes the architecture for embedding a JavaScript HTML editor within a JavaFX WebView component, integrated into the Swing-based j-lawyer desktop client. The document evaluates candidate editors and selects the best option meeting the project requirements.

## Architecture

### Component Stack

```
┌─────────────────────────────────────────────────┐
│ Swing Container (JPanel, JFrame, etc.)         │
├─────────────────────────────────────────────────┤
│ JFXPanel (Swing-JavaFX Bridge)                  │
├─────────────────────────────────────────────────┤
│ JavaFX Scene                                     │
│  └── WebView                                     │
│       └── WebEngine                              │
│            └── HTML Page (editor.html)           │
│                 ├── SunEditor CSS                │
│                 ├── SunEditor JS                 │
│                 └── JavaScript Bridge API        │
│                      (window.editorAPI)          │
└─────────────────────────────────────────────────┘
         ▲                           │
         │                           ▼
    executeScript()            JSObject callbacks
         │                           │
         └───────────────────────────┘
         Java ↔ JavaScript Bridge
```

### File Organization

```
j-lawyer-client/
├── src/
│   ├── com/jdimension/jlawyer/client/mail/
│   │   ├── EditorImplementation.java (interface)
│   │   ├── HtmlEditorPanel.java (existing SHEF-based)
│   │   └── WebViewHtmlEditorPanel.java (NEW)
│   └── resources/
│       └── html-editor/
│           ├── editor.html
│           ├── suneditor.min.js
│           └── suneditor.min.css
└── test/
    └── com/jdimension/jlawyer/client/mail/
        └── WebViewHtmlEditorPanelDemo.java (NEW)
```

## Editor Evaluation

### Requirements Summary

#### Must-Have Requirements
1. AGPL-compatible license (MIT, BSD, Apache, GPL)
2. Complete offline operation (no CDN)
3. Paste from Word with formatting preservation
4. Ctrl+Z/Ctrl+Y undo/redo
5. Toolbar features:
   - Font family, size, color
   - Bold, Italic, Underline
   - Bulleted/numbered lists
   - Text alignment (left, center, right)
   - Insert link, image, table

#### Nice-to-Have Requirements
1. German spell checking
2. Base64 image embedding

### Candidate Editors Comparison

| Feature | **Requirement** | Quill | **SunEditor** | TinyMCE | CKEditor 5 | Squire | Trix |
|---------|----------------|-------|---------------|---------|------------|--------|------|
| **License** | AGPL-compatible | ✅ BSD | ✅ MIT | ⚠️ GPL/Paid | ⚠️ GPL/Paid | ✅ MIT | ✅ MIT |
| **Offline** | Required | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **File Size** | - | 200KB | 350KB | 500KB+ | 300-700KB | 16KB | 60KB |
| **Paste from Word** | Required | ✅ Good | ✅ Excellent | ✅ Excellent | ✅ Good | ⚠️ Basic | ⚠️ Basic |
| **Undo/Redo (Ctrl+Z)** | Required | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Font Family** | Required | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| **Font Size** | Required | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| **Font Color** | Required | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| **Bold/Italic/Underline** | Required | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Lists** | Required | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Text Alignment** | Required | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| **Insert Link** | Required | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Insert Image** | Required | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No | ✅ Yes |
| **Insert Table** | Required | ⚠️ Plugin | ✅ Built-in | ✅ Built-in | ✅ Built-in | ❌ No | ❌ No |
| **German Spell Check** | Nice-to-have | ❌ No | ⚠️ Browser | ✅ Yes (paid) | ⚠️ Plugin | ❌ No | ❌ No |
| **Base64 Images** | Nice-to-have | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No | ✅ Yes |
| **Active Maintenance** | - | Very Active | Very Active | Very Active | Very Active | Active | Active |
| **Ready to Use** | - | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **HTML Source View** | - | ❌ No | ✅ Yes | ✅ Yes | ⚠️ Plugin | ❌ No | ❌ No |
| **Requirements Met** | - | 13/13 ⚠️ | **14/14 ✅** | 14/14 ⚠️ | 13/13 ⚠️ | 6/13 ❌ | 7/13 ❌ |

### Scoring

| Editor | Must-Have (13) | Nice-to-Have (2) | License Issue | Size | **Total Score** |
|--------|----------------|------------------|---------------|------|-----------------|
| **SunEditor** | 13/13 ✅ | 1/2 ⚠️ | None | 350KB | **⭐⭐⭐⭐⭐ (Best)** |
| TinyMCE | 13/13 ✅ | 1/2 ⚠️ | Dual GPL/Paid | 500KB+ | ⭐⭐⭐⭐ (Good, license concerns) |
| Quill | 12/13 ⚠️ | 1/2 ⚠️ | None | 200KB | ⭐⭐⭐⭐ (Tables need plugin) |
| CKEditor 5 | 12/13 ⚠️ | 1/2 ⚠️ | Dual GPL/Paid | 300-700KB | ⭐⭐⭐ (Complex, license) |
| Trix | 7/13 ❌ | 1/2 ⚠️ | None | 60KB | ⭐⭐ (Missing features) |
| Squire | 6/13 ❌ | 0/2 ❌ | None | 16KB | ⭐ (Too basic) |

**Note on Spell Checking**: Most editors rely on browser's native spell checking (right-click → language selection). TinyMCE offers premium spell-as-you-type checking with German support, but requires paid license. SunEditor and others use browser spell check which supports German if configured.

**Note on Base64 Images**: Most modern editors support pasting images which are automatically converted to base64 data URIs. SunEditor, Quill, TinyMCE, and CKEditor all support this.

## Design Decisions

### 1. JavaScript Editor Selection: SunEditor

**Decision**: Use SunEditor as the JavaScript HTML editor library

**Rationale**:
1. **All Must-Have Requirements Met**: Only editor that meets 100% of required features out-of-the-box without plugins
2. **Built-in Table Support**: Critical for legal documents, no plugin needed
3. **MIT License**: Fully compatible with AGPLv3, no dual-licensing concerns
4. **Best Word Paste**: Excellent handling of formatted content from Word
5. **HTML Source View**: Built-in code view for advanced users
6. **Reasonable Size**: 350KB is acceptable (not smallest, not largest)
7. **Active Maintenance**: Updated in 2025, responsive community
8. **Complete Offline**: No CDN dependencies, all resources bundled
9. **Base64 Image Support**: Automatically embeds pasted images as data URIs
10. **Browser Spell Check**: Leverages browser's built-in spell checking (supports German when configured)

**Why Not Others**:
- **Quill**: Requires plugin for tables (adds complexity), slightly less features
- **TinyMCE**: Dual GPL/Commercial licensing creates uncertainty, larger size, overkill for needs
- **CKEditor 5**: Complex API, dual licensing, more configuration needed
- **Squire/Trix**: Missing critical features (tables, font controls, alignment)

### 2. Embedding Approach: Client-Side Resources

**Decision**: Bundle SunEditor files as client resources (not server-hosted)

**Alternatives Considered**:
- Server-hosted: Deploy editor files in j-lawyer-server-war
- CDN: Load from external CDN
- Dynamic download: Download on first use

**Rationale**:
- **Offline Capability**: Works without network connection or server
- **Version Control**: Editor version tied to client version
- **No Server Changes**: j-lawyer-server unaffected
- **Deployment Simplicity**: Single JAR contains everything
- **Performance**: No network latency for editor loading

### 3. Thread Management: Platform.runLater() Pattern

**Decision**: Use JavaFX Platform.runLater() for all WebView operations

**Pattern** (from existing MailContentUI.java):
```java
Platform.runLater(() -> {
    webEngine.executeScript("...");
});
```

**Rationale**:
- **Thread Safety**: JavaFX requires operations on JavaFX Application Thread
- **Proven Pattern**: Successfully used in MailContentUI.java and JavaFxBrowserPanel.java
- **Prevents Deadlocks**: Avoids cross-thread UI access issues

**Implementation Note**: For synchronous operations (getText), use CountDownLatch:
```java
public String getText() {
    AtomicReference<String> result = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
        try {
            result.set((String) webEngine.executeScript("..."));
        } finally {
            latch.countDown();
        }
    });
    latch.await();
    return result.get();
}
```

### 4. Java-JavaScript Bridge: window.editorAPI

**Decision**: Expose JavaScript API via `window.editorAPI` object

**API Surface**:
```javascript
window.editorAPI = {
    // Content
    getText: () => string,
    setText: (html: string) => void,

    // Selection
    getSelectedText: () => string,

    // Insertion
    insert: (html: string) => void,

    // Control
    setEnabled: (enabled: boolean) => void,
    requestFocus: () => void,

    // Undo/Redo
    undo: () => void,
    redo: () => void,

    // Configuration
    setDefaultFontFamily: (family: string) => void,
    setDefaultFontSize: (size: string) => void,
    setDefaultFontColor: (color: string) => void
};
```

**Rationale**:
- **Simple Interface**: Single global object, easy to call from Java
- **Type Safety**: Clear method signatures
- **Extensibility**: Easy to add new methods
- **Testability**: Can be tested in browser console

### 5. Content Escaping: Template Literals

**Decision**: Use JavaScript template literals (backticks) for HTML content

**Implementation**:
```java
String escaped = escapeForTemplateLiteral(html);
webEngine.executeScript("window.editorAPI.setText(`" + escaped + "`)");

private String escapeForTemplateLiteral(String str) {
    return str.replace("\\", "\\\\")
              .replace("`", "\\`")
              .replace("$", "\\$");
}
```

**Alternatives Considered**:
- JSON.stringify: More overhead
- Single quotes: Still requires escaping
- Base64 encoding: Unnecessary complexity

**Rationale**:
- **Minimal Escaping**: Only backslash, backtick, and dollar sign
- **Preserves Formatting**: No changes to whitespace or newlines
- **Simple Implementation**: Straightforward string replacement

### 6. Initialization Handling: Async Ready State

**Decision**: Track WebView initialization state before allowing operations

**Implementation**:
```java
private volatile boolean initialized = false;

webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
    if (newState == Worker.State.SUCCEEDED) {
        initialized = true;
        // Notify waiting threads
    }
});

public String getText() {
    if (!initialized) {
        return ""; // or wait for initialization
    }
    // ... proceed with operation
}
```

**Rationale**:
- **Prevents NPE**: Avoids calling methods before WebView ready
- **User Feedback**: Can show loading state if needed
- **Graceful Degradation**: Returns safe defaults before init

### 7. Resource Loading: Classpath Resources

**Decision**: Load editor.html and assets from classpath

**Implementation**:
```java
String editorHtml = getClass().getResource("/html-editor/editor.html").toExternalForm();
webEngine.load(editorHtml);
```

**HTML References**:
```html
<link rel="stylesheet" href="suneditor.min.css">
<script src="suneditor.min.js"></script>
```

**Rationale**:
- **Standard Java Pattern**: Resource loading via classloader
- **Relative Paths Work**: HTML can reference sibling files
- **Build System Integration**: Resources included in JAR via Ant

## API Mapping

### EditorImplementation Interface

| Method | SunEditor JavaScript | Notes |
|--------|---------------------|-------|
| `String getContentType()` | N/A | Returns "text/html" constant |
| `String getText()` | `suneditor.getContents(false)` | false = no wrapper div |
| `void setText(String)` | `suneditor.setContents(html)` | Sets HTML content |
| `String getSelectedText()` | `suneditor.getSelection().toString() \|\| suneditor.getText()` | Falls back to full text if no selection |
| `void insert(String, int)` | `suneditor.insertHTML(html)` | Note: position parameter ignored (inserts at cursor) |
| `void setCaretPosition(int)` | Complex | Limited support, position-to-node conversion needed |
| `int getCaretPosition()` | Complex | Limited support, range-to-position conversion needed |

### Extended API (HtmlEditorPanel-compatible)

| Method | SunEditor JavaScript |
|--------|---------------------|
| `void setEnabled(boolean)` | `suneditor.enable()` / `suneditor.disable()` |
| `void requestFocus()` | `suneditor.core.focus()` |
| `void setDefaultFontFamily(String)` | Update `defaultStyle` config |
| `void setDefaultFontSize(String)` | Update `defaultStyle` config |
| `void setDefaultFontColor(String)` | Update `defaultStyle` config |

### Known Limitations

1. **Caret Position Control**:
   - `setCaretPosition(int)` and `getCaretPosition()` are difficult to implement
   - SunEditor uses DOM Range objects, not character positions
   - **Mitigation**: Document as limited support, most usages rely on current cursor position anyway

2. **Insert Position**:
   - `insert(String, int pos)` ignores position parameter
   - Always inserts at current cursor position
   - **Mitigation**: Most callers use -1 or current position anyway

## SunEditor Configuration

### Toolbar Buttons

Match current HtmlEditorPanel features:
```javascript
buttonList: [
    ['undo', 'redo'],
    ['font', 'fontSize', 'formatBlock'],
    ['bold', 'underline', 'italic', 'strike'],
    ['fontColor', 'hiliteColor'],
    ['removeFormat'],
    ['outdent', 'indent'],
    ['align', 'horizontalRule', 'list', 'table'],
    ['link', 'image'],
    ['codeView'] // HTML source editing
]
```

### Default Styles

Match HtmlEditorPanel defaults:
```javascript
defaultStyle: 'font-family: sans-serif; font-size: 12px; color: black;'
```

### Size Configuration

```javascript
height: '100%',
width: '100%',
minHeight: '200px'
```

## Integration with Existing Code

### No Changes Required To

- `EditorImplementation` interface
- Classes using `EditorImplementation` (SendEmailFrame, EmailTemplatesPanel, etc.)
- Build system (WebViewHtmlEditorPanel is just another source file)

### Demo/Test Integration

Create standalone demo frame for testing:
```java
public class WebViewHtmlEditorPanelDemo extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WebViewHtmlEditorPanel editor = new WebViewHtmlEditorPanel();
            JFrame frame = new JFrame("SunEditor Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(editor);
            frame.setSize(800, 600);
            frame.setVisible(true);

            // Test setText after initialization
            Timer timer = new Timer(2000, e -> {
                editor.setText("<h1>Hello</h1><p>Test content</p>");
            });
            timer.setRepeats(false);
            timer.start();
        });
    }
}
```

## Performance Considerations

### Memory Usage

- **WebView Overhead**: ~50-80MB per instance (JavaFX WebKit browser engine)
- **SunEditor**: ~5MB loaded JavaScript/CSS
- **Total**: ~60-90MB per editor instance
- **Comparison**: SHEF HTMLEditorPane uses ~20-30MB
- **Mitigation**: Proper disposal when closing, limit concurrent editors

### Initialization Time

- **JavaFX Startup**: ~500ms (first instance only, cached for subsequent)
- **WebView Load**: ~300-500ms
- **SunEditor Init**: ~100-200ms
- **Total**: ~1-1.2 seconds for first editor, ~400-700ms for subsequent
- **Comparison**: SHEF loads nearly instantly (~50ms)
- **Mitigation**: Lazy initialization, loading indicator

### Runtime Performance

- **Typing Latency**: Negligible (browser input handling is optimized)
- **JavaScript Bridge**: ~1-5ms per call
- **Content Updates**: Efficient (browser DOM manipulation)

## Security Considerations

### Content Sanitization

SunEditor provides built-in XSS protection:
- Sanitizes pasted content
- Removes dangerous scripts
- Allows safe HTML tags only

**Note**: j-lawyer is a desktop application with trusted content sources, but sanitization is still beneficial.

### Resource Isolation

- Editor runs in WebView sandbox
- No access to file system beyond provided HTML
- JavaScript confined to loaded page

## Cross-Platform Compatibility

### Tested Platforms

- **Linux**: Primary development platform
- **Windows**: JavaFX WebView uses IE/Edge engine (legacy) or WebKit
- **macOS**: JavaFX WebView uses WebKit

### Known Issues

- **Windows IE Mode**: Older Java versions may use IE engine (limited CSS support)
  - **Mitigation**: Require Java 11+ which uses WebKit
- **Font Rendering**: May vary slightly across platforms
  - **Mitigation**: Use web-safe fonts

## Extensibility

### Future Enhancements

1. **Plugins**: SunEditor has plugin system
   - Code syntax highlighting
   - Mathematical formulas
   - Charts/diagrams

2. **Collaborative Editing**: SunEditor supports conflict-free replicated data types (CRDTs)

3. **Templates**: SunEditor template system for legal document templates

4. **Custom Buttons**: Easy to add toolbar buttons for j-lawyer-specific features

## References

### Existing Patterns

- `MailContentUI.java`: JavaFX WebView read-only email viewer
- `JavaFxBrowserPanel.java`: JavaFX WebView document previewer
- `WebViewRegister.java`: Singleton pattern for WebView instance management

### External Documentation

- SunEditor: https://github.com/JiHong88/SunEditor
- JavaFX WebView: https://openjfx.io/javadoc/11/javafx.web/javafx/scene/web/WebView.html
- JFXPanel: https://openjfx.io/javadoc/11/javafx.swing/javafx/embed/swing/JFXPanel.html

## Rollback Plan

If PoC reveals insurmountable issues:

1. **Memory Issues**: SHEF remains option for resource-constrained environments
2. **Performance Issues**: SHEF has better cold-start performance
3. **Compatibility Issues**: SHEF works on all platforms without dependencies
4. **Feature Gaps**: SHEF covers all current use cases

The PoC is designed to identify these issues **before** production migration, allowing informed decision-making.

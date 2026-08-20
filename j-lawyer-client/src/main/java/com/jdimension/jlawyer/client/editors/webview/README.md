# SunEditor WebView Integration - Proof of Concept

## Overview

This package contains a **Proof of Concept (PoC)** implementation of an HTML editor using **SunEditor** embedded in a **JavaFX WebView**, designed as a replacement for the problematic SHEF (Simple HTML Editor Framework).

## Status

**PROOF OF CONCEPT - NOT PRODUCTION READY**

This implementation demonstrates the technical feasibility of replacing SHEF with SunEditor. It is not yet integrated into the production codebase.

## Components

### 1. WebViewHtmlEditorPanel.java

Main implementation class that:
- Implements the `EditorImplementation` interface (same as HtmlEditorPanel)
- Embeds JavaFX WebView in a Swing JFXPanel
- Loads SunEditor from local resources (fully offline)
- Provides Java-JavaScript bridge for bidirectional communication
- Thread-safe operations using `Platform.runLater()` and `CompletableFuture`

**Key Features:**
- All 7 EditorImplementation methods implemented
- Offline operation (no CDN dependencies)
- Thread-safe cross-thread communication
- Resource loading from classpath

### 2. WebViewEditorDemo.java

Standalone test application featuring:
- Visual editor testing interface
- Test buttons for all EditorImplementation methods
- SHEF bug validation tests
- Output panel for test results
- Complex HTML testing

**Run with:**
```bash
java com.jdimension.jlawyer.client.editors.webview.WebViewEditorDemo
```

### 3. Resources (src/resources/suneditor/)

- `suneditor-template.html` - HTML template with SunEditor configuration
- `js/suneditor.min.js` - SunEditor v2.47.8 library (~2.5MB)
- `css/suneditor.min.css` - SunEditor styles (~55KB)
- `lang/de.js` - German language file
- `lang/en.js` - English language file

## EditorImplementation Interface

All 7 methods are implemented:

| Method | Implementation Status | Notes |
|--------|----------------------|-------|
| `getContentType()` | ✅ Complete | Returns `ContentTypes.TEXT_HTML` |
| `getText()` | ✅ Complete | Calls `editorAPI.getText()` |
| `setText(String)` | ✅ Complete | Calls `editorAPI.setText(html)` |
| `insert(String, int)` | ✅ Complete | Inserts at cursor position |
| `getCaretPosition()` | ✅ Complete | Returns character offset |
| `setCaretPosition(int)` | ✅ Complete | Sets cursor position |
| `getSelectedText()` | ✅ Complete | Returns selected plain text |

## JavaScript Bridge API

The `window.editorAPI` object provides these methods:

- `getText()` - Get HTML content
- `setText(html)` - Set HTML content
- `insert(text, pos)` - Insert HTML at cursor
- `getCaretPosition()` - Get cursor position (character offset)
- `setCaretPosition(pos)` - Set cursor position
- `getSelectedText()` - Get selected text (plain text)
- `isReady()` - Check if editor is initialized
- `focus()` - Focus the editor

## SHEF Bug Fixes

This implementation addresses critical SHEF bugs:

### Bug 1: Font Size Mutation
**SHEF Problem:** Transforms `<font size="5">` to `<font size="12px">` (invalid HTML)
**SunEditor Solution:** Uses modern CSS, no deprecated `<font>` tags

### Bug 2: Format Loss on Line Breaks
**SHEF Problem:** Pressing ENTER changes formatting of adjacent lines
**SunEditor Solution:** Proper formatting preservation with modern editor

### Bug 3: Deprecated HTML
**SHEF Problem:** Generates `<font>` tags requiring complex cleanup
**SunEditor Solution:** Generates clean, standards-compliant HTML

## Testing Checklist

### ✅ Completed Implementation Tasks
- [x] Download SunEditor v2.47.8
- [x] Create resource directory structure
- [x] Bundle SunEditor files (JS, CSS, language files)
- [x] Create HTML template with editor configuration
- [x] Implement JavaScript bridge (window.editorAPI)
- [x] Create WebViewHtmlEditorPanel class
- [x] Implement all EditorImplementation methods
- [x] Set up JavaFX WebView in Swing JFXPanel
- [x] Implement thread-safe Java-JavaScript bridge
- [x] Create WebViewEditorDemo test application

### ⏳ Pending Testing Tasks
- [ ] Test all EditorImplementation methods
- [ ] Verify SHEF bugs are absent (font size, formatting)
- [ ] Test paste from Microsoft Word
- [ ] Verify offline operation (no network requests)
- [ ] Measure initialization time (target: ≤ 2 seconds)
- [ ] Measure memory usage (target: ≤ 100MB per instance)
- [ ] Test with complex HTML documents
- [ ] Test undo/redo functionality
- [ ] Test all toolbar features
- [ ] Test copy/paste from various sources

## How to Run the Demo

1. **Ensure JavaFX is installed:**
   ```bash
   # On Debian/Ubuntu
   sudo apt-get install openjfx
   ```

2. **Compile the classes:**
   ```bash
   cd /home/jens/dev/j-lawyer-org
   # Use the build.sh or build-fast.sh script
   ./build-fast.sh
   ```

3. **Run the demo:**
   ```bash
   java -cp <classpath> com.jdimension.jlawyer.client.editors.webview.WebViewEditorDemo
   ```

## Architecture

```
┌─────────────────────────────────────┐
│     Swing Application (EDT)         │
│  ┌───────────────────────────────┐  │
│  │  WebViewHtmlEditorPanel       │  │
│  │  (implements EditorImpl)      │  │
│  │  ┌─────────────────────────┐  │  │
│  │  │     JFXPanel            │  │  │
│  │  │  ┌──────────────────┐   │  │  │
│  │  │  │  JavaFX Thread   │   │  │  │
│  │  │  │  ┌────────────┐  │   │  │  │
│  │  │  │  │  WebView   │  │   │  │  │
│  │  │  │  │  WebEngine │  │   │  │  │
│  │  │  │  │ ┌────────┐ │  │   │  │  │
│  │  │  │  │ │SunEditor│ │  │   │  │  │
│  │  │  │  │ │  (JS)  │ │  │   │  │  │
│  │  │  │  │ └────────┘ │  │   │  │  │
│  │  │  │  └────────────┘  │   │  │  │
│  │  │  └──────────────────┘   │  │  │
│  │  └─────────────────────────┘  │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
         ↕ (Thread-safe communication)
┌─────────────────────────────────────┐
│    JavaScript (editorAPI)            │
│  - getText() / setText()             │
│  - insert() / getSelectedText()      │
│  - getCaretPosition() / setCaret...  │
└─────────────────────────────────────┘
```

## Asynchronous Initialization

**IMPORTANT:** The editor initializes asynchronously. The JavaFX WebView takes time to load SunEditor, so the editor may not be immediately visible after creation.

### Problem: Editor Not Visible Until Window Resize

If the editor is created in a background thread (e.g., via a factory method), the UI may not update when the editor becomes ready. Symptoms:
- Editor appears blank/empty after loading
- Editor becomes visible only after resizing the window
- Parent container doesn't reflect the editor's presence

### Solution: Use the Ready Callback

Set a callback that will be invoked when the editor is fully initialized:

```java
WebViewHtmlEditorPanel editorPanel = new WebViewHtmlEditorPanel();

// Set callback to update UI when editor is ready
editorPanel.setOnEditorReadyCallback(() -> {
    // Revalidate and repaint the parent container
    Container parent = editorPanel.getParent();
    if (parent != null) {
        parent.revalidate();
        parent.repaint();
    }
});

// Add editor to your UI
yourContainer.add(editorPanel);
```

**How it works:**
1. The callback runs on the Event Dispatch Thread (EDT) - safe for UI updates
2. It's called when JavaScript signals that SunEditor is fully loaded
3. `revalidate()` recalculates the layout
4. `repaint()` forces a visual refresh

**When to use:**
- ✅ Loading editor from background threads
- ✅ Using factory methods to create editors
- ✅ Dynamic UI where editors are added/removed
- ✅ Any scenario where the editor doesn't appear immediately

## Debugging Paste Issues

If paste is not working correctly, check the Java console for logs:

```
INFO [JS] === PASTE EVENT TRIGGERED ===
INFO [JS] Clipboard types: text/html, text/plain
INFO [JS] HTML length: 1234
INFO [JS] HTML sample: <p style="color: red;">...
INFO [JS] Cleaned HTML: <p style="color: red;">...
INFO [JS] ✓ SUCCESS: Content inserted with formatting preserved
```

These logs show:
- What data was in the clipboard
- How the HTML was cleaned
- Whether the insertion succeeded

## Resource Management (CRITICAL!)

**JavaFX WebView requires cleanup to prevent memory leaks!**

### Manual Cleanup (REQUIRED)

**IMPORTANT:** You must manually call `dispose()` when closing dialogs/windows containing the editor.

Automatic cleanup via `AncestorListener` was attempted but caused issues with complex UI layouts (CardLayout, TabbedPane) where panels are temporarily removed/added during normal operation. Therefore, **manual cleanup is required**.

```java
// Example: In your dialog/frame class

public class MyEmailDialog extends JDialog {
    private WebViewHtmlEditorPanel editorPanel;

    public MyEmailDialog() {
        editorPanel = new WebViewHtmlEditorPanel();
        // ... add to dialog ...

        // CRITICAL: Add window listener for cleanup
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
    }

    private void cleanup() {
        if (editorPanel != null) {
            editorPanel.dispose();  // ← CRITICAL: Prevents memory leaks!
        }
    }
}
```

### What dispose() does:

1. ✅ Stops WebEngine loading
2. ✅ Destroys SunEditor JavaScript instance
3. ✅ Clears WebView content
4. ✅ Removes all references (GC can clean up)
5. ✅ Removes Swing components

### When to call dispose():

- **ALWAYS** call `dispose()` in your window/dialog closing handler
- Before replacing one editor with another in the same container
- When explicitly freeing resources before closing the window
- In unit tests for deterministic cleanup

**Note:** `dispose()` is safe to call multiple times - it will only execute cleanup once.

## Known Limitations (PoC Phase)

1. **Caret positioning** - `setCaretPosition()` uses a simple text-walk algorithm that may not handle all HTML edge cases
2. **Insert position** - `insert(text, pos)` ignores position parameter and inserts at cursor (SunEditor limitation)
3. **No image upload** - Image upload disabled in PoC (can be enabled later)
4. **Performance not measured** - Initialization time and memory usage not yet benchmarked

## Next Steps

### Before Production Integration:
1. ✅ Complete all testing tasks (see checklist above)
2. ✅ Measure and document performance metrics
3. ✅ Test with real-world email templates
4. ✅ Create migration plan for existing code
5. ✅ Update build.xml to include resources
6. ✅ Consider creating a feature flag for gradual rollout

### Integration Path:
1. Keep `HtmlEditorPanel` (SHEF) as default
2. Add feature flag to switch between implementations
3. Update `SendEmailFrame`, `EmailTemplatesPanel`, etc. to use factory pattern
4. Gradual rollout with A/B testing
5. Remove SHEF dependency once stable

## License

SunEditor: MIT License (fully compatible with AGPLv3)
Implementation: AGPLv3 (j-lawyer.org)

## Author

j-lawyer.org development team

## References

- OpenSpec Proposal: `/openspec/changes/introduce-suneditor-poc/`
- SHEF Issues: Issue #3182 (font mutation bug, formatting loss)
- SunEditor: https://github.com/JiHong88/SunEditor

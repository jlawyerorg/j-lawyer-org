# Tasks: Introduce SunEditor Proof of Concept

## Overview

This document outlines the ordered tasks required to implement the SunEditor-based HTML editor proof of concept.

## Task List

### 1. Download and Bundle SunEditor Resources

**Description**: Download SunEditor library files and add them to the project as resources.

**Steps**:
1. Download SunEditor v2.x from https://github.com/JiHong88/SunEditor
   - suneditor.min.js (~250KB)
   - suneditor.min.css (~100KB)
2. Create resource directory: `j-lawyer-client/src/resources/html-editor/`
3. Copy SunEditor files to resource directory
4. Verify files are included in build via `nbproject/project.properties`

**Validation**:
- Files exist in `src/resources/html-editor/`
- Files are accessible via `getClass().getResource("/html-editor/suneditor.min.js")`

**Estimated Time**: 30 minutes

**Dependencies**: None

---

### 2. Create HTML Editor Template

**Description**: Create the HTML template that loads SunEditor and exposes JavaScript bridge API.

**Steps**:
1. Create `j-lawyer-client/src/resources/html-editor/editor.html`
2. Add HTML structure with:
   - DOCTYPE and meta tags
   - Link to suneditor.min.css
   - Script tag for suneditor.min.js
   - Textarea element with id="editor"
   - SunEditor initialization script
3. Configure SunEditor with:
   - Toolbar buttons matching HtmlEditorPanel features
   - Default styles (font-family: sans-serif, font-size: 12px, color: black)
   - Height/width: 100%
4. Implement `window.editorAPI` JavaScript bridge:
   - `getText()` → `suneditor.getContents(false)`
   - `setText(html)` → `suneditor.setContents(html)`
   - `getSelectedText()` → selection or full text
   - `insert(html)` → `suneditor.insertHTML(html)`
   - `setEnabled(enabled)` → `suneditor.enable()`/`disable()`
   - `requestFocus()` → `suneditor.core.focus()`
   - `undo()` → `suneditor.history.undo()`
   - `redo()` → `suneditor.history.redo()`
   - `setDefaultFontFamily(family)` → update config
   - `setDefaultFontSize(size)` → update config
   - `setDefaultFontColor(color)` → update config

**Validation**:
- editor.html loads in browser
- SunEditor renders with toolbar
- All `window.editorAPI` methods are defined
- Methods work when called from browser console

**Estimated Time**: 1-2 hours

**Dependencies**: Task 1

---

### 3. Implement WebViewHtmlEditorPanel Class

**Description**: Create the Java component that embeds WebView and implements EditorImplementation interface.

**File**: `j-lawyer-client/src/com/jdimension/jlawyer/client/mail/WebViewHtmlEditorPanel.java`

**Steps**:
1. Create class extending `JFXPanel`
2. Implement `EditorImplementation` interface
3. Add fields:
   - `private WebEngine webEngine`
   - `private volatile boolean initialized = false`
4. Implement constructor:
   - Call `Platform.runLater()`
   - Create `WebView` and get `WebEngine`
   - Load `editor.html` from classpath
   - Add listener for `Worker.State.SUCCEEDED` to set `initialized = true`
   - Create `Scene` and set on JFXPanel
5. Implement EditorImplementation methods:
   - `getContentType()` → return "text/html"
   - `getText()` → call `window.editorAPI.getText()` with CountDownLatch
   - `setText(String)` → call `window.editorAPI.setText()` in Platform.runLater
   - `getSelectedText()` → call `window.editorAPI.getSelectedText()`
   - `insert(String, int)` → call `window.editorAPI.insert()` (ignore position)
   - `setCaretPosition(int)` → no-op or log warning
   - `getCaretPosition()` → return 0
6. Implement extended methods:
   - `setEnabled(boolean)` → call `window.editorAPI.setEnabled()`
   - `requestFocus()` → call `window.editorAPI.requestFocus()`
   - `setDefaultFontFamily(String)` → call `window.editorAPI.setDefaultFontFamily()`
   - `setDefaultFontSize(String)` → call `window.editorAPI.setDefaultFontSize()`
   - `setDefaultFontColor(String)` → call `window.editorAPI.setDefaultFontColor()`
7. Add helper method:
   - `escapeForTemplateLiteral(String)` → escape backslash, backtick, dollar sign
8. Add null/initialization checks to all public methods

**Validation**:
- Class compiles without errors
- Implements all EditorImplementation methods
- No null pointer exceptions when calling methods before initialization
- Thread-safe execution

**Estimated Time**: 2-3 hours

**Dependencies**: Task 2

---

### 4. Create Demo/Test Frame

**Description**: Create a standalone demo application to test WebViewHtmlEditorPanel functionality.

**File**: `j-lawyer-client/test/com/jdimension/jlawyer/client/mail/WebViewHtmlEditorPanelDemo.java`

**Steps**:
1. Create class with `main()` method
2. Create `JFrame` with size 800x600
3. Create `WebViewHtmlEditorPanel` instance
4. Add editor to frame
5. Add test buttons:
   - "Set HTML" → calls `setText()` with sample HTML
   - "Get HTML" → calls `getText()` and prints to console
   - "Insert" → calls `insert()` with sample content
   - "Disable" → calls `setEnabled(false)`
   - "Enable" → calls `setEnabled(true)`
6. Add timer to set initial content after 2 seconds (wait for initialization)
7. Show frame

**Validation**:
- Demo runs without exceptions
- Editor displays with toolbar
- All test buttons work correctly
- Content can be typed and formatted
- HTML can be set and retrieved

**Estimated Time**: 1 hour

**Dependencies**: Task 3

---

### 5. Test Core Functionality

**Description**: Manually test all requirements from spec.md

**Test Cases**:
1. **Content Management**:
   - Set HTML with various structures (headings, paragraphs, lists, tables)
   - Get HTML and verify structure preserved
   - Verify no font size mutation (set size="5", get back size="5" not "12px")
2. **Text Selection**:
   - Select text and call `getSelectedText()`
   - Call `getSelectedText()` with no selection
3. **Content Insertion**:
   - Insert HTML at cursor position
   - Insert table, verify structure
4. **Editor State**:
   - Disable editor, verify read-only
   - Enable editor, verify editable
   - Request focus, verify cursor appears
5. **Formatting**:
   - Bold, italic, underline, strikethrough
   - Font family, size, color
   - Lists (ordered, unordered)
   - Tables (create, edit cells)
6. **Undo/Redo**:
   - Type text, undo, verify reverted
   - Redo, verify restored
7. **Paste**:
   - Copy formatted text from Word, paste, verify formatting preserved
   - Copy plain text, paste, verify line breaks preserved
8. **HTML Source**:
   - Switch to code view
   - Edit HTML
   - Switch back, verify changes applied
9. **Default Styling**:
   - Set default font family, type text, verify uses default
   - Set default size, type text, verify uses default
   - Set default color, type text, verify uses default
10. **Text Alignment** (REQUIRED):
   - Select text, click align left, verify alignment
   - Select text, click align center, verify centered
   - Select text, click align right, verify right-aligned
11. **Offline Operation** (REQUIRED):
   - Monitor network traffic during editor load
   - Verify no requests to external domains (no CDN)
   - Disconnect network, verify editor still works
12. **Base64 Image Embedding** (OPTIONAL):
   - Copy image to clipboard, paste, verify embedded as data URI
   - Use "Insert Image" button, verify base64 embedding
   - Drag-drop image, verify base64 in HTML
13. **Spell Checking** (OPTIONAL):
   - Configure browser for German language
   - Type German text with errors, verify underlined
   - Right-click, verify suggestions appear

**Validation**:
- All required test cases (1-11) pass
- Optional test cases (12-13) tested and results documented
- No exceptions thrown
- No console errors
- Formatting preserved correctly

**Estimated Time**: 3-4 hours (increased for new tests)

**Dependencies**: Task 4

---

### 6. Test No SHEF Bugs

**Description**: Verify that SHEF-specific bugs do not occur.

**Test Cases**:
1. **Font Size Mutation**:
   - Set content with `<font size="5">text</font>`
   - Call `getText()`
   - Call `setText()` with result
   - Call `getText()` again
   - Repeat 10 times
   - Verify size remains "5" (not mutated to "12px")

2. **Formatting on Enter**:
   - Set content with formatted text (bold, specific font, color)
   - Click at end of line
   - Press Enter 10 times
   - Verify original line formatting unchanged
   - Verify new lines follow standard browser behavior

**Validation**:
- No font size mutation after 10 cycles
- No formatting loss on Enter key presses
- Original content remains intact

**Estimated Time**: 1 hour

**Dependencies**: Task 5

---

### 7. Performance and Memory Testing

**Description**: Measure and validate performance characteristics.

**Test Cases**:
1. **Initialization Time**:
   - Measure time from `new WebViewHtmlEditorPanel()` to fully initialized
   - Repeat 5 times
   - Verify average ≤ 2 seconds (first instance), ≤ 1 second (subsequent)

2. **Memory Usage**:
   - Create single instance, measure memory (via profiler or Runtime.totalMemory())
   - Create 5 instances, measure total memory
   - Verify ≤ 100MB per instance

3. **Typing Latency**:
   - Type rapidly in editor
   - Verify no noticeable lag

4. **Content Update Performance**:
   - Call `setText()` with large HTML document (100KB)
   - Measure time to render
   - Verify ≤ 500ms

**Validation**:
- Performance metrics within acceptable ranges
- No memory leaks when disposing instances
- Responsive user experience

**Estimated Time**: 1-2 hours

**Dependencies**: Task 5

---

### 8. Cross-Platform Testing

**Description**: Test on different operating systems if available.

**Platforms**:
- Linux (primary)
- Windows (if available)
- macOS (if available)

**Test Cases**:
- Run demo application
- Verify editor displays correctly
- Test all formatting features
- Test paste from platform-specific apps (Word, LibreOffice)

**Validation**:
- Works on all tested platforms
- No platform-specific rendering issues
- Font rendering acceptable on all platforms

**Estimated Time**: 1-2 hours (per platform)

**Dependencies**: Task 5

**Note**: Can be parallelized if multiple platforms available

---

### 9. Documentation

**Description**: Document the implementation and findings.

**Deliverables**:
1. **Code Comments**:
   - JavaDoc for WebViewHtmlEditorPanel class
   - JavaDoc for all public methods
   - Inline comments for complex logic (thread synchronization, escaping)

2. **README**:
   - Create `docs/suneditor-poc-results.md` with:
     - Summary of PoC goals
     - Implementation approach
     - Test results (functionality, performance, memory)
     - Comparison with SHEF
     - Known limitations
     - Recommendations for next steps

3. **API Comparison**:
   - Document EditorImplementation method behavior differences
   - Note limited support for setCaretPosition/getCaretPosition
   - Note insert() position parameter ignored

**Validation**:
- All public APIs have JavaDoc
- README is clear and complete
- Known limitations are documented

**Estimated Time**: 1-2 hours

**Dependencies**: Tasks 5, 6, 7, 8

---

### 10. OpenSpec Validation

**Description**: Validate the OpenSpec proposal with strict checking.

**Steps**:
1. Run `openspec validate introduce-suneditor-poc --strict`
2. Review any warnings or errors
3. Fix validation issues
4. Repeat until validation passes

**Validation**:
- `openspec validate` passes with no errors
- All spec files are well-formed
- All requirements have scenarios

**Estimated Time**: 30 minutes

**Dependencies**: None (can run anytime after OpenSpec files created)

---

## Summary

**Total Estimated Time**: 13-19 hours (spread over 2-3 days)

**Parallelizable Tasks**:
- Task 8 (cross-platform testing) can be done by different people/machines
- Task 10 (OpenSpec validation) can be done independently

**Critical Path**:
1 → 2 → 3 → 4 → 5 → 6 → 7 → 9

**Deliverables**:
- `WebViewHtmlEditorPanel.java` (production code)
- `WebViewHtmlEditorPanelDemo.java` (test/demo code)
- `editor.html`, `suneditor.min.js`, `suneditor.min.css` (resources)
- `docs/suneditor-poc-results.md` (documentation)
- OpenSpec files (proposal.md, design.md, specs/, tasks.md)

**Success Criteria**:
All tasks completed, all validations pass, POC demonstrates feasibility of SunEditor replacement for SHEF.

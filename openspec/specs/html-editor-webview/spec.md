# html-editor-webview Specification

## Purpose
TBD - created by archiving change introduce-suneditor-poc. Update Purpose after archive.
## Requirements
### Requirement: WebView HTML Editor Component Implementation

The system SHALL provide a `WebViewHtmlEditorPanel` class that implements the `EditorImplementation` interface using JavaFX WebView and SunEditor JavaScript library.

#### Scenario: Create editor component

**Given** the j-lawyer client application is running
**When** a developer instantiates `WebViewHtmlEditorPanel`
**Then** a JFXPanel component is created
**And** JavaFX WebView is initialized within the panel
**And** SunEditor is loaded from classpath resources
**And** the editor is ready for user interaction within 2 seconds

#### Scenario: Display in Swing container

**Given** a `WebViewHtmlEditorPanel` instance exists
**When** the panel is added to a Swing container (JFrame, JPanel, etc.)
**Then** the editor displays correctly with toolbar
**And** the editor fills the available space
**And** the toolbar shows all formatting buttons (bold, italic, font, color, etc.)

---

### Requirement: Content Management

The editor SHALL support getting and setting HTML content while preserving formatting.

#### Scenario: Set HTML content

**Given** a `WebViewHtmlEditorPanel` instance is initialized
**When** `setText("<h1>Title</h1><p>Content with <b>bold</b> text</p>")` is called
**Then** the editor displays "Title" as heading 1
**And** "Content with bold text" is displayed as paragraph with bold formatting
**And** the HTML structure is preserved exactly

#### Scenario: Get HTML content

**Given** a `WebViewHtmlEditorPanel` with content
**When** `getText()` is called
**Then** it returns the current HTML content
**And** the HTML is well-formed and valid
**And** font sizes are preserved without mutation (no "5" → "12px" conversion)
**And** formatting is preserved without corruption

#### Scenario: Empty content handling

**Given** a `WebViewHtmlEditorPanel` instance is initialized
**When** `getText()` is called before any content is set
**Then** it returns an empty string or minimal valid HTML
**And** it does not throw an exception

---

### Requirement: Text Selection

The editor SHALL support retrieving selected text or full content when no selection exists.

#### Scenario: Get selected text

**Given** a `WebViewHtmlEditorPanel` with content "Hello World"
**And** the user has selected "World"
**When** `getSelectedText()` is called
**Then** it returns "World"

#### Scenario: Get text when no selection

**Given** a `WebViewHtmlEditorPanel` with content "Hello World"
**And** no text is selected
**When** `getSelectedText()` is called
**Then** it returns "Hello World" (full content)

---

### Requirement: Content Insertion

The editor SHALL support inserting HTML content at the current cursor position.

#### Scenario: Insert HTML at cursor

**Given** a `WebViewHtmlEditorPanel` with content "<p>Start</p>"
**And** the cursor is positioned after "Start"
**When** `insert(" <b>Bold</b>", -1)` is called
**Then** the content becomes "<p>Start <b>Bold</b></p>"
**And** the bold formatting is preserved

#### Scenario: Insert complex HTML

**Given** a `WebViewHtmlEditorPanel` with existing content
**When** `insert("<table><tr><td>Cell</td></tr></table>", -1)` is called
**Then** a table with one cell containing "Cell" is inserted at cursor position
**And** the table is rendered correctly

---

### Requirement: Editor State Control

The editor SHALL support enabling/disabling editing and requesting focus.

#### Scenario: Disable editor

**Given** a `WebViewHtmlEditorPanel` instance is enabled
**When** `setEnabled(false)` is called
**Then** the editor becomes read-only
**And** all toolbar buttons are disabled
**And** typing in the editor has no effect

#### Scenario: Enable editor

**Given** a `WebViewHtmlEditorPanel` instance is disabled
**When** `setEnabled(true)` is called
**Then** the editor becomes editable
**And** all toolbar buttons are enabled
**And** typing in the editor modifies content

#### Scenario: Request focus

**Given** a `WebViewHtmlEditorPanel` instance exists
**And** another component has focus
**When** `requestFocus()` is called
**Then** the editor receives keyboard focus
**And** a blinking cursor appears in the editor

---

### Requirement: Text Formatting

The editor SHALL provide standard text formatting capabilities via toolbar and keyboard shortcuts.

#### Scenario: Bold formatting

**Given** a `WebViewHtmlEditorPanel` with content
**And** the user selects some text
**When** the user clicks the Bold button or presses Ctrl+B
**Then** the selected text becomes bold
**And** the HTML contains `<b>` or `<strong>` tags
**And** the visual appearance changes to bold font weight

#### Scenario: Font family selection

**Given** a `WebViewHtmlEditorPanel` with content
**And** the user selects some text
**When** the user chooses "Arial" from the font family dropdown
**Then** the selected text is displayed in Arial font
**And** the HTML contains appropriate font-family styling

#### Scenario: Font size selection

**Given** a `WebViewHtmlEditorPanel` with content
**And** the user selects some text
**When** the user chooses "18px" from the font size dropdown
**Then** the selected text is displayed at 18px size
**And** the HTML contains appropriate font-size styling

#### Scenario: Font color selection

**Given** a `WebViewHtmlEditorPanel` with content
**And** the user selects some text
**When** the user chooses red color from the color picker
**Then** the selected text is displayed in red
**And** the HTML contains appropriate color styling

---

### Requirement: Lists and Tables

The editor SHALL support creating and editing ordered/unordered lists and tables.

#### Scenario: Create unordered list

**Given** a `WebViewHtmlEditorPanel` with cursor positioned
**When** the user clicks the unordered list button
**And** types "Item 1" and presses Enter
**And** types "Item 2"
**Then** a bulleted list with two items is created
**And** the HTML contains `<ul><li>Item 1</li><li>Item 2</li></ul>`

#### Scenario: Create table

**Given** a `WebViewHtmlEditorPanel` with cursor positioned
**When** the user clicks the table button
**And** selects a 2x2 table
**Then** a table with 2 rows and 2 columns is inserted
**And** the user can type in each cell
**And** the HTML contains proper `<table>` structure

#### Scenario: Edit table cells

**Given** a `WebViewHtmlEditorPanel` with an existing table
**When** the user clicks in a cell and types
**Then** the cell content is updated
**And** the table structure remains intact

---

### Requirement: Undo/Redo

The editor SHALL support undo and redo operations for content changes.

#### Scenario: Undo text change

**Given** a `WebViewHtmlEditorPanel` with content "Hello"
**When** the user types " World"
**And** presses Ctrl+Z or clicks the Undo button
**Then** the content returns to "Hello"

#### Scenario: Redo text change

**Given** a `WebViewHtmlEditorPanel` where an action was just undone
**When** the user presses Ctrl+Y or clicks the Redo button
**Then** the undone action is reapplied
**And** the content is restored

---

### Requirement: Paste from Word

The editor SHALL handle pasting formatted content from Microsoft Word and other rich text sources.

#### Scenario: Paste Word content

**Given** a `WebViewHtmlEditorPanel` instance
**And** the user has copied formatted text from Microsoft Word
**When** the user presses Ctrl+V or clicks Paste
**Then** the text is inserted with formatting preserved
**And** unnecessary Word-specific markup is cleaned
**And** fonts, sizes, and colors are preserved where appropriate
**And** tables from Word are converted to HTML tables

#### Scenario: Paste plain text

**Given** a `WebViewHtmlEditorPanel` instance
**And** the user has copied plain text "Line 1\nLine 2"
**When** the user pastes
**Then** the text is inserted with line breaks preserved
**And** "Line 1" and "Line 2" appear on separate lines

---

### Requirement: HTML Source Editing

The editor SHALL allow viewing and editing raw HTML source code.

#### Scenario: View HTML source

**Given** a `WebViewHtmlEditorPanel` with formatted content
**When** the user clicks the "Code View" button
**Then** the editor switches to HTML source mode
**And** displays the raw HTML markup
**And** allows editing the HTML directly

#### Scenario: Return from source view

**Given** the editor is in HTML source mode
**And** the user has edited the HTML
**When** the user clicks the "Code View" button again
**Then** the editor switches back to visual mode
**And** displays the formatted content according to the HTML
**And** changes made in source mode are reflected

---

### Requirement: Default Styling Configuration

The editor SHALL allow configuration of default font family, size, and color.

#### Scenario: Set default font family

**Given** a `WebViewHtmlEditorPanel` instance
**When** `setDefaultFontFamily("Arial")` is called
**Then** newly typed text uses Arial font by default
**And** existing content is not affected

#### Scenario: Set default font size

**Given** a `WebViewHtmlEditorPanel` instance
**When** `setDefaultFontSize("14px")` is called
**Then** newly typed text uses 14px size by default
**And** existing content is not affected

#### Scenario: Set default font color

**Given** a `WebViewHtmlEditorPanel` instance
**When** `setDefaultFontColor("#333333")` is called
**Then** newly typed text uses #333333 color by default
**And** existing content is not affected

---

### Requirement: Thread Safety

All operations SHALL be thread-safe and properly synchronized between Swing EDT and JavaFX Application Thread.

#### Scenario: Call from Swing thread

**Given** a `WebViewHtmlEditorPanel` instance
**When** `setText()` is called from the Swing Event Dispatch Thread
**Then** the operation is marshalled to JavaFX Application Thread
**And** completes without throwing IllegalStateException
**And** the content is updated correctly

#### Scenario: Concurrent operations

**Given** a `WebViewHtmlEditorPanel` instance
**When** multiple threads call `getText()` and `setText()` concurrently
**Then** all operations complete without data corruption
**And** no deadlocks occur
**And** no exceptions are thrown

---

### Requirement: Resource Management

The component SHALL properly manage resources and allow cleanup.

#### Scenario: Dispose resources

**Given** a `WebViewHtmlEditorPanel` instance is no longer needed
**When** the component is removed from its container
**Then** JavaFX resources are properly released
**And** WebView memory is freed
**And** no resource leaks occur

#### Scenario: Multiple instances

**Given** the application creates multiple `WebViewHtmlEditorPanel` instances
**When** all instances are active simultaneously
**Then** each instance operates independently
**And** memory usage scales linearly (approximately 60-90MB per instance)
**And** no performance degradation occurs

---

### Requirement: Initialization State Handling

The component SHALL handle operations gracefully before full initialization.

#### Scenario: Call before initialization complete

**Given** a `WebViewHtmlEditorPanel` is being initialized
**And** WebView has not finished loading
**When** `getText()` is called
**Then** it returns an empty string without throwing exception
**And** does not block indefinitely

#### Scenario: Initialization notification

**Given** a `WebViewHtmlEditorPanel` is being initialized
**When** WebView finishes loading and SunEditor is ready
**Then** the initialization is marked as complete
**And** subsequent operations proceed normally

---

### Requirement: Content Type Identification

The component SHALL identify itself as an HTML editor.

#### Scenario: Get content type

**Given** a `WebViewHtmlEditorPanel` instance
**When** `getContentType()` is called
**Then** it returns "text/html"

---

### Requirement: No SHEF Bugs

The component SHALL NOT exhibit the HTML corruption bugs present in SHEF.

#### Scenario: Font size preservation across open/close

**Given** a document with `<font size="5">` (18px intended)
**When** the document is loaded in `WebViewHtmlEditorPanel`
**And** the document is saved
**And** the document is reloaded
**Then** the font size remains consistent
**And** no mutation to `<font size="12px">` occurs

#### Scenario: Formatting preservation on Enter

**Given** a `WebViewHtmlEditorPanel` with formatted text
**When** the user presses Enter to create a new line
**Then** the formatting of the previous line is preserved
**And** the formatting of the new line follows standard browser behavior
**And** no unexpected formatting changes occur to adjacent lines

---

### Requirement: Offline Operation

The editor SHALL work completely offline without loading resources from external sources.

#### Scenario: No CDN requests

**Given** a `WebViewHtmlEditorPanel` is initialized
**And** network monitoring is active
**When** the editor loads
**Then** no HTTP/HTTPS requests to external domains are made
**And** all JavaScript and CSS are loaded from bundled resources
**And** the editor functions completely without internet connection

#### Scenario: Air-gapped environment

**Given** a computer with no internet connection
**When** a `WebViewHtmlEditorPanel` is created and used
**Then** all features work normally
**And** no errors related to missing external resources occur

---

### Requirement: Text Alignment

The editor SHALL support text alignment options (left, center, right, justify).

#### Scenario: Align text left

**Given** a `WebViewHtmlEditorPanel` with selected text
**When** the user clicks the "Align Left" button
**Then** the selected text is left-aligned
**And** the HTML contains appropriate text-align styling

#### Scenario: Align text center

**Given** a `WebViewHtmlEditorPanel` with selected text
**When** the user clicks the "Align Center" button
**Then** the selected text is centered
**And** the HTML contains text-align: center styling

#### Scenario: Align text right

**Given** a `WebViewHtmlEditorPanel` with selected text
**When** the user clicks the "Align Right" button
**Then** the selected text is right-aligned
**And** the HTML contains text-align: right styling

---

### Requirement: Spell Checking (Optional)

The editor SHALL support browser-based spell checking, preferably for German language.

**Note**: This is an optional feature. The requirement is satisfied if browser's built-in spell checking is accessible.

#### Scenario: Browser spell check available

**Given** a `WebViewHtmlEditorPanel` instance
**And** the browser has spell checking enabled
**When** the user types text with spelling errors
**Then** misspelled words are underlined (browser default behavior)
**And** right-click shows spelling suggestions

#### Scenario: German language support

**Given** a `WebViewHtmlEditorPanel` instance
**And** German language is configured in the browser
**When** the user types German text with errors (e.g., "Rechtsanwlt")
**Then** the misspelling is detected
**And** correct suggestions are provided (e.g., "Rechtsanwalt")

**Note**: This is a nice-to-have feature. Most modern browsers provide built-in spell checking. Dedicated spell-checking libraries are optional.

---

### Requirement: Base64 Image Embedding (Optional)

The editor SHALL support embedding images as base64 data URIs rather than file references.

**Note**: This is an optional feature. The requirement is satisfied if images can be embedded as base64 data URIs either through paste, insert, or drag-and-drop operations.

#### Scenario: Paste image from clipboard

**Given** a `WebViewHtmlEditorPanel` instance
**And** the user has copied an image to clipboard
**When** the user pastes (Ctrl+V)
**Then** the image is inserted into the editor
**And** the image is embedded as base64 data URI (`data:image/png;base64,...`)
**And** no external file reference is created

#### Scenario: Insert image via toolbar

**Given** a `WebViewHtmlEditorPanel` instance
**When** the user clicks "Insert Image" button
**And** selects an image file
**Then** the image is inserted and displayed
**And** the image is embedded as base64 data URI in the HTML
**And** the document remains self-contained (no external dependencies)

#### Scenario: Drag and drop image

**Given** a `WebViewHtmlEditorPanel` instance
**When** the user drags an image file into the editor
**Then** the image is inserted at the drop location
**And** the image is embedded as base64 data URI
**And** the HTML `<img>` tag has src="data:image/..."

**Note**: This is a nice-to-have feature. Base64 embedding creates self-contained HTML documents but increases file size.

---


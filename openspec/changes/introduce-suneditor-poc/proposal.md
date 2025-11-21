# Proposal: Introduce SunEditor HTML Editor (Proof of Concept)

## Change ID
`introduce-suneditor-poc`

## Status
Proposed

## Summary
Create a proof-of-concept implementation of a modern HTML editor component based on SunEditor JavaScript library, embedded in JavaFX WebView. This new `WebViewHtmlEditorPanel` will serve as a potential replacement for the current SHEF-based HTML editor, addressing HTML corruption bugs and providing better standards compliance.

## Motivation

The current HTML editor implementation uses the SHEF (Simple HTML Editor Framework) library, which has several significant issues:

### Current Problems with SHEF

1. **HTML Corruption**: SHEF incorrectly transforms font size attributes during edit cycles
   - Example: `<font size="5">` becomes `<font size="12px">` (mixing HTML attribute syntax with CSS values)
   - mapFontSize("12px") falls back to default instead of correct 18px mapping
   - Data corruption occurs on every document open/close cycle

2. **Formatting Loss on Line Breaks**: Pressing ENTER sometimes changes formatting of adjacent lines
   - SHEF's HTMLEditorKit inconsistently propagates formatting attributes
   - Post-processing methods (`cleanSHEFHtml`, `wrapPlainTextDivsInHtml`) cannot recover lost context
   - Adjacent lines lose their formatting when new structural elements are created

3. **Outdated HTML Practices**: Uses deprecated `<font>` tags instead of modern CSS
   - Requires extensive cleanup (`cleanSHEFHtml`) to convert to standards-compliant HTML
   - Complex post-processing increases maintenance burden

4. **Limited Modern Features**: No support for modern HTML5/CSS3 features

### Benefits of SunEditor Approach

- **Standards Compliant**: Native browser rendering with modern HTML5/CSS3 support
- **No HTML Corruption**: Eliminates font size mutation and formatting loss bugs
- **Better Paste Handling**: Improved handling of content from Word and other formatted sources
- **Active Maintenance**: SunEditor is actively maintained (2025 updates) vs. abandoned SHEF
- **Feature Rich**: Excellent table support, comprehensive formatting options out-of-the-box

## Requirements

### Must Have (Required)

1. **License Compatibility**: Editor must be compatible with AGPLv3 (e.g., MIT, BSD, Apache, GPL)
2. **Offline Operation**: Must work completely offline without loading code from CDN or external sources
3. **Paste from Word**: Must support pasting formatted content from Microsoft Word documents with formatting preserved
4. **Undo/Redo**: Must support Ctrl+Z for undo (and Ctrl+Y/Ctrl+Shift+Z for redo)
5. **Toolbar Features**: Must provide toolbar with:
   - Font family selection
   - Font size selection
   - Font color selection
   - Bold, Italic, Underline
   - Bulleted list
   - Numbered list
   - Text alignment (left, center, right)
   - Insert hyperlink
   - Insert image
   - Insert/edit table

### Nice to Have (Optional)

1. **Spell Checking**: German language spell checking support
2. **Base64 Image Embedding**: Images should be embedded as base64 data URIs rather than file references

## Scope

This is a **Proof of Concept** implementation with the following scope:

### In Scope
- Evaluate and select best JavaScript HTML editor meeting requirements above
- Create new `WebViewHtmlEditorPanel` class in j-lawyer-client
- Bundle selected editor JavaScript library as client resources
- Implement EditorImplementation interface for API compatibility
- Create HTML template with editor integration
- Implement Java-JavaScript bridge for communication
- Create demo/test frame to validate functionality
- Documentation of architecture and API
- Test all required features
- Test optional features if supported by selected editor

### Out of Scope (Future Work)
- Replacing existing SHEF usage in production code
- Migrating HtmlEditorPanel users (SendEmailFrame, etc.)
- Migrating HtmlPanel (document viewer)
- Removing SHEF library from dependencies
- Removing cleanSHEFHtml workarounds
- Production deployment and rollout

## Goals

1. **Select Best Editor**: Evaluate JavaScript HTML editors and select the one that best meets requirements
2. **Validate Technical Feasibility**: Demonstrate that the selected editor can run embedded in JavaFX WebView without a webserver
3. **Verify API Compatibility**: Confirm that EditorImplementation interface can be fully implemented
4. **Assess User Experience**: Compare editing experience with current SHEF implementation
5. **Validate Required Features**: Test all must-have features (paste from Word, Ctrl+Z, toolbar, offline operation)
6. **Evaluate Optional Features**: Test spell checking and base64 image embedding if supported
7. **Measure Performance**: Evaluate memory usage and responsiveness
8. **Identify Migration Challenges**: Document any issues or gaps for full migration planning

## Non-Goals

- Production deployment
- Complete SHEF replacement
- Breaking changes to existing code
- Performance optimization (beyond PoC validation)

## Dependencies

### Technical Dependencies
- JavaFX libraries (already present in project)
  - javafx.base.jar
  - javafx.web.jar
  - javafx.swing.jar
  - javafx.controls.jar
  - javafx.graphics.jar

- JavaScript HTML Editor (to be selected, candidates include):
  - SunEditor v2.x (MIT license, ~350KB)
  - Quill v1.x (BSD license, ~200KB)
  - TinyMCE v6.x (GPL v2+ or Commercial, ~500KB+)
  - Others meeting requirements

### Project Dependencies
- None - This is a standalone proof of concept

## Success Criteria

The PoC is considered successful if:

1. **Functional Completeness**: All EditorImplementation methods work correctly
   - getText() / setText()
   - insert()
   - getSelectedText()
   - setCaretPosition() / getCaretPosition()
   - setEnabled()
   - requestFocus()

2. **Required Features Work**: All must-have features are functional
   - Completely offline (no CDN requests observed)
   - Paste from Word preserves formatting (bold, italic, fonts, colors, tables)
   - Ctrl+Z/Ctrl+Y undo/redo works
   - Toolbar provides all required buttons and they work:
     - Font family, size, color
     - Bold, Italic, Underline
     - Bulleted and numbered lists
     - Text alignment (left, center, right)
     - Insert hyperlink, image, table

3. **Optional Features Evaluated**: Nice-to-have features tested and results documented
   - German spell checking (if available)
   - Base64 image embedding (if supported)

4. **No Regression**: Does not exhibit SHEF's bugs
   - No font size mutation on open/close cycles
   - No formatting loss when pressing ENTER

5. **Acceptable Performance**:
   - Editor loads within 2 seconds
   - No noticeable lag during typing
   - Memory usage comparable to SHEF

6. **Clear Path Forward**: Architecture allows for production migration

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| JavaFX threading complexity | High | Follow established patterns from MailContentUI.java |
| JavaScript bridge communication overhead | Medium | Implement efficient caching, minimize bridge calls |
| Caret position control limitations | Medium | Document limitations, provide workarounds |
| Memory consumption with multiple instances | Medium | Implement proper disposal, resource management |
| Browser inconsistencies across platforms | Low | Test on Linux, Windows, macOS |

## Timeline

Estimated effort: 2-3 days

- Day 1: Setup, bundling, HTML template, basic integration
- Day 2: Java bridge implementation, EditorImplementation interface
- Day 3: Demo frame, testing, documentation

## Related Work

- `MailContentUI.java`: Existing pattern for JavaFX WebView in Swing (read-only)
- `JavaFxBrowserPanel.java`: Another WebView integration example
- Bug fixes already implemented:
  - Font size mutation prevention (mapFontSize enhancement)
  - ENTER formatting preservation (wrapPlainTextDivsInHtml improvement)

## Future Considerations

If PoC is successful, future work may include:

1. **Phase 2**: Replace HtmlEditorPanel in email composition
2. **Phase 3**: Replace HtmlPanel in document viewer
3. **Phase 4**: Remove SHEF library and cleanup code
4. **Phase 5**: Enhanced features (collaborative editing, templates, etc.)

## Open Questions

- Should the PoC include a comparison mode (side-by-side with SHEF)?
- What level of automated testing is expected for the PoC?
- Should SunEditor plugins be included or just core library?

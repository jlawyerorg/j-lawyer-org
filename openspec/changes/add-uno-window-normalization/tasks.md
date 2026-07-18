## 1. Implementation
- [x] 1.1 After UNO `loadComponentFromURL`, resolve the document's container window via `XModel` → `XController` → `XFrame` → `XWindow`.
- [x] 1.2 Make the container window visible and read its current bounds.
- [x] 1.3 Treat the window as unusable when it is below a minimum size or too little of it intersects the virtual desktop.
- [x] 1.4 When unusable, move/resize the window to safe centered bounds on the active display via `setPosSize` with `PosSize.POSSIZE`.
- [x] 1.5 Retry the check briefly to defeat LibreOffice restoring the bad geometry asynchronously.
- [x] 1.6 Keep every step non-fatal so document opening and UNO close monitoring continue on failure.

## 2. Verification
- [ ] 2.1 With a corrupted profile geometry (tiny/off-screen `ooSetupFactoryWindowAttributes`), confirm the opened document window appears at usable size on screen.
- [ ] 2.2 Confirm a normally-sized restored window is left unchanged (no forced resize).
- [ ] 2.3 Confirm document opening and close detection still work when window normalization throws.
- [ ] 2.4 Compile the touched client class.

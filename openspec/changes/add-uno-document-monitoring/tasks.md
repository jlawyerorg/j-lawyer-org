## 1. Implementation
- [x] 1.1 Add a small UNO monitor/launcher helper that can start/connect to LibreOffice with an accept socket and load a document.
- [x] 1.2 Register UNO document listeners for close/unload confirmation, close notifications, and modify-event logging.
- [x] 1.3 Extend `ObservedOfficeDocument` with optional UNO close confirmation state.
- [x] 1.4 Implement hybrid close logic: UNO close/unload plus lock-file removal closes immediately; lock-only uses grace period.
- [x] 1.5 Wire LibreOffice/OpenOffice launchers to attempt UNO monitoring and fall back to existing launch behavior.
- [x] 1.6 Keep `WaitForDocumentAction` actively refreshing observer state while it waits.
- [x] 1.7 Normalize unusable LibreOffice document window bounds after UNO launch.

## 2. Verification
- [x] 2.1 Add or keep a Java harness/test proving UNO modify/save/close events are observed on a real ODT.
- [x] 2.2 Test fallback behavior when Java UNO classes are unavailable.
- [x] 2.3 Test lock-only grace behavior for #232 protection.
- [x] 2.4 Compile the touched client classes.
- [x] 2.5 Run the current Maven reactor build in Docker after seeding the local repository; note that client compilation is currently blocked by unrelated missing JavaFX, Nextcloud, and Mustang dependencies.

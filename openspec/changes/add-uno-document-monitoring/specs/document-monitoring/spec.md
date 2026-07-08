## ADDED Requirements
### Requirement: Hybrid LibreOffice Close Detection
The desktop client SHALL support optional UNO-backed close detection for LibreOffice/OpenOffice documents opened by j-lawyer while retaining lock-file monitoring as the fallback.

#### Scenario: UNO close and lock removal close immediately
- **GIVEN** a LibreOffice document opened by j-lawyer has active UNO monitoring
- **WHEN** the client observes a UNO close or unload event for that document
- **AND** the corresponding `.~lock.<filename>#` file is absent
- **THEN** the document SHALL be treated as closed without waiting for the lock-file grace period

#### Scenario: UNO close before lock removal waits for disk release
- **GIVEN** a LibreOffice document opened by j-lawyer has active UNO monitoring
- **WHEN** the client observes a UNO close or unload event for that document
- **AND** the corresponding `.~lock.<filename>#` file still exists
- **THEN** the document SHALL remain open until the lock file is absent
- **AND** once the lock file is absent, the document SHALL be treated as closed without an additional grace period

#### Scenario: Lock-only close keeps grace period
- **GIVEN** a LibreOffice document opened by j-lawyer has no active UNO close confirmation
- **WHEN** the corresponding `.~lock.<filename>#` file becomes absent
- **THEN** the document SHALL remain open until the configured lock-file grace period has elapsed
- **AND** if the lock file reappears during the grace period, the document SHALL remain open

#### Scenario: UNO failure falls back to lock monitoring
- **GIVEN** Java UNO support is unavailable or a UNO bridge fails while a LibreOffice document is open
- **WHEN** the client monitors the document lifecycle
- **THEN** the client SHALL use lock-file monitoring with the grace period
- **AND** document save/close handling SHALL continue without requiring UNO

#### Scenario: UNO launch avoids unusable restored window geometry
- **GIVEN** LibreOffice has saved a tiny or off-screen document window state
- **WHEN** j-lawyer opens a LibreOffice document with active UNO launching
- **THEN** the client SHALL make the document window visible
- **AND** if the restored document window is too small or outside the available desktop, the client SHALL move and resize it to usable bounds

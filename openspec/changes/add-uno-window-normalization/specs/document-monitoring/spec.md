## ADDED Requirements
### Requirement: Usable LibreOffice Document Window Geometry
When the desktop client opens a LibreOffice document through the UNO launch path, it SHALL
ensure the document window is visible and usable, correcting persisted geometry that would
otherwise leave the window too small or off-screen. This correction SHALL be best-effort and
SHALL NOT prevent the document from opening or UNO close monitoring from running.

#### Scenario: Tiny restored window is resized to usable bounds
- **GIVEN** LibreOffice has persisted a document window geometry smaller than the usable minimum
- **WHEN** j-lawyer opens a LibreOffice document with active UNO launching
- **THEN** the client SHALL make the document window visible
- **AND** the client SHALL move and resize the window to usable bounds on the active display

#### Scenario: Off-screen restored window is moved onto the desktop
- **GIVEN** LibreOffice has persisted a document window position that leaves too little of the window intersecting the available desktop
- **WHEN** j-lawyer opens a LibreOffice document with active UNO launching
- **THEN** the client SHALL move and resize the window to usable bounds on the active display

#### Scenario: Usable restored window is left unchanged
- **GIVEN** LibreOffice has persisted a document window geometry that is large enough and sufficiently on-screen
- **WHEN** j-lawyer opens a LibreOffice document with active UNO launching
- **THEN** the client SHALL make the document window visible
- **AND** the client SHALL NOT move or resize the window

#### Scenario: Normalization failure does not block document opening
- **GIVEN** the client cannot inspect or adjust the LibreOffice document window
- **WHEN** j-lawyer opens a LibreOffice document with active UNO launching
- **THEN** the document SHALL still open
- **AND** UNO close monitoring SHALL continue as normal

# Feldübersicht: reine Datumsangaben im installierten Client

Stand: Quellcodeprüfung vom 26.09.2026. Die Tabelle nennt die bereits gefundenen Eingaben und dient als konkrete Prüfliste für die abschließende Inventur vor der Umsetzung. „Direkt editierbar“ beschreibt die sichtbaren Swing-Feldkonfigurationen; zur endgültigen Abnahme sind die jeweiligen Bildschirm- und Speicherpfade zu testen. `txt` und `dt` sind interne Feldnamen.

## Direkt editierbare Datumsfelder: im Vorschlag enthalten

| Maske / Fachbereich | Datei (`j-lawyer-client/.../client/`) | Feld(er) |
| --- | --- | --- |
| Kontakte: Geburt, Tod, SEPA-Mandat seit | `editors/addresses/AddressPanel.java` | `txtBirthDate`, `txtDeathDate`, `txtSepaSince` |
| Kontakt-Schnellerfassung: Geburt | `editors/addresses/QuickCreateAddressDialog.java` | `txtBirthDate` |
| Buchhaltung: Buchungsdatum einer Aktenkontobuchung | `editors/files/CaseAccountEntryPanel.java` | `txtDate` (`JFormattedTextField`; Formatter gesondert prüfen) |
| Forderung: Mahnversanddatum | `editors/files/ClaimLedgerDunningPanel.java` | `txtSentDate` |
| Forderung: Titel, Ausstellungs-, Klausel- und Zustellungsdatum | `editors/files/ClaimLedgerTitleDialog.java` | `txtIssueDate`, `txtClauseDate`, `txtServiceDate` |
| Forderung: Stichtag einer Forderungsaufstellung | `editors/files/ClaimStatementDialog.java` | `txtKeyDate` |
| Mahnverfahren: Ereignisdatum und Auftragsdatum | `editors/files/DunningStatusDialog.java`, `editors/files/DunningExportDialog.java` | `txtEventDate`, `txtOrderDate` |
| Mahnverfahren: Zinsen bis (je ausgewählter Forderungsposition) | `editors/files/DunningExportDialog.java` | editierbare Tabellenspalte `tblClaims` / `COL_INTEREST_TO` |
| Vollstreckungsmaßnahme: Anordnung und Versand | `editors/files/EnforcementMeasureDialog.java` | `txtOrderedDate`, `txtDispatchedDate` |
| Drittschuldner: Zustellung und Erklärungseingang | `editors/files/ThirdPartyDebtorDialog.java` | `txtServed`, `txtDeclaration` |
| E-Mail-Konto: Ablauf des Client-Geheimnisses | `configuration/MailboxSetupDialog.java` | `txtSecretExpiry` |
| Wiedervorlagensuche: Zeitraum von/bis | `editors/files/ArchiveFileReviewsFindPanel.java` | `txtFromDate`, `txtToDate` (`JDateChooser` mit Texteingabe; Laufzeittest noch erforderlich) |

Das sind 17 identifizierte Textfelder, zwei Datums-Auswahlfelder mit Texteingabe und eine editierbare Datums-Tabellenspalte in zwölf Masken/Dateien. Die Tabellenspalte kann mehrere Werte enthalten; sie wird nicht als einzelnes Textfeld gezählt. Geburt/Tod haben zusätzlich die besonderen Regeln zu Zukunft und Reihenfolge. Für die anderen Felder gilt keine pauschale Zukunftssperre; bisherige fachliche Beschränkungen bleiben zu prüfen.

## Ausdrücklich abgrenzen (Beispiele)

| Maske / Feld | Warum kein achtstelliges Tippen im bisherigen Feld |
| --- | --- |
| Rechnung `InvoiceDialog.dtCreated`, `dtDue`, `dtFrom`, `dtTo`; Zahlung `PaymentDialog.dtCreated`, `dtTarget` | Textfelder stehen auf `setEditable(false)` und nutzen die Kalenderauswahl. Insbesondere die Rechnungsfälligkeit ist bisher kein frei beschreibbares Feld. |
| Termin-Neuanlage `NewEventPanel.txtEventBeginDateField`, `txtEventEndDateField`; Wiedervorlage per Mail/Notiz `txtReviewDateField` | Felder sind deaktiviert bzw. nicht editierbar und nutzen andere Auswahlwege. |
| `TimesheetPositionEntryPanel.txtStarted`, `txtStopped` sowie `TimesheetLogEntryPanel.txtStart`, `txtEnd` | Erfassung enthält zusätzlich eine Uhrzeit (`dd.MM.yy HH:mm`). |
| Beliebige Notizen, Namen, Aktenzeichen und andere Freitextfelder | Acht Ziffern dürfen dort unverändert Text bleiben. |

Die Liste umfasst statisch gefundene Desktop-Masken. Sie erhebt noch keinen Anspruch auf Vollständigkeit für dynamisch erzeugte Formulare, weitere Komponenten oder Sonderpfade. Diese Prüfung und der Abgleich mit den `.form`-Dateien sind in `tasks.md` als verpflichtender erster Schritt festgelegt; weitere geeignete Felder werden ergänzt, bevor die Umsetzung als vollständig gilt.

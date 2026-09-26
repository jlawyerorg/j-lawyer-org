-- Fachliche Metadaten an Aktendokumenten: Bezeichnung, Schlagworte, Eingangsdatum, Von/An und
-- Eltern-Dokument (z. B. Anlagen einer E-Mail).
--
-- Von/An verweist optional auf einen Kontakt; der Name wird mitgefuehrt, damit die
-- Dokumentliste ohne Zusatzabfrage auskommt und der Name erhalten bleibt, wenn der Kontakt
-- geloescht wird. Die Richtung ist 0 = keine, 1 = eingehend (Von), 2 = ausgehend (An).
--
-- Das Eltern-Dokument bleibt gesetzt, solange das Eltern-Dokument im Papierkorb liegt - so
-- stellt ein Wiederherstellen die Beziehung wieder her. Erst die endgueltige Loeschung loest
-- sie.

alter table case_documents add column `title` VARCHAR(500) BINARY DEFAULT NULL;
alter table case_documents add column `keywords` VARCHAR(2000) BINARY DEFAULT NULL;
alter table case_documents add column `received_date` DATETIME DEFAULT NULL;
alter table case_documents add column `correspondent_id` VARCHAR(50) BINARY DEFAULT NULL;
alter table case_documents add column `correspondent_name` VARCHAR(500) BINARY DEFAULT NULL;
alter table case_documents add column `correspondent_direction` INTEGER DEFAULT 0 NOT NULL;
alter table case_documents add column `parent_id` VARCHAR(50) BINARY DEFAULT NULL;

alter table case_documents add index `IDX_CASEDOCS_PARENT` (parent_id);
alter table case_documents add index `IDX_CASEDOCS_CORRESPONDENT` (correspondent_id);
alter table case_documents add index `IDX_CASEDOCS_RECEIVED` (received_date);

alter table case_documents add CONSTRAINT `fk_casedocs_parent` FOREIGN KEY (parent_id) REFERENCES case_documents(id) ON DELETE SET NULL;
alter table case_documents add CONSTRAINT `fk_casedocs_correspondent` FOREIGN KEY (correspondent_id) REFERENCES contacts(id) ON DELETE SET NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.49') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.49';
commit;

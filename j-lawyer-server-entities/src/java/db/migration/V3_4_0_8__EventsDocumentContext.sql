-- Add optional document reference to case_events and index/constraint
ALTER TABLE case_events ADD `document_id` VARCHAR(50) BINARY DEFAULT NULL;
ALTER TABLE case_events ADD CONSTRAINT case_events_document_fk FOREIGN KEY (document_id) REFERENCES case_documents (id) ON DELETE SET NULL;
ALTER TABLE case_events ADD INDEX `IDX_EVENTS_DOCUMENT` (document_id);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.8') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.8';
commit;

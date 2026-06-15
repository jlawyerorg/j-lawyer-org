ALTER TABLE case_tags ADD COLUMN tag_value VARCHAR(250) DEFAULT NULL;
ALTER TABLE document_tags ADD COLUMN tag_value VARCHAR(250) DEFAULT NULL;
ALTER TABLE contact_tags ADD COLUMN tag_value VARCHAR(250) DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.5.0.9') ON DUPLICATE KEY UPDATE settingValue = '3.5.0.9';
commit;
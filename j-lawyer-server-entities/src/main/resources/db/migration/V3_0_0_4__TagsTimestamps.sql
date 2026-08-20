ALTER TABLE case_tags
ADD COLUMN date_set TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE case_tags
SET date_set = CURRENT_TIMESTAMP
WHERE date_set IS NULL;

ALTER TABLE case_tags
MODIFY COLUMN date_set TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE document_tags
ADD COLUMN date_set TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE document_tags
SET date_set = CURRENT_TIMESTAMP
WHERE date_set IS NULL;

ALTER TABLE document_tags
MODIFY COLUMN date_set TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.0.0.4') ON DUPLICATE KEY UPDATE settingValue     = '3.0.0.4';
commit;
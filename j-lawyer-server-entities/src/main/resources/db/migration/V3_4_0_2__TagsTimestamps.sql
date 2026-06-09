ALTER TABLE contact_tags
ADD COLUMN date_set TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE contact_tags
SET date_set = CURRENT_TIMESTAMP
WHERE date_set IS NULL;

ALTER TABLE contact_tags
MODIFY COLUMN date_set TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.2') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.2';
commit;
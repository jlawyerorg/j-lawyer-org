ALTER TABLE case_events ADD COLUMN reminder_minutes INT NOT NULL DEFAULT -1;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.5.0.4') ON DUPLICATE KEY UPDATE settingValue     = '3.5.0.4';
commit;

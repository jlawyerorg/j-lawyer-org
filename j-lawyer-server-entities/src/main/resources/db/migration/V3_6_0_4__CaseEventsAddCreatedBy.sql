ALTER TABLE case_events ADD `created_by` VARCHAR(50) BINARY DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.4') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.4';
commit;

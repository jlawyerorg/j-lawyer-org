ALTER TABLE communication_epost MODIFY last_status_details VARCHAR(1000) BINARY;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.5') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.5';
commit;
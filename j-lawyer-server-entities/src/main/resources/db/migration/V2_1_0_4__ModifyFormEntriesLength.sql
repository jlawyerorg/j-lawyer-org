ALTER TABLE case_forms_entries MODIFY string_value MEDIUMTEXT;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.1.0.4') ON DUPLICATE KEY UPDATE settingValue     = '2.1.0.4';
commit;
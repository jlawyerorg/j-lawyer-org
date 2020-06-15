alter table case_forms_entries modify string_value VARCHAR(4096) BINARY;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.1') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.1';
commit;
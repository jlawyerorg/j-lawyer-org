alter table case_forms_entries modify placeholder VARCHAR(100) BINARY NOT NULL;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.16') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.16';
commit;
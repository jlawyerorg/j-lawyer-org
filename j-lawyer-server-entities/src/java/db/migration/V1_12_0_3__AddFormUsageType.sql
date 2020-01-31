alter table form_types add `usagetype` VARCHAR(25) BINARY;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.3') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.3';
commit;
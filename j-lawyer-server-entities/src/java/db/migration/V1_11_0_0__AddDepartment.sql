alter table contacts add `department` VARCHAR(250) BINARY;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.11.0.0') ON DUPLICATE KEY UPDATE settingValue     = '1.11.0.0';
commit;
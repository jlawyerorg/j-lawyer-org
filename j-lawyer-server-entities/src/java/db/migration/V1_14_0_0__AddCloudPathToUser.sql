alter table security_users add `cloudPath` VARCHAR(250) BINARY;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.14.0.0') ON DUPLICATE KEY UPDATE settingValue     = '1.14.0.0';
commit;
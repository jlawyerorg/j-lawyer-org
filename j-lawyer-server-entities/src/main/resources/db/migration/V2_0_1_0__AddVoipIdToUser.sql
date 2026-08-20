alter table security_users add `voipId` VARCHAR(50) BINARY;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','2.0.1.0') ON DUPLICATE KEY UPDATE settingValue     = '2.0.1.0';
commit;
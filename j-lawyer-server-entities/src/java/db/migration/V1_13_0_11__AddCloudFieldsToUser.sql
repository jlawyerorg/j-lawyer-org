alter table security_users add `cloudHost` VARCHAR(250) BINARY;
alter table security_users add `cloudPort` INTEGER NOT NULL;
update security_users set cloudPort = 443;
alter table security_users add `cloudSsl` TINYINT;
update security_users set cloudSsl = 1;
alter table security_users add `cloudUser` VARCHAR(50) BINARY;
alter table security_users add `cloudPassword` VARCHAR(50) BINARY;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.11') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.11';
commit;
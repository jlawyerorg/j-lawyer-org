alter table security_users add `emailOutPort` VARCHAR(30) BINARY;
update security_users set emailOutPort='';
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.11.0.1') ON DUPLICATE KEY UPDATE settingValue     = '1.11.0.1';
commit;
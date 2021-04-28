alter table security_users modify password VARCHAR(200) BINARY;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.14.0.1') ON DUPLICATE KEY UPDATE settingValue     = '1.14.0.1';
commit;
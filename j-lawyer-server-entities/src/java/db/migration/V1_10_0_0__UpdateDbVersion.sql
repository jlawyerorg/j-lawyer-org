insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.0') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.0';
commit;
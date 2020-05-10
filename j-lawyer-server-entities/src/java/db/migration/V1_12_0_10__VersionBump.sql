insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.10') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.10';
commit;
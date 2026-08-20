insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.19') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.19';
commit;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.23') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.23';
commit;
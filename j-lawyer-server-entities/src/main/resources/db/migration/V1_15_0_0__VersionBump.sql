insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','1.15.0.0') ON DUPLICATE KEY UPDATE settingValue     = '1.15.0.0';
commit;
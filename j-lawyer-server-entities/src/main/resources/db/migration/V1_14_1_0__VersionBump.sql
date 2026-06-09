insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','1.14.1.0') ON DUPLICATE KEY UPDATE settingValue     = '1.14.1.0';
commit;
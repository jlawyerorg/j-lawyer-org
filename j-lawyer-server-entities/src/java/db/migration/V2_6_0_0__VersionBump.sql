insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.0') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.0';
commit;
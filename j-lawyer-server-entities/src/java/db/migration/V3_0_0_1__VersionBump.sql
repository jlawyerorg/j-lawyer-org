insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.0.0.1') ON DUPLICATE KEY UPDATE settingValue     = '3.0.0.1';
commit;
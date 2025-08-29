insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.3.0.2') ON DUPLICATE KEY UPDATE settingValue     = '3.3.0.2';
commit;
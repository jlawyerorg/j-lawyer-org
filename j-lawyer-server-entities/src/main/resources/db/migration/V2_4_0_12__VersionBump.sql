insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.12') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.12';
commit;
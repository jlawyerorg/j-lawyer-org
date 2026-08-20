insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.1.0.16') ON DUPLICATE KEY UPDATE settingValue     = '2.1.0.16';
commit;
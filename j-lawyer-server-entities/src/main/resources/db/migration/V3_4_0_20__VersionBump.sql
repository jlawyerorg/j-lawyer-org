insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.20') ON DUPLICATE KEY UPDATE settingValue = '3.4.0.20';
commit;

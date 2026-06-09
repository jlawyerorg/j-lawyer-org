insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.5.0.16') ON DUPLICATE KEY UPDATE settingValue     = '3.5.0.16';
commit;
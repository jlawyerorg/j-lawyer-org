ALTER TABLE security_users MODIFY role VARCHAR(500) BINARY DEFAULT NULL;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.2.0.2') ON DUPLICATE KEY UPDATE settingValue     = '3.2.0.2';
commit;
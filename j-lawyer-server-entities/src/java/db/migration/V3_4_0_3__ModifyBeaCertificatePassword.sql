ALTER TABLE security_users MODIFY beaCertificatePassword VARCHAR(150) BINARY DEFAULT NULL;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.3') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.3';
commit;
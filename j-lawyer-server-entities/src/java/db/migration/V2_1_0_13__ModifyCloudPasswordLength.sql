alter table security_users modify cloudPassword VARCHAR(151) BINARY;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.1.0.13') ON DUPLICATE KEY UPDATE settingValue     = '2.1.0.13';
commit;
alter table calendar_setup modify cloudPassword VARCHAR(150) BINARY;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.1.0.6') ON DUPLICATE KEY UPDATE settingValue     = '2.1.0.6';
commit;
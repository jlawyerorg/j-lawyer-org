alter table contacts add `default_role` VARCHAR(250) BINARY;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.1.0.12') ON DUPLICATE KEY UPDATE settingValue     = '2.1.0.12';
commit;
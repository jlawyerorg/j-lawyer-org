alter table contacts add `state` varchar(80) BINARY DEFAULT '';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.13') ON DUPLICATE KEY UPDATE settingValue = '3.4.0.13';
commit;

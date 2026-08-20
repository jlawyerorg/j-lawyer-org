alter table contacts add `leitweg_id` varchar(50) BINARY DEFAULT '';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.13') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.13';
commit;
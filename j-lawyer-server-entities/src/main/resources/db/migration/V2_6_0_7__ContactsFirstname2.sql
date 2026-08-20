alter table contacts add `firstName2` varchar(150) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.7') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.7';
commit;
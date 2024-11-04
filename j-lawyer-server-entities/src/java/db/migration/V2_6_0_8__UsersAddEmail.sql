alter table security_users add `email` varchar(155) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.8') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.8';
commit;
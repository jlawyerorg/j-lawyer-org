alter table mailbox_setup add `scan_excludeadresses` varchar(1500) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.1.5') ON DUPLICATE KEY UPDATE settingValue     = '2.6.1.5';
commit;
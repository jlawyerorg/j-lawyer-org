alter table mailbox_setup add `tenant_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.1') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.1';
commit;
alter table mailbox_setup add `client_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
alter table mailbox_setup add `client_secret` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
alter table mailbox_setup add `msexchange` TINYINT;

update mailbox_setup set msexchange=0;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.0') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.0';
commit;
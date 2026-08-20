alter table mailbox_setup add `scan_inbox` BIT(1) DEFAULT 0;
alter table mailbox_setup add `scan_documenttags` varchar(1500) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
alter table mailbox_setup add `scan_blacklistedtypes` varchar(1500) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.1.2') ON DUPLICATE KEY UPDATE settingValue     = '2.6.1.2';
commit;
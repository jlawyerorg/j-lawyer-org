ALTER TABLE mailbox_setup MODIFY emailInPwd varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
ALTER TABLE mailbox_setup MODIFY emailOutPwd varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.16') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.16';
commit;
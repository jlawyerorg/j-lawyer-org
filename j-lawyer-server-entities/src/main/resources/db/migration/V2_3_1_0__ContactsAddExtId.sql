alter table contacts add `ext_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
alter table contacts add unique index `idx_contacts_extid` (ext_id);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.1.0') ON DUPLICATE KEY UPDATE settingValue     = '2.3.1.0';
commit;
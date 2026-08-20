ALTER TABLE contacts CHANGE ext_id ext_id_1 varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;

alter table contacts add `ext_id_2` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
alter table contacts add unique index `idx_contacts_extid2` (ext_id_2);

alter table contacts add `ext_id_3` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
alter table contacts add unique index `idx_contacts_extid3` (ext_id_3);

alter table contacts add `ext_id_4` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
alter table contacts add unique index `idx_contacts_extid4` (ext_id_4);

alter table contacts add `ext_id_5` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
alter table contacts add unique index `idx_contacts_extid5` (ext_id_5);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.16') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.16';
commit;
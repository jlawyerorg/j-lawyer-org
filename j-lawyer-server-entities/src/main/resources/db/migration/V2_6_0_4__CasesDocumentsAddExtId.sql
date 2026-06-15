alter table cases add `ext_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
alter table cases add unique index `idx_cases_extid` (ext_id);

alter table case_documents add `ext_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
alter table case_documents add unique index `idx_casedocs_extid` (ext_id);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.4') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.4';
commit;
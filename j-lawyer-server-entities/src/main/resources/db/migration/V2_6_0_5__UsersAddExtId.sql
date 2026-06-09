alter table security_users add `ext_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
alter table security_users add unique index `idx_users_extid` (ext_id);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.5') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.5';
commit;
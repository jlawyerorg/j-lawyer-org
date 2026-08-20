ALTER TABLE server_settings MODIFY settingValue varchar(2500) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.5.0.1') ON DUPLICATE KEY UPDATE settingValue     = '3.5.0.1';
commit;
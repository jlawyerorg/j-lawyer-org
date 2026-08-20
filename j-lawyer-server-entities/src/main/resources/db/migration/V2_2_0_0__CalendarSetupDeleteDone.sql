alter table calendar_setup add `delete_done` TINYINT;
update calendar_setup set delete_done=0;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.2.0.0') ON DUPLICATE KEY UPDATE settingValue     = '2.2.0.0';
commit;
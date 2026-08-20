update calendar_setup set delete_done=1;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.2.0.2') ON DUPLICATE KEY UPDATE settingValue     = '2.2.0.2';
commit;
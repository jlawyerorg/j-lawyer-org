update calendar_setup set delete_done=1;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.0') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.0';
commit;
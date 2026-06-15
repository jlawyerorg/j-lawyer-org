rename table case_followups to case_events;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','1.15.0.1') ON DUPLICATE KEY UPDATE settingValue     = '1.15.0.1';
commit;
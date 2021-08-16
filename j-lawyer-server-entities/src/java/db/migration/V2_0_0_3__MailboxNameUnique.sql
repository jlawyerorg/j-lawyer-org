alter table mailbox_setup modify display_name VARCHAR(100) BINARY unique;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.0.0.3') ON DUPLICATE KEY UPDATE settingValue     = '2.0.0.3';
commit;
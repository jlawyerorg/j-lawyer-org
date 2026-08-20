alter table mailbox_setup add `settings` MEDIUMBLOB;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.0.0.5') ON DUPLICATE KEY UPDATE settingValue     = '3.0.0.5';
commit;
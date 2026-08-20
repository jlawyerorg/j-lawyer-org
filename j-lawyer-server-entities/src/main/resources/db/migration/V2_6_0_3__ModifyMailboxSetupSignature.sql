alter table mailbox_setup modify emailSignature TEXT default null;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.3') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.3';
commit;
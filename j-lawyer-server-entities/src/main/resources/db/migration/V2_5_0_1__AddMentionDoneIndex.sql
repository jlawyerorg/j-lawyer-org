alter table instantmessage_mention add index `IDX_INSTMSG_DONE` (done);
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.5.0.1') ON DUPLICATE KEY UPDATE settingValue     = '2.5.0.1';
commit;
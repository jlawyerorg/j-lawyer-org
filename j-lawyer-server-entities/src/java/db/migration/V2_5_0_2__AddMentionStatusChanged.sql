alter table instantmessage_mention add status_changed datetime default NULL;
alter table instantmessage_mention add index `IDX_INSTMSG_MSTATUS` (status_changed);
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.5.0.2') ON DUPLICATE KEY UPDATE settingValue     = '2.5.0.2';
commit;
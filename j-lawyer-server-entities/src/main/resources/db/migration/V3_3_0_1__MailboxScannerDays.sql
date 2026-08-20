alter table mailbox_setup add `scan_days` INTEGER DEFAULT 2;
update mailbox_setup set scan_days=2;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.3.0.1') ON DUPLICATE KEY UPDATE settingValue     = '3.3.0.1';
commit;
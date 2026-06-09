alter table mailbox_setup add `scan_ignoreinline` BIT(1) DEFAULT 1;
alter table mailbox_setup add `scan_minattachmentsize` INTEGER DEFAULT 5000;

update mailbox_setup set scan_ignoreinline=1;
update mailbox_setup set scan_minattachmentsize=5000;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.1') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.1';
commit;
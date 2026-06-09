ALTER TABLE mailbox_setup
ADD COLUMN scan_defaultcase VARCHAR(50) BINARY DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.15') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.15';
commit;
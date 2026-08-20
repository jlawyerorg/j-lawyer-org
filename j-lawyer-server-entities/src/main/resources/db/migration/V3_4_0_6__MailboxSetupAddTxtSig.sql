ALTER TABLE mailbox_setup
ADD COLUMN email_signature_txt VARCHAR(3500) BINARY DEFAULT '';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.6') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.6';
commit;
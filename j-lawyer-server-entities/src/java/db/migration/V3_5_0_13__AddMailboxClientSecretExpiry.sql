ALTER TABLE mailbox_setup ADD COLUMN client_secret_expiry DATE DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.5.0.13') ON DUPLICATE KEY UPDATE settingValue = '3.5.0.13';

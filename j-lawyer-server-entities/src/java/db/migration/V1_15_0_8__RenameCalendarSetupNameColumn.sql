ALTER TABLE calendar_setup CHANGE `name` `href` VARCHAR(1024) BINARY;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','1.15.0.8') ON DUPLICATE KEY UPDATE settingValue     = '1.15.0.8';
commit;
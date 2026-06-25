ALTER TABLE security_users ADD `dropscan_scanboxes` VARCHAR(200) BINARY DEFAULT '';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.5') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.5';
commit;

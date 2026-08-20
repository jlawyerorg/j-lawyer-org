ALTER TABLE security_users ADD COLUMN dropscan_api_token VARCHAR(200) BINARY DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.dropscan.pollinginterval','15') ON DUPLICATE KEY UPDATE settingValue = '15';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.5.0.11') ON DUPLICATE KEY UPDATE settingValue = '3.5.0.11';
commit;

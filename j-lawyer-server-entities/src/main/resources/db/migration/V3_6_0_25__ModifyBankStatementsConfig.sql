ALTER TABLE config_bankstatement MODIFY number_format VARCHAR(20) BINARY NOT NULL DEFAULT '#,##0.00';
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.25') ON DUPLICATE KEY UPDATE settingValue     = '3.6.0.25';
commit;

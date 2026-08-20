ALTER TABLE contacts ADD `insurant` VARCHAR(255) DEFAULT NULL;
ALTER TABLE contacts ADD `trafficInsurant` VARCHAR(255) DEFAULT NULL;
ALTER TABLE contacts ADD `motorInsurant` VARCHAR(255) DEFAULT NULL;
ALTER TABLE contacts ADD `motorLegalProtection` BIT(1) DEFAULT 0;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.1') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.1';
commit;

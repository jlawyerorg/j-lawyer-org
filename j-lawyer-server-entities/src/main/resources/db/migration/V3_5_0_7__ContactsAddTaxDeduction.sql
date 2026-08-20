ALTER TABLE contacts ADD `tax_deduction` BIT(1) DEFAULT 0;
ALTER TABLE contacts ADD `bank_account_owner` VARCHAR(250) DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.5.0.7') ON DUPLICATE KEY UPDATE settingValue = '3.5.0.7';
commit;

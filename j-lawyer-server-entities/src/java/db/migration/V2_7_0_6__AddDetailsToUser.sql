alter table security_users add `first_name` VARCHAR(100) BINARY DEFAULT NULL;
alter table security_users add `name` VARCHAR(150) BINARY DEFAULT NULL;
alter table security_users add `company` VARCHAR(250) BINARY DEFAULT NULL;
alter table security_users add `role` VARCHAR(150) BINARY DEFAULT NULL;
alter table security_users add `street` VARCHAR(300) BINARY DEFAULT NULL;
alter table security_users add `adjunct` VARCHAR(150) BINARY DEFAULT NULL;
alter table security_users add `zip_code` VARCHAR(30) BINARY DEFAULT NULL;
alter table security_users add `city` VARCHAR(150) BINARY DEFAULT NULL;
alter table security_users add `country_code_invoicing` VARCHAR(10) BINARY DEFAULT NULL;
alter table security_users add `phone` VARCHAR(155) BINARY DEFAULT NULL;
alter table security_users add `fax` VARCHAR(155) BINARY DEFAULT NULL;
alter table security_users add `mobile` VARCHAR(155) BINARY DEFAULT NULL;
alter table security_users add `website` VARCHAR(255) BINARY DEFAULT NULL;
alter table security_users add `bank_name` VARCHAR(250) BINARY DEFAULT NULL;
alter table security_users add `bank_bic` VARCHAR(50) BINARY DEFAULT NULL;
alter table security_users add `bank_iban` VARCHAR(80) BINARY DEFAULT NULL;
alter table security_users add `tax_nr` VARCHAR(50) BINARY DEFAULT NULL;
alter table security_users add `tax_vatid` VARCHAR(50) BINARY DEFAULT NULL;


insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.6') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.6';
commit;
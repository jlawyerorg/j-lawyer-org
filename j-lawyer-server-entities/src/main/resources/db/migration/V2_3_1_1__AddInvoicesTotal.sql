alter table invoices add `total` FLOAT NOT NULL DEFAULT 0; 

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.1.1') ON DUPLICATE KEY UPDATE settingValue     = '2.3.1.1';
commit;
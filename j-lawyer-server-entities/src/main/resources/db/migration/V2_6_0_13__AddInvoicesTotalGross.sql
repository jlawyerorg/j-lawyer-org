alter table invoices add `total_gross` FLOAT NOT NULL DEFAULT 0; 

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.13') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.13';
commit;
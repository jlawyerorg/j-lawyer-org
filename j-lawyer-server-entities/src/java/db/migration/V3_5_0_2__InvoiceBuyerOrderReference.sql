alter table invoices add `buyer_order_reference` varchar(100) DEFAULT NULL;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.5.0.2') ON DUPLICATE KEY UPDATE settingValue     = '3.5.0.2';
commit;

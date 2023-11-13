alter table invoices add `last_pool_id` VARCHAR(50) BINARY;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.1.9') ON DUPLICATE KEY UPDATE settingValue     = '2.3.1.9';
commit;
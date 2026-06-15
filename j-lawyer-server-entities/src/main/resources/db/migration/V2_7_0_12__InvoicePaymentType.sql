alter table invoices add `payment_type` varchar(25) BINARY DEFAULT 'OTHER';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.12') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.12';
commit;
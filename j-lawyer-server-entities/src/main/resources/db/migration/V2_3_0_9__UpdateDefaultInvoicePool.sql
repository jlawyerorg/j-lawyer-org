update invoice_pools set schema_syntax='RGyynnnn' where id='invoicepool01';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.9') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.9';
commit;
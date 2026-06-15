insert into invoice_pools (id, display_name, schema_syntax, manual_adjust, small_business, index_start, index_last) values ('invoicepool02', 'Testnummernkreis', 'TEST-RG-yynnn', 0, 0, 0, 0);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.12') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.12';
commit;
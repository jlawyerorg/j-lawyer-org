UPDATE invoices i SET total_gross = IFNULL((SELECT SUM(units * unit_price * (100 + tax_rate) / 100) FROM invoice_positions WHERE invoice_id = i.id), 0);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.14') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.14';
commit;
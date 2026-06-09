insert into invoice_position_tpls (id, name, description, tax_rate, units, unit_price) values ('postpl01', 'Beratung 200 EUR/h', 'Beratungsleistungen nach Zeit, Stundensatz 200 EUR', 19.0, 10, 200);
insert into invoice_position_tpls (id, name, description, tax_rate, units, unit_price) values ('postpl02', 'Beratung 250 EUR/h', 'Beratungsleistungen nach Zeit, Stundensatz 250 EUR', 19.0, 10, 250);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.1.7') ON DUPLICATE KEY UPDATE settingValue     = '2.3.1.7';
commit;
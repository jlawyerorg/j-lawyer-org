insert into server_options(id,optionGroup, value) values('invoice.taxrate.0', 'invoice.taxrates','0,0');
insert into server_options(id,optionGroup, value) values('invoice.taxrate.19', 'invoice.taxrates','19,0');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.1.5') ON DUPLICATE KEY UPDATE settingValue     = '2.3.1.4';
commit;
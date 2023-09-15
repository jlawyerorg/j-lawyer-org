alter table invoices add `currency` VARCHAR(20) BINARY;

update invoices set currency='EUR';

insert into server_options(id,optionGroup, value) values('invoice.currency.eur', 'invoice.currency','EUR');
insert into server_options(id,optionGroup, value) values('invoice.currency.gbp', 'invoice.currency','GBP');
insert into server_options(id,optionGroup, value) values('invoice.currency.usd', 'invoice.currency','USD');
insert into server_options(id,optionGroup, value) values('invoice.currency.chf', 'invoice.currency','CHF');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.1.4') ON DUPLICATE KEY UPDATE settingValue     = '2.3.1.4';
commit;
insert into server_options(id,optionGroup, value) values('address.legalform.1', 'address.legalform','AG');
insert into server_options(id,optionGroup, value) values('address.legalform.2', 'address.legalform','Einzelunternehmen');
insert into server_options(id,optionGroup, value) values('address.legalform.3', 'address.legalform','e.K');
insert into server_options(id,optionGroup, value) values('address.legalform.4', 'address.legalform','GbR');
insert into server_options(id,optionGroup, value) values('address.legalform.5', 'address.legalform','OHG');
insert into server_options(id,optionGroup, value) values('address.legalform.6', 'address.legalform','KG');
insert into server_options(id,optionGroup, value) values('address.legalform.7', 'address.legalform','GmbH');
insert into server_options(id,optionGroup, value) values('address.legalform.8', 'address.legalform','UG');
insert into server_options(id,optionGroup, value) values('address.legalform.9', 'address.legalform','eG');

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.6') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.6';
commit;
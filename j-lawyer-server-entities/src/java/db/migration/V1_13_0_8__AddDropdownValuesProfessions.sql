insert into server_options(id,optionGroup, value) values('address.profession.1', 'address.profession','Rechtsanwalt');
insert into server_options(id,optionGroup, value) values('address.profession.2', 'address.profession','Rechtsanwältin');
insert into server_options(id,optionGroup, value) values('address.profession.3', 'address.profession','Gerichtsvollzieher');
insert into server_options(id,optionGroup, value) values('address.profession.4', 'address.profession','Gerichtsvollzieherin');

insert into server_options(id,optionGroup, value) values('address.role.1', 'address.role','Geschäftsführer');
insert into server_options(id,optionGroup, value) values('address.role.2', 'address.role','1. Vorsitzender');
insert into server_options(id,optionGroup, value) values('address.role.3', 'address.role','Vorstand');
insert into server_options(id,optionGroup, value) values('address.role.4', 'address.role','Geschäftsführerin');
insert into server_options(id,optionGroup, value) values('address.role.5', 'address.role','1. Vorsitzende');

insert into server_options(id,optionGroup, value) values('address.titleinaddress.1', 'address.titleinaddress','Herrn');

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.8') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.8';
commit;
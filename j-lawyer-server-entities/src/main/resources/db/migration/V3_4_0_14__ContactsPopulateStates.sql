insert into server_options(id, optionGroup, value) values('address.state.1',  'address.state', 'Baden-Württemberg');
insert into server_options(id, optionGroup, value) values('address.state.2',  'address.state', 'Bayern');
insert into server_options(id, optionGroup, value) values('address.state.3',  'address.state', 'Berlin');
insert into server_options(id, optionGroup, value) values('address.state.4',  'address.state', 'Brandenburg');
insert into server_options(id, optionGroup, value) values('address.state.5',  'address.state', 'Bremen');
insert into server_options(id, optionGroup, value) values('address.state.6',  'address.state', 'Hamburg');
insert into server_options(id, optionGroup, value) values('address.state.7',  'address.state', 'Hessen');
insert into server_options(id, optionGroup, value) values('address.state.8',  'address.state', 'Mecklenburg-Vorpommern');
insert into server_options(id, optionGroup, value) values('address.state.9',  'address.state', 'Niedersachsen');
insert into server_options(id, optionGroup, value) values('address.state.10', 'address.state', 'Nordrhein-Westfalen');
insert into server_options(id, optionGroup, value) values('address.state.11', 'address.state', 'Rheinland-Pfalz');
insert into server_options(id, optionGroup, value) values('address.state.12', 'address.state', 'Saarland');
insert into server_options(id, optionGroup, value) values('address.state.13', 'address.state', 'Sachsen');
insert into server_options(id, optionGroup, value) values('address.state.14', 'address.state', 'Sachsen-Anhalt');
insert into server_options(id, optionGroup, value) values('address.state.15', 'address.state', 'Schleswig-Holstein');
insert into server_options(id, optionGroup, value) values('address.state.16', 'address.state', 'Thüringen');


insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.14') ON DUPLICATE KEY UPDATE settingValue = '3.4.0.14';
commit;

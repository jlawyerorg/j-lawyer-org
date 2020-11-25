insert into server_options(id,optionGroup, value) values('address.degreeprefix.1', 'address.degreeprefix','Dr.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.2', 'address.degreeprefix','Dipl. Betriebswirt');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.3', 'address.degreeprefix','Dipl. Ing (TU)');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.4', 'address.degreeprefix','Dipl. Ing.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.5', 'address.degreeprefix','Dipl. Ing. (FH)');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.6', 'address.degreeprefix','Dipl. jur.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.7', 'address.degreeprefix','Dipl. Kfm.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.8', 'address.degreeprefix','Dipl.-Ing.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.9', 'address.degreeprefix','Dipl.-Jur.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.10', 'address.degreeprefix','Dipl.-Kfm.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.11', 'address.degreeprefix','Dipl.-Päd.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.12', 'address.degreeprefix','Dipl.-SV');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.13', 'address.degreeprefix','Dr. jur.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.14', 'address.degreeprefix','Dr. med.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.15', 'address.degreeprefix','Dr. med. vet.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.16', 'address.degreeprefix','Dr. med.dent.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.17', 'address.degreeprefix','Dres.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.18', 'address.degreeprefix','Dres. med.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.19', 'address.degreeprefix','Prof. Dipl.-Ing.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.20', 'address.degreeprefix','Prof. Dr.');

insert into server_options(id,optionGroup, value) values('address.degreesuffix.1', 'address.degreesuffix','MA');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.2', 'address.degreesuffix','MSc');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.3', 'address.degreesuffix','MEng');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.4', 'address.degreesuffix','BA');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.5', 'address.degreesuffix','BSc');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.6', 'address.degreesuffix','BEng');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.7', 'address.degreesuffix','PhD');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.8', 'address.degreesuffix','LLD');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.9', 'address.degreesuffix','LLM');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.10', 'address.degreesuffix','LLB');


insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.7') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.7';
commit;
insert into server_options(id,optionGroup, value) values('document.pdfstamps.k1', 'document.pdfstamps','K1');
insert into server_options(id,optionGroup, value) values('document.pdfstamps.k2', 'document.pdfstamps','K2');
insert into server_options(id,optionGroup, value) values('document.pdfstamps.k3', 'document.pdfstamps','K3');
insert into server_options(id,optionGroup, value) values('document.pdfstamps.vertr', 'document.pdfstamps','VERTRAULICH');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.2.0.7') ON DUPLICATE KEY UPDATE settingValue     = '3.2.0.7';
commit;
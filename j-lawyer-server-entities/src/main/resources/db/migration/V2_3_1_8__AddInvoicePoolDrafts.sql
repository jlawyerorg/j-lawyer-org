insert into invoice_pools (id, display_name, schema_syntax, manual_adjust, small_business, index_start, index_last) values ('ipdrafts', 'Entwürfe', 'ENTWURF-yynnn', 0, 0, 0, 0);

alter table invoice_types drop column number_required;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.1.8') ON DUPLICATE KEY UPDATE settingValue     = '2.3.1.8';
commit;
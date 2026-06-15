insert into document_name_tpls (id, display_name, schema_syntax, default_tpl) values ('dateiname-tpl','ursprünglicher Name','DATEINAME',0);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.9') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.9';
commit;
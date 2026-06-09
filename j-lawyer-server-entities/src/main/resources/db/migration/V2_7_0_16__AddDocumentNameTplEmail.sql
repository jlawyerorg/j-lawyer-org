insert into document_name_tpls (id, display_name, schema_syntax, default_tpl) values ('email-tpl','E-Mail','[yyyy]-[mm]-[dd]_[HH][MM]_DATEINAME',1);
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.16') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.16';
commit;
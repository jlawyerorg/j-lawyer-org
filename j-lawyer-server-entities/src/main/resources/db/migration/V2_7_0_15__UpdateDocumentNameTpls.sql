update document_name_tpls set schema_syntax='[yyyy]-[mm]-[dd]_DATEINAME' where id='default-tpl';
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.15') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.15';
commit;
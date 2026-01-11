alter table case_contacts add index `IDX_REFERENCE` (reference);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.10') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.10';
commit;
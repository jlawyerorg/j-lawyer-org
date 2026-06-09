alter table case_documents add `document_type` integer default 10;

update case_documents set document_type=10;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.1.2') ON DUPLICATE KEY UPDATE settingValue     = '2.3.1.2';
commit;
alter table case_documents add `version` integer default 1;

update case_documents set version=1;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.2') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.2';
commit;
alter table case_documents add `highlight1` integer not null default -1;
alter table case_documents add `highlight2` integer not null default -1;

update case_documents set highlight1=-1;
update case_documents set highlight2=-1;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.3') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.3';
commit;
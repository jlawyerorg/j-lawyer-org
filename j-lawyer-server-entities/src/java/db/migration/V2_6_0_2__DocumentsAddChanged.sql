ALTER TABLE case_documents ADD `date_changed` datetime default null;
alter table case_documents add index `IDX_DOCDATECHANGED` (date_changed);
update case_documents set date_changed=creationDate;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.2') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.2';
commit;
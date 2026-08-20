alter table case_documents add index `idx_case_documents_deleted` (deleted);

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.14.0.6') ON DUPLICATE KEY UPDATE settingValue     = '1.14.0.6';
commit;
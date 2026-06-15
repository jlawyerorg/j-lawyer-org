alter table case_documents add `deleted_by` VARCHAR(250) BINARY;
alter table case_documents add `deletion_date` DATETIME;
alter table case_documents add `deleted` TINYINT DEFAULT 0;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.14.0.5') ON DUPLICATE KEY UPDATE settingValue     = '1.14.0.5';
commit;
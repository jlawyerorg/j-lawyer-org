alter table contacts add index `IDX_CONTACTS_DEPARTMENT` (department);
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.1') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.1';
commit;
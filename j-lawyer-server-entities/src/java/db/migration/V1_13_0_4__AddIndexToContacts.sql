alter table contacts add index `IDX_CONTACTS_DISTRICT` (district);
alter table contacts add index `IDX_CONTACTS_BIRTHNAME` (birthName);
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.4') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.4';
commit;
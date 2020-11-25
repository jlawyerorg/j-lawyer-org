alter table contacts add index `IDX_CONTACTS_ZIPCODE` (zipCode);
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.9') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.9';
commit;
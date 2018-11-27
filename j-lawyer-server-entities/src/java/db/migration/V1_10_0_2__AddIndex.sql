alter table ArchiveFileBean add index `IDX_ARCHIVEFILEBEAN_SUBJECTFIELD` (subjectField);
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.2') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.2';
commit;
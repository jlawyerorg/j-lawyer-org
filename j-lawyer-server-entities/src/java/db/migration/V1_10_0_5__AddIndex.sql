alter table contacts add index `IDX_BEASAFEID` (beaSafeId);

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.5') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.5';
commit;
use jlawyerdb;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','01810') ON DUPLICATE KEY UPDATE settingValue     = '01810';

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.backup.maxbackups','2');

alter table AddressBean MODIFY `custom3` VARCHAR(500) BINARY;
alter table ArchiveFileBean MODIFY `custom3` VARCHAR(500) BINARY;
alter table ArchiveFileHistoryBean MODIFY `changeDescription` VARCHAR(500) BINARY;

alter table AddressBean add index `IDX_ADDRESS_CUSTOM1` (custom1);
alter table AddressBean add index `IDX_ADDRESS_CUSTOM2` (custom2);
alter table AddressBean add index `IDX_ADDRESS_CUSTOM3` (custom3);

alter table ArchiveFileBean add index `IDX_CASE_CUSTOM1` (custom1);
alter table ArchiveFileBean add index `IDX_CASE_CUSTOM2` (custom2);
alter table ArchiveFileBean add index `IDX_CASE_CUSTOM3` (custom3);


commit;
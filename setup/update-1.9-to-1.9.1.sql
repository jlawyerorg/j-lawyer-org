use jlawyerdb;

alter table ArchiveFileAddressesBean add `reference` VARCHAR(50) BINARY;
# Sachbearbeiter
alter table ArchiveFileAddressesBean add `contact` VARCHAR(50) BINARY;
alter table ArchiveFileAddressesBean add `custom1` VARCHAR(250) BINARY;
alter table ArchiveFileAddressesBean add `custom2` VARCHAR(250) BINARY;
alter table ArchiveFileAddressesBean add `custom3` VARCHAR(250) BINARY;

alter table AddressBean add `encryptionPwd` VARCHAR(30) BINARY;

alter table ArchiveFileDocumentsBean add `favorite` TINYINT default 0;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','01910') ON DUPLICATE KEY UPDATE settingValue     = '01910';

commit;
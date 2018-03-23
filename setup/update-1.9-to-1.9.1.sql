use jlawyerdb;

alter table ArchiveFileAddressesBean add `reference` VARCHAR(50) BINARY;
# Sachbearbeiter
alter table ArchiveFileAddressesBean add `contact` VARCHAR(50) BINARY;
alter table ArchiveFileAddressesBean add `custom1` VARCHAR(250) BINARY;
alter table ArchiveFileAddressesBean add `custom2` VARCHAR(250) BINARY;
alter table ArchiveFileAddressesBean add `custom3` VARCHAR(250) BINARY;


commit;
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

delete from ServerSettingsBean where settingKey='jlawyer.server.backup.maxbackups';

CREATE TABLE campaign (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(250) BINARY, 
CONSTRAINT `pk_campaign` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE campaign_addresses (
`id` VARCHAR(50) BINARY NOT NULL, 
`campaignKey` VARCHAR(50) BINARY, 
`addressKey` VARCHAR(250) BINARY, 
CONSTRAINT `pk_campaignaddresses` PRIMARY KEY (`id`),
FOREIGN KEY (campaignKey) REFERENCES campaign(id) ON DELETE CASCADE,
FOREIGN KEY (addressKey) REFERENCES AddressBean(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table campaign_addresses add index `IDX_CAMPAIGNADDRESSES_CAMPAIGNKEY` (campaignKey);
alter table campaign_addresses add index `IDX_CAMPAIGNADDRESSES_ADDRESSKEY` (addressKey);

alter table ArchiveFileBean MODIFY `reason` VARCHAR(250) BINARY;

commit;
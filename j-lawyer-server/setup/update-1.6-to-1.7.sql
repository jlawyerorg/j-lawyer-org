use jlawyerdb;

CREATE TABLE ArchiveFileTagsBean (
`id` VARCHAR(250) BINARY NOT NULL, 
`archiveFileKey` VARCHAR(250) BINARY, 
`tagName` VARCHAR(250) BINARY, 
CONSTRAINT `pk_ArchiveFileTagsBean` PRIMARY KEY (`id`),
FOREIGN KEY (archiveFileKey) REFERENCES ArchiveFileBean(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table ArchiveFileTagsBean add index `IDX_TAGNAME` (tagName);
alter table ArchiveFileTagsBean add index `IDX_ARCHIVEFILEKEY` (archiveFileKey);

alter table ArchiveFileTagsBean add index `IDX_ARCHIVEFILEKEY_TAGNAME` (archiveFileKey, tagName);
alter table ArchiveFileTagsBean add unique index `IDX_ARCHIVEFILEKEY_TAGNAME_UQ` (archiveFileKey, tagName);

insert into AppOptionGroupBean (id, optionGroup, value) values ('3', 'archiveFile.tags', 'Markierung 1');
insert into AppOptionGroupBean (id, optionGroup, value) values ('4', 'archiveFile.tags', 'Markierung 2');

alter table AddressBean add `title` VARCHAR(50) BINARY;

insert into AppOptionGroupBean (id, optionGroup, value) values ('5', 'address.title', 'Herr');
insert into AppOptionGroupBean (id, optionGroup, value) values ('6', 'address.title', 'Frau');
insert into AppOptionGroupBean (id, optionGroup, value) values ('7', 'address.title', 'Dr.');

alter table ArchiveFileReviewsBean add `reviewType` INTEGER NOT NULL;

# default: all review are just follow-ups
update ArchiveFileReviewsBean set reviewType = 10;

alter table ArchiveFileReviewsBean add index `IDX_REVTYPE` (reviewType);

CREATE TABLE AddressTagsBean (
`id` VARCHAR(250) BINARY NOT NULL, 
`addressKey` VARCHAR(250) BINARY, 
`tagName` VARCHAR(250) BINARY, 
CONSTRAINT `pk_AddressTagsBean` PRIMARY KEY (`id`),
FOREIGN KEY (addressKey) REFERENCES AddressBean(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table AddressTagsBean add index `IDX_TAGNAME` (tagName);
alter table AddressTagsBean add index `IDX_ADDRESSKEY` (addressKey);

alter table AddressTagsBean add index `IDX_ADDRESSKEY_TAGNAME` (addressKey, tagName);
alter table AddressTagsBean add unique index `IDX_ADDRESSKEY_TAGNAME_UQ` (addressKey, tagName);

insert into AppOptionGroupBean (id, optionGroup, value) values ('8', 'address.tags', 'Gericht');
insert into AppOptionGroupBean (id, optionGroup, value) values ('9', 'address.tags', 'Versicherung');

alter table AppUserBean add `emailInSsl` TINYINT;
alter table AppUserBean add `emailOutSsl` TINYINT;

update AppUserBean set emailInSsl = 0;
update AppUserBean set emailOutSsl = 0;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.backup.dbport','3306');

# potential speed up of searchReviews
alter table ArchiveFileReviewsBean add index `IDX_REVDATE_DONE` (reviewDate, done);

alter table AddressBean add `custom1` VARCHAR(250) BINARY;
alter table AddressBean add `custom2` VARCHAR(250) BINARY;
alter table AddressBean add `custom3` VARCHAR(250) BINARY;
alter table AddressBean add `birthDate` VARCHAR(50) BINARY;

alter table ArchiveFileBean add `custom1` VARCHAR(250) BINARY;
alter table ArchiveFileBean add `custom2` VARCHAR(250) BINARY;
alter table ArchiveFileBean add `custom3` VARCHAR(250) BINARY;

insert into ServerSettingsBean(settingKey, settingValue) values('data.customfields.address.1','Eigenes Feld 1');
insert into ServerSettingsBean(settingKey, settingValue) values('data.customfields.address.2','Eigenes Feld 2');
insert into ServerSettingsBean(settingKey, settingValue) values('data.customfields.address.3','Eigenes Feld 3');

insert into ServerSettingsBean(settingKey, settingValue) values('data.customfields.archivefile.1','Eigenes Feld 1');
insert into ServerSettingsBean(settingKey, settingValue) values('data.customfields.archivefile.2','Eigenes Feld 2');
insert into ServerSettingsBean(settingKey, settingValue) values('data.customfields.archivefile.3','Eigenes Feld 3');

# fixes a discrepancy between AppUserBean.princialId and ArchiveFileBean.lawyer
alter table ArchiveFileBean MODIFY `lawyer` VARCHAR(50) BINARY;
alter table ArchiveFileBean MODIFY `assistant` VARCHAR(50) BINARY;

# live system update to here Feb 10 2015


commit;
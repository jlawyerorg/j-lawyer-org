drop database if exists jlawyerdb;

create database jlawyerdb;

use jlawyerdb;

CREATE TABLE AddressBean (
`id` VARCHAR(250) BINARY NOT NULL, 
`firstName` VARCHAR(250) BINARY, 
`name` VARCHAR(250) BINARY, 
`company` VARCHAR(250) BINARY, 
`legalProtection` TINYINT NOT NULL, 
`insuranceNumber` VARCHAR(250) BINARY, 
`salutation` VARCHAR(250) BINARY, 
`complimentaryClose` VARCHAR(250) BINARY, 
`street` VARCHAR(250) BINARY, 
`country` VARCHAR(250) BINARY, 
`zipCode` VARCHAR(250) BINARY, 
`city` VARCHAR(250) BINARY, 
`phone` VARCHAR(250) BINARY, 
`fax` VARCHAR(250) BINARY, 
`bankName` VARCHAR(250) BINARY, 
`bankCode` VARCHAR(250) BINARY, 
`bankAccount` VARCHAR(250) BINARY, 
`email` VARCHAR(250) BINARY, 
`website` VARCHAR(250) BINARY, 
CONSTRAINT `pk_AddressBean` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table AddressBean add `creator` VARCHAR(250) BINARY;
alter table AddressBean add `lastModifier` VARCHAR(250) BINARY;
alter table AddressBean add `creationDate` DATETIME;
alter table AddressBean add `modificationDate` DATETIME;

alter table AddressBean add index `IDX_FIRSTNAME` (firstName);
alter table AddressBean add index `IDX_NAME` (name);
alter table AddressBean add index `IDX_COMPANY` (company);

CREATE TABLE AppUserBean (
`principalId` VARCHAR(50) BINARY NOT NULL, 
`password` VARCHAR(50) BINARY, 
CONSTRAINT `pk_AppUserBean` PRIMARY KEY (`principalId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE AppRoleBean (
`id` VARCHAR(250) BINARY NOT NULL, 
`principalId` VARCHAR(250) BINARY, 
`role` VARCHAR(250) BINARY, 
`roleGroup` VARCHAR(250) BINARY, 
CONSTRAINT `pk_AppRolesBean` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE AppOptionGroupBean (
`id` VARCHAR(250) BINARY NOT NULL, 
`optionGroup` VARCHAR(250) BINARY, 
`value` VARCHAR(250) BINARY, 
CONSTRAINT `pk_AppOptionGroupBean` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table AppOptionGroupBean add index `IDX_OPTIONGROUP` (optionGroup);

CREATE TABLE BankDataBean (
`id` VARCHAR(250) BINARY NOT NULL, 
`name` VARCHAR(250) BINARY, 
`bankCode` VARCHAR(250) BINARY, 
CONSTRAINT `pk_BankDataBean` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table BankDataBean add index `IDX_NAME` (name);
alter table BankDataBean add index `IDX_BANKCODE` (bankCode);

CREATE TABLE CityDataBean (
`id` VARCHAR(250) BINARY NOT NULL, 
`city` VARCHAR(250) BINARY, 
`zipCode` VARCHAR(250) BINARY, 
CONSTRAINT `pk_CityDataBean` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table CityDataBean add index `IDX_CITY` (city);
alter table CityDataBean add index `IDX_ZIPCODE` (zipCode);

CREATE TABLE ArchiveFileBean (
`id` VARCHAR(250) BINARY NOT NULL, 
`name` VARCHAR(250) BINARY, 
`fileNumber` VARCHAR(250) BINARY, 
`claimNumber` VARCHAR(250) BINARY, 
`claimValue` FLOAT NOT NULL, 
`archived` TINYINT NOT NULL, 
`dictateSign` VARCHAR(250) BINARY, 
`notice` VARCHAR(250) BINARY, 
CONSTRAINT `pk_ArchiveFileBean` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table ArchiveFileBean add index `IDX_FILENUMBER` (fileNumber);
alter table ArchiveFileBean add index `IDX_NAME` (name);
alter table ArchiveFileBean add index `IDX_ARCHIVED` (archived);

CREATE TABLE ArchiveFileReviewsBean (
`id` VARCHAR(250) BINARY NOT NULL, 
`archiveFileKey` VARCHAR(250) BINARY, 
`reviewReason` VARCHAR(250) BINARY, 
`reviewDate` DATETIME, 
`done` TINYINT NOT NULL, 
CONSTRAINT `pk_ArchiveFileReviewsBean` PRIMARY KEY (`id`),
FOREIGN KEY (archiveFileKey) REFERENCES ArchiveFileBean(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table ArchiveFileReviewsBean add index `IDX_DONE` (done);
alter table ArchiveFileReviewsBean add index `IDX_REVDATE` (reviewDate);
alter table ArchiveFileReviewsBean add index `IDX_ARCHIVEFILEKEY` (archiveFileKey);

CREATE TABLE ArchiveFileAddressesBean (
`id` VARCHAR(250) BINARY NOT NULL, 
`archiveFileKey` VARCHAR(250) BINARY, 
`addressKey` VARCHAR(250) BINARY, 
`referenceType` INTEGER NOT NULL, 
CONSTRAINT `pk_ArchiveFileAddressesBean` PRIMARY KEY (`id`),
FOREIGN KEY (archiveFileKey) REFERENCES ArchiveFileBean(id) ON DELETE CASCADE, 
FOREIGN KEY (addressKey) REFERENCES AddressBean(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table ArchiveFileAddressesBean add index `IDX_ARCHIVEFILEKEY_REFERENCETYPE` (archiveFileKey, referenceType);
alter table ArchiveFileAddressesBean add index `IDX_ARCHIVEFILEKEY` (archiveFileKey);

CREATE TABLE ArchiveFileHistoryBean (
`id` VARCHAR(250) BINARY NOT NULL, 
`principal` VARCHAR(250) BINARY, 
`archiveFileKey` VARCHAR(250) BINARY, 
`changeDescription` VARCHAR(250) BINARY, 
`changeDate` DATETIME, 
CONSTRAINT `pk_ArchiveFileHistoryBean` PRIMARY KEY (id), 
FOREIGN KEY (archiveFileKey) REFERENCES ArchiveFileBean(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table ArchiveFileHistoryBean add index `IDX_ARCHIVEFILEKEY` (archiveFileKey);
alter table ArchiveFileHistoryBean add index `IDX_PRINCIPAL` (principal);
alter table ArchiveFileHistoryBean add index `IDX_CHANGEDATE` (changeDate);

CREATE TABLE ArchiveFileDocumentsBean (
`id` VARCHAR(250) BINARY NOT NULL, 
`name` VARCHAR(250) BINARY, 
`archiveFileKey` VARCHAR(250) BINARY, 
`creationDate` DATETIME, 
`content` MEDIUMBLOB, 
CONSTRAINT `pk_ArchiveFileDocumentsBean` PRIMARY KEY (id), 
FOREIGN KEY (archiveFileKey) REFERENCES ArchiveFileBean(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table ArchiveFileDocumentsBean add index `IDX_ARCHIVEFILEKEY` (archiveFileKey);

insert into AppUserBean(principalId, password) values('admin','a');

insert into AppRoleBean(id, principalid, role, roleGroup) values('15','admin','adminRole','Roles');

insert into AppRoleBean(id, principalid, role, roleGroup) values('1','admin','loginRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('9','admin','importRole','Roles');

insert into AppRoleBean(id, principalid, role, roleGroup) values('2','admin','createAddressRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('3','admin','readAddressRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('4','admin','writeAddressRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('13','admin','removeAddressRole','Roles');

insert into AppRoleBean(id, principalid, role, roleGroup) values('5','admin','createOptionGroupRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('6','admin','readOptionGroupRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('7','admin','writeOptionGroupRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('8','admin','deleteOptionGroupRole','Roles');

insert into AppRoleBean(id, principalid, role, roleGroup) values('10','admin','createArchiveFileRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('11','admin','readArchiveFileRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('12','admin','writeArchiveFileRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('14','admin','removeArchiveFileRole','Roles');


insert into AppUserBean(principalId, password) values('user','u');

insert into AppRoleBean(id, principalid, role, roleGroup) values('31','user','loginRole','Roles');

insert into AppRoleBean(id, principalid, role, roleGroup) values('32','user','createAddressRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('33','user','readAddressRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('34','user','writeAddressRole','Roles');

insert into AppRoleBean(id, principalid, role, roleGroup) values('35','user','createOptionGroupRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('36','user','readOptionGroupRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('37','user','writeOptionGroupRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('38','user','deleteOptionGroupRole','Roles');

insert into AppRoleBean(id, principalid, role, roleGroup) values('40','user','createArchiveFileRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('41','user','readArchiveFileRole','Roles');
insert into AppRoleBean(id, principalid, role, roleGroup) values('42','user','writeArchiveFileRole','Roles');

##############################################
# introduced with change from 1.1 to 1.2
##############################################

alter table ArchiveFileBean add `subjectField` VARCHAR(100) BINARY;
alter table ArchiveFileBean add `reason` VARCHAR(100) BINARY;
alter table ArchiveFileBean add `lawyer` VARCHAR(15) BINARY;
alter table ArchiveFileBean add `assistant` VARCHAR(15) BINARY;

alter table ArchiveFileDocumentsBean add `dictateSign` VARCHAR(15) BINARY;
alter table ArchiveFileBean drop `dictateSign`;

alter table AppUserBean add `lawyer` TINYINT;
update AppUserBean set lawyer=1;

insert into AppOptionGroupBean (id, optionGroup, value) values ('1', 'archiveFile.subjectField', 'Familienrecht');
insert into AppOptionGroupBean (id, optionGroup, value) values ('2', 'archiveFile.subjectField', 'Verkehrsrecht');

##############################################
# introduced with change from 1.3 to 1.4
##############################################

alter table AppUserBean add `countryCode` VARCHAR(10) BINARY;
alter table AppUserBean add `areaCode` VARCHAR(10) BINARY;

update AppUserBean set countryCode='DE';

CREATE TABLE ServerSettingsBean (
`settingKey` VARCHAR(150) BINARY NOT NULL, 
`settingValue` VARCHAR(250) BINARY, 
CONSTRAINT `pk_ServerSettingsBean` PRIMARY KEY (`settingKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table AppUserBean add `emailAddress` VARCHAR(80) BINARY;
alter table AppUserBean add `emailInType` VARCHAR(15) BINARY;
alter table AppUserBean add `emailInServer` VARCHAR(80) BINARY;
alter table AppUserBean add `emailInUser` VARCHAR(30) BINARY;
alter table AppUserBean add `emailInPwd` VARCHAR(30) BINARY;
alter table AppUserBean add `emailOutServer` VARCHAR(80) BINARY;
alter table AppUserBean add `emailOutUser` VARCHAR(30) BINARY;
alter table AppUserBean add `emailOutPwd` VARCHAR(30) BINARY;
alter table AppUserBean add `emailSenderName` VARCHAR(150) BINARY;
alter table AppUserBean add `emailSignature` VARCHAR(600) BINARY;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.cpuwarn','80');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.cpuerror','90');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.memwarn','80');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.memerror','90');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.diskwarn','80');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.diskerror','90');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.vmwarn','75');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.vmerror','85');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.notify','off');

##############################################
# introduced with change from 1.4 to 1.5
##############################################

alter table AppUserBean MODIFY `emailSignature` VARCHAR(3500) BINARY;
alter table AppUserBean MODIFY `emailInPwd` VARCHAR(75) BINARY;
alter table AppUserBean MODIFY `emailOutPwd` VARCHAR(75) BINARY;
alter table AppUserBean MODIFY `emailInUser` VARCHAR(75) BINARY;
alter table AppUserBean MODIFY `emailOutUser` VARCHAR(75) BINARY;

alter table AddressBean add `mobile` VARCHAR(250) BINARY;

alter table ArchiveFileReviewsBean add `assignee` VARCHAR(250) BINARY;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.voip.voipmode','off');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.voip.voipendpoint','https://samurai.sipgate.net/RPC2');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.voip.voipuser','<sipgate-nutzerkennung>');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.voip.voippwd','<sipgate-passwort>');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.voip.sipprefix','sip:');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.voip.sipsuffix','@sipgate.de');


CREATE TABLE FaxQueueBean (
`sessionId` VARCHAR(100) BINARY NOT NULL, 
`archiveFileKey` VARCHAR(250) BINARY, 
`localUri` VARCHAR(50) BINARY,
`remoteUri` VARCHAR(50) BINARY,
`remoteName` VARCHAR(350) BINARY,
`pdfName` VARCHAR(260) BINARY, 
`pdfQueueFile` VARCHAR(260) BINARY, 
`sentDate` DATETIME, 
`lastStatusDate` DATETIME,
`lastStatus` VARCHAR(30) BINARY, 
`sentBy` VARCHAR(50) BINARY, 
FOREIGN KEY (archiveFileKey) REFERENCES ArchiveFileBean(id) ON DELETE CASCADE, 
CONSTRAINT `pk_FaxQueueBean` PRIMARY KEY (`sessionId`) 

) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table FaxQueueBean add index `IDX_ARCHIVEFILEKEY` (archiveFileKey);
alter table FaxQueueBean add index `IDX_LASTSTATUS` (lastStatus);


##############################################
# introduced with change from 1.5 to 1.6
##############################################

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.drebis.drebismode','off');

# test system: https://test.drebis.de/KanzleiWebservice/services/KanzleiService
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.drebis.drebisendpoint','https://www.drebis.de/KanzleiWebservice/services/KanzleiService');

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.drebis.localuser','<kanzlei-nutzerkennung>');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.drebis.localpwd','<kanzlei-passwort>');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.drebis.techuser','8jqgNGifrKlLwKQpfaCwhA==');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.drebis.techpwd','NMHBVk3R4W8G0LCxmNywsA==');


alter table AddressBean add `insuranceName` VARCHAR(250) BINARY;

alter table AddressBean add `trafficInsuranceName` VARCHAR(250) BINARY;
alter table AddressBean add `trafficInsuranceNumber` VARCHAR(250) BINARY;
alter table AddressBean add `trafficLegalProtection` TINYINT NOT NULL;

alter table ArchiveFileBean add index `IDX_REASON` (reason);

alter table ArchiveFileBean MODIFY `notice` VARCHAR(2500) BINARY;

alter table AddressBean add `motorInsuranceName` VARCHAR(250) BINARY;
alter table AddressBean add `motorInsuranceNumber` VARCHAR(250) BINARY;


##############################################
# introduced with change from 1.6 to 1.7
##############################################

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


##############################################
# introduced with change from 1.7 to 1.8
##############################################

alter table AppUserBean add `emailStartTls` TINYINT;
update AppUserBean set emailStartTls = 0;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.documents.waitforprocess.mac','4000');

alter table AppUserBean add `settings` MEDIUMBLOB;

# version for release 1.8.0, 1.8.1 would be database version 01810
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','01800');

##############################################
# introduced with change from 1.8 to 1.8.1
##############################################

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


##############################################
# introduced with change from 1.8.1 to 1.9
##############################################


alter table ArchiveFileDocumentsBean add `size` BIGINT;
update ArchiveFileDocumentsBean set size = -1;

alter table AppUserBean add `beaCertificate` MEDIUMBLOB;
alter table AppUserBean add `beaCertificateAutoLogin` TINYINT;
update AppUserBean set beaCertificateAutoLogin = 1;
alter table AppUserBean add `beaCertificatePassword` VARCHAR(50) BINARY;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.bea.beamode','off');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.bea.beaendpoint','https://schulung-ksw.bea-brak.de');

alter table AddressBean add `beaSafeId` VARCHAR(100) BINARY;

alter table AddressBean add index `IDX_EMAIL` (email);


# 1.9 pilot users until here
alter table AddressBean add index `IDX_PHONE` (phone);
alter table AddressBean add index `IDX_MOBILE` (mobile);

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','01900') ON DUPLICATE KEY UPDATE settingValue     = '01900';


##############################################
# introduced with change from 1.9 to 1.9.1
##############################################

alter table ArchiveFileAddressesBean add `reference` VARCHAR(250) BINARY;
# Sachbearbeiter
alter table ArchiveFileAddressesBean add `contact` VARCHAR(250) BINARY;
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

##############################################
# From this version on Flyway will handle the 
# migrations
##############################################

commit;


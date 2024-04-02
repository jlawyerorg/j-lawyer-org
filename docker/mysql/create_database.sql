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
# Introduced with change from 1.9.1 to 1.10
# This is just for documentation purposes.
# It MUST NOT be executed because starting 
# with this version Flyway will handle the 
# migrations
##############################################

-- insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.3') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.3';
-- 
-- alter table ArchiveFileBean add index `IDX_ARCHIVEFILEBEAN_SUBJECTFIELD` (subjectField);
-- 
-- rename table AddressBean to contacts;
-- rename table AddressTagsBean to contact_tags;
-- 
-- rename table AppOptionGroupBean to server_options;
-- rename table ServerSettingsBean to server_settings;
-- -- required for j-lawyer.BOX
-- CREATE VIEW ServerSettingsBean AS SELECT * FROM server_settings;
-- 
-- -- views are required for the login module whose configuration will not change
-- rename table AppRoleBean to security_roles;
-- CREATE VIEW AppRoleBean AS SELECT * FROM security_roles;
-- rename table AppUserBean to security_users;
-- CREATE VIEW AppUserBean AS SELECT * FROM security_users;
-- 
-- rename table ArchiveFileBean to cases;
-- rename table ArchiveFileAddressesBean to case_contacts;
-- rename table ArchiveFileDocumentsBean to case_documents;
-- rename table ArchiveFileHistoryBean to case_history;
-- rename table ArchiveFileReviewsBean to case_followups;
-- rename table ArchiveFileTagsBean to case_tags;
-- rename table BankDataBean to directory_banks;
-- rename table CityDataBean to directory_cities;
-- rename table FaxQueueBean to communication_fax;
-- 
-- rename table campaign to campaigns;
-- rename table campaign_addresses to campaign_contacts;

-- delete from security_roles where role='readOptionGroupRole';
-- insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.5') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.5';

-- alter table contacts add index `IDX_BEASAFEID` (beaSafeId);



-- insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.6') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.6';

-- CREATE TABLE document_tags (
-- `id` VARCHAR(250) BINARY NOT NULL, 
-- `documentKey` VARCHAR(250) BINARY, 
-- `tagName` VARCHAR(250) BINARY, 
-- CONSTRAINT `pk_document_tags` PRIMARY KEY (`id`),
-- FOREIGN KEY (documentKey) REFERENCES case_documents(id) ON DELETE CASCADE
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- 
-- alter table document_tags add index `IDX_TAGNAME` (tagName);
-- alter table document_tags add index `IDX_DOCUMENTKEY` (documentKey);
-- 
-- alter table document_tags add index `IDX_DOCUMENTKEY_TAGNAME` (documentKey, tagName);
-- alter table document_tags add unique index `IDX_DOCUMENTKEY_TAGNAME_UQ` (documentKey, tagName);

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.0') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.0';

alter table ArchiveFileBean add index `IDX_ARCHIVEFILEBEAN_SUBJECTFIELD` (subjectField);
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.2') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.2';

rename table AddressBean to contacts;
rename table AddressTagsBean to contact_tags;

rename table AppOptionGroupBean to server_options;
rename table ServerSettingsBean to server_settings;
-- required for j-lawyer.BOX
CREATE VIEW ServerSettingsBean AS SELECT * FROM server_settings;

-- views are required for the login module whose configuration will not change
rename table AppRoleBean to security_roles;
CREATE VIEW AppRoleBean AS SELECT * FROM security_roles;
rename table AppUserBean to security_users;
CREATE VIEW AppUserBean AS SELECT * FROM security_users;

rename table ArchiveFileBean to cases;
rename table ArchiveFileAddressesBean to case_contacts;
rename table ArchiveFileDocumentsBean to case_documents;
rename table ArchiveFileHistoryBean to case_history;
rename table ArchiveFileReviewsBean to case_followups;
rename table ArchiveFileTagsBean to case_tags;
rename table BankDataBean to directory_banks;
rename table CityDataBean to directory_cities;
rename table FaxQueueBean to communication_fax;

rename table campaign to campaigns;
rename table campaign_addresses to campaign_contacts;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.3') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.3';

delete from security_roles where role='readOptionGroupRole';

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.4') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.4';

alter table contacts add index `IDX_BEASAFEID` (beaSafeId);

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.5') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.5';

CREATE TABLE document_tags (
`id` VARCHAR(250) BINARY NOT NULL, 
`documentKey` VARCHAR(250) BINARY, 
`tagName` VARCHAR(250) BINARY, 
CONSTRAINT `pk_document_tags` PRIMARY KEY (`id`),
FOREIGN KEY (documentKey) REFERENCES case_documents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table document_tags add index `IDX_TAGNAME` (tagName);
alter table document_tags add index `IDX_DOCUMENTKEY` (documentKey);

alter table document_tags add index `IDX_DOCUMENTKEY_TAGNAME` (documentKey, tagName);
alter table document_tags add unique index `IDX_DOCUMENTKEY_TAGNAME_UQ` (documentKey, tagName);

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.6') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.6';

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.bea.beaendpoint','https://ksw.bea-brak.de') ON DUPLICATE KEY UPDATE settingValue     = 'https://ksw.bea-brak.de';
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.7') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.7';


insert into server_options (id, optionGroup, value) values ('1.10.0.8-1', 'document.tags', 'aMdt zK mBuwV');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-2', 'document.tags', 'aMdt zK');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-3', 'document.tags', 'aMdt zK StN');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-4', 'document.tags', 'drucken');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-5', 'document.tags', 'ENTWURF');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-6', 'document.tags', 'Frist notieren');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-7', 'document.tags', 'korr.');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-8', 'document.tags', 'korr. > ausf.');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-9', 'document.tags', 'Posteingang');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-10', 'document.tags', 'Priorität Mitarbeiter 1');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-11', 'document.tags', 'Priorität Mitarbeiter 2');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-12', 'document.tags', 'Priorität Mitarbeiter 3');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-13', 'document.tags', 'Priorität RA 1');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-14', 'document.tags', 'Priorität RA 2');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-15', 'document.tags', 'ToDo Mitarbeiter 1');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-16', 'document.tags', 'ToDo Mitarbeiter 2');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-17', 'document.tags', 'ToDo Mitarbeiter 3');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-18', 'document.tags', 'ToDo RA 1');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-19', 'document.tags', 'ToDo RA 2');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-20', 'document.tags', 'versenden');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-21', 'document.tags', 'versenden als Fax');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-22', 'document.tags', 'versenden als Fax vorab');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-23', 'document.tags', 'versenden als eMail');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-24', 'document.tags', 'versenden als eMail vorab');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-25', 'document.tags', 'versenden via beA');
insert into server_options (id, optionGroup, value) values ('1.10.0.8-26', 'document.tags', 'zdA [Handakte]');

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.8') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.8';

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.9') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.9';

ALTER TABLE campaign_contacts DROP FOREIGN KEY campaign_contacts_ibfk_2;
alter table campaign_contacts add CONSTRAINT campaign_contacts_ibfk_2 FOREIGN KEY (addressKey) REFERENCES contacts (id) ON DELETE CASCADE;

ALTER TABLE case_contacts DROP FOREIGN KEY case_contacts_ibfk_1;
alter table case_contacts add CONSTRAINT case_contacts_ibfk_1 FOREIGN KEY (archiveFileKey) REFERENCES cases (id) ON DELETE CASCADE;

ALTER TABLE case_contacts DROP FOREIGN KEY case_contacts_ibfk_2;
alter table case_contacts add CONSTRAINT case_contacts_ibfk_2 FOREIGN KEY (addressKey) REFERENCES contacts (id) ON DELETE CASCADE;

ALTER TABLE case_documents DROP FOREIGN KEY case_documents_ibfk_1;
alter table case_documents add CONSTRAINT case_documents_ibfk_1 FOREIGN KEY (archiveFileKey) REFERENCES cases (id) ON DELETE CASCADE;

ALTER TABLE case_followups DROP FOREIGN KEY case_followups_ibfk_1;
alter table case_followups add CONSTRAINT case_followups_ibfk_1 FOREIGN KEY (archiveFileKey) REFERENCES cases (id) ON DELETE CASCADE;

ALTER TABLE case_history DROP FOREIGN KEY case_history_ibfk_1;
alter table case_history add CONSTRAINT case_history_ibfk_1 FOREIGN KEY (archiveFileKey) REFERENCES cases (id) ON DELETE CASCADE;

ALTER TABLE case_tags DROP FOREIGN KEY case_tags_ibfk_1;
alter table case_tags add CONSTRAINT case_tags_ibfk_1 FOREIGN KEY (archiveFileKey) REFERENCES cases (id) ON DELETE CASCADE;

ALTER TABLE communication_fax DROP FOREIGN KEY communication_fax_ibfk_1;
alter table communication_fax add CONSTRAINT communication_fax_ibfk_1 FOREIGN KEY (archiveFileKey) REFERENCES cases (id) ON DELETE CASCADE;

ALTER TABLE contact_tags DROP FOREIGN KEY contact_tags_ibfk_1;
alter table contact_tags add CONSTRAINT contact_tags_ibfk_1 FOREIGN KEY (addressKey) REFERENCES contacts (id) ON DELETE CASCADE;


insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.10') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.10';

alter table contacts add `department` VARCHAR(250) BINARY;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.11.0.0') ON DUPLICATE KEY UPDATE settingValue     = '1.11.0.0';

alter table security_users add `emailOutPort` VARCHAR(30) BINARY;
update security_users set emailOutPort='';
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.11.0.1') ON DUPLICATE KEY UPDATE settingValue     = '1.11.0.1';

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.11.0.2') ON DUPLICATE KEY UPDATE settingValue     = '1.11.0.2';

CREATE TABLE party_types (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(250) BINARY unique, 
`placeholder` VARCHAR(250) BINARY, 
`color` INTEGER NOT NULL, 
CONSTRAINT `pk_party_types` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into party_types (id, name, placeholder, color) values ('10', 'Mandant', 'MANDANT', -6832371);
insert into party_types (id, name, placeholder, color) values ('20', 'Gegner', 'GEGNER', -2215621);
insert into party_types (id, name, placeholder, color) values ('30', 'Dritte', 'DRITTE', -15830347);

alter table case_contacts change referenceType referenceType varchar(50) BINARY NOT NULL;

DROP INDEX IDX_ARCHIVEFILEKEY_REFERENCETYPE ON case_contacts;
alter table case_contacts add index `IDX_ARCHIVEFILEKEY_REFERENCETYPE` (archiveFileKey, referenceType);

ALTER TABLE case_contacts ADD CONSTRAINT fk_reference_type FOREIGN KEY (referenceType) REFERENCES party_types(id);

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.0') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.0';

alter table contacts add index `IDX_CONTACTS_DEPARTMENT` (department);
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.1') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.1';

CREATE TABLE form_types (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(250) BINARY NOT NULL unique, 
`placeholder` VARCHAR(30) BINARY NOT NULL unique, 
`version` VARCHAR(25) BINARY NOT NULL, 
CONSTRAINT `pk_form_types` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE form_types_artefacts (
`id` VARCHAR(50) BINARY NOT NULL, 
`filename` VARCHAR(250) BINARY NOT NULL, 
`content` MEDIUMBLOB, 
`formtype` VARCHAR(50) BINARY NOT NULL, 
CONSTRAINT `pk_form_types_artefacts` PRIMARY KEY (`id`), 
FOREIGN KEY (formtype) REFERENCES form_types(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE case_forms (
`id` VARCHAR(50) BINARY NOT NULL, 
`case_id` VARCHAR(50) BINARY NOT NULL, 
`formtype` VARCHAR(50) BINARY NOT NULL,
`description` VARCHAR(250) BINARY,
`placeholder` VARCHAR(30) BINARY NOT NULL, 
`creationDate` DATETIME, 
CONSTRAINT `pk_case_forms` PRIMARY KEY (`id`), 
FOREIGN KEY (formtype) REFERENCES form_types(id) ON DELETE RESTRICT, 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE case_forms_entries (
`id` VARCHAR(50) BINARY NOT NULL, 
`case_id` VARCHAR(50) BINARY NOT NULL, 
`form_id` VARCHAR(50) BINARY NOT NULL,
`entry_key` VARCHAR(50) BINARY NOT NULL,
`string_value` VARCHAR(250) BINARY,
`placeholder` VARCHAR(30) BINARY NOT NULL, 
CONSTRAINT `pk_case_forms_entries` PRIMARY KEY (`id`), 
FOREIGN KEY (form_id) REFERENCES case_forms(id) ON DELETE CASCADE, 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.2') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.2';

alter table form_types add `usagetype` VARCHAR(25) BINARY;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.3') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.3';

CREATE TABLE security_groups (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(250) BINARY NOT NULL unique, 
`abbreviation` VARCHAR(30) BINARY NOT NULL unique,
CONSTRAINT `pk_security_groups` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE security_group_memberships (
`id` VARCHAR(50) BINARY NOT NULL, 
`principal_id` VARCHAR(50) BINARY NOT NULL, 
`group_id` VARCHAR(50) BINARY NOT NULL,
CONSTRAINT `pk_group_memberships` PRIMARY KEY (`id`), 
FOREIGN KEY (principal_id) REFERENCES security_users(principalId) ON DELETE CASCADE, 
FOREIGN KEY (group_id) REFERENCES security_groups(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.4') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.4';

alter table security_users add `abbreviation` VARCHAR(50) BINARY;
alter table security_users add `primary_group` VARCHAR(50) BINARY;

ALTER TABLE security_users
    ADD FOREIGN KEY
    fk_primary_group (primary_group)
    REFERENCES security_groups (id)
    ON DELETE SET NULL;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.5') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.5';

alter table cases add `owner_group` VARCHAR(30) BINARY;

ALTER TABLE cases
    ADD FOREIGN KEY
    fk_group (owner_group)
    REFERENCES security_groups (id) ON DELETE RESTRICT;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.6') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.6';

CREATE TABLE case_groups (
`id` VARCHAR(50) BINARY NOT NULL, 
`case_id` VARCHAR(50) BINARY NOT NULL, 
`group_id` VARCHAR(50) BINARY NOT NULL,
CONSTRAINT `pk_case_groups` PRIMARY KEY (`id`), 
FOREIGN KEY (group_id) REFERENCES security_groups(id) ON DELETE RESTRICT, 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.7') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.7';

alter table cases modify owner_group VARCHAR(50) BINARY;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.8') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.8';

alter table cases add `filenumberext` VARCHAR(100) BINARY;

alter table cases add index `IDX_CASES_FILENUMBEREXT` (filenumberext);

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.10') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.10';

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.0') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.0';

alter table case_forms_entries modify string_value VARCHAR(4096) BINARY;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.1') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.1';

alter table contacts add `district` VARCHAR(250) BINARY;
alter table contacts add `notice` VARCHAR(2500) BINARY;
alter table contacts add `nationality` VARCHAR(150) BINARY;
alter table contacts add `birthName` VARCHAR(250) BINARY;
alter table contacts add `placeOfBirth` VARCHAR(250) BINARY;
alter table contacts add `dateOfDeath` VARCHAR(50) BINARY;
alter table contacts add `vatId` VARCHAR(100) BINARY;
alter table contacts add `tin` VARCHAR(100) BINARY;
alter table contacts add `legalForm` VARCHAR(250) BINARY;
alter table contacts add `companyRegNr` VARCHAR(250) BINARY;
alter table contacts add `companyRegCourt` VARCHAR(250) BINARY;
alter table contacts add `gender` VARCHAR(50) BINARY;
alter table contacts add `streetNumber` VARCHAR(20) BINARY;
alter table contacts add `initials` VARCHAR(20) BINARY;
alter table contacts add `degreePrefix` VARCHAR(150) BINARY;
alter table contacts add `degreeSuffix` VARCHAR(150) BINARY;
alter table contacts add `profession` VARCHAR(250) BINARY;
alter table contacts add `role` VARCHAR(250) BINARY;
alter table contacts add `adjunct` VARCHAR(250) BINARY;



insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.2') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.2';

alter table contacts add `titleInAddress` VARCHAR(250) BINARY;


insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.3') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.3';

alter table contacts add index `IDX_CONTACTS_DISTRICT` (district);
alter table contacts add index `IDX_CONTACTS_BIRTHNAME` (birthName);
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.4') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.4';

insert into server_options(id,optionGroup, value) values('address.nationality.1', 'address.nationality','afghanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.2', 'address.nationality','ägyptisch');
insert into server_options(id,optionGroup, value) values('address.nationality.3', 'address.nationality','albanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.4', 'address.nationality','algerisch');
insert into server_options(id,optionGroup, value) values('address.nationality.5', 'address.nationality','andorranisch');
insert into server_options(id,optionGroup, value) values('address.nationality.6', 'address.nationality','angolanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.7', 'address.nationality','antiguanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.8', 'address.nationality','äquatorialguineisch');
insert into server_options(id,optionGroup, value) values('address.nationality.9', 'address.nationality','argentinisch');
insert into server_options(id,optionGroup, value) values('address.nationality.10', 'address.nationality','armenisch');
insert into server_options(id,optionGroup, value) values('address.nationality.11', 'address.nationality','aserbaidschanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.12', 'address.nationality','äthiopisch');
insert into server_options(id,optionGroup, value) values('address.nationality.13', 'address.nationality','australisch');
insert into server_options(id,optionGroup, value) values('address.nationality.14', 'address.nationality','bahamaisch');
insert into server_options(id,optionGroup, value) values('address.nationality.15', 'address.nationality','bahrainisch');
insert into server_options(id,optionGroup, value) values('address.nationality.16', 'address.nationality','bangladeschisch');
insert into server_options(id,optionGroup, value) values('address.nationality.17', 'address.nationality','barbadisch');
insert into server_options(id,optionGroup, value) values('address.nationality.18', 'address.nationality','belgisch');
insert into server_options(id,optionGroup, value) values('address.nationality.19', 'address.nationality','belizisch');
insert into server_options(id,optionGroup, value) values('address.nationality.20', 'address.nationality','beninisch');
insert into server_options(id,optionGroup, value) values('address.nationality.21', 'address.nationality','bhutanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.22', 'address.nationality','bolivianisch');
insert into server_options(id,optionGroup, value) values('address.nationality.23', 'address.nationality','bosnisch-herzegowinisch');
insert into server_options(id,optionGroup, value) values('address.nationality.24', 'address.nationality','botsuanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.25', 'address.nationality','brasilianisch');
insert into server_options(id,optionGroup, value) values('address.nationality.26', 'address.nationality','bruneiisch');
insert into server_options(id,optionGroup, value) values('address.nationality.27', 'address.nationality','bulgarisch');
insert into server_options(id,optionGroup, value) values('address.nationality.28', 'address.nationality','burkinisch');
insert into server_options(id,optionGroup, value) values('address.nationality.29', 'address.nationality','burundisch');
insert into server_options(id,optionGroup, value) values('address.nationality.30', 'address.nationality','chilenisch');
insert into server_options(id,optionGroup, value) values('address.nationality.31', 'address.nationality','chinesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.32', 'address.nationality','neuseeländisch');
insert into server_options(id,optionGroup, value) values('address.nationality.33', 'address.nationality','costa-ricanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.34', 'address.nationality','ivorisch');
insert into server_options(id,optionGroup, value) values('address.nationality.35', 'address.nationality','dänisch');
insert into server_options(id,optionGroup, value) values('address.nationality.36', 'address.nationality','deutsch');
insert into server_options(id,optionGroup, value) values('address.nationality.37', 'address.nationality','dominicanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.38', 'address.nationality','dominikanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.39', 'address.nationality','dschibutisch');
insert into server_options(id,optionGroup, value) values('address.nationality.40', 'address.nationality','ecuadorianisch');
insert into server_options(id,optionGroup, value) values('address.nationality.41', 'address.nationality','salvadorianisch');
insert into server_options(id,optionGroup, value) values('address.nationality.42', 'address.nationality','eritreisch');
insert into server_options(id,optionGroup, value) values('address.nationality.43', 'address.nationality','estnisch');
insert into server_options(id,optionGroup, value) values('address.nationality.44', 'address.nationality','fidschianisch');
insert into server_options(id,optionGroup, value) values('address.nationality.45', 'address.nationality','finnisch');
insert into server_options(id,optionGroup, value) values('address.nationality.46', 'address.nationality','französisch');
insert into server_options(id,optionGroup, value) values('address.nationality.47', 'address.nationality','gabunisch');
insert into server_options(id,optionGroup, value) values('address.nationality.48', 'address.nationality','gambisch');
insert into server_options(id,optionGroup, value) values('address.nationality.49', 'address.nationality','georgisch');
insert into server_options(id,optionGroup, value) values('address.nationality.50', 'address.nationality','ghanaisch');
insert into server_options(id,optionGroup, value) values('address.nationality.51', 'address.nationality','grenadisch');
insert into server_options(id,optionGroup, value) values('address.nationality.52', 'address.nationality','griechisch');
insert into server_options(id,optionGroup, value) values('address.nationality.53', 'address.nationality','guatemaltekisch');
insert into server_options(id,optionGroup, value) values('address.nationality.54', 'address.nationality','guineisch');
insert into server_options(id,optionGroup, value) values('address.nationality.55', 'address.nationality','guinea-bissauisch');
insert into server_options(id,optionGroup, value) values('address.nationality.56', 'address.nationality','guyanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.57', 'address.nationality','haitianisch');
insert into server_options(id,optionGroup, value) values('address.nationality.58', 'address.nationality','honduranisch');
insert into server_options(id,optionGroup, value) values('address.nationality.59', 'address.nationality','indisch');
insert into server_options(id,optionGroup, value) values('address.nationality.60', 'address.nationality','indonesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.61', 'address.nationality','irakisch');
insert into server_options(id,optionGroup, value) values('address.nationality.62', 'address.nationality','iranisch');
insert into server_options(id,optionGroup, value) values('address.nationality.63', 'address.nationality','irisch');
insert into server_options(id,optionGroup, value) values('address.nationality.64', 'address.nationality','isländisch');
insert into server_options(id,optionGroup, value) values('address.nationality.65', 'address.nationality','israelisch');
insert into server_options(id,optionGroup, value) values('address.nationality.66', 'address.nationality','italienisch');
insert into server_options(id,optionGroup, value) values('address.nationality.67', 'address.nationality','jamaikanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.68', 'address.nationality','japanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.69', 'address.nationality','jemenitisch');
insert into server_options(id,optionGroup, value) values('address.nationality.70', 'address.nationality','jordanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.71', 'address.nationality','kambodschanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.72', 'address.nationality','kamerunisch');
insert into server_options(id,optionGroup, value) values('address.nationality.73', 'address.nationality','kanadisch');
insert into server_options(id,optionGroup, value) values('address.nationality.74', 'address.nationality','kap-verdisch');
insert into server_options(id,optionGroup, value) values('address.nationality.75', 'address.nationality','kasachisch');
insert into server_options(id,optionGroup, value) values('address.nationality.76', 'address.nationality','katarisch');
insert into server_options(id,optionGroup, value) values('address.nationality.77', 'address.nationality','kenianisch');
insert into server_options(id,optionGroup, value) values('address.nationality.78', 'address.nationality','kirgisisch');
insert into server_options(id,optionGroup, value) values('address.nationality.79', 'address.nationality','kiribatisch');
insert into server_options(id,optionGroup, value) values('address.nationality.80', 'address.nationality','kolumbianisch');
insert into server_options(id,optionGroup, value) values('address.nationality.81', 'address.nationality','komorisch');
insert into server_options(id,optionGroup, value) values('address.nationality.82', 'address.nationality','kongolesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.83', 'address.nationality','der Demokratischen Republik Kongo');
insert into server_options(id,optionGroup, value) values('address.nationality.84', 'address.nationality','der Demokratischen Volksrepublik Korea');
insert into server_options(id,optionGroup, value) values('address.nationality.85', 'address.nationality','der Republik Korea');
insert into server_options(id,optionGroup, value) values('address.nationality.86', 'address.nationality','kosovarisch');
insert into server_options(id,optionGroup, value) values('address.nationality.87', 'address.nationality','kroatisch');
insert into server_options(id,optionGroup, value) values('address.nationality.88', 'address.nationality','kubanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.89', 'address.nationality','kuwaitisch');
insert into server_options(id,optionGroup, value) values('address.nationality.90', 'address.nationality','laotisch');
insert into server_options(id,optionGroup, value) values('address.nationality.91', 'address.nationality','lesothisch');
insert into server_options(id,optionGroup, value) values('address.nationality.92', 'address.nationality','lettisch');
insert into server_options(id,optionGroup, value) values('address.nationality.93', 'address.nationality','libanesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.94', 'address.nationality','liberianisch');
insert into server_options(id,optionGroup, value) values('address.nationality.95', 'address.nationality','libysch');
insert into server_options(id,optionGroup, value) values('address.nationality.96', 'address.nationality','liechtensteinisch');
insert into server_options(id,optionGroup, value) values('address.nationality.97', 'address.nationality','litauisch');
insert into server_options(id,optionGroup, value) values('address.nationality.98', 'address.nationality','luxemburgisch');
insert into server_options(id,optionGroup, value) values('address.nationality.99', 'address.nationality','madagassisch');
insert into server_options(id,optionGroup, value) values('address.nationality.100', 'address.nationality','malawisch');
insert into server_options(id,optionGroup, value) values('address.nationality.101', 'address.nationality','malaysisch');
insert into server_options(id,optionGroup, value) values('address.nationality.102', 'address.nationality','maledivisch');
insert into server_options(id,optionGroup, value) values('address.nationality.103', 'address.nationality','malisch');
insert into server_options(id,optionGroup, value) values('address.nationality.104', 'address.nationality','maltesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.105', 'address.nationality','marokkanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.106', 'address.nationality','marshallisch');
insert into server_options(id,optionGroup, value) values('address.nationality.107', 'address.nationality','mauretanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.108', 'address.nationality','mauritisch');
insert into server_options(id,optionGroup, value) values('address.nationality.109', 'address.nationality','mazedonisch');
insert into server_options(id,optionGroup, value) values('address.nationality.110', 'address.nationality','mexikanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.111', 'address.nationality','mikronesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.112', 'address.nationality','moldauisch');
insert into server_options(id,optionGroup, value) values('address.nationality.113', 'address.nationality','monegassisch');
insert into server_options(id,optionGroup, value) values('address.nationality.114', 'address.nationality','mongolisch');
insert into server_options(id,optionGroup, value) values('address.nationality.115', 'address.nationality','montenegrinisch');
insert into server_options(id,optionGroup, value) values('address.nationality.116', 'address.nationality','mosambikanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.117', 'address.nationality','myanmarisch');
insert into server_options(id,optionGroup, value) values('address.nationality.118', 'address.nationality','namibisch');
insert into server_options(id,optionGroup, value) values('address.nationality.119', 'address.nationality','nauruisch');
insert into server_options(id,optionGroup, value) values('address.nationality.120', 'address.nationality','nepalesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.121', 'address.nationality','neuseeländisch');
insert into server_options(id,optionGroup, value) values('address.nationality.122', 'address.nationality','nicaraguanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.123', 'address.nationality','niederländisch');
insert into server_options(id,optionGroup, value) values('address.nationality.124', 'address.nationality','nigrisch');
insert into server_options(id,optionGroup, value) values('address.nationality.125', 'address.nationality','nigerianisch');
insert into server_options(id,optionGroup, value) values('address.nationality.126', 'address.nationality','neuseeländisch');
insert into server_options(id,optionGroup, value) values('address.nationality.127', 'address.nationality','norwegisch');
insert into server_options(id,optionGroup, value) values('address.nationality.128', 'address.nationality','omanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.129', 'address.nationality','österreichisch');
insert into server_options(id,optionGroup, value) values('address.nationality.130', 'address.nationality','pakistanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.131', 'address.nationality','palauisch');
insert into server_options(id,optionGroup, value) values('address.nationality.132', 'address.nationality','panamaisch');
insert into server_options(id,optionGroup, value) values('address.nationality.133', 'address.nationality','papua-neuguineisch');
insert into server_options(id,optionGroup, value) values('address.nationality.134', 'address.nationality','paraguayisch');
insert into server_options(id,optionGroup, value) values('address.nationality.135', 'address.nationality','peruanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.136', 'address.nationality','philippinisch');
insert into server_options(id,optionGroup, value) values('address.nationality.137', 'address.nationality','polnisch');
insert into server_options(id,optionGroup, value) values('address.nationality.138', 'address.nationality','portugiesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.139', 'address.nationality','ruandisch');
insert into server_options(id,optionGroup, value) values('address.nationality.140', 'address.nationality','rumänisch');
insert into server_options(id,optionGroup, value) values('address.nationality.141', 'address.nationality','russisch');
insert into server_options(id,optionGroup, value) values('address.nationality.142', 'address.nationality','salomonisch');
insert into server_options(id,optionGroup, value) values('address.nationality.143', 'address.nationality','sambisch');
insert into server_options(id,optionGroup, value) values('address.nationality.144', 'address.nationality','samoanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.145', 'address.nationality','san-marinesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.146', 'address.nationality','são-toméisch');
insert into server_options(id,optionGroup, value) values('address.nationality.147', 'address.nationality','saudi-arabisch');
insert into server_options(id,optionGroup, value) values('address.nationality.148', 'address.nationality','schwedisch');
insert into server_options(id,optionGroup, value) values('address.nationality.149', 'address.nationality','schweizerisch');
insert into server_options(id,optionGroup, value) values('address.nationality.150', 'address.nationality','senegalesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.151', 'address.nationality','serbisch');
insert into server_options(id,optionGroup, value) values('address.nationality.152', 'address.nationality','seychellisch');
insert into server_options(id,optionGroup, value) values('address.nationality.153', 'address.nationality','sierra-leonisch');
insert into server_options(id,optionGroup, value) values('address.nationality.154', 'address.nationality','simbabwisch');
insert into server_options(id,optionGroup, value) values('address.nationality.155', 'address.nationality','singapurisch');
insert into server_options(id,optionGroup, value) values('address.nationality.156', 'address.nationality','slowakisch');
insert into server_options(id,optionGroup, value) values('address.nationality.157', 'address.nationality','slowenisch');
insert into server_options(id,optionGroup, value) values('address.nationality.158', 'address.nationality','somalisch');
insert into server_options(id,optionGroup, value) values('address.nationality.159', 'address.nationality','spanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.160', 'address.nationality','sri-lankisch');
insert into server_options(id,optionGroup, value) values('address.nationality.161', 'address.nationality','von St. Kitts und Nevis');
insert into server_options(id,optionGroup, value) values('address.nationality.162', 'address.nationality','lucianisch');
insert into server_options(id,optionGroup, value) values('address.nationality.163', 'address.nationality','vincentisch');
insert into server_options(id,optionGroup, value) values('address.nationality.164', 'address.nationality','südafrikanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.165', 'address.nationality','sudanesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.166', 'address.nationality','südsudanesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.167', 'address.nationality','surinamisch');
insert into server_options(id,optionGroup, value) values('address.nationality.168', 'address.nationality','swasiländisch');
insert into server_options(id,optionGroup, value) values('address.nationality.169', 'address.nationality','syrisch');
insert into server_options(id,optionGroup, value) values('address.nationality.170', 'address.nationality','tadschikisch');
insert into server_options(id,optionGroup, value) values('address.nationality.171', 'address.nationality','tansanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.172', 'address.nationality','thailändisch');
insert into server_options(id,optionGroup, value) values('address.nationality.173', 'address.nationality','von Timor-Leste');
insert into server_options(id,optionGroup, value) values('address.nationality.174', 'address.nationality','togoisch');
insert into server_options(id,optionGroup, value) values('address.nationality.175', 'address.nationality','tongaisch');
insert into server_options(id,optionGroup, value) values('address.nationality.176', 'address.nationality','von Trinidad und Tobago');
insert into server_options(id,optionGroup, value) values('address.nationality.177', 'address.nationality','tschadisch');
insert into server_options(id,optionGroup, value) values('address.nationality.178', 'address.nationality','tschechisch');
insert into server_options(id,optionGroup, value) values('address.nationality.179', 'address.nationality','tunesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.180', 'address.nationality','türkisch');
insert into server_options(id,optionGroup, value) values('address.nationality.181', 'address.nationality','turkmenisch');
insert into server_options(id,optionGroup, value) values('address.nationality.182', 'address.nationality','tuvaluisch');
insert into server_options(id,optionGroup, value) values('address.nationality.183', 'address.nationality','ugandisch');
insert into server_options(id,optionGroup, value) values('address.nationality.184', 'address.nationality','ukrainisch');
insert into server_options(id,optionGroup, value) values('address.nationality.185', 'address.nationality','ungarisch');
insert into server_options(id,optionGroup, value) values('address.nationality.186', 'address.nationality','uruguayisch');
insert into server_options(id,optionGroup, value) values('address.nationality.187', 'address.nationality','usbekisch');
insert into server_options(id,optionGroup, value) values('address.nationality.188', 'address.nationality','vanuatuisch');
insert into server_options(id,optionGroup, value) values('address.nationality.189', 'address.nationality','vatikanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.190', 'address.nationality','venezolanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.191', 'address.nationality','der Vereinigten Arabischen Emirate');
insert into server_options(id,optionGroup, value) values('address.nationality.192', 'address.nationality','amerikanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.193', 'address.nationality','britisch');
insert into server_options(id,optionGroup, value) values('address.nationality.194', 'address.nationality','vietnamesisch');
insert into server_options(id,optionGroup, value) values('address.nationality.195', 'address.nationality','weißrussisch');
insert into server_options(id,optionGroup, value) values('address.nationality.196', 'address.nationality','zentralafrikanisch');
insert into server_options(id,optionGroup, value) values('address.nationality.197', 'address.nationality','zyprisch');


insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.5') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.5';

insert into server_options(id,optionGroup, value) values('address.legalform.1', 'address.legalform','AG');
insert into server_options(id,optionGroup, value) values('address.legalform.2', 'address.legalform','Einzelunternehmen');
insert into server_options(id,optionGroup, value) values('address.legalform.3', 'address.legalform','e.K');
insert into server_options(id,optionGroup, value) values('address.legalform.4', 'address.legalform','GbR');
insert into server_options(id,optionGroup, value) values('address.legalform.5', 'address.legalform','OHG');
insert into server_options(id,optionGroup, value) values('address.legalform.6', 'address.legalform','KG');
insert into server_options(id,optionGroup, value) values('address.legalform.7', 'address.legalform','GmbH');
insert into server_options(id,optionGroup, value) values('address.legalform.8', 'address.legalform','UG');
insert into server_options(id,optionGroup, value) values('address.legalform.9', 'address.legalform','eG');

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.6') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.6';

insert into server_options(id,optionGroup, value) values('address.degreeprefix.1', 'address.degreeprefix','Dr.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.2', 'address.degreeprefix','Dipl. Betriebswirt');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.3', 'address.degreeprefix','Dipl. Ing (TU)');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.4', 'address.degreeprefix','Dipl. Ing.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.5', 'address.degreeprefix','Dipl. Ing. (FH)');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.6', 'address.degreeprefix','Dipl. jur.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.7', 'address.degreeprefix','Dipl. Kfm.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.8', 'address.degreeprefix','Dipl.-Ing.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.9', 'address.degreeprefix','Dipl.-Jur.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.10', 'address.degreeprefix','Dipl.-Kfm.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.11', 'address.degreeprefix','Dipl.-Päd.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.12', 'address.degreeprefix','Dipl.-SV');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.13', 'address.degreeprefix','Dr. jur.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.14', 'address.degreeprefix','Dr. med.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.15', 'address.degreeprefix','Dr. med. vet.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.16', 'address.degreeprefix','Dr. med.dent.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.17', 'address.degreeprefix','Dres.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.18', 'address.degreeprefix','Dres. med.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.19', 'address.degreeprefix','Prof. Dipl.-Ing.');
insert into server_options(id,optionGroup, value) values('address.degreeprefix.20', 'address.degreeprefix','Prof. Dr.');

insert into server_options(id,optionGroup, value) values('address.degreesuffix.1', 'address.degreesuffix','MA');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.2', 'address.degreesuffix','MSc');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.3', 'address.degreesuffix','MEng');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.4', 'address.degreesuffix','BA');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.5', 'address.degreesuffix','BSc');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.6', 'address.degreesuffix','BEng');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.7', 'address.degreesuffix','PhD');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.8', 'address.degreesuffix','LLD');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.9', 'address.degreesuffix','LLM');
insert into server_options(id,optionGroup, value) values('address.degreesuffix.10', 'address.degreesuffix','LLB');


insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.7') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.7';

insert into server_options(id,optionGroup, value) values('address.profession.1', 'address.profession','Rechtsanwalt');
insert into server_options(id,optionGroup, value) values('address.profession.2', 'address.profession','Rechtsanwältin');
insert into server_options(id,optionGroup, value) values('address.profession.3', 'address.profession','Gerichtsvollzieher');
insert into server_options(id,optionGroup, value) values('address.profession.4', 'address.profession','Gerichtsvollzieherin');

insert into server_options(id,optionGroup, value) values('address.role.1', 'address.role','Geschäftsführer');
insert into server_options(id,optionGroup, value) values('address.role.2', 'address.role','1. Vorsitzender');
insert into server_options(id,optionGroup, value) values('address.role.3', 'address.role','Vorstand');
insert into server_options(id,optionGroup, value) values('address.role.4', 'address.role','Geschäftsführerin');
insert into server_options(id,optionGroup, value) values('address.role.5', 'address.role','1. Vorsitzende');

insert into server_options(id,optionGroup, value) values('address.titleinaddress.1', 'address.titleinaddress','Herrn');

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.8') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.8';

alter table contacts add index `IDX_CONTACTS_ZIPCODE` (zipCode);
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.9') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.9';

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.1', 'address.complimentaryclose','Liebe Grüße' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Liebe Grüße' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.2', 'address.complimentaryclose','Lieber Gruß' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Lieber Gruß' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.3', 'address.complimentaryclose','Mit freundlichen Grüßen' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Mit freundlichen Grüßen' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.4', 'address.complimentaryclose','Mit freundlichen kollegialen Grüßen' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Mit freundlichen kollegialen Grüßen' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.5', 'address.complimentaryclose','Mit liebem Gruß' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Mit liebem Gruß' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.6', 'address.complimentaryclose','Kind regards,' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Kind regards,' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.7', 'address.complimentaryclose','Yours sincerely,' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Yours sincerely,' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.8', 'address.complimentaryclose','Mit vorzüglicher Hochachtung' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Mit vorzüglicher Hochachtung' LIMIT 1);



INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.1', 'address.salutation','Sehr geehrte Damen und Herren' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Sehr geehrte Damen und Herren' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.2', 'address.salutation','Sehr geehrte Frau' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Sehr geehrte Frau' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.3', 'address.salutation','Sehr geehrter Herr' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Sehr geehrter Herr' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.4', 'address.salutation','Sehr geehrte Kollegen' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Sehr geehrte Kollegen' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.5', 'address.salutation','Sehr geehrte Frau Kollegin' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Sehr geehrte Frau Kollegin' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.6', 'address.salutation','Sehr geehrter Herr Kollege' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Sehr geehrter Herr Kollege' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.7', 'address.salutation','Dear Mister' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Dear Mister' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.8', 'address.salutation','Dear Miss' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Dear Miss' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.9', 'address.salutation','Dear Sir or Madam' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Dear Sir or Madam' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.10', 'address.salutation','Liebe' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Liebe' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.11', 'address.salutation','Lieber' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Lieber' LIMIT 1);

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.10') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.10';

alter table security_users add `cloudHost` VARCHAR(250) BINARY;
alter table security_users add `cloudPort` INTEGER NOT NULL;
update security_users set cloudPort = 443;
alter table security_users add `cloudSsl` TINYINT;
update security_users set cloudSsl = 1;
alter table security_users add `cloudUser` VARCHAR(50) BINARY;
alter table security_users add `cloudPassword` VARCHAR(50) BINARY;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.11') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.11';

CREATE TABLE document_folders (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(100) BINARY NOT NULL, 
`parent_id` VARCHAR(50) BINARY,
CONSTRAINT `pk_document_folders` PRIMARY KEY (`id`), 
FOREIGN KEY (parent_id) REFERENCES document_folders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE document_folders ADD UNIQUE index `idx_name_parentid_documentfolders`(`name`, `parent_id`);

CREATE TABLE document_folder_tpls (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(100) BINARY NOT NULL unique, 
`root_folder` VARCHAR(50) BINARY NOT NULL,
CONSTRAINT `pk_document_folder_tpls` PRIMARY KEY (`id`), 
FOREIGN KEY (root_folder) REFERENCES document_folders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.12') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.12';

CREATE TABLE case_folders (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(100) BINARY NOT NULL, 
`parent_id` VARCHAR(50) BINARY,
CONSTRAINT `pk_case_folders` PRIMARY KEY (`id`), 
FOREIGN KEY (parent_id) REFERENCES case_folders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE case_folders ADD UNIQUE index `idx_name_parentid_casefolders`(`name`, `parent_id`);


alter table cases add `root_folder` VARCHAR(50) BINARY;
ALTER TABLE cases
    ADD FOREIGN KEY
    fk_root_folder (root_folder)
    REFERENCES case_folders (id) ON DELETE CASCADE;

alter table case_documents add `folder` VARCHAR(50) BINARY;
ALTER TABLE case_documents
    ADD FOREIGN KEY
    fk_case_documents_folder (folder)
    REFERENCES case_folders (id) ON DELETE NO ACTION;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.13') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.13';

insert into document_folders (id, name, parent_id) values ('Standardordner', 'Standardordner', null);
insert into document_folders (id, name, parent_id) values ('Abrechnung', 'Abrechnung', 'Standardordner');
insert into document_folders (id, name, parent_id) values ('Beratungshilfe', 'Beratungshilfe', 'Standardordner');
insert into document_folders (id, name, parent_id) values ('Mandant Korrespondenz', 'Mandant Korrespondenz', 'Standardordner');
insert into document_folders (id, name, parent_id) values ('Mandant Unterlagen', 'Mandant Unterlagen', 'Standardordner');
insert into document_folders (id, name, parent_id) values ('Kanzlei Dokumente', 'Kanzlei Dokumente', 'Standardordner');

insert into document_folder_tpls (id, name, root_folder) values ('default-folders', 'Standardordner', 'Standardordner');

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.14') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.14';

alter table case_forms modify placeholder VARCHAR(100) BINARY NOT NULL;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.15') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.15';

alter table case_forms_entries modify placeholder VARCHAR(100) BINARY NOT NULL;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.16') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.16';



commit;


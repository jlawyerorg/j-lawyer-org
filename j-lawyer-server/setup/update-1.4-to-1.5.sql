use jlawyerdb;

alter table AppUserBean MODIFY `emailSignature` VARCHAR(3500) BINARY;
alter table AppUserBean MODIFY `emailInPwd` VARCHAR(75) BINARY;
alter table AppUserBean MODIFY `emailOutPwd` VARCHAR(75) BINARY;

alter table AddressBean add `mobile` VARCHAR(250) BINARY;

# snip - KH update from here - changes above already performed with upgrade to 1.5alpha

alter table ArchiveFileReviewsBean add `assignee` VARCHAR(250) BINARY;

alter table AppUserBean MODIFY `emailInUser` VARCHAR(75) BINARY;
alter table AppUserBean MODIFY `emailOutUser` VARCHAR(75) BINARY;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.voip.voipmode','off');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.voip.voipendpoint','https://samurai.sipgate.net/RPC2');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.voip.voipuser','<sipgate-nutzerkennung>');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.voip.voippwd','<sipgate-passwort>');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.voip.sipprefix','sip:');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.voip.sipsuffix','@sipgate.de');

# NOTE: FaxQueueBean creation does not specify a default charset
# create_database.sql defined latin1 before 31.07.12 and utf8 afterwards
# PK and FK fields must match in collation - so we leave it out here - the database picks the right one automatically 

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

) ENGINE=InnoDB;

alter table FaxQueueBean add index `IDX_ARCHIVEFILEKEY` (archiveFileKey);
alter table FaxQueueBean add index `IDX_LASTSTATUS` (lastStatus);

commit;
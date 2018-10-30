use jlawyerdb;

# alter table AddressBean add `creator` VARCHAR(250) BINARY;
# alter table AddressBean add index `IDX_FIRSTNAME` (firstName);
# insert into AppRoleBean(id, principalid, role, roleGroup) values('31','user','loginRole','Roles');

alter table ArchiveFileBean add `subjectField` VARCHAR(100) BINARY;
alter table ArchiveFileBean add `reason` VARCHAR(100) BINARY;
alter table ArchiveFileBean add `lawyer` VARCHAR(15) BINARY;
alter table ArchiveFileBean add `assistant` VARCHAR(15) BINARY;

alter table ArchiveFileDocumentsBean add `dictateSign` VARCHAR(15) BINARY;
update ArchiveFileDocumentsBean d set d.dictateSign=(select f.dictateSign from ArchiveFileBean f where f.id=d.archiveFileKey) where lcase(name) like '%.odt';
update ArchiveFileDocumentsBean d set d.dictateSign=(select f.dictateSign from ArchiveFileBean f where f.id=d.archiveFileKey) where lcase(name) like '%.doc';
update ArchiveFileDocumentsBean d set d.dictateSign=(select f.dictateSign from ArchiveFileBean f where f.id=d.archiveFileKey) where lcase(name) like '%.docx';
alter table ArchiveFileBean drop `dictateSign`;


alter table AppUserBean add `lawyer` TINYINT;
update AppUserBean set lawyer=1;

insert into AppOptionGroupBean (id, optionGroup, value) values ('1', 'archiveFile.subjectField', 'Familienrecht');
insert into AppOptionGroupBean (id, optionGroup, value) values ('2', 'archiveFile.subjectField', 'Verkehrsrecht');


commit;

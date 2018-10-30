use jlawyerdb;

alter table AppUserBean add `emailStartTls` TINYINT;
update AppUserBean set emailStartTls = 0;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.documents.waitforprocess.mac','4000');

alter table AppUserBean add `settings` MEDIUMBLOB;

# version for release 1.8.0, 1.8.1 would be database version 01810
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','01800');

commit;
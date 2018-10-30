use jlawyerdb;

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

# snip - KH update from here - changes above already performed 

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.cpuwarn','80');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.cpuerror','90');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.memwarn','80');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.memerror','90');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.diskwarn','80');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.diskerror','90');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.vmwarn','75');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.vmerror','85');
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.monitor.notify','off');

commit;
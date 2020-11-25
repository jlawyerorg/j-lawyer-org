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
commit;
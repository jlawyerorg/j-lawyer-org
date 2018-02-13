use jlawyerdb;

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

# live system update 20171006

# 1.9 pilot users until here
alter table AddressBean add index `IDX_PHONE` (phone);
alter table AddressBean add index `IDX_MOBILE` (mobile);

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','01900') ON DUPLICATE KEY UPDATE settingValue     = '01900';

commit;
use jlawyerdb;

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

# snip - KH update from here - changes above already performed with upgrade to 1.6rc3

alter table AddressBean add `motorInsuranceName` VARCHAR(250) BINARY;
alter table AddressBean add `motorInsuranceNumber` VARCHAR(250) BINARY;

commit;
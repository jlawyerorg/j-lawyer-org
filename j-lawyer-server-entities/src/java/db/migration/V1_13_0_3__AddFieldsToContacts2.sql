alter table contacts add `titleInAddress` VARCHAR(250) BINARY;


insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.3') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.3';
commit;
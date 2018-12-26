delete from security_roles where role='readOptionGroupRole';

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.4') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.4';
commit;
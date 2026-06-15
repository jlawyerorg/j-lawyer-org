drop view AppUserBean;
drop view AppRoleBean;
drop view ServerSettingsBean;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.1.0.11') ON DUPLICATE KEY UPDATE settingValue     = '2.1.0.11';
commit;
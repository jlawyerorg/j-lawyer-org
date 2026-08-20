drop view if exists AppUserBean;
drop view if exists AppRoleBean;
drop view if exists ServerSettingsBean;
drop view if exists appuserBean;
drop view if exists approleBean;
drop view if exists serversettingsbean;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.1.0.14') ON DUPLICATE KEY UPDATE settingValue     = '2.1.0.14';
commit;
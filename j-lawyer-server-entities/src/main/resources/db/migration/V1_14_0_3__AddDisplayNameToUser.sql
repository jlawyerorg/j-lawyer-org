alter table security_users add `displayName` VARCHAR(250) BINARY;
alter table security_users add `voipUser` VARCHAR(100) BINARY;
alter table security_users add `voipPassword` VARCHAR(100) BINARY;

update security_users set displayName='';
update security_users set voipUser=(SELECT settingValue FROM server_settings where settingKey='jlawyer.server.voip.voipuser');
update security_users set voipPassword=(SELECT settingValue FROM server_settings where settingKey='jlawyer.server.voip.voippwd');

update security_users set voipUser='' where voipUser='<sipgate-nutzerkennung>';
update security_users set voipPassword='' where voipPassword='<sipgate-passwort>';

delete from server_settings where settingKey='jlawyer.server.voip.voippwd';
delete from server_settings where settingKey='jlawyer.server.voip.voipuser';
delete from server_settings where settingKey='jlawyer.server.voip.voipmode';

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.14.0.3') ON DUPLICATE KEY UPDATE settingValue     = '1.14.0.3';
commit;
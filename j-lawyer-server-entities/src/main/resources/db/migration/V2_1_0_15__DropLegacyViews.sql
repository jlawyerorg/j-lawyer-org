drop view if exists appuserbean;
drop view if exists approlebean;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.1.0.15') ON DUPLICATE KEY UPDATE settingValue     = '2.1.0.15';
commit;
alter table security_users add epost_customer VARCHAR(50) BINARY default null;
alter table security_users add epost_password VARCHAR(100) BINARY default null;
alter table security_users add epost_secret VARCHAR(100) BINARY default null;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.5.0.3') ON DUPLICATE KEY UPDATE settingValue     = '2.5.0.3';
commit;
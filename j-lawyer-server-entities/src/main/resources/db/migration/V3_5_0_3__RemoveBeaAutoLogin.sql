alter table security_users drop column beaCertificateAutoLogin;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.5.0.3') ON DUPLICATE KEY UPDATE settingValue     = '3.5.0.3';
commit;
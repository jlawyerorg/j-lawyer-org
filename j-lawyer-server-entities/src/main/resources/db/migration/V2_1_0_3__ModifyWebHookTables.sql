alter table integration_hooks modify auth_user VARCHAR(100) BINARY NULL;
alter table integration_hooks modify auth_pwd VARCHAR(250) BINARY NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.1.0.3') ON DUPLICATE KEY UPDATE settingValue     = '2.1.0.3';
commit;
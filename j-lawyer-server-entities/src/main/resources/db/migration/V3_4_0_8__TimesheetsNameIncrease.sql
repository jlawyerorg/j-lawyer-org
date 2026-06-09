alter table timesheets modify name VARCHAR(250) BINARY;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.8') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.8';
commit;
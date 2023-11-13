alter table case_contacts modify reference VARCHAR(251) BINARY;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.5.0.0') ON DUPLICATE KEY UPDATE settingValue     = '2.5.0.0';
commit;
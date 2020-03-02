alter table cases modify owner_group VARCHAR(50) BINARY;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.8') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.8';
commit;
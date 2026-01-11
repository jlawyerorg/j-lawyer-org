delete from server_settings where settingKey like '%drebis%';
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.2.0.1') ON DUPLICATE KEY UPDATE settingValue     = '3.2.0.1';
commit;
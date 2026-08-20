insert into server_settings(settingKey, settingValue) values('jlawyer.server.usagelimit.maxusers','999') ON DUPLICATE KEY UPDATE settingValue     = '999';
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.1.0.0') ON DUPLICATE KEY UPDATE settingValue     = '2.1.0.0';
commit;
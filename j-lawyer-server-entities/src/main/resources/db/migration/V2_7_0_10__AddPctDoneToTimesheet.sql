alter table timesheets add `pct_done` float NOT NULL default 0;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.10') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.10';
commit;
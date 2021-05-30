alter table case_events modify eventType int NOT NULL default 10;

alter table case_events add `endDate` datetime default null;
alter table case_events add `description` varchar(8096) default null;
alter table case_events add `location` varchar(4096) default null;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','1.15.0.3') ON DUPLICATE KEY UPDATE settingValue     = '1.15.0.3';
commit;
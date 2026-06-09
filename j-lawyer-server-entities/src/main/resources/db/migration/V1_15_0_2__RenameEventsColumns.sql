ALTER TABLE `case_events` CHANGE `reviewReason` `summary` varchar(250) default null;
ALTER TABLE `case_events` CHANGE `reviewDate` `beginDate` datetime default null;
ALTER TABLE `case_events` CHANGE `reviewType` `eventType` int default 10;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','1.15.0.2') ON DUPLICATE KEY UPDATE settingValue     = '1.15.0.2';
commit;
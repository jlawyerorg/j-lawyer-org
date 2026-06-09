ALTER TABLE cases ADD `cal_followups` VARCHAR(50) BINARY default null;
ALTER TABLE cases ADD `cal_respites` VARCHAR(50) BINARY default null;
ALTER TABLE cases ADD `cal_events` VARCHAR(50) BINARY default null;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.11') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.11';
commit;
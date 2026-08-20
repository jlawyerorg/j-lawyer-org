alter table case_events add index `idx_case_events_calsetup` (calendar_setup);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','1.15.0.10') ON DUPLICATE KEY UPDATE settingValue     = '1.15.0.10';
commit;
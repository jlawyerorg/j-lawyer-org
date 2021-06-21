alter table case_events add `calendar_setup` VARCHAR(250) BINARY;

alter table case_events add CONSTRAINT case_events_calendarsetup_fk FOREIGN KEY (calendar_setup) REFERENCES calendar_setup (id) ON DELETE RESTRICT;

update case_events set calendar_setup='wiedervorlagen-id' where eventType=10;
update case_events set calendar_setup='fristen-id' where eventType=20;
update case_events set calendar_setup='termine-id' where eventType=30;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','1.15.0.7') ON DUPLICATE KEY UPDATE settingValue     = '1.15.0.7';
commit;
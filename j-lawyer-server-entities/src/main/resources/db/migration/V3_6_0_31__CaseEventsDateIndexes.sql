-- the cross-case calendar views query case_events by date, and by done for the list of open
-- entries; until now the only index on the table was the one on calendar_setup
alter table case_events add index `idx_case_events_done_begin` (done, beginDate);
alter table case_events add index `idx_case_events_begin` (beginDate);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.31') ON DUPLICATE KEY UPDATE settingValue     = '3.6.0.31';
commit;

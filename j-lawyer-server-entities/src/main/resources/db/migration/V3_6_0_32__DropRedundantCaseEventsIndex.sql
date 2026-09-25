-- V3_6_0_31 added an index on case_events(beginDate). That was based on the Flyway migrations,
-- which only ever created idx_case_events_calsetup for this table - but the table also carries
-- indexes from the pre-Flyway setup scripts (j-lawyer-server/setup/create_database.sql and
-- update-1.6-to-1.7.sql), created there as IDX_REVDATE and IDX_REVDATE_DONE on the old table and
-- column names and carried over by the renames in V1_15_0_1 and V1_15_0_2.
--
-- IDX_REVDATE already indexes beginDate, so idx_case_events_begin is a duplicate that costs
-- write throughput and disk without ever being chosen. The compound index from V3_6_0_31 stays:
-- IDX_REVDATE_DONE is (beginDate, done), whereas the calendar queries filter on done and order
-- by beginDate, for which (done, beginDate) is the usable order - and it is what the optimizer
-- picks for them.
drop index `idx_case_events_begin` on case_events;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.32') ON DUPLICATE KEY UPDATE settingValue     = '3.6.0.32';
commit;

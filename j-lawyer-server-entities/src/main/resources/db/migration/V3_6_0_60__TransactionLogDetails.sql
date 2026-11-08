-- Bank statement import: record what was booked for a transaction (case, invoice paid, payment executed,
-- case account entry, tags), so that "already booked" transactions can show their booking details (#3333).

alter table transaction_log add column `case_id` VARCHAR(50) BINARY default NULL;
alter table transaction_log add column `details` TEXT default NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.60') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.60';
commit;

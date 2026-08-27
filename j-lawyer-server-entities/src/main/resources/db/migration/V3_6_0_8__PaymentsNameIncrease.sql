-- payments.name was created with VARCHAR(80) (V3_4_0_0__AddPayments.sql), which is well below
-- what the free text field in the payment dialog allows. Any longer value is rejected with
-- error 1406 "Data too long for column 'name'", which fails the flush and rolls back the whole
-- transaction - including the case history entry written in the same call.
--
-- 250 matches the length used for comparable name columns (invoices, timesheets, cases).

alter table payments modify name VARCHAR(250) BINARY;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.8') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.8';
commit;

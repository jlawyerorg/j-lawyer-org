-- A claim statement is handed to the debtor, annexed to an enforcement application or sent to the
-- creditor. The ledger therefore records that one was produced, with its key date and the user who
-- produced it, so it is visible later which figures were communicated and when.

alter table claimledgers add column `last_statement_date` DATETIME DEFAULT NULL;
alter table claimledgers add column `last_statement_key_date` DATE DEFAULT NULL;
alter table claimledgers add column `last_statement_by` VARCHAR(150) BINARY DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.11') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.11';
commit;

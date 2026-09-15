-- Bookings made by the dunning and enforcement workflows have to be traceable back to what caused
-- them: the fees of a dunning application, the costs of an enforcement measure under § 788 ZPO, a
-- reminder charge. Without that reference a cancelled measure could not offer to reverse exactly
-- the bookings it created.
--
-- A reversal keeps the original entry and books an adjustment against it, so that the ledger
-- history stays complete; reversal_of_entry_id links the two and also prevents a second reversal
-- of the same entry.

alter table claimledger_entries add column `origin_reference` VARCHAR(150) BINARY DEFAULT NULL;
alter table claimledger_entries add column `reversal_of_entry_id` VARCHAR(50) BINARY DEFAULT NULL;

alter table claimledger_entries add index `IDX_LEDGERENTRIES_ORIGIN` (origin_reference);

alter table claimledger_entries
    add constraint `fk_entry_reversalof` FOREIGN KEY (reversal_of_entry_id) REFERENCES claimledger_entries(id) ON DELETE SET NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.10') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.10';
commit;

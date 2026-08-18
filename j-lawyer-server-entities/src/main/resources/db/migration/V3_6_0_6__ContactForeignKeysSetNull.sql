-- invoices.contact_id and payments.contact_id were created with ON DELETE CASCADE.
-- Deleting a contact therefore silently deleted all invoices / payments where that
-- contact was the recipient (and, cascading further, their invoice_positions).
-- Both columns are nullable, so ON DELETE SET NULL is the correct behaviour - this is
-- also what case_account_entries.contact_id already uses (V2_6_0_10__AddCaseAccount.sql).
-- Both constraints were created unnamed, so their generated name has to be looked up.

SET @invoices_fk = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'invoices'
      AND COLUMN_NAME = 'contact_id'
      AND REFERENCED_TABLE_NAME = 'contacts'
);

SET @sql = IF(@invoices_fk IS NULL, 'SELECT 1', CONCAT(
    'ALTER TABLE invoices DROP FOREIGN KEY ', @invoices_fk,
    ', ADD CONSTRAINT invoices_contacts_fk FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE SET NULL;'
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @payments_fk = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'payments'
      AND COLUMN_NAME = 'contact_id'
      AND REFERENCED_TABLE_NAME = 'contacts'
);

SET @sql = IF(@payments_fk IS NULL, 'SELECT 1', CONCAT(
    'ALTER TABLE payments DROP FOREIGN KEY ', @payments_fk,
    ', ADD CONSTRAINT payments_contacts_fk FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE SET NULL;'
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.6') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.6';
commit;

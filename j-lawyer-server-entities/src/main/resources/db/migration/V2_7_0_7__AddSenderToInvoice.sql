alter table invoices add `sender_id` VARCHAR(50) BINARY DEFAULT NULL;

alter table invoices add `einvoice_document` VARCHAR(50) BINARY;
alter table invoices add CONSTRAINT einvoices_casedocuments_fk FOREIGN KEY (einvoice_document) REFERENCES case_documents (id) ON DELETE SET NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.7') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.7';
commit;
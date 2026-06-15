alter table invoices add `invoice_document` VARCHAR(50) BINARY;

alter table invoices add CONSTRAINT invoices_casedocuments_fk FOREIGN KEY (invoice_document) REFERENCES case_documents (id) ON DELETE SET NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.1.3') ON DUPLICATE KEY UPDATE settingValue     = '2.3.1.3';
commit;
alter table invoices add index `IDX_INVOICESTATUS` (invoice_status);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.9') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.9';
commit;
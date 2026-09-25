alter table invoices add index `IDX_INVOICENO` (invoice_no);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.26') ON DUPLICATE KEY UPDATE settingValue     = '3.6.0.26';
commit;

alter table invoices add `created` datetime default NULL;
alter table invoice_pools add `payment_term` INTEGER NOT NULL default 14;

alter table invoices add unique index `IDX_INVOICES_INVOICENO_UQ` (invoice_no);
alter table invoice_pools add unique index `IDX_INVOICEPOOLS_DISPNAME_UQ` (display_name);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.14') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.14';
commit;
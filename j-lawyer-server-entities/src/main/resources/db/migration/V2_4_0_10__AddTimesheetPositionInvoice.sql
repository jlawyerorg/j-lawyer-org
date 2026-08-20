alter table timesheet_positions add `invoice` VARCHAR(50) BINARY;

alter table timesheet_positions add CONSTRAINT timesheet_positions_invoice_fk FOREIGN KEY (invoice) REFERENCES invoices (id) ON DELETE SET NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.10') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.10';
commit;
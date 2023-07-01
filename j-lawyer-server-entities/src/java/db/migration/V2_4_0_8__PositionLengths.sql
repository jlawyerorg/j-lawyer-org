alter table invoice_positions modify description VARCHAR(4096) BINARY;
alter table invoice_position_tpls modify description VARCHAR(4096) BINARY;
alter table invoices modify description VARCHAR(4096) BINARY;

alter table timesheet_positions modify description VARCHAR(4096) BINARY;
alter table timesheet_position_tpls modify description VARCHAR(4096) BINARY;
alter table timesheets modify description VARCHAR(4096) BINARY;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.8') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.8';
commit;
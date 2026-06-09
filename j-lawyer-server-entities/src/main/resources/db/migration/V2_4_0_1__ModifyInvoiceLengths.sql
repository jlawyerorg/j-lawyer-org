alter table invoices modify name VARCHAR(250) BINARY;
alter table invoices modify description VARCHAR(1500) BINARY;

alter table invoice_positions modify name VARCHAR(500) BINARY;
alter table invoice_position_tpls modify name VARCHAR(500) BINARY;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.1') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.1';
commit;
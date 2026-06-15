insert into invoice_types (id, display_name, turnover, description) values ('invoicetype11', 'Quittung', 0, '');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.23') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.23';
commit;
CREATE TABLE invoice_types (
`id` VARCHAR(50) BINARY NOT NULL, 
`display_name` VARCHAR(100) BINARY,
`description` VARCHAR(250) BINARY,
`turnover` TINYINT,
CONSTRAINT `pk_invoice_types` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table invoices add `invoice_type` VARCHAR(50) BINARY;

alter table invoices add CONSTRAINT invoices_invoicetypes_fk FOREIGN KEY (invoice_type) REFERENCES invoice_types (id) ON DELETE RESTRICT;


insert into invoice_types (id, display_name, turnover, description) values ('invoicetype01', 'Rechnung', 1, '');
insert into invoice_types (id, display_name, turnover, description) values ('invoicetype02', 'Berechnung', 0, 'testweise Berechnung ohne Auswirkungen auf Buchungen');
insert into invoice_types (id, display_name, turnover, description) values ('invoicetype03', 'Angebot', 0, '');
insert into invoice_types (id, display_name, turnover, description) values ('invoicetype04', 'Kostenantrag', 0, '');
insert into invoice_types (id, display_name, turnover, description) values ('invoicetype05', 'Stornorechnung', 1, '');
insert into invoice_types (id, display_name, turnover, description) values ('invoicetype06', 'Korrekturrechnung', 1, '');
insert into invoice_types (id, display_name, turnover, description) values ('invoicetype07', 'Kostenvoranschlag', 0, '');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.20') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.20';
commit;
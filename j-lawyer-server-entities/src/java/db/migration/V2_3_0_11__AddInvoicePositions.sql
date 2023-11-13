CREATE TABLE invoice_positions (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(80) BINARY, 
`description` VARCHAR(2500) BINARY, 
`position` INTEGER NOT NULL, 
`tax_rate` FLOAT NOT NULL, 
`invoice_id` VARCHAR(50) BINARY NOT NULL, 
`units` INTEGER NOT NULL, 
`unit_price` FLOAT NOT NULL, 
`total` FLOAT NOT NULL, 
CONSTRAINT `pk_invoice_positions` PRIMARY KEY (`id`), 
FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.11') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.11';
commit;
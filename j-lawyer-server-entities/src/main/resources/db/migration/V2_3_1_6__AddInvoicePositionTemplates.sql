CREATE TABLE invoice_position_tpls (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(80) BINARY unique, 
`description` VARCHAR(2500) BINARY, 
`tax_rate` FLOAT NOT NULL, 
`units` FLOAT NOT NULL, 
`unit_price` FLOAT NOT NULL, 
CONSTRAINT `pk_invoice_position_tpls` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.1.6') ON DUPLICATE KEY UPDATE settingValue     = '2.3.1.6';
commit;
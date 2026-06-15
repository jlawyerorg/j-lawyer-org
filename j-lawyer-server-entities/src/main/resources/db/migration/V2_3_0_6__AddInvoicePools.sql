CREATE TABLE invoice_pools (
`id` VARCHAR(50) BINARY NOT NULL, 
`display_name` VARCHAR(100) BINARY,
CONSTRAINT `pk_invoice_pools` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table invoice_pools add `schema_syntax` VARCHAR(50) BINARY;
alter table invoice_pools add `manual_adjust` TINYINT;
alter table invoice_pools add `small_business` TINYINT;
alter table invoice_pools add `index_start` INTEGER NOT NULL;
alter table invoice_pools add `index_last` INTEGER NOT NULL;

insert into invoice_pools (id, display_name, schema_syntax, manual_adjust, small_business, index_start, index_last) values ('invoicepool01', 'Standardnummernkreis', 'R23-###', 0, 0, 0, 0);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.6') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.6';
commit;
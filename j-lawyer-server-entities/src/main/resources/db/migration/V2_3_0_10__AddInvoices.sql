CREATE TABLE invoices (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(80) BINARY, 
`description` VARCHAR(160) BINARY, 
`invoice_no` VARCHAR(50) BINARY, 
`invoice_status` INTEGER NOT NULL, 

`due_date` datetime default NULL, 
`period_from` datetime default NULL, 
`period_to` datetime default NULL, 

`case_id` VARCHAR(50) BINARY NOT NULL, 
`contact_id` VARCHAR(50) BINARY,
CONSTRAINT `pk_invoices` PRIMARY KEY (`id`), 
FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE, 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.10') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.10';
commit;
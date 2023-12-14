CREATE TABLE case_account_entries (
`id` VARCHAR(50) BINARY NOT NULL, 
`entry_date` datetime default NULL, 
`contact_id` VARCHAR(50) BINARY,
`case_id` VARCHAR(50) BINARY NOT NULL, 
`invoice_id` VARCHAR(50) BINARY, 
`description` VARCHAR(1500) BINARY, 
`in_earnings` FLOAT DEFAULT 0, 
`out_spendings` FLOAT DEFAULT 0, 
`in_escrow` FLOAT DEFAULT 0, 
`out_escrow` FLOAT DEFAULT 0, 
`in_expenditure` FLOAT DEFAULT 0, 
`out_expenditure` FLOAT DEFAULT 0, 
CONSTRAINT `pk_caseaccounts` PRIMARY KEY (`id`), 
FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE SET NULL, 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE,
FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.10') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.10';
commit;
CREATE TABLE config_bankstatement (
`id` VARCHAR(50) BINARY NOT NULL, 
`display_name` VARCHAR(250) BINARY NOT NULL, 
`csv_delimiter` VARCHAR(5) BINARY NOT NULL DEFAULT ';', 
`number_format` VARCHAR(15) BINARY NOT NULL DEFAULT '#,##0.00', 
`number_locale` VARCHAR(15) BINARY NOT NULL DEFAULT 'DE', 
`has_header` BIT(1) DEFAULT 1,
`col_date` INTEGER DEFAULT 0, 
`col_name` INTEGER DEFAULT 1, 
`col_bookingtype` INTEGER DEFAULT 2, 
`col_iban` INTEGER DEFAULT 3, 
`col_purpose` INTEGER DEFAULT 4, 
`col_amount` INTEGER DEFAULT 5, 
`col_currency` INTEGER DEFAULT 6, 

CONSTRAINT `pk_config_bankstatement` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

insert into config_bankstatement (id, display_name, csv_delimiter, number_format, number_locale, has_header, col_date, col_name, col_bookingtype, col_iban, col_purpose, col_amount, col_currency) values ('grenke-csv','GRENKE AG',';','#,##0.00', 'DE', 1, 4, 6, 9, 7, 10, 11, 12);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.2.0.3') ON DUPLICATE KEY UPDATE settingValue     = '3.2.0.3';
commit;
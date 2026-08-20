CREATE TABLE payments (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(80) BINARY, 
`description` VARCHAR(160) BINARY, 
`reason` VARCHAR(160) BINARY, 
`payment_no` VARCHAR(50) BINARY, 
`payment_status` INTEGER NOT NULL, 
`total` DECIMAL(10,2) DEFAULT 0.00 NOT NULL, 
`currency` VARCHAR(20) BINARY, 
`payment_type` varchar(25) BINARY DEFAULT 'OTHER', 

`date_created` datetime default NULL, 
`date_target` datetime default NULL, 
`sender_id` VARCHAR(50) BINARY DEFAULT NULL, 

`case_id` VARCHAR(50) BINARY NOT NULL, 
`contact_id` VARCHAR(50) BINARY,
CONSTRAINT `pk_payments` PRIMARY KEY (`id`), 
FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE, 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.0') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.0';
commit;
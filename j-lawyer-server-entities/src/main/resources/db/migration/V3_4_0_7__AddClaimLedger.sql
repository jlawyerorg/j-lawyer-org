CREATE TABLE claimledgers (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(150) BINARY, 
`description` VARCHAR(500) BINARY, 
`tax_rate` DECIMAL(10,2) DEFAULT 0.00 NOT NULL,
`case_id` VARCHAR(50) BINARY NOT NULL, 
CONSTRAINT `pk_claimledgers` PRIMARY KEY (`id`), 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE claimledger_entries (
`id` VARCHAR(50) BINARY NOT NULL, 
`comment` VARCHAR(500) BINARY, 
`description` VARCHAR(250) BINARY, 
`entry_date` datetime default NULL,
`ledger_id` VARCHAR(50) BINARY NOT NULL, 
`amount` DECIMAL(10,2) DEFAULT 0.00 NOT NULL,
`entry_type` VARCHAR(50) BINARY NOT NULL, 
CONSTRAINT `pk_claimledger_entries` PRIMARY KEY (`id`), 
FOREIGN KEY (ledger_id) REFERENCES claimledgers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table claimledger_entries add index `IDX_LEDGERENTRIES_DATE` (entry_date);
alter table claimledger_entries add index `IDX_LEDGERENTRIES_TYPE` (entry_type);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.7') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.7';
commit;
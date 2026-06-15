CREATE TABLE transaction_log (
`id` VARCHAR(50) BINARY NOT NULL, 
`date_processed` datetime default NULL, 
`date_expiry` datetime default NULL, 
`principal` VARCHAR(50) BINARY,
`tx_checksum` VARCHAR(100) BINARY NOT NULL, 
CONSTRAINT `pk_transactionlog` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

alter table transaction_log add index `IDX_TXCHECKSUM` (tx_checksum);
alter table transaction_log add index `IDX_DATEEXPIRY` (date_expiry);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.2.0.6') ON DUPLICATE KEY UPDATE settingValue     = '3.2.0.6';
commit;
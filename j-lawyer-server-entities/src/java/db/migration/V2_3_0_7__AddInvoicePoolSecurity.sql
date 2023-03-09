CREATE TABLE security_invoicepool_access (
`id` VARCHAR(50) BINARY NOT NULL, 
`principal_id` VARCHAR(50) BINARY NOT NULL, 
`pool_id` VARCHAR(50) BINARY NOT NULL,
CONSTRAINT `pk_invoicepool_access` PRIMARY KEY (`id`), 
FOREIGN KEY (principal_id) REFERENCES security_users(principalId) ON DELETE CASCADE, 
FOREIGN KEY (pool_id) REFERENCES invoice_pools(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.7') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.7';
commit;
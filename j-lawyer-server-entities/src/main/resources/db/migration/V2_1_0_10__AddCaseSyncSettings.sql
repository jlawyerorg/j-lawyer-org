CREATE TABLE case_syncs (
`id` VARCHAR(50) BINARY NOT NULL, 
`case_id` VARCHAR(50) BINARY NOT NULL, 
`principal_id` VARCHAR(50) BINARY NOT NULL,
CONSTRAINT `pk_case_syncs` PRIMARY KEY (`id`), 
FOREIGN KEY (principal_id) REFERENCES security_users(principalId) ON DELETE CASCADE, 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.1.0.10') ON DUPLICATE KEY UPDATE settingValue     = '2.1.0.10';
commit;
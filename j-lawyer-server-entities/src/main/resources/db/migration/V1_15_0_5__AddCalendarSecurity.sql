CREATE TABLE security_calendar_access (
`id` VARCHAR(50) BINARY NOT NULL, 
`principal_id` VARCHAR(50) BINARY NOT NULL, 
`calendar_id` VARCHAR(50) BINARY NOT NULL,
CONSTRAINT `pk_calendar_access` PRIMARY KEY (`id`), 
FOREIGN KEY (principal_id) REFERENCES security_users(principalId) ON DELETE CASCADE, 
FOREIGN KEY (calendar_id) REFERENCES calendar_setup(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','1.15.0.5') ON DUPLICATE KEY UPDATE settingValue     = '1.15.0.5';
commit;
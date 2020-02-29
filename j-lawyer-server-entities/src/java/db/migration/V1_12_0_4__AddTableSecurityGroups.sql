CREATE TABLE security_groups (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(250) BINARY NOT NULL unique, 
`abbreviation` VARCHAR(30) BINARY NOT NULL unique,
CONSTRAINT `pk_security_groups` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE security_group_memberships (
`id` VARCHAR(50) BINARY NOT NULL, 
`principal_id` VARCHAR(50) BINARY NOT NULL, 
`group_id` VARCHAR(50) BINARY NOT NULL,
CONSTRAINT `pk_group_memberships` PRIMARY KEY (`id`), 
FOREIGN KEY (principal_id) REFERENCES security_users(principalId) ON DELETE CASCADE, 
FOREIGN KEY (group_id) REFERENCES security_groups(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.4') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.4';
commit;
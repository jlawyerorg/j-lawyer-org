CREATE TABLE case_groups (
`id` VARCHAR(50) BINARY NOT NULL, 
`case_id` VARCHAR(50) BINARY NOT NULL, 
`group_id` VARCHAR(50) BINARY NOT NULL,
CONSTRAINT `pk_case_groups` PRIMARY KEY (`id`), 
FOREIGN KEY (group_id) REFERENCES security_groups(id) ON DELETE RESTRICT, 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.7') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.7';
commit;
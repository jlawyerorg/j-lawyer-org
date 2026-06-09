CREATE TABLE assistant_replacements (
`id` VARCHAR(50) BINARY NOT NULL, 
`search_string` VARCHAR(250) BINARY NOT NULL DEFAULT '', 
`replace_with` VARCHAR(10000) BINARY NOT NULL DEFAULT '', 
`case_insensitive` BIT(1) DEFAULT 1,

CONSTRAINT `pk_assistant_replacements` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

insert into assistant_replacements (id, search_string, replace_with, case_insensitive) values ('id.mfg', 'MfG', 'Mit freundlichen Grüßen', 1);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.2.0.8') ON DUPLICATE KEY UPDATE settingValue     = '3.2.0.8';
commit;
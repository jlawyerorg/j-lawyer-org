CREATE TABLE case_folder_settings (
`id` VARCHAR(250) BINARY NOT NULL, 
`principal` VARCHAR(250) BINARY, 
`folder` VARCHAR(250) BINARY, 
`hidden` TINYINT NOT NULL, 
CONSTRAINT `pk_case_folder_settings` PRIMARY KEY (id), 
FOREIGN KEY (principal) REFERENCES security_users(principalId) ON DELETE CASCADE,
FOREIGN KEY (folder) REFERENCES case_folders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE case_folder_settings ADD UNIQUE index `idx_principal_folder_casefoldersettings`(`principal`, `folder`);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','1.14.0.8') ON DUPLICATE KEY UPDATE settingValue     = '1.14.0.8';
commit;
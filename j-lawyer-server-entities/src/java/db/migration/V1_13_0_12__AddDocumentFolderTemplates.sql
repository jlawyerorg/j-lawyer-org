CREATE TABLE document_folders (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(100) BINARY NOT NULL, 
`parent_id` VARCHAR(50) BINARY,
CONSTRAINT `pk_document_folders` PRIMARY KEY (`id`), 
FOREIGN KEY (parent_id) REFERENCES document_folders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE document_folders ADD UNIQUE index `idx_name_parentid_documentfolders`(`name`, `parent_id`);

CREATE TABLE document_folder_tpls (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(100) BINARY NOT NULL unique, 
`root_folder` VARCHAR(50) BINARY NOT NULL,
CONSTRAINT `pk_document_folder_tpls` PRIMARY KEY (`id`), 
FOREIGN KEY (root_folder) REFERENCES document_folders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.12') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.12';
commit;
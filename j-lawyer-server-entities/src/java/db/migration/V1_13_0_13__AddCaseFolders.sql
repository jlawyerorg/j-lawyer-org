CREATE TABLE case_folders (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(100) BINARY NOT NULL, 
`parent_id` VARCHAR(50) BINARY,
CONSTRAINT `pk_case_folders` PRIMARY KEY (`id`), 
FOREIGN KEY (parent_id) REFERENCES case_folders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE case_folders ADD UNIQUE index `idx_name_parentid_casefolders`(`name`, `parent_id`);


alter table cases add `root_folder` VARCHAR(50) BINARY;
ALTER TABLE cases
    ADD FOREIGN KEY
    fk_root_folder (root_folder)
    REFERENCES case_folders (id) ON DELETE CASCADE;

alter table case_documents add `folder` VARCHAR(50) BINARY;
ALTER TABLE case_documents
    ADD FOREIGN KEY
    fk_case_documents_folder (folder)
    REFERENCES case_folders (id) ON DELETE NO ACTION;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.13') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.13';
commit;
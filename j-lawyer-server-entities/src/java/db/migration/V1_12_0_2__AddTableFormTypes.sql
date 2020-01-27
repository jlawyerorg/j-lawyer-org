CREATE TABLE form_types (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(250) BINARY NOT NULL unique, 
`placeholder` VARCHAR(30) BINARY NOT NULL unique, 
`version` VARCHAR(25) BINARY NOT NULL, 
CONSTRAINT `pk_form_types` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE form_types_artefacts (
`id` VARCHAR(50) BINARY NOT NULL, 
`filename` VARCHAR(250) BINARY NOT NULL, 
`content` MEDIUMBLOB, 
`formtype` VARCHAR(50) BINARY NOT NULL, 
CONSTRAINT `pk_form_types_artefacts` PRIMARY KEY (`id`), 
FOREIGN KEY (formtype) REFERENCES form_types(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE case_forms (
`id` VARCHAR(50) BINARY NOT NULL, 
`case_id` VARCHAR(50) BINARY NOT NULL, 
`formtype` VARCHAR(50) BINARY NOT NULL,
`description` VARCHAR(250) BINARY,
`placeholder` VARCHAR(30) BINARY NOT NULL, 
`creationDate` DATETIME, 
CONSTRAINT `pk_case_forms` PRIMARY KEY (`id`), 
FOREIGN KEY (formtype) REFERENCES form_types(id) ON DELETE RESTRICT, 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE case_forms_entries (
`id` VARCHAR(50) BINARY NOT NULL, 
`case_id` VARCHAR(50) BINARY NOT NULL, 
`form_id` VARCHAR(50) BINARY NOT NULL,
`entry_key` VARCHAR(50) BINARY NOT NULL,
`string_value` VARCHAR(250) BINARY,
`placeholder` VARCHAR(30) BINARY NOT NULL, 
CONSTRAINT `pk_case_forms_entries` PRIMARY KEY (`id`), 
FOREIGN KEY (form_id) REFERENCES case_forms(id) ON DELETE CASCADE, 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.2') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.2';
commit;
CREATE TABLE document_tag_rules (
`id` VARCHAR(50) BINARY NOT NULL, 
`rule_name` VARCHAR(150) BINARY NOT NULL, 
`tag_list` VARCHAR(500) BINARY NOT NULL, 
`operator_andor` INTEGER NOT NULL default 10,
`sequence_no` INTEGER NOT NULL default 1, 
`cancel_onmatch` BIT(1) DEFAULT 1,
CONSTRAINT `pk_document_tag_rules` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE document_tag_rule_conditions (
`id` VARCHAR(50) BINARY NOT NULL, 
`rule_id` VARCHAR(50) BINARY NOT NULL, 
`comp_value` VARCHAR(150) BINARY NOT NULL, 
`comp_mode` INTEGER NOT NULL default 10,
CONSTRAINT `pk_document_tag_rule_conditions` PRIMARY KEY (`id`),
FOREIGN KEY (rule_id) REFERENCES document_tag_rules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.2.0.0') ON DUPLICATE KEY UPDATE settingValue     = '3.2.0.0';
commit;
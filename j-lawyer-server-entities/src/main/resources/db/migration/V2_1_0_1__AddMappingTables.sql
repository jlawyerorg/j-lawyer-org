CREATE TABLE mapping_tables (
`table_name` VARCHAR(100) BINARY NOT NULL, 
`system_table` TINYINT DEFAULT 0,
`key1_name` VARCHAR(250) BINARY NOT NULL, 
`key2_name` VARCHAR(250) BINARY, 
`key3_name` VARCHAR(250) BINARY, 
CONSTRAINT `pk_mapping_tables` PRIMARY KEY (`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE mapping_entries (
`id` VARCHAR(50) BINARY NOT NULL,
`table_name` VARCHAR(100) BINARY NOT NULL, 
`key1_value` VARCHAR(250) BINARY NOT NULL, 
`key2_value` VARCHAR(250) BINARY, 
`key3_value` VARCHAR(250) BINARY, 
`mapping_value` VARCHAR(4096) BINARY, 
CONSTRAINT `pk_mapping_entries` PRIMARY KEY (`id`),
FOREIGN KEY (table_name) REFERENCES mapping_tables(table_name) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table mapping_entries add index `IDX_MAPENT_TABLENAME` (table_name);
alter table mapping_entries add index `IDX_MAPENT_KEY1` (key1_value);
alter table mapping_entries add index `IDX_MAPENT_KEY2` (key2_value);
alter table mapping_entries add index `IDX_MAPENT_KEY3` (key3_value);

alter table mapping_entries add unique index `IDX_MAPENT_UQE` (table_name, key1_value, key2_value, key3_value);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.1.0.1') ON DUPLICATE KEY UPDATE settingValue     = '2.1.0.1';
commit;
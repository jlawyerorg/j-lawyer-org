CREATE TABLE timesheets (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(80) BINARY, 
`description` VARCHAR(160) BINARY, 
`interval_minutes` INTEGER NOT NULL default 5, 
`limited` TINYINT default 0,
`limit_net` float NOT NULL default 0, 
`case_id` VARCHAR(50) BINARY NOT NULL, 
CONSTRAINT `pk_timesheets` PRIMARY KEY (`id`), 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.3') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.3';
commit;
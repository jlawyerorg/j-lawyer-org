CREATE TABLE timesheet_positions (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(80) BINARY, 
`description` VARCHAR(2500) BINARY, 
`tax_rate` FLOAT NOT NULL, 
`timesheet_id` VARCHAR(50) BINARY NOT NULL, 
`unit_price` FLOAT NOT NULL, 
`total` FLOAT NOT NULL, 
`principal` VARCHAR(250) BINARY, 
`time_started` datetime default NULL, 
`time_stopped` datetime default NULL, 
CONSTRAINT `pk_timesheet_positions` PRIMARY KEY (`id`), 
FOREIGN KEY (timesheet_id) REFERENCES timesheets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.4') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.4';
commit;
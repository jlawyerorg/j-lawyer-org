CREATE TABLE timesheet_tpls_allowed (
`id` VARCHAR(50) BINARY NOT NULL, 
`timesheet_id` VARCHAR(50) BINARY NOT NULL, 
`pos_tpl_id` VARCHAR(50) BINARY NOT NULL, 
CONSTRAINT `pk_timesheet_tpls_allowed` PRIMARY KEY (`id`), 
FOREIGN KEY (timesheet_id) REFERENCES timesheets(id) ON DELETE CASCADE,
FOREIGN KEY (pos_tpl_id) REFERENCES timesheet_position_tpls(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.3') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.3';
commit;
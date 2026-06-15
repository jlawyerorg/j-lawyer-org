CREATE TABLE calendar_entry_template (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(250) BINARY NOT NULL, 
`description` TEXT BINARY, 
`related` BIT(1) DEFAULT 0,
`related_name` VARCHAR(250) BINARY NOT NULL, 
`related_description` TEXT BINARY, 
`related_offset_days` INTEGER DEFAULT -14, 
CONSTRAINT `pk_calendar_entry_template` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

insert into calendar_entry_template (id, name, description, related, related_name, related_description, related_offset_days) select id, value, '', 0, '', '', 0 from server_options where optionGroup='archiveFile.reviewReason';

delete from server_options where optionGroup='archiveFile.reviewReason';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.5') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.5';
commit;
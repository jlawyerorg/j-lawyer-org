CREATE TABLE communication_epost (
`letter_id` INTEGER default 0 NOT NULL, 
`case_id` VARCHAR(250) BINARY, 
`file_name` VARCHAR(250) BINARY,
`last_status_id` INTEGER default 0,
`last_status_details` VARCHAR(500) BINARY,
`created_date` DATETIME,
`processed_date` DATETIME,
`printupload_date` DATETIME,
`printfeedback_date` DATETIME,
`no_of_pages` INTEGER default 1,
`reg_letter_status` VARCHAR(500) BINARY,
`reg_letter_status_date` DATETIME,
`dest_area_status` VARCHAR(500) BINARY,
`dest_area_status_date` DATETIME,
`sent_by` VARCHAR(50) BINARY, 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE, 
CONSTRAINT `pk_communication_epost` PRIMARY KEY (`letter_id`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.5.0.4') ON DUPLICATE KEY UPDATE settingValue     = '2.5.0.4';
commit;
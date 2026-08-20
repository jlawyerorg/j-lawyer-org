CREATE TABLE assistant_prompts (
`id` VARCHAR(50) BINARY NOT NULL, 
`prompt_name` VARCHAR(250) BINARY NOT NULL, 
`request_type` VARCHAR(20) BINARY NOT NULL, 
`prompt_text` VARCHAR(10000) BINARY NOT NULL, 

CONSTRAINT `pk_assistant_prompts` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.2') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.2';
commit;
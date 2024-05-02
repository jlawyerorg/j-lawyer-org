CREATE TABLE assistant_config (
`id` VARCHAR(50) BINARY NOT NULL, 
`url` VARCHAR(1500) BINARY NOT NULL, 
`user_name` VARCHAR(50) BINARY NOT NULL, 
`password` VARCHAR(200) BINARY NOT NULL, 

CONSTRAINT `pk_assistant_config` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.1.6') ON DUPLICATE KEY UPDATE settingValue     = '2.6.1.6';
commit;
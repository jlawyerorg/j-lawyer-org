CREATE TABLE integration_hooks_log (
`hook_id` VARCHAR(50) BINARY NOT NULL, 
`hook_type` VARCHAR(100) BINARY NOT NULL, 
`date_request` datetime default NULL, 
`payload_size` BIGINT DEFAULT 0, 
`duration` BIGINT DEFAULT 0, 
`status` INTEGER DEFAULT 0, 
`url` VARCHAR(750) BINARY NOT NULL, 
`auth_user` VARCHAR(100) BINARY DEFAULT NULL, 
`failed` BIT(1) DEFAULT 0,
`fail_message` VARCHAR(2500) BINARY DEFAULT NULL, 
`response` VARCHAR(4500) BINARY DEFAULT NULL, 

CONSTRAINT `pk_integration_hooks_log` PRIMARY KEY (`hook_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

alter table integration_hooks_log add index `IDX_INTHOOKSLOG_DREQ` (date_request);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.1.4') ON DUPLICATE KEY UPDATE settingValue     = '2.6.1.4';
commit;
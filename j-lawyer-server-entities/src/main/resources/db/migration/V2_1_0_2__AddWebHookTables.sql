CREATE TABLE integration_hooks (
`hook_name` VARCHAR(100) BINARY NOT NULL, 
`url` VARCHAR(750) BINARY NOT NULL, 
`hook_type` VARCHAR(100) BINARY NOT NULL, 
`auth_user` VARCHAR(100) BINARY NOT NULL, 
`auth_pwd` VARCHAR(250) BINARY NOT NULL, 
`timeout_connect` BIGINT, 
`timeout_read` BIGINT, 
CONSTRAINT `pk_integration_hooks` PRIMARY KEY (`hook_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table integration_hooks add index `IDX_INTHOOKS_HTYPE` (hook_type);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.1.0.2') ON DUPLICATE KEY UPDATE settingValue     = '2.1.0.2';
commit;
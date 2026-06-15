alter table assistant_config add `timeout_connect` BIGINT DEFAULT 5;
alter table assistant_config add `timeout_read` BIGINT DEFAULT 60;
alter table assistant_config add `assistant_name` VARCHAR(100) BINARY NOT NULL;

ALTER TABLE assistant_config MODIFY user_name VARCHAR(50) BINARY DEFAULT NULL;
ALTER TABLE assistant_config MODIFY password VARCHAR(200) BINARY DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.0') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.0';
commit;
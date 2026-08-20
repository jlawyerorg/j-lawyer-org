alter table assistant_config add `configuration` VARCHAR(500) BINARY DEFAULT '';
alter table assistant_config modify `timeout_connect` INTEGER DEFAULT 5;
alter table assistant_config modify `timeout_read` INTEGER DEFAULT 60;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.14') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.14';
commit;
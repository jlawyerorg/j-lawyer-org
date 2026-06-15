ALTER TABLE assistant_prompts ADD COLUMN model_ref VARCHAR(250) BINARY DEFAULT NULL;
ALTER TABLE assistant_prompts ADD COLUMN configuration VARCHAR(2500) BINARY DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.5.0.5') ON DUPLICATE KEY UPDATE settingValue = '3.5.0.5';
commit;

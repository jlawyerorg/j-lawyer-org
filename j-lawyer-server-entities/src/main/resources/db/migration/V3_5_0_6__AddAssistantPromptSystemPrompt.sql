ALTER TABLE assistant_prompts ADD COLUMN system_prompt VARCHAR(2500) BINARY DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.5.0.6') ON DUPLICATE KEY UPDATE settingValue = '3.5.0.6';
commit;

ALTER TABLE assistant_prompts MODIFY configuration VARCHAR(1000) BINARY DEFAULT NULL;
ALTER TABLE assistant_prompts MODIFY prompt_text VARCHAR(7500) BINARY DEFAULT NULL;
ALTER TABLE assistant_prompts MODIFY system_prompt VARCHAR(5000) BINARY DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.5.0.12') ON DUPLICATE KEY UPDATE settingValue     = '3.5.0.12';
commit;
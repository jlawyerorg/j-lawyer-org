-- Per-prompt option to anonymize personal data before a request is sent to the AI provider
-- (j-lawyer-ai v2 "anonymize"). Off by default; prompts with file inputs cannot be anonymized.
ALTER TABLE assistant_prompts ADD COLUMN anonymize TINYINT(1) NOT NULL DEFAULT 0;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.56') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.56';
commit;

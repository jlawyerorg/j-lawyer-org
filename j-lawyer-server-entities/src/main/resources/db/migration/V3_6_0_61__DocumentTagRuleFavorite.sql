-- Document tag rules can additionally mark matching documents as favorite (#3608).

alter table document_tag_rules add column `mark_favorite` BIT(1) DEFAULT 0;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.61') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.61';
commit;

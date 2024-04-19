-- SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = 'jlawyerdb';
-- SHOW TABLE STATUS LIKE 'case_documents';
-- SHOW FULL COLUMNS FROM case_history;

-- ALTER TABLE case_documents CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


ALTER TABLE case_documents MODIFY name VARCHAR(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE case_history MODIFY changeDescription VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.1.3') ON DUPLICATE KEY UPDATE settingValue     = '2.6.1.3';
commit;
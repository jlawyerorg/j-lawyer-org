SET @constraint_name = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_NAME = 'instantmessage'
    AND COLUMN_NAME = 'case_id'
);

SET @sql = CONCAT('ALTER TABLE instantmessage
    DROP FOREIGN KEY ', @constraint_name, ',
    ADD CONSTRAINT fk_cases_caseid FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE;'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.1.1') ON DUPLICATE KEY UPDATE settingValue     = '2.6.1.1';
commit;
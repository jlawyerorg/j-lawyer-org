alter table security_users add `abbreviation` VARCHAR(50) BINARY;
alter table security_users add `primary_group` VARCHAR(50) BINARY;

ALTER TABLE security_users
    ADD FOREIGN KEY
    fk_primary_group (primary_group)
    REFERENCES security_groups (id)
    ON DELETE SET NULL;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.5') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.5';
commit;
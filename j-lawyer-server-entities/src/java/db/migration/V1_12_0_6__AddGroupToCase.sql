alter table cases add `owner_group` VARCHAR(30) BINARY;

ALTER TABLE cases
    ADD FOREIGN KEY
    fk_group (owner_group)
    REFERENCES security_groups (id) ON DELETE RESTRICT;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.6') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.6';
commit;
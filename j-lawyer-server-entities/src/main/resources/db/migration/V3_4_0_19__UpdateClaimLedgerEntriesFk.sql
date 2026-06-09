ALTER TABLE claimledger_entries
    DROP FOREIGN KEY fk_entry_component;

ALTER TABLE claimledger_entries
    ADD CONSTRAINT fk_entry_component
        FOREIGN KEY (component_id) REFERENCES claimcomponents(id) ON DELETE CASCADE;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.19') ON DUPLICATE KEY UPDATE settingValue = '3.4.0.19';
commit;

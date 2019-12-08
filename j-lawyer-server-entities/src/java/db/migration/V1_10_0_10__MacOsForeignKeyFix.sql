ALTER TABLE campaign_contacts DROP FOREIGN KEY campaign_contacts_ibfk_2;
alter table campaign_contacts add CONSTRAINT campaign_contacts_ibfk_2 FOREIGN KEY (addressKey) REFERENCES contacts (id) ON DELETE CASCADE;

ALTER TABLE case_contacts DROP FOREIGN KEY case_contacts_ibfk_1;
alter table case_contacts add CONSTRAINT case_contacts_ibfk_1 FOREIGN KEY (archiveFileKey) REFERENCES cases (id) ON DELETE CASCADE;

-- IF EXISTS (
--               SELECT *
--               FROM INFORMATION_SCHEMA.STATISTICS
--               WHERE INDEX_SCHEMA = DATABASE()
--                     AND TABLE_NAME='case_contacts'
--                     AND INDEX_NAME = 'case_contacts_ibfk_2')
--         BEGIN
--             ALTER TABLE case_contacts DROP FOREIGN KEY case_contacts_ibfk_2;
--         END
--         END IF;


ALTER TABLE case_contacts DROP FOREIGN KEY case_contacts_ibfk_2;
alter table case_contacts add CONSTRAINT case_contacts_ibfk_2 FOREIGN KEY (addressKey) REFERENCES contacts (id) ON DELETE CASCADE;

ALTER TABLE case_documents DROP FOREIGN KEY case_documents_ibfk_1;
alter table case_documents add CONSTRAINT case_documents_ibfk_1 FOREIGN KEY (archiveFileKey) REFERENCES cases (id) ON DELETE CASCADE;

ALTER TABLE case_followups DROP FOREIGN KEY case_followups_ibfk_1;
alter table case_followups add CONSTRAINT case_followups_ibfk_1 FOREIGN KEY (archiveFileKey) REFERENCES cases (id) ON DELETE CASCADE;

ALTER TABLE case_history DROP FOREIGN KEY case_history_ibfk_1;
alter table case_history add CONSTRAINT case_history_ibfk_1 FOREIGN KEY (archiveFileKey) REFERENCES cases (id) ON DELETE CASCADE;

ALTER TABLE case_tags DROP FOREIGN KEY case_tags_ibfk_1;
alter table case_tags add CONSTRAINT case_tags_ibfk_1 FOREIGN KEY (archiveFileKey) REFERENCES cases (id) ON DELETE CASCADE;

ALTER TABLE communication_fax DROP FOREIGN KEY communication_fax_ibfk_1;
alter table communication_fax add CONSTRAINT communication_fax_ibfk_1 FOREIGN KEY (archiveFileKey) REFERENCES cases (id) ON DELETE CASCADE;

ALTER TABLE contact_tags DROP FOREIGN KEY contact_tags_ibfk_1;
alter table contact_tags add CONSTRAINT contact_tags_ibfk_1 FOREIGN KEY (addressKey) REFERENCES contacts (id) ON DELETE CASCADE;


insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.10') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.10';
commit;

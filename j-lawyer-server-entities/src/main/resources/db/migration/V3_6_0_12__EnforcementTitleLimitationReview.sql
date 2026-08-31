-- The follow-up that guards a title against lapsing was previously found again by a marker written
-- into its description ("[Titel:<id>]"). That marker is user-visible text and says nothing to the
-- person reading the follow-up, so the link belongs on the title instead.

alter table enforcement_titles add column `limitation_review_id` VARCHAR(50) BINARY DEFAULT NULL;

alter table enforcement_titles
    add constraint `fk_title_limitationreview` FOREIGN KEY (limitation_review_id) REFERENCES case_events(id) ON DELETE SET NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.12') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.12';
commit;

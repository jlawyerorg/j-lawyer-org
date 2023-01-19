ALTER TABLE cases ADD `date_created` datetime default null;
ALTER TABLE cases ADD `date_changed` datetime default null;
ALTER TABLE cases ADD `date_archived` datetime default null;

alter table cases add index `IDX_DATECHANGED` (date_changed);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.17') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.17';
commit;
alter table cases add index `IDX_LAWYER` (lawyer);
alter table cases add index `IDX_ASSISTANT` (assistant);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.0.0.2') ON DUPLICATE KEY UPDATE settingValue     = '3.0.0.2';
commit;
-- just a version bump. needed to fix backup sync for Synology and a proteced #recycle folder that cannot be deleted.
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.11.0.2') ON DUPLICATE KEY UPDATE settingValue     = '1.11.0.2';
commit;
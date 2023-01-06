alter table party_types add `sequence_no` INTEGER NOT NULL default 1;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.5') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.5';
commit;
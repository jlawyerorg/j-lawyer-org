alter table party_types add index `IDX_PARTYTYPES_SEQNO` (sequence_no);
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.4') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.4';
commit;
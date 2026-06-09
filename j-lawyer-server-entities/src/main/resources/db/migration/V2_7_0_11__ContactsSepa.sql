alter table contacts add `sepa_reference` varchar(50) BINARY DEFAULT '';
alter table contacts add `sepa_since` varchar(50) BINARY DEFAULT '';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.11') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.11';
commit;
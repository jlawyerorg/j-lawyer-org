alter table communication_epost add letter_type VARCHAR(100) BINARY default null;
alter table communication_epost add recipient_info VARCHAR(500) BINARY default null;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.5.0.3') ON DUPLICATE KEY UPDATE settingValue     = '2.5.0.3';
commit;
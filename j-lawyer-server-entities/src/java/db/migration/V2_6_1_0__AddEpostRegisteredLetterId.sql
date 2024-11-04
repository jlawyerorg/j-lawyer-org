alter table communication_epost add reg_letter_id VARCHAR(100) BINARY default null;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.1.0') ON DUPLICATE KEY UPDATE settingValue     = '2.6.1.0';
commit;
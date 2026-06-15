alter table claimcomponents add name VARCHAR(150) BINARY DEFAULT '';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.11') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.11';
commit;
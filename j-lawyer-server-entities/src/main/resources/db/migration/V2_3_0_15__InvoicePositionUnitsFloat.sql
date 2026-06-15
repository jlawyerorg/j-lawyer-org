alter table invoice_positions modify units FLOAT NOT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.15') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.15';
commit;
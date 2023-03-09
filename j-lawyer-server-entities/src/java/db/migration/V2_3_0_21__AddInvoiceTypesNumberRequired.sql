alter table invoice_types add `number_required` TINYINT DEFAULT 1;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.21') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.21';
commit;
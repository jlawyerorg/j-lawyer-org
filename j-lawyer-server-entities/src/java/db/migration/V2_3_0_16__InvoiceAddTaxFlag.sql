alter table invoices add `small_business` TINYINT;
update invoices set small_business=0;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.16') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.16';
commit;
alter table config_bankstatement add header_lines INTEGER DEFAULT 1;

update config_bankstatement set header_lines=1 where has_header=1;
update config_bankstatement set header_lines=0 where has_header=0;

alter table config_bankstatement drop column has_header;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.2.0.10') ON DUPLICATE KEY UPDATE settingValue     = '3.2.0.10';
commit;
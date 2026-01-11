alter table config_bankstatement add footer_lines INTEGER DEFAULT 0;
update config_bankstatement set footer_lines=0;

alter table config_bankstatement add dec_separator VARCHAR(5) BINARY NOT NULL DEFAULT ',';
alter table config_bankstatement add dec_groupingchar VARCHAR(5) BINARY NOT NULL DEFAULT '.';
update config_bankstatement set dec_separator=',';
update config_bankstatement set dec_groupingchar='.';

alter table config_bankstatement add dec_grouping BIT(1) DEFAULT 0;
update config_bankstatement set dec_grouping=0;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.17') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.17';
commit;
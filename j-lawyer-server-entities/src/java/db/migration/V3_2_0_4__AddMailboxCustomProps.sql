alter table mailbox_setup add props_in VARCHAR(1500) BINARY DEFAULT '';
alter table mailbox_setup add props_out VARCHAR(1500) BINARY DEFAULT '';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.2.0.4') ON DUPLICATE KEY UPDATE settingValue     = '3.2.0.4';
commit;
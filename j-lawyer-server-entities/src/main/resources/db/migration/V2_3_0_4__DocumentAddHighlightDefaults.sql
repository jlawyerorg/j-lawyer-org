alter table case_documents modify highlight1 integer not null default -2147483648;
alter table case_documents modify highlight2 integer not null default -2147483648;

update case_documents set highlight1=-2147483648;
update case_documents set highlight2=-2147483648;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.3.0.4') ON DUPLICATE KEY UPDATE settingValue     = '2.3.0.4';
commit;
ALTER TABLE contacts ADD `email_home` VARCHAR(250) BINARY DEFAULT NULL;
ALTER TABLE contacts ADD `email_misc` VARCHAR(250) BINARY DEFAULT NULL;

alter table contacts add index `IDX_CONTACTS_EMAIL_HOME` (email_home);
alter table contacts add index `IDX_CONTACTS_EMAIL_MISC` (email_misc);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.2') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.2';
commit;

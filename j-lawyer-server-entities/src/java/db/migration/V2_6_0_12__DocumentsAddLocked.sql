ALTER TABLE case_documents ADD `date_locked` datetime default null;
ALTER TABLE case_documents ADD `locked_by` VARCHAR(50) default null;

alter table security_users ADD `lock_documents` BIT(1) DEFAULT 1;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.12') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.12';
commit;
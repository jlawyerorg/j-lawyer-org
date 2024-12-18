alter table mailbox_setup add token_auth VARCHAR(4000) BINARY DEFAULT '';
alter table mailbox_setup add token_refresh VARCHAR(4000) BINARY DEFAULT '';
alter table mailbox_setup add token_expiry BIGINT DEFAULT 0;

alter table mailbox_setup add index idx_msexchange (msexchange);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.0.0.6') ON DUPLICATE KEY UPDATE settingValue     = '3.0.0.6';
commit;
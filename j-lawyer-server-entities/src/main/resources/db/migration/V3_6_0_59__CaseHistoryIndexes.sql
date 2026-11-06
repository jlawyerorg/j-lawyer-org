-- The cross-case history report (Auswertungen -> Aktenhistorie) searches case_history by change date,
-- optionally restricted to users, and sorts newest first. Without an index every search scans the
-- whole table, which grows with every change to any case.

alter table case_history add index `IDX_CASEHISTORY_CHANGEDATE` (changeDate);
alter table case_history add index `IDX_CASEHISTORY_PRINCIPAL_CHANGEDATE` (principal, changeDate);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.59') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.59';
commit;

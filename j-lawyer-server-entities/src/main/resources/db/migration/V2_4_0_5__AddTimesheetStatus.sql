alter table timesheets add `status` INTEGER NOT NULL DEFAULT 10;

alter table timesheets add index `IDX_TIMESHEETS_STATUS` (status);
alter table timesheet_positions add index `IDX_TIMESHEETPOS_PRINCIPAL` (principal);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.5') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.5';
commit;
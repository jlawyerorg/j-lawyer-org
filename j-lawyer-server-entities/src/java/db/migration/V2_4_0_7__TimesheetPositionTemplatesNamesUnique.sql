alter table timesheet_position_tpls add unique index `IDX_TIMESHTTPLS_NAME_UQ` (name);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.7') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.7';
commit;
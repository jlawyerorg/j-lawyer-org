-- Checks [[SCRIPT:...]] expressions of document templates against the allowed functions and classes
-- ("enforce"). Setting it to "log" only logs expressions that do not pass the check and runs them anyway.
-- An existing value is kept.
insert ignore into server_settings(settingKey, settingValue) values('jlawyer.server.templates.scriptcheck','enforce');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.57') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.57';
commit;

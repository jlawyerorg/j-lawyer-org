insert into server_options(id,optionGroup, value) values('timesheet.intervalminutes.1', 'timesheet.intervalminutes','1');
insert into server_options(id,optionGroup, value) values('timesheet.intervalminutes.5', 'timesheet.intervalminutes','5');
insert into server_options(id,optionGroup, value) values('timesheet.intervalminutes.10', 'timesheet.intervalminutes','10');
insert into server_options(id,optionGroup, value) values('timesheet.intervalminutes.15', 'timesheet.intervalminutes','15');
insert into server_options(id,optionGroup, value) values('timesheet.intervalminutes.30', 'timesheet.intervalminutes','30');


insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.2') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.2';
commit;
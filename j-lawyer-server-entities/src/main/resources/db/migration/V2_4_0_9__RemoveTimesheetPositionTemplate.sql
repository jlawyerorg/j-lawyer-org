delete from timesheet_position_tpls where id='tpl3';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.9') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.9';
commit;
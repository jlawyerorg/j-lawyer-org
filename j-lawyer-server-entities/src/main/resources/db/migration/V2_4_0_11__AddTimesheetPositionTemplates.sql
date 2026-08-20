insert into timesheet_position_tpls (id, name, description, tax_rate, unit_price) values ('tpl4', 'Arbeitszeiterfassung', 'Dokumentation von Beginn, Ende, Dauer der Arbeitszeiten, Pausenzeiten und Überstunden', 0.0, 0.0);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.11') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.11';
commit;
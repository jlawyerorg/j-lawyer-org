CREATE TABLE timesheet_position_tpls (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(80) BINARY, 
`description` VARCHAR(2500) BINARY, 
`tax_rate` FLOAT NOT NULL, 
`unit_price` FLOAT NOT NULL, 
CONSTRAINT `pk_timesheet_position_tpls` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into timesheet_position_tpls (id, name, description, tax_rate, unit_price) values ('tpl1', 'Beratung 150 EUR/h', 'Beratung, auch telefonisch / per Videokonferenz', 19.0, 150.0);
insert into timesheet_position_tpls (id, name, description, tax_rate, unit_price) values ('tpl2', 'Beratung 200 EUR/h', 'Beratung, auch telefonisch / per Videokonferenz', 19.0, 200.0);
insert into timesheet_position_tpls (id, name, description, tax_rate, unit_price) values ('tpl3', 'gefahrene Kilometer', 'gefahrene Strecke:', 19.0, 0.42);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.6') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.6';
commit;
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.bea.beaendpoint','https://ksw.bea-brak.de') ON DUPLICATE KEY UPDATE settingValue     = 'https://ksw.bea-brak.de';
insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.7') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.7';
commit;
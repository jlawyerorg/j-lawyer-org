insert into document_folders (id, name, parent_id) values ('Standardordner', 'Standardordner', null);
insert into document_folders (id, name, parent_id) values ('Abrechnung', 'Abrechnung', 'Standardordner');
insert into document_folders (id, name, parent_id) values ('Beratungshilfe', 'Beratungshilfe', 'Standardordner');
insert into document_folders (id, name, parent_id) values ('Mandant Korrespondenz', 'Mandant Korrespondenz', 'Standardordner');
insert into document_folders (id, name, parent_id) values ('Mandant Unterlagen', 'Mandant Unterlagen', 'Standardordner');
insert into document_folders (id, name, parent_id) values ('Kanzlei Dokumente', 'Kanzlei Dokumente', 'Standardordner');

insert into document_folder_tpls (id, name, root_folder) values ('default-folders', 'Standardordner', 'Standardordner');

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.14') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.14';
commit;
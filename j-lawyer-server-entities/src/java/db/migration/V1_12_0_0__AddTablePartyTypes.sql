CREATE TABLE party_types (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(250) BINARY unique, 
`placeholder` VARCHAR(250) BINARY, 
`color` INTEGER NOT NULL, 
CONSTRAINT `pk_party_types` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into party_types (id, name, placeholder, color) values ('10', 'Mandant', 'MANDANT', -6832371);
insert into party_types (id, name, placeholder, color) values ('20', 'Gegner', 'GEGNER', -2215621);
insert into party_types (id, name, placeholder, color) values ('30', 'Dritte', 'DRITTE', -15830347);

alter table case_contacts change referenceType referenceType varchar(50) BINARY NOT NULL;

DROP INDEX IDX_ARCHIVEFILEKEY_REFERENCETYPE ON case_contacts;
alter table case_contacts add index `IDX_ARCHIVEFILEKEY_REFERENCETYPE` (archiveFileKey, referenceType);

ALTER TABLE case_contacts ADD CONSTRAINT fk_reference_type FOREIGN KEY (referenceType) REFERENCES party_types(id);

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.12.0.0') ON DUPLICATE KEY UPDATE settingValue     = '1.12.0.0';
commit;
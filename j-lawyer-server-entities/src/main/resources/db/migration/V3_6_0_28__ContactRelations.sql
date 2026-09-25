-- Relationships between contacts (Kontaktbeziehungen) and the administrable catalogue of
-- relationship types.
--
-- A type carries one label per direction: "ist Mutter von" and, read from the other end,
-- "ist Kind von". That is the whole point of having its own table instead of an option group in
-- server_options, which can hold exactly one string per row. The user picks a label, and that
-- choice fixes both the type and the direction.
--
-- symmetric marks types that read the same in both directions ("ist Ehepartner von"). For those
-- the service keeps label_to equal to label_from, so the display needs no special case.
--
-- active hides a type from the pickers without deleting it, so relationships that already use it
-- keep their wording. Deleting a type is refused by the service while it is referenced; the
-- ON DELETE RESTRICT below keeps a direct SQL edit honest.
--
-- On contact_relations the order of the two ends is deliberately NOT normalised away: it carries
-- the meaning. (A, B, Mutter-Kind) says A is B's mother, (B, A, Mutter-Kind) says the opposite,
-- and both are storable - the model does not pretend to detect a factual contradiction. The one
-- exception is a symmetric type, where the two directions are the same statement: there the
-- service normalises the pair (smaller id first) before insert, so the unique index below can see
-- the mirrored duplicate.
--
-- The check constraint refuses a contact related to itself. It is accepted by every engine this
-- runs on but only enforced by MariaDB 10.2.1+ and MySQL 8.0.16+; the service rejects it first.
--
-- Both contact foreign keys cascade on delete: deleting a contact removes its relationships. That
-- is intentional - relationships are descriptive metadata and must not make a contact
-- undeletable - and it is why the client warns before deleting a contact that still has some.

CREATE TABLE contact_relation_types (
`id` VARCHAR(50) BINARY NOT NULL,
`name` VARCHAR(100) BINARY NOT NULL,
-- what the contact on the from side is to the other one, e.g. 'ist Mutter von'
`label_from` VARCHAR(100) BINARY NOT NULL,
-- the reverse, e.g. 'ist Kind von'; equal to label_from for symmetric types
`label_to` VARCHAR(100) BINARY NOT NULL,
`symmetric` TINYINT(1) DEFAULT 0 NOT NULL,
`category` VARCHAR(50) BINARY DEFAULT NULL,
`color` INTEGER DEFAULT 0,
`sequence_no` INTEGER DEFAULT 0,
`active` TINYINT(1) DEFAULT 1 NOT NULL,
CONSTRAINT `pk_contact_relation_types` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table contact_relation_types add unique index `UQ_CONTACTRELATIONTYPES_NAME` (name);
alter table contact_relation_types add index `IDX_CONTACTRELATIONTYPES_ACTIVE` (active);

CREATE TABLE contact_relations (
`id` VARCHAR(50) BINARY NOT NULL,
`from_contact_id` VARCHAR(50) BINARY NOT NULL,
`to_contact_id` VARCHAR(50) BINARY NOT NULL,
`type_id` VARCHAR(50) BINARY NOT NULL,
`note` VARCHAR(250) BINARY DEFAULT NULL,
`date_created` DATETIME DEFAULT NULL,
`created_by` VARCHAR(50) BINARY DEFAULT NULL,
CONSTRAINT `pk_contact_relations` PRIMARY KEY (`id`),
CONSTRAINT `fk_contactrelations_from` FOREIGN KEY (from_contact_id) REFERENCES contacts(id) ON DELETE CASCADE,
CONSTRAINT `fk_contactrelations_to` FOREIGN KEY (to_contact_id) REFERENCES contacts(id) ON DELETE CASCADE,
CONSTRAINT `fk_contactrelations_type` FOREIGN KEY (type_id) REFERENCES contact_relation_types(id) ON DELETE RESTRICT,
CONSTRAINT `ck_contactrelations_not_self` CHECK (from_contact_id <> to_contact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table contact_relations add unique index `UQ_CONTACTRELATIONS_TRIPLE` (from_contact_id, to_contact_id, type_id);
alter table contact_relations add index `IDX_CONTACTRELATIONS_TO` (to_contact_id);
alter table contact_relations add index `IDX_CONTACTRELATIONS_TYPE` (type_id);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.28') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.28';
commit;

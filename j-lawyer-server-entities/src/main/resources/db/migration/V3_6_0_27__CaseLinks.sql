-- Links two cases to each other (Aktenverknüpfung).
--
-- A link is symmetric: it does not distinguish a source from a target case, so it is stored
-- exactly once per pair. To make "the same pair only once, no matter which side the user started
-- from" enforceable by the database, the service normalises the order of the two ids before
-- insert (case_id_a < case_id_b), and the unique index below then rejects a duplicate in either
-- direction. Reading the links of a case therefore matches on both columns.
--
-- The check constraint refuses a case linked to itself. It is accepted by every engine this runs
-- on, but only enforced by MariaDB 10.2.1+ and MySQL 8.0.16+; older versions parse and ignore it.
-- That is acceptable because the service rejects a self-link before insert (and the unique index
-- would let the pair (X, X) through exactly once, so the constraint is a second line of defence,
-- not the first).
--
-- Both foreign keys cascade on delete, so removing a case removes its links and never leaves a
-- link pointing at a case that no longer exists.

CREATE TABLE case_links (
`id` VARCHAR(50) BINARY NOT NULL,
`case_id_a` VARCHAR(50) BINARY NOT NULL,
`case_id_b` VARCHAR(50) BINARY NOT NULL,
-- free text such as "Gegenakte" or "Folgesache zu 12/24"; deliberately not a configurable type
`description` VARCHAR(250) BINARY DEFAULT NULL,
`date_created` DATETIME DEFAULT NULL,
`created_by` VARCHAR(50) BINARY DEFAULT NULL,
CONSTRAINT `pk_case_links` PRIMARY KEY (`id`),
CONSTRAINT `fk_caselinks_case_a` FOREIGN KEY (case_id_a) REFERENCES cases(id) ON DELETE CASCADE,
CONSTRAINT `fk_caselinks_case_b` FOREIGN KEY (case_id_b) REFERENCES cases(id) ON DELETE CASCADE,
CONSTRAINT `ck_caselinks_not_self` CHECK (case_id_a <> case_id_b)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table case_links add unique index `UQ_CASELINKS_PAIR` (case_id_a, case_id_b);
alter table case_links add index `IDX_CASELINKS_B` (case_id_b);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.27') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.27';
commit;

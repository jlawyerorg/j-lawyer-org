-- Fristen und Dokumente einer Vollstreckungsmaßnahme.
--
-- Die Fristen liegen in einer eigenen Tabelle und nicht als einzelne Spalte an der Maßnahme:
-- eine Maßnahme traegt mehrere Fristen nebeneinander, und eine berichtigte Frist muss ihren
-- Kalendereintrag wiederfinden, statt einen zweiten anzulegen. Der Fristtyp ist es, der sie
-- wiederfindet - daher der eindeutige Index ueber Maßnahme und Typ.

CREATE TABLE enforcement_measure_deadlines (
`id` VARCHAR(50) BINARY NOT NULL,
`measure_id` VARCHAR(50) BINARY NOT NULL,
`deadline_type` VARCHAR(40) BINARY NOT NULL,
`deadline_date` DATE NOT NULL,
`review_id` VARCHAR(50) BINARY DEFAULT NULL,
`closed` TINYINT(1) DEFAULT 0 NOT NULL,
`closed_reason` VARCHAR(250) BINARY DEFAULT NULL,
CONSTRAINT `pk_enforcement_measure_deadlines` PRIMARY KEY (`id`),
CONSTRAINT `fk_enfdeadline_measure` FOREIGN KEY (measure_id) REFERENCES enforcement_measures(id) ON DELETE CASCADE,
CONSTRAINT `fk_enfdeadline_review` FOREIGN KEY (review_id) REFERENCES case_events(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table enforcement_measure_deadlines add index `IDX_ENFDEADLINES_MEASURE` (measure_id);
alter table enforcement_measure_deadlines add unique index `UQ_ENFDEADLINES_MEASURE_TYPE` (measure_id, deadline_type);

-- Welches Papier aus welcher Maßnahme entstanden ist. Das Dokument selbst bleibt ein Dokument der
-- Akte; wird die Maßnahme entfernt, faellt nur die Verknuepfung weg - was bei einem Gericht war,
-- bleibt in der Akte.

CREATE TABLE enforcement_measure_documents (
`id` VARCHAR(50) BINARY NOT NULL,
`measure_id` VARCHAR(50) BINARY NOT NULL,
`document_id` VARCHAR(50) BINARY NOT NULL,
`form_key` VARCHAR(50) BINARY DEFAULT NULL,
`form_version` VARCHAR(50) BINARY DEFAULT NULL,
`created_date` DATETIME DEFAULT NULL,
CONSTRAINT `pk_enforcement_measure_documents` PRIMARY KEY (`id`),
CONSTRAINT `fk_enfmeasuredoc_measure` FOREIGN KEY (measure_id) REFERENCES enforcement_measures(id) ON DELETE CASCADE,
CONSTRAINT `fk_enfmeasuredoc_document` FOREIGN KEY (document_id) REFERENCES case_documents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table enforcement_measure_documents add index `IDX_ENFMEASUREDOCS_MEASURE` (measure_id);
alter table enforcement_measure_documents add unique index `UQ_ENFMEASUREDOCS_DOCUMENT` (measure_id, document_id);

-- Die Sperrfrist des § 802d ZPO haengt an der Vermoegensauskunft. Woran die Maßnahmeart erkannt
-- wird, darf keine Zeichenkette im Namen sein: eine Kanzlei darf ihre Arten umbenennen und eigene
-- anlegen, ohne dass eine Frist von zwei Jahren still verschwindet.

alter table enforcement_measure_types add column `asset_disclosure` TINYINT(1) DEFAULT 0 NOT NULL;
update enforcement_measure_types set asset_disclosure = 1 where id = 'seed-measure-asset-disclosure';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.48') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.48';
commit;

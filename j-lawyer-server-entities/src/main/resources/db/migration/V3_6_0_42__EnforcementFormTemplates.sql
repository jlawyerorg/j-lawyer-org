-- Die amtlichen Formulare, ihre Fassungen und das Profil, das sagt, wie sie gefüllt werden.
--
-- Die ZVFV löst ihre Formulare von Zeit zu Zeit ab. Eine unter der alten Fassung erzeugte Maßnahme
-- war nicht falsch, sie war aktuell - deshalb stehen Fassungen nebeneinander, jede mit ihrem
-- Gültigkeitszeitraum, und jede Maßnahme vermerkt, welche sie benutzt hat.
--
-- Das PDF liegt in der Datenbank. Die Zahl der Formulare ist klein und die der Fassungen erst
-- recht; dafür ist eine Sicherung der Datenbank zugleich eine Sicherung von allem, was zum
-- Nachvollziehen einer Einreichung nötig ist.
--
-- Das Zuordnungsprofil gehört zur *Fassung*, nicht zum Formular: eine neue Fassung darf ihre Felder
-- umbenennen, und ein Profil der alten zeigte dann auf Felder, die es nicht mehr gibt.

CREATE TABLE IF NOT EXISTS `enforcement_form_templates` (
`id` VARCHAR(50) BINARY NOT NULL,
`form_key` VARCHAR(50) BINARY NOT NULL,          -- Anlage der ZVFV
`name` VARCHAR(255) BINARY NOT NULL,
`version` VARCHAR(50) BINARY,                    -- Stand, wie der Herausgeber ihn datiert
`valid_from` DATE,
`valid_to` DATE,                                 -- NULL, solange die Fassung die geltende ist
`pdf_content` LONGBLOB,
`file_name` VARCHAR(255) BINARY,
`description` VARCHAR(1000) BINARY,
PRIMARY KEY (`id`),
KEY `idx_formtemplate_key` (`form_key`, `valid_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Warum die Feldzuordnung Zeilen sind und kein Dokument in einer Spalte: ein Profil wird Feld für
-- Feld bearbeitet, zwischen Fassungen verglichen und gegen das Formular geprüft. Eine Zuordnung,
-- die ein Feld nennt, das es nicht mehr gibt, soll sich abfragen lassen und nicht erst beim Lesen
-- eines Dokuments auffallen.
CREATE TABLE IF NOT EXISTS `enforcement_form_field_mappings` (
`id` VARCHAR(50) BINARY NOT NULL,
`template_id` VARCHAR(50) BINARY NOT NULL,
`field_name` VARCHAR(255) BINARY NOT NULL,       -- der Name im PDF, nichtssagend für sich
`field_label` VARCHAR(500) BINARY,               -- der Tooltip, beim Schreiben des Profils übernommen
`source_key` VARCHAR(255) BINARY,                -- woher der Wert kommt
`fixed_value` VARCHAR(500) BINARY,               -- oder was fest eingetragen wird
`mandatory` TINYINT(1) DEFAULT 0 NOT NULL,
`comment` VARCHAR(500) BINARY,
PRIMARY KEY (`id`),
KEY `idx_formmapping_template` (`template_id`, `field_name`),
CONSTRAINT `fk_formmapping_template` FOREIGN KEY (template_id) REFERENCES enforcement_form_templates(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Die Maßnahme hält fest, mit welcher Fassung ihre Formulare erzeugt wurden. form_version gab es
-- schon als Text; hier kommt der Verweis auf die Vorlage dazu, damit sich die Datei selbst
-- wiederfinden lässt.
alter table enforcement_measures add column `form_template_id` VARCHAR(50) BINARY DEFAULT NULL;
alter table enforcement_measures add constraint `fk_measure_formtemplate`
  FOREIGN KEY (form_template_id) REFERENCES enforcement_form_templates(id) ON DELETE SET NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.42') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.42';
commit;

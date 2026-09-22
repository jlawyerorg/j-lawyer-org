-- Die Formulare einer Maßnahmeart als Liste mit Rollen statt als zwei lose Spalten.
--
-- Eine Maßnahme geht selten auf einem Blatt hinaus. Ein Pfändungs- und Überweisungsbeschluss
-- besteht aus dem Antrag (Anlage 4), dem Entwurf des Beschlusses, den das Gericht als seinen
-- übernimmt (Anlage 5), und der Forderungsaufstellung (Anlage 7 oder 8). Der Gerichtsvollzieher-
-- auftrag aus Anlage 1 und Anlage 6.
--
-- Drei ist heute das Maximum, aber die Zahl ist nicht der Punkt - die *Rolle* ist es, denn sie
-- entscheidet, woraus gefüllt wird: der Antrag aus Maßnahme, Parteien und Titel, der Entwurf aus
-- demselben in der Sprache einer Entscheidung, die Aufstellung aus dem Forderungskonto. Drei
-- namenlose Spalten müssten das aus ihrer Reihenfolge erraten.
--
-- V3_6_0_38 und V3_6_0_39 hatten form_key und itemisation_form_key als Spalten geführt. Ihre
-- Inhalte wandern hier in die Liste; die Spalten fallen weg.

CREATE TABLE IF NOT EXISTS `enforcement_measure_type_forms` (
`id` VARCHAR(50) BINARY NOT NULL,
`measure_type_id` VARCHAR(50) BINARY NOT NULL,
`form_key` VARCHAR(50) BINARY NOT NULL,
`form_role` VARCHAR(50) NOT NULL DEFAULT 'APPLICATION',
`sequence_number` INT DEFAULT 1 NOT NULL,
PRIMARY KEY (`id`),
KEY `idx_measuretypeform_type` (`measure_type_id`),
CONSTRAINT `fk_measuretypeform_type` FOREIGN KEY (measure_type_id) REFERENCES enforcement_measure_types(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Der Antrag: was bisher in form_key stand.
insert into enforcement_measure_type_forms (id, measure_type_id, form_key, form_role, sequence_number)
select concat(t.id, '-a'), t.id, t.form_key, 'APPLICATION', 1
  from enforcement_measure_types t
 where t.form_key is not null and t.form_key <> '';

-- Die Forderungsaufstellung: was in itemisation_form_key stand.
insert into enforcement_measure_type_forms (id, measure_type_id, form_key, form_role, sequence_number)
select concat(t.id, '-i'), t.id, t.itemisation_form_key, 'ITEMISATION', 3
  from enforcement_measure_types t
 where t.itemisation_form_key is not null and t.itemisation_form_key <> '';

-- Der Entwurf des Beschlusses, den es bisher überhaupt nicht gab. Das Gericht erwartet ihn
-- mitgeliefert; ohne ihn ist der Antrag unvollständig.
insert into enforcement_measure_type_forms (id, measure_type_id, form_key, form_role, sequence_number)
select concat(t.id, '-d'), t.id, 'ANLAGE_5', 'DRAFT_ORDER', 2
  from enforcement_measure_types t
 where t.form_key = 'ANLAGE_4';

insert into enforcement_measure_type_forms (id, measure_type_id, form_key, form_role, sequence_number)
select concat(t.id, '-d'), t.id, 'ANLAGE_3', 'DRAFT_ORDER', 2
  from enforcement_measure_types t
 where t.form_key = 'ANLAGE_2';

alter table enforcement_measure_types drop column `form_key`;
alter table enforcement_measure_types drop column `itemisation_form_key`;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.41') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.41';
commit;

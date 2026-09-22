-- Zwangsvollstreckungsmaßnahmen und der Katalog ihrer Arten.
--
-- Eine Maßnahme ist ein Vorgang, kein zweiter Ort, an dem Geld liegt: was beigetrieben wird, wird
-- ins Forderungskonto gebucht, und diese Zeile hält fest, was getan wurde, gegenüber wem, wann und
-- was dabei herauskam.
--
-- Die Arten stehen in einer Tabelle und nicht in einem Aufzählungstyp. Kanzleien nutzen
-- Verschiedenes - wer nie eine Zwangssicherungshypothek eintragen lässt, soll sie auch nicht
-- angeboten bekommen -, und was eine Art ausmacht, ist Konfiguration: welches amtliche Formular,
-- welcher Empfänger, welche Frist danach.
--
-- Zeichensatz utf8 wie das übrige Schema. Ein Fremdschlüssel verlangt übereinstimmende Zeichensätze,
-- und eine einzelne Tabelle in utf8mb4 scheitert beim Deployment mit errno 150.

CREATE TABLE IF NOT EXISTS `enforcement_measure_types` (
`id` VARCHAR(50) BINARY NOT NULL,
`name` VARCHAR(255) BINARY NOT NULL,
`sequence_number` INT DEFAULT 0 NOT NULL,
`addressee_type` VARCHAR(50),                  -- BAILIFF / ENFORCEMENT_COURT / ...
`form_key` VARCHAR(50),                        -- Anlage der ZVFV, NULL wo kein Formular vorgeschrieben ist
`requires_title` TINYINT(1) DEFAULT 1 NOT NULL,
`involves_third_party` TINYINT(1) DEFAULT 0 NOT NULL,
`follow_up_days` INT DEFAULT 0 NOT NULL,
`active` TINYINT(1) DEFAULT 1 NOT NULL,
`description` VARCHAR(1000) BINARY,
CONSTRAINT `pk_enforcement_measure_types` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `enforcement_measures` (
`id` VARCHAR(50) BINARY NOT NULL,
`ledger_id` VARCHAR(50) BINARY NOT NULL,
`title_id` VARCHAR(50) BINARY,                 -- NULL bei Arten, die keinen Titel brauchen
`measure_type_id` VARCHAR(50) BINARY,
`addressee_type` VARCHAR(50),
`addressee_contact_id` VARCHAR(50) BINARY,
`addressee_designation` VARCHAR(500) BINARY,   -- festgeschrieben wie eine Parteibezeichnung
`addressee_address` VARCHAR(1000) BINARY,
`ordered_date` DATE,
`dispatched_date` DATE,
`outcome` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
`outcome_date` DATE,
`form_version` VARCHAR(50),
`notes` VARCHAR(2000) BINARY,
CONSTRAINT `pk_enforcement_measures` PRIMARY KEY (`id`),
CONSTRAINT `fk_measure_ledger` FOREIGN KEY (ledger_id) REFERENCES claimledgers(id) ON DELETE CASCADE,
CONSTRAINT `fk_measure_title` FOREIGN KEY (title_id) REFERENCES enforcement_titles(id) ON DELETE SET NULL,
CONSTRAINT `fk_measure_type` FOREIGN KEY (measure_type_id) REFERENCES enforcement_measure_types(id) ON DELETE SET NULL,
CONSTRAINT `fk_measure_addressee` FOREIGN KEY (addressee_contact_id) REFERENCES contacts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table enforcement_measures add index `IDX_MEASURES_LEDGER` (ledger_id, ordered_date);

-- Gegen welche Schuldner sich die Maßnahme richtet. Nicht gegen alle des Forderungskontos: die
-- Vollstreckung läuft gegen den im Titel genannten Schuldner und kann gegen einen Gesamtschuldner
-- betrieben werden und gegen den anderen nicht.
CREATE TABLE IF NOT EXISTS `enforcement_measure_debtors` (
`measure_id` VARCHAR(50) BINARY NOT NULL,
`party_id` VARCHAR(50) BINARY NOT NULL,
CONSTRAINT `pk_enforcement_measure_debtors` PRIMARY KEY (`measure_id`, `party_id`),
CONSTRAINT `fk_measuredebtor_measure` FOREIGN KEY (measure_id) REFERENCES enforcement_measures(id) ON DELETE CASCADE,
CONSTRAINT `fk_measuredebtor_party` FOREIGN KEY (party_id) REFERENCES claimledger_parties(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.37') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.37';
commit;

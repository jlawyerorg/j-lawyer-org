-- Value-based fee scales (Gebührentabellen) as maintainable data.
--
-- § 13 Abs. 1 RVG and § 34 Abs. 1 GKG are not tables but the same algorithm with different numbers:
-- a fixed fee up to a first value, then, for each started amount of a further step, an increment -
-- with the steps and increments changing at defined bracket boundaries. The published tables (RVG
-- Anlage 2, GKG Anlage 2) are only a rendering of that algorithm and stop at 500,000 Euro, while the
-- law itself continues above it. Storing the algorithm therefore covers more than storing the table
-- would, and it is what is stored here.
--
--
-- Ein Beispiel, weil die Tabelle in fee_scale_brackets anders aussieht als die gedruckte
-- Gebührentabelle: dort steht *keine* Zeile "1.500 Euro". Der Streitwert 1.500 Euro liegt in der
-- ersten Stufe (bis 2.000 Euro, je angefangene 500 Euro + 21,00 Euro) und wird daraus berechnet:
--
--   40,00 Euro                        Grundbetrag fuer Werte bis 500 Euro
-- + 21,00 Euro x 2                    (1.500 - 500) = 1.000 Euro, das sind 2 angefangene 500er
-- = 82,00 Euro                        -- genau der Wert, den die amtliche Tabelle fuer 1.500 nennt
--
-- Die sieben Zeilen je Tabelle sind also die sieben Staffeln des Gesetzes, nicht die rund 42
-- Zeilen der gedruckten Wertetabelle. Letztere ist vollstaendig aus ersteren ableitbar - und
-- umgekehrt nicht: die gedruckte Tabelle endet bei 500.000 Euro, das Gesetz laeuft weiter.
-- Validity ranges are not optional. Fee law changes on a fixed date and § 60 RVG has matters
-- commissioned before it billed under the previous law, so an installation regularly needs two
-- scales at once: the current one and its predecessor. valid_to stays empty for the scale in force.
--
-- Seeded is the state after the KostBRÄG 2025, in force since 1 June 2025 (RVG Anlage 2: BGBl. 2025
-- I Nr. 109, S. 30). The predecessor scale is deliberately not invented here - an installation that
-- still bills matters commissioned before that date enters it, or gets it with a later update.

CREATE TABLE fee_scales (
`id` VARCHAR(50) BINARY NOT NULL,
`scale_key` VARCHAR(50) BINARY NOT NULL,      -- e.g. RVG_13, GKG_34
`name` VARCHAR(250) BINARY NOT NULL,
`legal_basis` VARCHAR(250) BINARY DEFAULT NULL,
`source_reference` VARCHAR(250) BINARY DEFAULT NULL,
`valid_from` DATE DEFAULT NULL,
`valid_to` DATE DEFAULT NULL,                 -- empty while the scale is the one in force
`base_up_to` DECIMAL(12,2) NOT NULL,          -- the first bracket: a value up to this costs base_amount
`base_amount` DECIMAL(10,2) NOT NULL,
`minimum_amount` DECIMAL(10,2) DEFAULT NULL,  -- § 13 Abs. 3 RVG, § 34 Abs. 2 GKG
`active` TINYINT(1) DEFAULT 1 NOT NULL,
`notes` VARCHAR(1000) BINARY DEFAULT NULL,
CONSTRAINT `pk_fee_scales` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table fee_scales add index `IDX_FEESCALES_KEY` (scale_key);

CREATE TABLE fee_scale_brackets (
`id` VARCHAR(50) BINARY NOT NULL,
`scale_id` VARCHAR(50) BINARY NOT NULL,
-- the upper bound of the bracket; empty for the last one, which continues without limit
`up_to` DECIMAL(12,2) DEFAULT NULL,
`step_amount` DECIMAL(12,2) NOT NULL,         -- "für jeden angefangenen Betrag von weiteren ... Euro"
`increment_amount` DECIMAL(10,2) NOT NULL,    -- "um ... Euro"
`sequence_number` INT DEFAULT 0 NOT NULL,
CONSTRAINT `pk_fee_scale_brackets` PRIMARY KEY (`id`),
CONSTRAINT `fk_feebracket_scale` FOREIGN KEY (scale_id) REFERENCES fee_scales(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table fee_scale_brackets add index `IDX_FEEBRACKETS_SCALE` (scale_id, sequence_number);

INSERT INTO fee_scales (id, scale_key, name, legal_basis, source_reference, valid_from,
    base_up_to, base_amount, minimum_amount, active)
SELECT * FROM (SELECT 'seed-feescale-rvg13-2025' AS id, 'RVG_13' AS scale_key,
    'Anwaltsgebühren nach dem Gegenstandswert' AS name,
    '§ 13 Abs. 1 RVG' AS legal_basis,
    'KostBRÄG 2025, Anlage 2 zu § 13 Abs. 1 S. 3 RVG (BGBl. 2025 I Nr. 109, S. 30)' AS source_reference,
    '2025-06-01' AS valid_from, 500.00 AS base_up_to, 51.50 AS base_amount, 15.00 AS minimum_amount,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scales s WHERE s.id = 'seed-feescale-rvg13-2025');

INSERT INTO fee_scales (id, scale_key, name, legal_basis, source_reference, valid_from,
    base_up_to, base_amount, minimum_amount, active)
SELECT * FROM (SELECT 'seed-feescale-gkg34-2025' AS id, 'GKG_34' AS scale_key,
    'Gerichtsgebühren nach dem Streitwert' AS name,
    '§ 34 Abs. 1 GKG' AS legal_basis,
    'KostBRÄG 2025, Anlage 2 zu § 34 Abs. 1 S. 3 GKG' AS source_reference,
    '2025-06-01' AS valid_from, 500.00 AS base_up_to, 40.00 AS base_amount, 15.00 AS minimum_amount,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scales s WHERE s.id = 'seed-feescale-gkg34-2025');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-rvg13-2025-0' AS id, 'seed-feescale-rvg13-2025' AS scale_id, 2000.00 AS up_to,
    500.00 AS step_amount, 41.50 AS increment_amount, 0 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-rvg13-2025-0');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-rvg13-2025-1' AS id, 'seed-feescale-rvg13-2025' AS scale_id, 10000.00 AS up_to,
    1000.00 AS step_amount, 59.50 AS increment_amount, 1 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-rvg13-2025-1');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-rvg13-2025-2' AS id, 'seed-feescale-rvg13-2025' AS scale_id, 25000.00 AS up_to,
    3000.00 AS step_amount, 55.00 AS increment_amount, 2 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-rvg13-2025-2');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-rvg13-2025-3' AS id, 'seed-feescale-rvg13-2025' AS scale_id, 50000.00 AS up_to,
    5000.00 AS step_amount, 86.00 AS increment_amount, 3 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-rvg13-2025-3');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-rvg13-2025-4' AS id, 'seed-feescale-rvg13-2025' AS scale_id, 200000.00 AS up_to,
    15000.00 AS step_amount, 99.50 AS increment_amount, 4 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-rvg13-2025-4');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-rvg13-2025-5' AS id, 'seed-feescale-rvg13-2025' AS scale_id, 500000.00 AS up_to,
    30000.00 AS step_amount, 140.00 AS increment_amount, 5 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-rvg13-2025-5');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-rvg13-2025-6' AS id, 'seed-feescale-rvg13-2025' AS scale_id, NULL AS up_to,
    50000.00 AS step_amount, 175.00 AS increment_amount, 6 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-rvg13-2025-6');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-gkg34-2025-0' AS id, 'seed-feescale-gkg34-2025' AS scale_id, 2000.00 AS up_to,
    500.00 AS step_amount, 21.00 AS increment_amount, 0 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-gkg34-2025-0');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-gkg34-2025-1' AS id, 'seed-feescale-gkg34-2025' AS scale_id, 10000.00 AS up_to,
    1000.00 AS step_amount, 22.50 AS increment_amount, 1 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-gkg34-2025-1');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-gkg34-2025-2' AS id, 'seed-feescale-gkg34-2025' AS scale_id, 25000.00 AS up_to,
    3000.00 AS step_amount, 30.50 AS increment_amount, 2 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-gkg34-2025-2');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-gkg34-2025-3' AS id, 'seed-feescale-gkg34-2025' AS scale_id, 50000.00 AS up_to,
    5000.00 AS step_amount, 40.50 AS increment_amount, 3 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-gkg34-2025-3');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-gkg34-2025-4' AS id, 'seed-feescale-gkg34-2025' AS scale_id, 200000.00 AS up_to,
    15000.00 AS step_amount, 140.00 AS increment_amount, 4 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-gkg34-2025-4');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-gkg34-2025-5' AS id, 'seed-feescale-gkg34-2025' AS scale_id, 500000.00 AS up_to,
    30000.00 AS step_amount, 210.00 AS increment_amount, 5 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-gkg34-2025-5');

INSERT INTO fee_scale_brackets (id, scale_id, up_to, step_amount, increment_amount, sequence_number)
SELECT * FROM (SELECT 'seed-feebracket-gkg34-2025-6' AS id, 'seed-feescale-gkg34-2025' AS scale_id, NULL AS up_to,
    50000.00 AS step_amount, 210.00 AS increment_amount, 6 AS sequence_number) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_scale_brackets b WHERE b.id = 'seed-feebracket-gkg34-2025-6');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.21') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.21';
commit;

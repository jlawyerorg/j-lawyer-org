-- A usable default reminder cycle, so the pre-court dunning works out of the box instead of
-- requiring a firm to invent one before it can send a first reminder.
--
-- The ids are fixed rather than generated, so re-running this seed cannot create a second copy of
-- a stage. Existing rows are left untouched: a firm that has already adjusted a stage keeps its
-- own wording, period and charge.
--
-- Two notes on what the configuration does and does not decide:
--
-- triggers_default marks the reminder that puts the debtor in default under § 286 Abs. 1 BGB, which
-- is what starts default interest running. It is set on the first proper Mahnung, not on the
-- payment reminder. It is not the only way default arises - § 286 Abs. 2 BGB lets it follow from a
-- calendar date or a period fixed by the contract, and § 286 Abs. 3 BGB from 30 days after an
-- invoice, against a consumer only if he was told so. Those cases are recorded on the claim itself,
-- not here.
--
-- charge_amount is what the reminder is billed at. A reminder charge may only cover the costs the
-- creditor actually incurs, so the amount is deliberately modest and every firm should check it
-- against its own situation.

INSERT INTO dunning_stages (id, name, sequence_number, template_folder, template_name,
    payment_period_days, charge_amount, triggers_default, active, description)
SELECT * FROM (SELECT
    'seed-dunning-stage-1' AS id,
    'Zahlungserinnerung' AS name,
    10 AS sequence_number,
    NULL AS template_folder,
    NULL AS template_name,
    14 AS payment_period_days,
    0.00 AS charge_amount,
    0 AS triggers_default,
    1 AS active,
    'Höflicher Hinweis auf die offene Forderung. Löst für sich genommen keinen Verzug aus und wird nicht berechnet.' AS description
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_stages WHERE id = 'seed-dunning-stage-1');

INSERT INTO dunning_stages (id, name, sequence_number, template_folder, template_name,
    payment_period_days, charge_amount, triggers_default, active, description)
SELECT * FROM (SELECT
    'seed-dunning-stage-2' AS id,
    '1. Mahnung' AS name,
    20 AS sequence_number,
    NULL AS template_folder,
    NULL AS template_name,
    14 AS payment_period_days,
    2.50 AS charge_amount,
    1 AS triggers_default,
    1 AS active,
    'Erste förmliche Mahnung. Setzt den Schuldner nach § 286 Abs. 1 BGB in Verzug, sofern er es nicht bereits ist; ab diesem Zeitpunkt laufen Verzugszinsen nach § 288 BGB.' AS description
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_stages WHERE id = 'seed-dunning-stage-2');

INSERT INTO dunning_stages (id, name, sequence_number, template_folder, template_name,
    payment_period_days, charge_amount, triggers_default, active, description)
SELECT * FROM (SELECT
    'seed-dunning-stage-3' AS id,
    '2. Mahnung' AS name,
    30 AS sequence_number,
    NULL AS template_folder,
    NULL AS template_name,
    10 AS payment_period_days,
    2.50 AS charge_amount,
    0 AS triggers_default,
    1 AS active,
    'Zweite Mahnung mit verkürzter Frist. Der Verzug besteht bereits seit der ersten Mahnung und beginnt nicht neu.' AS description
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_stages WHERE id = 'seed-dunning-stage-3');

INSERT INTO dunning_stages (id, name, sequence_number, template_folder, template_name,
    payment_period_days, charge_amount, triggers_default, active, description)
SELECT * FROM (SELECT
    'seed-dunning-stage-4' AS id,
    'Letzte Mahnung vor gerichtlichen Schritten' AS name,
    40 AS sequence_number,
    NULL AS template_folder,
    NULL AS template_name,
    7 AS payment_period_days,
    2.50 AS charge_amount,
    0 AS triggers_default,
    1 AS active,
    'Ankündigung des gerichtlichen Mahnverfahrens mit kurzer Frist. Läuft auch diese ab, ist der Mahnbescheid zu beantragen.' AS description
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_stages WHERE id = 'seed-dunning-stage-4');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.14') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.14';
commit;

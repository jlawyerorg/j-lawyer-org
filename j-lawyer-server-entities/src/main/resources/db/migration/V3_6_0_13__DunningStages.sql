-- Pre-court reminder cycle (außergerichtliches Mahnwesen).
--
-- dunning_stages is firm-wide configuration: which reminders exist, in which order, with which
-- payment period, which charge and whether sending one puts the debtor in default. dunning_stage_
-- events records per claim ledger which stage was actually sent, when, with which deadline, and
-- which document, charge booking and follow-up it produced - so a stage can be traced and its
-- effects undone.

CREATE TABLE dunning_stages (
`id` VARCHAR(50) BINARY NOT NULL,
`name` VARCHAR(150) BINARY NOT NULL,
`sequence_number` INT DEFAULT 0 NOT NULL,
`template_folder` VARCHAR(500) BINARY,
`template_name` VARCHAR(250) BINARY,
`payment_period_days` INT DEFAULT 14 NOT NULL,
`charge_amount` DECIMAL(10,2) DEFAULT 0.00 NOT NULL,
`triggers_default` TINYINT(1) DEFAULT 0 NOT NULL,
`active` TINYINT(1) DEFAULT 1 NOT NULL,
`description` VARCHAR(500) BINARY,
CONSTRAINT `pk_dunning_stages` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table dunning_stages add index `IDX_DUNNINGSTAGES_SEQUENCE` (sequence_number);

CREATE TABLE dunning_stage_events (
`id` VARCHAR(50) BINARY NOT NULL,
`ledger_id` VARCHAR(50) BINARY NOT NULL,
`stage_id` VARCHAR(50) BINARY,
`stage_name` VARCHAR(150) BINARY,          -- kept so history survives a renamed or deleted stage
`sequence_number` INT DEFAULT 0 NOT NULL,
`sent_date` DATE NOT NULL,
`due_date` DATE DEFAULT NULL,
`triggered_default` TINYINT(1) DEFAULT 0 NOT NULL,
`charge_amount` DECIMAL(10,2) DEFAULT 0.00 NOT NULL,
`document_id` VARCHAR(50) BINARY DEFAULT NULL,
`charge_entry_id` VARCHAR(50) BINARY DEFAULT NULL,
`review_id` VARCHAR(50) BINARY DEFAULT NULL,
`comment` VARCHAR(500) BINARY,
CONSTRAINT `pk_dunning_stage_events` PRIMARY KEY (`id`),
CONSTRAINT `fk_stageevent_ledger` FOREIGN KEY (ledger_id) REFERENCES claimledgers(id) ON DELETE CASCADE,
CONSTRAINT `fk_stageevent_stage` FOREIGN KEY (stage_id) REFERENCES dunning_stages(id) ON DELETE SET NULL,
CONSTRAINT `fk_stageevent_document` FOREIGN KEY (document_id) REFERENCES case_documents(id) ON DELETE SET NULL,
CONSTRAINT `fk_stageevent_entry` FOREIGN KEY (charge_entry_id) REFERENCES claimledger_entries(id) ON DELETE SET NULL,
CONSTRAINT `fk_stageevent_review` FOREIGN KEY (review_id) REFERENCES case_events(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table dunning_stage_events add index `IDX_STAGEEVENTS_LEDGER` (ledger_id, sent_date);

-- § 288 Abs. 2 BGB grants the higher default interest only for payment claims (Entgeltforderungen),
-- and § 288 Abs. 5 BGB ties the lump sum to the same condition, so the claim has to say whether it
-- is one.
alter table claimcomponents add column `payment_claim` TINYINT(1) DEFAULT 0 NOT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.13') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.13';
commit;

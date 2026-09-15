-- The court dunning procedure (gerichtliches Mahnverfahren) as a record attached to a claim ledger,
-- together with the journal of its status changes.
--
-- A few decisions are worth stating, because they are not obvious from the columns alone.
--
-- The court is held by its XJustiz identifier rather than by a foreign key: the court master data
-- arrives with the court directory, and until then the identifier is the stable key that a later
-- foreign key can be derived from. Name, postcode and place of the court are stored alongside it,
-- not out of redundancy but because an application that has gone out has to stay reconstructable
-- after the court record is corrected - and because the EDA application addresses the court by
-- postcode and place, so those two are what was actually used.
--
-- The procedural dates each get their own column instead of being left to the journal. The deadline
-- engine has to ask "when was the Mahnbescheid served" and answer it in a query; reading that out
-- of a history table would turn every deadline computation into a scan.
--
-- The amounts applied for are a snapshot taken when the application goes out. The ledger keeps
-- moving afterwards - payments, further costs - and what was applied for must not move with it.

CREATE TABLE dunning_cases (
`id` VARCHAR(50) BINARY NOT NULL,
`ledger_id` VARCHAR(50) BINARY NOT NULL,

`court_xjustiz_id` VARCHAR(20) BINARY DEFAULT NULL,
`court_name` VARCHAR(250) BINARY DEFAULT NULL,       -- snapshot: what the application was addressed to
`court_postal_code` VARCHAR(10) BINARY DEFAULT NULL,
`court_city` VARCHAR(250) BINARY DEFAULT NULL,

`own_reference` VARCHAR(50) BINARY DEFAULT NULL,     -- echoed by the court, so the import matches on it
`court_file_number` VARCHAR(50) BINARY DEFAULT NULL, -- assigned by the court once it has processed
`kennziffer` VARCHAR(20) BINARY DEFAULT NULL,        -- identifies the acting lawyer to the court

`status` VARCHAR(30) BINARY NOT NULL,
`status_date` DATE DEFAULT NULL,

`application_date` DATE DEFAULT NULL,
`applied_principal` DECIMAL(10,2) DEFAULT NULL,
`applied_interest` DECIMAL(10,2) DEFAULT NULL,
`applied_costs` DECIMAL(10,2) DEFAULT NULL,
`applied_total` DECIMAL(10,2) DEFAULT NULL,

`mb_applied_date` DATE DEFAULT NULL,
`mb_issued_date` DATE DEFAULT NULL,
`mb_served_date` DATE DEFAULT NULL,
`objection_date` DATE DEFAULT NULL,
-- set only for an objection against part of the claim; the uncontested remainder follows from the
-- amount applied for and is not stored a second time
`contested_amount` DECIMAL(10,2) DEFAULT NULL,
`vb_applied_date` DATE DEFAULT NULL,
`vb_issued_date` DATE DEFAULT NULL,
`vb_served_date` DATE DEFAULT NULL,
`einspruch_date` DATE DEFAULT NULL,
`referral_date` DATE DEFAULT NULL,
`completed_date` DATE DEFAULT NULL,

`description` VARCHAR(500) BINARY DEFAULT NULL,
`created` DATETIME DEFAULT NULL,
`created_by` VARCHAR(50) BINARY DEFAULT NULL,

CONSTRAINT `pk_dunning_cases` PRIMARY KEY (`id`),
CONSTRAINT `fk_dunningcase_ledger` FOREIGN KEY (ledger_id) REFERENCES claimledgers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table dunning_cases add index `IDX_DUNNINGCASES_STATUS` (status);
alter table dunning_cases add index `IDX_DUNNINGCASES_OWNREF` (own_reference);
alter table dunning_cases add index `IDX_DUNNINGCASES_FILENUMBER` (court_file_number);

-- The journal of status changes. It records who changed what and on what basis, because a status
-- that arrived through an imported court message carries different weight than one a user typed,
-- and later questions about a procedure are questions about that difference.

CREATE TABLE dunning_case_events (
`id` VARCHAR(50) BINARY NOT NULL,
`dunning_case_id` VARCHAR(50) BINARY NOT NULL,
`previous_status` VARCHAR(30) BINARY DEFAULT NULL,
`new_status` VARCHAR(30) BINARY NOT NULL,
`event_date` DATE DEFAULT NULL,                      -- the procedural date, not the day it was typed
`recorded` DATETIME DEFAULT NULL,
`recorded_by` VARCHAR(50) BINARY DEFAULT NULL,
`source` VARCHAR(20) BINARY NOT NULL,
`comment` VARCHAR(500) BINARY DEFAULT NULL,
CONSTRAINT `pk_dunning_case_events` PRIMARY KEY (`id`),
CONSTRAINT `fk_dunningcaseevent_case` FOREIGN KEY (dunning_case_id) REFERENCES dunning_cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table dunning_case_events add index `IDX_DUNNINGCASEEVENTS_CASE` (dunning_case_id);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.15') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.15';
commit;

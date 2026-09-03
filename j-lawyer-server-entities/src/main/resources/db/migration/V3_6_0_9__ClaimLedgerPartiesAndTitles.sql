-- Claim ledger foundation for the dunning and enforcement workflow.
--
-- Parties of a claim ledger are identified by a reference to the address book contact, the same
-- reference invoices, payments and case_account_entries use. The FK is ON DELETE SET NULL, as
-- established by V3_6_0_6: deleting a contact must not delete the ledger it is a party of. The
-- case party record (case_contacts) is kept only as an optional back-reference to the role the
-- party holds in the case - a ledger party need not be a case party at all. Because an
-- enforcement title stays enforceable for 30 years (§ 197 Abs. 1 Nr. 3 BGB), designation and
-- address as used towards the court are additionally frozen in a snapshot on first use.

CREATE TABLE claimledger_parties (
`id` VARCHAR(50) BINARY NOT NULL,
`ledger_id` VARCHAR(50) BINARY NOT NULL,
`party_role` VARCHAR(20) NOT NULL,                  -- CREDITOR / DEBTOR
`sequence_number` INT DEFAULT 0 NOT NULL,
`contact_id` VARCHAR(50) BINARY,                    -- identity of the party
`case_contact_id` VARCHAR(50) BINARY,               -- optional: role it was derived from
`legal_representative_id` VARCHAR(50) BINARY,       -- gesetzlicher Vertreter
`authorised_representative_id` VARCHAR(50) BINARY,  -- Bevollmächtigter
`consumer` TINYINT(1) DEFAULT 0 NOT NULL,
`snapshot_taken` DATETIME DEFAULT NULL,             -- set on first use towards a court
`snapshot_designation` VARCHAR(500) BINARY,
`snapshot_address` VARCHAR(1000) BINARY,
CONSTRAINT `pk_claimledger_parties` PRIMARY KEY (`id`),
CONSTRAINT `fk_ledgerparty_ledger` FOREIGN KEY (ledger_id) REFERENCES claimledgers(id) ON DELETE CASCADE,
CONSTRAINT `fk_ledgerparty_contact` FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE SET NULL,
CONSTRAINT `fk_ledgerparty_casecontact` FOREIGN KEY (case_contact_id) REFERENCES case_contacts(id) ON DELETE SET NULL,
CONSTRAINT `fk_ledgerparty_legalrep` FOREIGN KEY (legal_representative_id) REFERENCES contacts(id) ON DELETE SET NULL,
CONSTRAINT `fk_ledgerparty_authrep` FOREIGN KEY (authorised_representative_id) REFERENCES contacts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table claimledger_parties add index `IDX_LEDGERPARTIES_LEDGER` (ledger_id, party_role, sequence_number);

-- Ledger master data: sub-ledger (Unterkonto), the effective number of creditors (one contact may
-- stand for several creditors, which drives the Nr. 1008 VV RVG increase), the payment allocation
-- mode and how a remaining surplus is handled, and the consumer-loan flag for § 497 Abs. 3 BGB.

alter table claimledgers add column `parent_ledger_id` VARCHAR(50) BINARY DEFAULT NULL;
alter table claimledgers add column `effective_creditor_count` INT DEFAULT NULL;
alter table claimledgers add column `allocation_mode` VARCHAR(30) DEFAULT 'LEGAL' NOT NULL;
alter table claimledgers add column `surplus_handling` VARCHAR(30) DEFAULT 'REALLOCATE' NOT NULL;
alter table claimledgers add column `consumer_loan` TINYINT(1) DEFAULT 0 NOT NULL;

alter table claimledgers
    add constraint `fk_claimledger_parent` FOREIGN KEY (parent_ledger_id) REFERENCES claimledgers(id) ON DELETE SET NULL;

-- Enforcement titles (Titelverwaltung). The three formal prerequisites of enforcement are Titel,
-- Klausel and Zustellung; limitation_date carries the 30-year date of § 197 Abs. 1 Nr. 3 BGB.

CREATE TABLE enforcement_titles (
`id` VARCHAR(50) BINARY NOT NULL,
`ledger_id` VARCHAR(50) BINARY NOT NULL,
`title_type` VARCHAR(50) NOT NULL,
`issuing_body` VARCHAR(250) BINARY,
`court_id` VARCHAR(50) BINARY DEFAULT NULL,         -- filled once the court directory exists
`file_number` VARCHAR(100) BINARY,
`issue_date` DATE DEFAULT NULL,
`clause_date` DATE DEFAULT NULL,                    -- vollstreckbare Ausfertigung mit Klausel
`service_date` DATE DEFAULT NULL,                   -- Zustellung an den Schuldner
`limitation_date` DATE DEFAULT NULL,
`subject_matter` VARCHAR(1000) BINARY,
`comment` VARCHAR(1000) BINARY,
CONSTRAINT `pk_enforcement_titles` PRIMARY KEY (`id`),
CONSTRAINT `fk_title_ledger` FOREIGN KEY (ledger_id) REFERENCES claimledgers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table enforcement_titles add index `IDX_TITLES_LEDGER` (ledger_id);
alter table enforcement_titles add index `IDX_TITLES_LIMITATION` (limitation_date);

-- A title may run against a subset of the ledger's debtors only.
CREATE TABLE enforcement_title_debtors (
`title_id` VARCHAR(50) BINARY NOT NULL,
`party_id` VARCHAR(50) BINARY NOT NULL,
CONSTRAINT `pk_enforcement_title_debtors` PRIMARY KEY (`title_id`, `party_id`),
CONSTRAINT `fk_titledebtor_title` FOREIGN KEY (title_id) REFERENCES enforcement_titles(id) ON DELETE CASCADE,
CONSTRAINT `fk_titledebtor_party` FOREIGN KEY (party_id) REFERENCES claimledger_parties(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Claim components: the EDA dunning application keeps the pre-court cost categories apart and
-- will not accept them merged, so component_type carries them individually. interest_start_mode
-- distinguishes a fixed start date from interest running from service of the dunning order, which
-- the EDA format expresses by leaving the interest start date empty.

alter table claimcomponents add column `interest_start_mode` VARCHAR(20) DEFAULT 'FIXED_DATE' NOT NULL;
alter table claimcomponents add column `recurrence_start_month` VARCHAR(7) DEFAULT NULL;   -- YYYY-MM
alter table claimcomponents add column `recurrence_end_month` VARCHAR(7) DEFAULT NULL;     -- YYYY-MM
alter table claimcomponents add column `origin_reference` VARCHAR(150) BINARY DEFAULT NULL;

-- Main claim classification: a catalogue number of the Hauptforderungskatalog published by the
-- dunning courts, or a free-text claim ("sonstiger Anspruch") where the catalogue has no entry.
-- Some catalogue numbers demand additional data - the property for 17/19/20/90, the contract
-- designation for 28, account/meter/service details for others - which the EDA application
-- rejects if missing.

alter table claimcomponents add column `catalogue_number` VARCHAR(10) DEFAULT NULL;
alter table claimcomponents add column `free_text_claim` TINYINT(1) DEFAULT 0 NOT NULL;
alter table claimcomponents add column `catalogue_property_zip` VARCHAR(20) BINARY DEFAULT NULL;
alter table claimcomponents add column `catalogue_property_city` VARCHAR(150) BINARY DEFAULT NULL;
alter table claimcomponents add column `catalogue_contract_designation` VARCHAR(250) BINARY DEFAULT NULL;
alter table claimcomponents add column `catalogue_reference_detail` VARCHAR(250) BINARY DEFAULT NULL;

-- A cost booking may be owed by one debtor alone instead of jointly (§ 788 Abs. 1 S. 2 ZPO).
alter table claimledger_entries add column `debtor_party_id` VARCHAR(50) BINARY DEFAULT NULL;
alter table claimledger_entries
    add constraint `fk_entry_debtorparty` FOREIGN KEY (debtor_party_id) REFERENCES claimledger_parties(id) ON DELETE SET NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.9') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.9';
commit;

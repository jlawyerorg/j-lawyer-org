-- Which dunning court is competent for an applicant, following § 689 Abs. 2, 3 ZPO.
--
-- The rules sit on top of the court directory and reference a court instead of repeating its
-- address. What they add is the selection key and the attributes that only matter for the dunning
-- procedure: which submission channels a court accepts, whether it issues its own Kennziffer, and
-- whether a direct debit authorisation has to be registered as nationwide.
--
-- The selection key is deliberately not a single column. The default key is the federal state of
-- the applicant's general venue, but a state can be divided between two courts - North Rhine-
-- Westphalia is split along the OLG district of Cologne - and applicants without a domestic general
-- venue are assigned to one designated court regardless of any state.
--
-- Where a division exists, restriction holds it in the words the dunning courts publish. It is
-- deliberately text and not a machine-evaluated expression: the division follows the OLG district,
-- which cannot be derived from anything stored here. postal_code_from/to are offered for the case
-- where a firm can express its own division numerically; where they are empty the rule matches the
-- whole state and the caller has to choose between the candidates rather than be given a guess.

CREATE TABLE dunning_court_rules (
`id` VARCHAR(50) BINARY NOT NULL,
`court_id` VARCHAR(50) BINARY NOT NULL,

-- the federal state of the applicant's general venue; empty for the foreign-applicant rule
`federal_state` VARCHAR(100) BINARY DEFAULT NULL,
-- marks the rule of § 689 Abs. 2 S. 2 ZPO for applicants without a domestic general venue
`foreign_applicant` TINYINT(1) DEFAULT 0 NOT NULL,
-- human-readable restriction where a state is divided, e.g. "OLG-Bezirk Köln"
`restriction` VARCHAR(250) BINARY DEFAULT NULL,
-- optional numeric division; both empty means the rule covers the whole state
`postal_code_from` VARCHAR(10) BINARY DEFAULT NULL,
`postal_code_to` VARCHAR(10) BINARY DEFAULT NULL,

`accepted_channels` VARCHAR(500) BINARY DEFAULT NULL,
`requires_own_kennziffer` TINYINT(1) DEFAULT 0 NOT NULL,
`direct_debit_nationwide` TINYINT(1) DEFAULT 0 NOT NULL,

`sequence_number` INT DEFAULT 0 NOT NULL,
`active` TINYINT(1) DEFAULT 1 NOT NULL,
`notes` VARCHAR(1000) BINARY DEFAULT NULL,

CONSTRAINT `pk_dunning_court_rules` PRIMARY KEY (`id`),
CONSTRAINT `fk_dunningrule_court` FOREIGN KEY (court_id) REFERENCES courts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table dunning_court_rules add index `IDX_DUNNINGRULES_STATE` (federal_state);
alter table dunning_court_rules add index `IDX_DUNNINGRULES_COURT` (court_id);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.18') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.18';
commit;

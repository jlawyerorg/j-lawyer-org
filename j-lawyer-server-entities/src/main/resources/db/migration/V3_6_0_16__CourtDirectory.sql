-- Courts and comparable judicial bodies as central master data, so that every feature referring to
-- a court uses one record instead of free text.
--
-- Two addresses are held, because the courts publish two and they differ. The postal address is
-- where post and applications go; for most central dunning courts it is a bulk-mail address that
-- consists of postcode and place with no street at all, and for others it is a Postfach, which is
-- why postal_street is nullable and may hold "Postfach 1170". The house address is the one for
-- visitors and couriers. Keeping only one of them would lose the address applications are actually
-- sent to, or the one somebody has to drive to.
--
-- The XJustiz identifier is the stable key. Names and addresses change, the identifier does not, so
-- seeding, matching and later imports go through it. It is deliberately not unique at the database
-- level: a superseded court and its successor may carry the same identifier while the old record is
-- kept for the records that still point at it. Uniqueness is required among active courts and is
-- enforced by the service, which can express that condition.
--
-- Scopes live in their own table rather than as flags, because a court can serve several purposes -
-- an Amtsgericht is regularly both an enforcement court and a litigation court - and because new
-- scopes must not require a schema change.

CREATE TABLE courts (
`id` VARCHAR(50) BINARY NOT NULL,
`xjustiz_id` VARCHAR(20) BINARY DEFAULT NULL,
`name` VARCHAR(250) BINARY NOT NULL,
`additional_designation` VARCHAR(250) BINARY DEFAULT NULL,

`postal_street` VARCHAR(250) BINARY DEFAULT NULL,   -- null for a bulk-mail address; may be a Postfach
`postal_code` VARCHAR(10) BINARY DEFAULT NULL,
`city` VARCHAR(250) BINARY DEFAULT NULL,

`house_street` VARCHAR(250) BINARY DEFAULT NULL,
`house_postal_code` VARCHAR(10) BINARY DEFAULT NULL,
`house_city` VARCHAR(250) BINARY DEFAULT NULL,

`phone` VARCHAR(100) BINARY DEFAULT NULL,
`fax` VARCHAR(100) BINARY DEFAULT NULL,
`email` VARCHAR(250) BINARY DEFAULT NULL,
`web` VARCHAR(250) BINARY DEFAULT NULL,
-- EGVP/beA SAFE-ID of the court, used for electronic transmission
`electronic_recipient_id` VARCHAR(250) BINARY DEFAULT NULL,

`bank_account_holder` VARCHAR(250) BINARY DEFAULT NULL,
`bank_iban` VARCHAR(50) BINARY DEFAULT NULL,
`bank_bic` VARCHAR(20) BINARY DEFAULT NULL,

`valid_from` DATE DEFAULT NULL,
`valid_to` DATE DEFAULT NULL,       -- set when a court is superseded; records already pointing at it keep resolving
`active` TINYINT(1) DEFAULT 1 NOT NULL,
`notes` VARCHAR(1000) BINARY DEFAULT NULL,

CONSTRAINT `pk_courts` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table courts add index `IDX_COURTS_XJUSTIZ` (xjustiz_id);
alter table courts add index `IDX_COURTS_NAME` (name);

CREATE TABLE court_scopes (
`id` VARCHAR(50) BINARY NOT NULL,
`court_id` VARCHAR(50) BINARY NOT NULL,
`scope` VARCHAR(30) BINARY NOT NULL,
CONSTRAINT `pk_court_scopes` PRIMARY KEY (`id`),
CONSTRAINT `fk_courtscope_court` FOREIGN KEY (court_id) REFERENCES courts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table court_scopes add index `IDX_COURTSCOPES_COURT` (court_id);
alter table court_scopes add index `IDX_COURTSCOPES_SCOPE` (scope);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.16') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.16';
commit;

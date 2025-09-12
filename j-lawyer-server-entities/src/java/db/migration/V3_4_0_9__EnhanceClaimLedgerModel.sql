alter table claimledgers drop column tax_rate;

CREATE TABLE claimcomponents (
    id VARCHAR(50) BINARY NOT NULL,
    ledger_id VARCHAR(50) BINARY NOT NULL,
    principal_amount DECIMAL(15,2) DEFAULT 0.00,
    component_type VARCHAR(50) NOT NULL, -- MAIN_CLAIM / COST_INTEREST_BEARING / COST_NON_INTEREST_BEARING
    comment VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_claimcomponent_ledger FOREIGN KEY (ledger_id) REFERENCES claimledgers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE interest_rules (
    id VARCHAR(50) NOT NULL,
    component_id VARCHAR(50) BINARY NOT NULL,
    valid_from DATE,
    interest_type VARCHAR(30) NOT NULL, -- FIXED / BASIS_RELATED
    fixed_rate DECIMAL(5,2),            -- nur wenn interest_type=FIXED
    base_margin DECIMAL(5,2),           -- nur wenn interest_type=BASIS_RELATED
    comment VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_interestrule_component FOREIGN KEY (component_id) REFERENCES claimcomponents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE claimledger_entries
    ADD COLUMN component_id VARCHAR(50) BINARY NULL AFTER ledger_id;

ALTER TABLE claimledger_entries
    ADD CONSTRAINT fk_entry_component
        FOREIGN KEY (component_id) REFERENCES claimcomponents(id) ON DELETE SET NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.9') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.9';
commit;
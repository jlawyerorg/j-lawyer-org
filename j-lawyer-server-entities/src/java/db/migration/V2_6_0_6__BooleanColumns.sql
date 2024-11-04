alter table case_documents modify `deleted` TINYINT(1) DEFAULT 0;
alter table case_documents modify `deleted` BIT(1) DEFAULT 0;

alter table case_documents modify `favorite` TINYINT(1) DEFAULT 0;
alter table case_documents modify `favorite` BIT(1) DEFAULT 0;

alter table contacts modify `legalProtection` TINYINT(1) DEFAULT 0;
alter table contacts modify `legalProtection` BIT(1) DEFAULT 0;

alter table contacts modify `trafficLegalProtection` TINYINT(1) DEFAULT 0;
alter table contacts modify `trafficLegalProtection` BIT(1) DEFAULT 0;

alter table security_users modify `lawyer` TINYINT(1) DEFAULT 0;
alter table security_users modify `lawyer` BIT(1) DEFAULT 0;

alter table security_users modify `beaCertificateAutoLogin` TINYINT(1) DEFAULT 0;
alter table security_users modify `beaCertificateAutoLogin` BIT(1) DEFAULT 0;

alter table security_users modify `cloudSsl` TINYINT(1) DEFAULT 0;
alter table security_users modify `cloudSsl` BIT(1) DEFAULT 0;

alter table cases modify `archived` TINYINT(1) DEFAULT 0;
alter table cases modify `archived` BIT(1) DEFAULT 0;

alter table case_events modify `done` TINYINT(1) DEFAULT 0;
alter table case_events modify `done` BIT(1) DEFAULT 0;

alter table calendar_setup modify `cloudSsl` TINYINT(1) DEFAULT 0;
alter table calendar_setup modify `cloudSsl` BIT(1) DEFAULT 0;

alter table calendar_setup modify `delete_done` TINYINT(1) DEFAULT 0;
alter table calendar_setup modify `delete_done` BIT(1) DEFAULT 0;

alter table case_folder_settings modify `hidden` TINYINT(1) DEFAULT 0;
alter table case_folder_settings modify `hidden` BIT(1) DEFAULT 0;

alter table instantmessage_mention modify `done` TINYINT(1) DEFAULT 0;
alter table instantmessage_mention modify `done` BIT(1) DEFAULT 0;

alter table invoices modify `small_business` TINYINT(1) DEFAULT 0;
alter table invoices modify `small_business` BIT(1) DEFAULT 0;

alter table invoice_pools modify `manual_adjust` TINYINT(1) DEFAULT 0;
alter table invoice_pools modify `manual_adjust` BIT(1) DEFAULT 0;

alter table invoice_pools modify `small_business` TINYINT(1) DEFAULT 0;
alter table invoice_pools modify `small_business` BIT(1) DEFAULT 0;

alter table invoice_types modify `turnover` TINYINT(1) DEFAULT 0;
alter table invoice_types modify `turnover` BIT(1) DEFAULT 0;

alter table mailbox_setup modify `emailInSsl` TINYINT(1) DEFAULT 0;
alter table mailbox_setup modify `emailInSsl` BIT(1) DEFAULT 0;

alter table mailbox_setup modify `emailOutSsl` TINYINT(1) DEFAULT 0;
alter table mailbox_setup modify `emailOutSsl` BIT(1) DEFAULT 0;

alter table mailbox_setup modify `emailStartTls` TINYINT(1) DEFAULT 0;
alter table mailbox_setup modify `emailStartTls` BIT(1) DEFAULT 0;

alter table mailbox_setup modify `msexchange` TINYINT(1) DEFAULT 0;
alter table mailbox_setup modify `msexchange` BIT(1) DEFAULT 0;

alter table mapping_tables modify `system_table` TINYINT(1) DEFAULT 0;
alter table mapping_tables modify `system_table` BIT(1) DEFAULT 0;

alter table timesheets modify `limited` TINYINT(1) DEFAULT 0;
alter table timesheets modify `limited` BIT(1) DEFAULT 0;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.6') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.6';
commit;
rename table AddressBean to contacts;
rename table AddressTagsBean to contact_tags;

rename table AppOptionGroupBean to server_options;
rename table ServerSettingsBean to server_settings;
-- required for j-lawyer.BOX
CREATE VIEW ServerSettingsBean AS SELECT * FROM server_settings;

-- views are required for the login module whose configuration will not change
rename table AppRoleBean to security_roles;
CREATE VIEW AppRoleBean AS SELECT * FROM security_roles;
rename table AppUserBean to security_users;
CREATE VIEW AppUserBean AS SELECT * FROM security_users;

rename table ArchiveFileBean to cases;
rename table ArchiveFileAddressesBean to case_contacts;
rename table ArchiveFileDocumentsBean to case_documents;
rename table ArchiveFileHistoryBean to case_history;
rename table ArchiveFileReviewsBean to case_followups;
rename table ArchiveFileTagsBean to case_tags;
rename table BankDataBean to directory_banks;
rename table CityDataBean to directory_cities;
rename table FaxQueueBean to communication_fax;

rename table campaign to campaigns;
rename table campaign_addresses to campaign_contacts;

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.3') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.3';
commit;
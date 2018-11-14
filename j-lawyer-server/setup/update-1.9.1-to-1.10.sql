use jlawyerdb;

# Remember to put database migrations into class DatabaseMigrator, this SQL file
# is for documentation mainly or for cases where the servers datasource does not
# have enough privileges to perform schema changes.

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.0') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.0';

commit;
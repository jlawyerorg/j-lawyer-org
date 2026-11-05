-- Server settings may hold larger values, e.g. the UI definition (JSON) of the freeform01 form plugin
-- exceeded 2500 characters (https://github.com/jlawyerorg/j-lawyer-org/issues/3779).
-- settingValue is only read by its key, never searched for, and is not indexed.
ALTER TABLE server_settings MODIFY settingValue varchar(16000) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.58') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.58';
commit;

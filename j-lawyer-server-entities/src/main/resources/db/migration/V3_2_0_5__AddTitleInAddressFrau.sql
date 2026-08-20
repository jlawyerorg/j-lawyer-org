INSERT INTO server_options (id, optionGroup, value)
SELECT 'address.titleinaddress.frau', 'address.titleinaddress', 'Frau'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM server_options
    WHERE optionGroup = 'address.titleinaddress'
      AND value = 'Frau'
);


insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.2.0.5') ON DUPLICATE KEY UPDATE settingValue     = '3.2.0.5';
commit;
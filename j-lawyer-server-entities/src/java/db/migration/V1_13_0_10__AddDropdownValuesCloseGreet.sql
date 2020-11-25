INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.1', 'address.complimentaryclose','Liebe Grüße' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Liebe Grüße' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.2', 'address.complimentaryclose','Lieber Gruß' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Lieber Gruß' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.3', 'address.complimentaryclose','Mit freundlichen Grüßen' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Mit freundlichen Grüßen' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.4', 'address.complimentaryclose','Mit freundlichen kollegialen Grüßen' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Mit freundlichen kollegialen Grüßen' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.5', 'address.complimentaryclose','Mit liebem Gruß' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Mit liebem Gruß' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.6', 'address.complimentaryclose','Kind regards,' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Kind regards,' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.7', 'address.complimentaryclose','Yours sincerely,' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Yours sincerely,' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.complimentaryclose.8', 'address.complimentaryclose','Mit vorzüglicher Hochachtung' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.complimentaryclose' AND value='Mit vorzüglicher Hochachtung' LIMIT 1);



INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.1', 'address.salutation','Sehr geehrte Damen und Herren' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Sehr geehrte Damen und Herren' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.2', 'address.salutation','Sehr geehrte Frau' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Sehr geehrte Frau' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.3', 'address.salutation','Sehr geehrter Herr' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Sehr geehrter Herr' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.4', 'address.salutation','Sehr geehrte Kollegen' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Sehr geehrte Kollegen' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.5', 'address.salutation','Sehr geehrte Frau Kollegin' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Sehr geehrte Frau Kollegin' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.6', 'address.salutation','Sehr geehrter Herr Kollege' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Sehr geehrter Herr Kollege' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.7', 'address.salutation','Dear Mister' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Dear Mister' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.8', 'address.salutation','Dear Miss' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Dear Miss' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.9', 'address.salutation','Dear Sir or Madam' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Dear Sir or Madam' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.10', 'address.salutation','Liebe' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Liebe' LIMIT 1);

INSERT INTO server_options(id,optionGroup, value) 
SELECT 'address.salutation.11', 'address.salutation','Lieber' FROM DUAL 
WHERE NOT EXISTS (SELECT * FROM server_options 
      WHERE optionGroup='address.salutation' AND value='Lieber' LIMIT 1);

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.13.0.10') ON DUPLICATE KEY UPDATE settingValue     = '1.13.0.10';
commit;
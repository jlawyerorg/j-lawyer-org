INSERT INTO security_roles (id, principalId, role, roleGroup)
SELECT
    CONCAT(sr.principalId, '_sysAdmin'),
    sr.principalId,
    'sysAdminRole',
    'Roles'
FROM security_roles sr
WHERE sr.role = 'adminRole'
  AND NOT EXISTS (
    SELECT 1
    FROM security_roles sr2
    WHERE sr2.principalId = sr.principalId
      AND sr2.role = 'sysAdminRole'
  );

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.1') ON DUPLICATE KEY UPDATE settingValue     = '3.4.0.1';
commit;
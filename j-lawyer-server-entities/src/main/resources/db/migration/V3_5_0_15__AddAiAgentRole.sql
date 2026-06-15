INSERT INTO security_roles (id, principalId, role, roleGroup)
SELECT UUID(), sr.principalId, 'aiAgentRole', 'Roles'
FROM (SELECT DISTINCT principalId FROM security_roles) sr
WHERE NOT EXISTS (
    SELECT 1 FROM security_roles sr2
    WHERE sr2.principalId = sr.principalId AND sr2.role = 'aiAgentRole'
);

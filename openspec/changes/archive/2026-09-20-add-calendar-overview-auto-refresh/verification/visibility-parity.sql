-- Sichtbarkeits-Parität: alte Regel (SecurityUtils.getAllowedCasesForUser)
-- gegen neue Regel (VISIBILITY-Prädikat in ArchiveFileReviewsBeanFacade).
--
-- Aufruf:  mysql -u jlawyer -p jlawyerdb -e "SET @principal='admin';" < visibility-parity.sql
-- oder den SET-Befehl unten anpassen.

SET @principal = 'admin';

-- Gruppen des Nutzers, wie SecurityService.getGroupsForUser sie liefert
DROP TEMPORARY TABLE IF EXISTS tmp_user_groups;
CREATE TEMPORARY TABLE tmp_user_groups AS
SELECT gm.group_id AS id FROM security_group_memberships gm WHERE gm.principal_id = @principal;

-- ALTE Regel: die vier UNION-Zweige, wörtlich aus getAllowedCasesForUser
DROP TEMPORARY TABLE IF EXISTS tmp_old_rule;
CREATE TEMPORARY TABLE tmp_old_rule AS
SELECT DISTINCT t1.id FROM (
    SELECT id FROM cases WHERE owner_group IN (SELECT id FROM tmp_user_groups)
    UNION
    SELECT id FROM cases WHERE owner_group IS NULL
    UNION
    SELECT DISTINCT case_id id FROM case_groups WHERE group_id IN (SELECT id FROM tmp_user_groups)
    UNION
    SELECT id FROM cases WHERE id NOT IN (SELECT case_id FROM case_groups)
) t1;

-- NEUE Regel: dasselbe als Prädikat, wie es die JPQL-Query erzeugt
DROP TEMPORARY TABLE IF EXISTS tmp_new_rule;
CREATE TEMPORARY TABLE tmp_new_rule AS
SELECT c.id FROM cases c
WHERE c.owner_group IS NULL
   OR c.owner_group IN (SELECT id FROM tmp_user_groups)
   OR EXISTS (SELECT 1 FROM case_groups g
              WHERE g.case_id = c.id AND g.group_id IN (SELECT id FROM tmp_user_groups))
   OR NOT EXISTS (SELECT 1 FROM case_groups g2 WHERE g2.case_id = c.id);

SELECT '--- Akten gesamt / alte Regel / neue Regel ---' AS abschnitt;
SELECT (SELECT COUNT(*) FROM cases)        AS akten_gesamt,
       (SELECT COUNT(*) FROM tmp_old_rule) AS alte_regel,
       (SELECT COUNT(*) FROM tmp_new_rule) AS neue_regel;

SELECT '--- NUR neu sichtbar (Sicherheitsproblem, muss leer sein) ---' AS abschnitt;
SELECT c.id, c.fileNumber, c.name, c.owner_group
FROM tmp_new_rule n JOIN cases c ON c.id = n.id
WHERE n.id NOT IN (SELECT id FROM tmp_old_rule)
LIMIT 50;

SELECT '--- NUR alt sichtbar (Regression, muss leer sein) ---' AS abschnitt;
SELECT c.id, c.fileNumber, c.name, c.owner_group
FROM tmp_old_rule o JOIN cases c ON c.id = o.id
WHERE o.id NOT IN (SELECT id FROM tmp_new_rule)
LIMIT 50;

SELECT '--- Abdeckung: kommen alle vier Zweige im Bestand ueberhaupt vor? ---' AS abschnitt;
SELECT
  (SELECT COUNT(*) FROM cases WHERE owner_group IS NULL) AS zweig1_ohne_ownergroup,
  (SELECT COUNT(*) FROM cases WHERE owner_group IN (SELECT id FROM tmp_user_groups)) AS zweig2_eigene_ownergroup,
  (SELECT COUNT(DISTINCT case_id) FROM case_groups WHERE group_id IN (SELECT id FROM tmp_user_groups)) AS zweig3_freigegeben,
  (SELECT COUNT(*) FROM cases c WHERE c.owner_group IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM case_groups g WHERE g.case_id = c.id)) AS zweig4_ownergroup_ohne_freigaben;

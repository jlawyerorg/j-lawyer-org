-- Die amtlichen Formulare der Zwangsvollstreckung sind zum 01.10.2026 geändert worden.
--
-- Die bis dahin geltende Fassung trug kein Gültigkeitsende, denn sie war die geltende. Das ist sie
-- nicht mehr, und ohne Ende bliebe sie es in den Stammdaten jeder Installation: der Fassungswähler
-- hielte sie für aktuell und lieferte stillschweigend ein abgelöstes Formular aus.
--
-- Deshalb endet sie hier, am Tag vor dem Inkrafttreten der neuen. Die neue Fassung selbst kommt
-- nicht aus einer Migration - sie besteht aus PDF-Dateien und wird über "Standardpaket importieren"
-- eingelesen. Bis das geschehen ist, gilt am heutigen Tag keine der hinterlegten Fassungen; der
-- Wähler nimmt dann die jüngste und sagt ausdrücklich, dass das Formular möglicherweise nicht mehr
-- das vorgeschriebene ist. Diese Warnung ist der Zweck: sie ist in einer Kanzlei zu sehen, während
-- ein lautlos veraltetes Formular es nicht ist.
--
-- Ein von Hand gesetztes Ende bleibt unberührt - wer die Zeiträume selbst pflegt, behält sie.

update enforcement_form_templates
   set valid_to = '2026-09-30'
 where version = '2024-09-01'
   and valid_to is null;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.52') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.52';
commit;

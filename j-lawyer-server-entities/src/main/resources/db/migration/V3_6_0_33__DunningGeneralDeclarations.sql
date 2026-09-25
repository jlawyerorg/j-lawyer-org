-- Die allgemeinen Erklärungen zum Mahnbescheidsantrag (Kennsatz C01, Felder VGLM1, VGLM2, ASTRVM).
--
-- Die beiden Erklärungen zur Gegenleistung sind keine Formalie: nach § 688 Abs. 2 Nr. 2 ZPO ist
-- der Mahnbescheid unzulässig, wenn der Anspruch von einer Gegenleistung abhängt, die noch nicht
-- erbracht ist. Das Gericht muss deshalb wissen, welcher der beiden Fälle vorliegt - entweder
-- "hängt ab, ist aber erbracht" (VGLM1) oder "hängt nicht ab" (VGLM2). Ein Antrag ohne eine dieser
-- Erklärungen wird moniert.
--
-- Die Satzbeschreibung erlaubt ausdrücklich, dass bei mehreren Ansprüchen beide Felder belegt sind
-- ("Bei mehreren Ansprüchen können auch beide Felder belegt sein!"). Sie schließen einander also
-- nicht aus und werden deshalb als zwei Merkmale geführt und nicht als eine Auswahl.
--
-- litigation_requested ist der Antrag nach § 696 Abs. 1 ZPO, die Sache im Falle eines Widerspruchs
-- an das Streitgericht abzugeben. Er ist freiwillig; leer heißt "nicht beantragt".

alter table dunning_cases add column `counter_performance_rendered` TINYINT(1) NOT NULL DEFAULT 0;
alter table dunning_cases add column `counter_performance_independent` TINYINT(1) NOT NULL DEFAULT 0;
alter table dunning_cases add column `litigation_requested` TINYINT(1) NOT NULL DEFAULT 0;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.33') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.33';
commit;

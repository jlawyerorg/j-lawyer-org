-- Das Zuordnungsprofil für Anlage 1, den Vollstreckungsauftrag an den Gerichtsvollzieher.
--
-- Die Feldnamen des Formulars sagen nichts - "Textfeld 220", "Kontrollkästchen 166". Was ein Feld
-- bedeutet, steht allein in seinem Tooltip, und der ist hier in field_label mitgeführt: er ist der
-- einzige Beleg dafür, dass die Zuordnung das meint, was sie behauptet. Ändert eine spätere Fassung
-- die Bedeutung eines Namens, fällt es beim Vergleich mit dem Feldverzeichnis auf.
--
-- Zugeordnet ist, was ein Auftrag mindestens braucht: Empfänger, Auftraggeber, Gläubiger,
-- Schuldner, Ort und Datum. Die Optionen des § 802a Abs. 2 ZPO (Sachpfändung, gütliche Erledigung,
-- Vermögensauskunft, Haftbefehl) hängen an der einzelnen Maßnahme und nicht am Formular; sie kommen
-- mit der Maßnahmeoberfläche.
--
-- Die Zuordnung ist am Formular abgelesen - an den Bezeichnungen und der Reihenfolge der Felder.
-- Weil die Bezeichnungen sich über die Blöcke wiederholen ("Name/Firma" steht beim Gläubiger, beim
-- Schuldner und beim Vertreter), beweist das für sich noch nichts. Geprüft wurde sie deshalb an
-- einem ausgefüllten Muster: Gläubiger, Schuldner und Kanzlei stehen dort, wo sie hingehören.

insert into enforcement_form_field_mappings (id, template_id, field_name, field_label, source_key, fixed_value, mandatory, comment)
select concat(t.id, '-m001') as id, t.id as template_id, 'Textfeld 1' as field_name, 'Name Gerichtsvollzieher oder Bezeichnung Verteilungsstelle des Amtsgerichts' as field_label, 'empfaenger.name' as source_key, null as fixed_value, 1 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m001'))
union all
select concat(t.id, '-m002') as id, t.id as template_id, 'Textfeld 2' as field_name, 'ggfls. Fortsetzung Gerichtsvollzieher oder Bezeichnung Verteilungsstelle des Amtsgerichts' as field_label, 'empfaenger.name_fortsetzung' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m002'))
union all
select concat(t.id, '-m003') as id, t.id as template_id, 'Textfeld 3' as field_name, 'Postfach oder Straße und Hausnummer' as field_label, 'empfaenger.strasse' as source_key, null as fixed_value, 1 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m003'))
union all
select concat(t.id, '-m004') as id, t.id as template_id, 'Textfeld 4' as field_name, 'Postleitzahl und Ort' as field_label, 'empfaenger.plz_ort' as source_key, null as fixed_value, 1 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m004'))
union all
select concat(t.id, '-m005') as id, t.id as template_id, 'Kontrollkästchen 5' as field_name, 'Gläubiger' as field_label, 'absender.ist_glaeubiger' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m005'))
union all
select concat(t.id, '-m006') as id, t.id as template_id, 'Kontrollkästchen 7' as field_name, 'Bevollmächtigter' as field_label, 'absender.ist_bevollmaechtigter' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m006'))
union all
select concat(t.id, '-m007') as id, t.id as template_id, 'Textfeld 192' as field_name, 'Name/Firma' as field_label, 'bevollmaechtigter.name' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m007'))
union all
select concat(t.id, '-m008') as id, t.id as template_id, 'Textfeld 193' as field_name, 'ggf. Vorname(n)' as field_label, 'bevollmaechtigter.vorname' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m008'))
union all
select concat(t.id, '-m009') as id, t.id as template_id, 'Textfeld 194' as field_name, 'Straße' as field_label, 'bevollmaechtigter.strasse' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m009'))
union all
select concat(t.id, '-m010') as id, t.id as template_id, 'Textfeld 195' as field_name, 'Hausnummer' as field_label, 'bevollmaechtigter.hausnummer' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m010'))
union all
select concat(t.id, '-m011') as id, t.id as template_id, 'Textfeld 196' as field_name, 'Postleitzahl' as field_label, 'bevollmaechtigter.plz' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m011'))
union all
select concat(t.id, '-m012') as id, t.id as template_id, 'Textfeld 197' as field_name, 'Ort' as field_label, 'bevollmaechtigter.ort' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m012'))
union all
select concat(t.id, '-m013') as id, t.id as template_id, 'Textfeld 19' as field_name, 'Telefon' as field_label, 'bevollmaechtigter.telefon' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m013'))
union all
select concat(t.id, '-m014') as id, t.id as template_id, 'Textfeld 20' as field_name, 'E-Mail' as field_label, 'bevollmaechtigter.email' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m014'))
union all
select concat(t.id, '-m015') as id, t.id as template_id, 'Textfeld 204' as field_name, 'Geschäftszeichen' as field_label, 'akte.zeichen' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m015'))
union all
select concat(t.id, '-m016') as id, t.id as template_id, 'Textfeld 5' as field_name, 'Ort' as field_label, 'ort' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m016'))
union all
select concat(t.id, '-m017') as id, t.id as template_id, 'Textfeld 6' as field_name, 'Datum' as field_label, 'datum' as source_key, null as fixed_value, 1 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m017'))
union all
select concat(t.id, '-m018') as id, t.id as template_id, 'Kontrollkästchen 166' as field_name, 'Herr' as field_label, 'glaeubiger.ist_herr' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m018'))
union all
select concat(t.id, '-m019') as id, t.id as template_id, 'Kontrollkästchen 165' as field_name, 'Frau' as field_label, 'glaeubiger.ist_frau' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m019'))
union all
select concat(t.id, '-m020') as id, t.id as template_id, 'Kontrollkästchen 164' as field_name, 'Unternehmen' as field_label, 'glaeubiger.ist_unternehmen' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m020'))
union all
select concat(t.id, '-m021') as id, t.id as template_id, 'Kontrollkästchen 163' as field_name, 'Sonstige' as field_label, 'glaeubiger.ist_sonstige' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m021'))
union all
select concat(t.id, '-m022') as id, t.id as template_id, 'Textfeld 220' as field_name, 'Name/Firma' as field_label, 'glaeubiger.name' as source_key, null as fixed_value, 1 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m022'))
union all
select concat(t.id, '-m023') as id, t.id as template_id, 'Textfeld 219' as field_name, 'ggf. Vorname(n)' as field_label, 'glaeubiger.vorname' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m023'))
union all
select concat(t.id, '-m024') as id, t.id as template_id, 'Textfeld 218' as field_name, 'Straße' as field_label, 'glaeubiger.strasse' as source_key, null as fixed_value, 1 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m024'))
union all
select concat(t.id, '-m025') as id, t.id as template_id, 'Textfeld 217' as field_name, 'Hausnummer' as field_label, 'glaeubiger.hausnummer' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m025'))
union all
select concat(t.id, '-m026') as id, t.id as template_id, 'Textfeld 216' as field_name, 'Postleitzahl' as field_label, 'glaeubiger.plz' as source_key, null as fixed_value, 1 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m026'))
union all
select concat(t.id, '-m027') as id, t.id as template_id, 'Textfeld 215' as field_name, 'Ort' as field_label, 'glaeubiger.ort' as source_key, null as fixed_value, 1 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m027'))
union all
select concat(t.id, '-m028') as id, t.id as template_id, 'Textfeld 222' as field_name, 'Land (wenn nicht Deutschland)' as field_label, 'glaeubiger.land' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m028'))
union all
select concat(t.id, '-m029') as id, t.id as template_id, 'Kontrollkästchen 174' as field_name, 'Herr' as field_label, 'schuldner.ist_herr' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m029'))
union all
select concat(t.id, '-m030') as id, t.id as template_id, 'Kontrollkästchen 173' as field_name, 'Frau' as field_label, 'schuldner.ist_frau' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m030'))
union all
select concat(t.id, '-m031') as id, t.id as template_id, 'Kontrollkästchen 172' as field_name, 'Unternehmen' as field_label, 'schuldner.ist_unternehmen' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m031'))
union all
select concat(t.id, '-m032') as id, t.id as template_id, 'Kontrollkästchen 171' as field_name, 'Sonstige' as field_label, 'schuldner.ist_sonstige' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m032'))
union all
select concat(t.id, '-m033') as id, t.id as template_id, 'Textfeld 240' as field_name, 'Name/Firma' as field_label, 'schuldner.name' as source_key, null as fixed_value, 1 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m033'))
union all
select concat(t.id, '-m034') as id, t.id as template_id, 'Textfeld 239' as field_name, 'ggf. Vorname(n)' as field_label, 'schuldner.vorname' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m034'))
union all
select concat(t.id, '-m035') as id, t.id as template_id, 'Textfeld 238' as field_name, 'Straße' as field_label, 'schuldner.strasse' as source_key, null as fixed_value, 1 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m035'))
union all
select concat(t.id, '-m036') as id, t.id as template_id, 'Textfeld 237' as field_name, 'Hausnummer' as field_label, 'schuldner.hausnummer' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m036'))
union all
select concat(t.id, '-m037') as id, t.id as template_id, 'Textfeld 236' as field_name, 'Postleitzahl' as field_label, 'schuldner.plz' as source_key, null as fixed_value, 1 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m037'))
union all
select concat(t.id, '-m038') as id, t.id as template_id, 'Textfeld 244' as field_name, 'Ort' as field_label, 'schuldner.ort' as source_key, null as fixed_value, 1 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m038'))
union all
select concat(t.id, '-m039') as id, t.id as template_id, 'Textfeld 260' as field_name, 'Land (wenn nicht Deutschland)' as field_label, 'schuldner.land' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m039'))
union all
select concat(t.id, '-m040') as id, t.id as template_id, 'Textfeld 247' as field_name, 'Geburtsdatum' as field_label, 'schuldner.geburtsdatum' as source_key, null as fixed_value, 0 as mandatory, null as comment
  from enforcement_form_templates t where t.form_key = 'ANLAGE_1' and t.version = '2024-09-01'
   and not exists (select 1 from enforcement_form_field_mappings m where m.id = concat(t.id, '-m040'));

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.43') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.43';
commit;

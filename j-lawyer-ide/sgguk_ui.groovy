import groovy.swing.SwingBuilder
import java.awt.BorderLayout as BL
import groovy.beans.Bindable
import java.text.DecimalFormat
import java.text.NumberFormat
import javax.swing.SwingConstants
import java.util.ArrayList
import java.util.List
import java.util.Locale
import javax.swing.JTable
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JTextArea
import javax.swing.JRadioButton
import javax.swing.JSpinner
import java.awt.Component
import java.awt.Container
import com.jdimension.jlawyer.client.plugins.form.FormPluginCallback

public class sgguk_ui implements com.jdimension.jlawyer.client.plugins.form.FormPluginMethods {

    JPanel SCRIPTPANEL=null;
    JTextArea lblText = null;
    JComboBox cmbWsSendeart
    JTextField txtBescheidTitel
    JTextField txtBescheidDatum
    JTextField txtWsDatum
    JTextField txtWsSendeDatum
    JTextField txtWsBestaetigungDatum
    JRadioButton rdEingangJa
    JRadioButton rdEingangNein
    JRadioButton rdPkhJa
    JRadioButton rdPkhNein
    FormPluginCallback callback=null;

    public sgguk_ui() {
        super();
    }

    public ArrayList<String> getPlaceHolders(String prefix) {
        ArrayList<String> placeHolders=FormsLib.getPlaceHolders(prefix, this.SCRIPTPANEL);
        return placeHolders;
    }
    
    public Hashtable getPlaceHolderValues(String prefix) {
        Hashtable placeHolders=FormsLib.getPlaceHolderValues(prefix, this.SCRIPTPANEL);
        return placeHolders;
    }
    
    public void setPlaceHolderValues(String prefix, Hashtable placeHolderValues) {
        FormsLib.setPlaceHolderValues(prefix, placeHolderValues, this.SCRIPTPANEL);
    }

    public void setCallback(FormPluginCallback callback) {
        this.callback=callback;
    }
    
    
   private String erstellen() {
        
        
        String text='Namens und in Auftrag des Klägers erhebe ich Klage und beantrage,\r\n\r\n1. die Beklagte zu verpflichten, den Widerspruch vom ' + txtWsDatum.text + ' zu bescheiden.\r\n'
		
		if(rdPkhJa.selected)
			text=text + '2. Prozesskostenhilfe zu gewähren und den Prozessbevollmächtigten beizuordnen.\r\n'
		
		text=text + '\r\n\r\nBegründung:\r\n\r\nZu 1)\r\n\r\nDie Beklagte hat am ' + txtBescheidDatum.text + ' einen ' + txtBescheidTitel.text + ' erlassen. \r\n\r\nDagegen hat der Kläger am ' + txtWsDatum.text + ' Widerspruch eingelegt.\r\n\r\nBeweis: Widerspruch vom ' + txtWsDatum.text + ' (Anlage K)'
		
		if(!(cmbWsSendeart.selectedItem in ['Brief']))
		text=text + ', ' + cmbWsSendeart.selectedItem.toString() + '-Sendenachweis vom ' + txtWsSendeDatum.text + ' (Anlage K)';
		
		if(rdEingangJa.selected)
            text=text + ', Eingangsbestätigung vom ' + txtWsBestaetigungDatum.text + ' (Anlage K)'
		
		text=text + '\r\n\r\nÜber den Widerspruch ist bislang nicht entschieden worden.\r\n\r\nDie Klage ist nach § 88 SGG auch ohne Erlass eines Widerspruchsbescheides zulässig, nachdem seit Einlegung des Widerspruchs mehr als drei Monate vergangen sind und ein Grund für die Nichtbescheidung des Widerspruchs nicht ersichtlich ist.'

		if(rdPkhJa.selected)
			text=text + '\r\n\r\nZu 2)\r\n\r\nDen persönlichen und wirtschaftlichen Verhältnissen nach ist der Kläger außerstande, die Kosten des beabsichtigten Rechtsstreits aufzubringen. Einzusetzendes Einkommen im Sinne von § 115 Abs. 1 ZPO ist nicht vorhanden, so dass der Kläger nicht durch monatliche Raten zu den Kosten beitragen kann. Auch eigenes Vermögen ist nicht vorhanden. Dies ergibt die Erklärung Klägers über die persönlichen und wirtschaftlichen Verhältnisse.\r\n\r\nBeweis: Erklärung über die persönlichen und wirtschaftlichen Verhältnisse (Anlage K), Kontoauszüge (Anlage K)\r\n\r\nDie beabsichtigte Rechtsverfolgung hat hinreichende Aussicht auf Erfolg und ist nicht mutwillig. Hierzu wird auf die Klagebegründung Bezug genommen.'


		
        lblText.text=text;
        return text;
        //processResultToClipboard
        
    }
    



    public JPanel getUi() {

        SwingBuilder swing=new SwingBuilder()
        swing.edt {
            SCRIPTPANEL=panel(size: [300, 300]) {
                tableLayout (cellpadding: 5) {
            
                    tr {
                        td  {
                            label(text: 'Titel des Bescheids:')
                        }
    
                        td  {
                            txtBescheidTitel=textField(name: "_BESCHEID-TITEL", text: '', columns:60, keyPressed: { erstellen() })
                            
                        }
                    } 

                    tr {
                        td  {
                            label(text: 'Datum des Bescheids:')
                        }
    
                        td  {
                            txtBescheidDatum=textField(name: "_BESCHEID-DATUM", text: '', columns:10, keyPressed: { erstellen() })
                        }
                    }
					
					tr {
                        td  {
                            label(text: 'Datum des Widerspruchs:')
                        }
    
                        td  {
                            txtWsDatum=textField(name: "_WS-DATUM", text: '', columns:10, keyPressed: { erstellen() })
                        }
                    }

					tr {
                        td  {
                            label(text: 'Datum des Sendenachweises:')
                        }
    
                        td  {
                            txtWsSendeDatum=textField(name: "_SENDE-DATUM", text: '', columns:10, keyPressed: { erstellen() })
                        }
                    }
					
                    tr {
                        td  {
                            label(text: 'Übersendungsweg:')
                        }
    
                        td  {
                            cmbWsSendeart = comboBox(items: [
                                            'beA',
                                            'EGVP',
                                            'Fax',
											'Brief'
                                ], name: "_WS_SENDE-ART", editable: true, actionPerformed: { erstellen() }
                            )
                        }
                    }
                    tr {
                        td {
                            label(text: 'Eingangsbestätigung:')        
                        }
                        td {
                            btnGrpEingang = buttonGroup(id:'grpEingang')
                            rdEingangJa=radioButton (text: 'Ja', name: "_BESTAETIGUNG-JA", buttonGroup: btnGrpEingang, selected: true, actionPerformed: {
                                    erstellen()
                                })

                        }
                    }
                    tr {
                        td {
                            label(text: '')        
                        }
                        td {
                            rdEingangNein=radioButton (text: 'Nein', name: "_BESTAETIGUNG-NEIN", buttonGroup: btnGrpEingang, actionPerformed: {  
                                    erstellen()
                                })
                        }
                    }
					tr {
                        td  {
                            label(text: 'Datum der Eingangsbestätigung:')
                        }
    
                        td  {
                            txtWsBestaetigungDatum=textField(name: "_BESTAETIGUNG-DATUM", text: '', columns:10, keyPressed: { erstellen() })
                        }
                    }
					
					
					tr {
                        td {
                            label(text: 'PKH beantragen:')        
                        }
                        td {
                            btnGrpPKH = buttonGroup(id:'grpPKH')
                            rdPkhJa=radioButton (text: 'Ja', name: "_PKH-JA", buttonGroup: btnGrpPKH, selected: true, actionPerformed: {
                                    erstellen()
                                })

                        }
                    }
                    tr {
                        td {
                            label(text: '')        
                        }
                        td {
                            rdPkhNein=radioButton (text: 'Nein', name: "_PKH-NEIN", buttonGroup: btnGrpPKH, actionPerformed: {  
                                    erstellen()
                                })
                        }
                    }
					
					
					
                    tr {
                        td (colfill:true, colspan: 2) {
                            panel(border: titledBorder(title: 'Schriftsatz')) {
                                tableLayout (cellpadding: 5) {
                                    tr {
                                        td {
                                            panel{
                                                scrollPane(preferredSize:[850, 800]){
                                                    lblText=textArea(text: 'Musterschriftsatz', lineWrap: true, wrapStyleWord: true)
                                                }
                                            }
                                        }
                                    }
                                    
 
                                    }
                                    
                                }
                            }
                        }

					
                }  
            }
        }

        return SCRIPTPANEL;

    }
    

}


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

public class flug01_ui implements com.jdimension.jlawyer.client.plugins.form.FormPluginMethods {

    JPanel SCRIPTPANEL=null;
    JTextArea lblText = null;
    JComboBox cmbFlugGesellschaft
    JTextField txtFlugNr
    JTextField txtFlugStrecke
    JTextField txtFlugDatum
    JRadioButton rdVerspaetet
    JRadioButton rdAusgefallen
    JRadioButton rdUeberbucht
    JSpinner spnReisende
    
    FormPluginCallback callback=null;

    public flug01_ui() {
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
    
    
    def erstellen() {
        
        int strecken = 0;
        try {
            strecke=Integer.parseInt(txtFlugStrecke.text);
        } catch (Throwable t) {}
        int betrag = 0;
        if(strecke>0)
            betrag=250;
        if(strecke>1500)
            betrag=400;
        if(strecke>3500)
            betrag=600;    
        
        String text='Der ' + cmbFlugGesellschaft.selectedItem.toString() + '-Flug mit der Flugnummer ' + txtFlugNr.text + ' vom ' + txtFlugDatum.text + ' meiner Mandantin';
        if(rdVerspaetet.selected)
            text=text + ' hat sich um mehr als drei Stunden verspätet.'
        
        if(rdUeberbucht.selected)
            text=text + ' war überbucht und das Boarding wurde durch das Personal der Fluglinie verweigert.'
            
        if(rdAusgefallen.selected)
            text=text + ' ist unangekündigt ausgefallen.'
            
        text= text + '\r\n\r\n' + 'Aus den mir vorliegenden Informationen kann ich nicht erkennen, dass es sich um außergewöhnliche Umstände gehandelt hätte. Daher steht meiner Mandantin folgende Entschädigung zu:'

        text = text + '\r\n\r\n' + betrag + ' EUR pro Passagier  x  ' + spnReisende.value + ' = ' + (betrag * spnReisende.value) + ' EUR'
        
        text = text + '\r\n\r\nBitte überweisen Sie den Betrag innerhalb von 14 Tagen auf folgendes Konto:'
        
        lblText.text=text;
        //processResultToClipboard
        
    }
    
    private String erstellenHtml() {
        
        int strecke = 0;
        try {
            strecke=Integer.parseInt(txtFlugStrecke.text);
        } catch (Throwable t) {}
        int betrag = 0;
        if(strecke>0)
            betrag=250;
        if(strecke>1500)
            betrag=400;
        if(strecke>3500)
            betrag=600;    
        
        String text='<html><font face=\"Verdana\"><p>Der ' + cmbFlugGesellschaft.selectedItem.toString() + '-Flug mit der Flugnummer <b>' + txtFlugNr.text + '</b> vom ' + txtFlugDatum.text + ' meiner Mandantin';
        if(rdVerspaetet.selected)
            text=text + ' hat sich um mehr als drei Stunden verspätet.'
        
        if(rdUeberbucht.selected)
            text=text + ' war überbucht und das Boarding wurde durch das Personal der Fluglinie verweigert.'
            
        if(rdAusgefallen.selected)
            text=text + ' ist unangekündigt ausgefallen.'
            
        text= text + '</p><p>' + 'Aus den mir vorliegenden Informationen kann ich nicht erkennen, dass es sich um außergewöhnliche Umstände gehandelt hätte. Daher steht meiner Mandantin folgende Entschädigung zu:</p>'

        text = text + '<table border=\"0\" width=\"100%\"><tr><td>' + betrag + ' EUR pro Passagier  x  ' + spnReisende.value + ' Reisende</td><td align=\"right\">' + (betrag * spnReisende.value) + ',00</td></tr>'
        text = text + '<tr><td>Sonstige Kosten</td><td align=\"right\">0,00</td></tr>'
        text = text + '<tr><td><b><ul>Summe</ul></b></td><td align=\"right\"><b><ul>' + (betrag * spnReisende.value) + ',00 EUR</ul></b></td></tr>'
        
        text = text + '</table><p>&nbsp;</p><p>Bitte überweisen Sie den Betrag innerhalb von 14 Tagen auf folgendes Konto:</p></font></html>'
        
        return text;
        
    }

    public JPanel getUi() {

        SwingBuilder swing=new SwingBuilder()
        swing.edt {
            SCRIPTPANEL=panel(size: [300, 300]) {
                tableLayout (cellpadding: 5) {
            
                    tr {
                        td  {
                            label(text: 'Fluggesellschaft:')
                        }
    
                        td  {
                            cmbFlugGesellschaft = comboBox(items: [
                                            'easyJet',
                                            'Lufthansa',
                                            'Ryanair'
                                ], name: "_FLUGGES", editable: true, actionPerformed: { erstellen() }
                            )
                        }
                    }

                    tr {
                        td  {
                            label(text: 'Flugnummer:')
                        }
    
                        td  {
                            txtFlugNr=textField(name: "_FLUGNR", text: '', columns:10, keyTyped: { erstellen() })
                        }
                    }
                    
                    tr {
                        td  {
                            label(text: 'Datum:')
                        }
    
                        td  {
                            txtFlugDatum=textField(name: "_FLUGDATUM", text: '', columns:10, keyReleased: { erstellen() })
                        }
                    }
                    
                    tr {
                        td  {
                            label(text: 'Flugstrecke in km:')
                        }
    
                        td  {
                            txtFlugStrecke=textField(name: "_FLUGSTRECKE", text: '', columns:10, keyTyped: { erstellen() })
                        }
                    }
                    
                    tr {
                        td {
                            label(text: 'Schadenart:')        
                        }
                        td {
                            btnGrpSchadenArt = buttonGroup(id:'grpSchadenArt')
                            rdVerspaetet=radioButton (text: 'Verspätung größer 3 Std.', name: "_SCHADVERSP", buttonGroup: btnGrpSchadenArt, selected: true, actionPerformed: {
                                    erstellen()
                                })
                        }
                    }
                    tr {
                        td {
                              label(text: ' ')       
                        }
                        td {
                            rdAusgefallen=radioButton (text: 'Ausgefallen', name: "_SCHADAUSFALL", buttonGroup: btnGrpSchadenArt, actionPerformed: {  
                                    erstellen()
                                })
                        }
                    }
                    tr {
                        td {
                              label(text: ' ')      
                        }
                        td {
                            rdUeberbucht=radioButton (text: 'Überbucht / Boarding verweigert', name: "_SCHADUEBERBUCHT", buttonGroup: btnGrpSchadenArt, actionPerformed: {   
                                    erstellen()
                                })
                        }
                    }
                    tr {
                        td {
                            label(text: 'Mitreisende:')
                        }
                        td {
                            spnReisende = spinner(name: "_MITREISENDE", 
                                model:spinnerNumberModel(minimum:1, 
                                    maximum: 30,
                                    value:1,
                                    stepSize:1), stateChanged: { erstellen() })
                        }
                        td {
                                    
                        }
                    }
                    
                    tr {
                        td (colfill:true, colspan: 2) {
                            panel(border: titledBorder(title: 'Schriftsatz')) {
                                tableLayout (cellpadding: 5) {
                                    tr {
                                        td {
                                            panel{
                                                scrollPane(preferredSize:[550, 400]){
                                                    lblText=textArea(text: 'Musterschriftsatz', lineWrap: true, wrapStyleWord: true)
                                                }
                                            }
                                        }
                                    }
                                    tr {
                                        td {
                                            button(text: 'Kopieren', actionPerformed: {
                                                        callback.processResultToClipboard(erstellenHtml());
                                                    })
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


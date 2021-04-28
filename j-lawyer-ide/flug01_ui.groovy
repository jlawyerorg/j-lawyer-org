import groovy.swing.SwingBuilder
import javax.swing.SwingConstants
import javax.swing.JPanel
import java.util.ArrayList
import com.jdimension.jlawyer.client.plugins.form.FormPluginCallback

public class flug01_ui implements com.jdimension.jlawyer.client.plugins.form.FormPluginMethods {

    JPanel SCRIPTPANEL=null;
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

        lblSteuern.text = 0.19 * Float.parseFloat(txtBetrag.text)
    }

    public JPanel getUi() {

        SwingBuilder swing=new SwingBuilder()
        swing.edt {
            SCRIPTPANEL=panel(size: [300, 300]) {
                tableLayout (cellpadding: 5) {
            
                    tr {
                        td  {
                            label(text: 'Fluggesellschaften:')
                        }
    
                        td  {
                            comboBox(items: [
                                            'easyJet',
                                            'Lufthansa'
                                            'Ryanair',
						'Taxi','blubb'
                                ], name: "_FLUGGES", editable: true
                            )
                        }
                    }

                    tr {
                        td  {
                            label(text: 'Flugnummer:')
                        }
    
                        td  {
                            textField(name: "_FLUGNR", text: '', columns:10)
                        }
                    }
                    
                    tr {
                        td {
                            label(text: 'Schadenart:')        
                        }
                        td {
                            btnGrpSchadenArt = buttonGroup(id:'grpSchadenArt')
                            radioButton (text: 'Verspätung', name: "_SCHADVERSP", buttonGroup: btnGrpSchadenArt, selected: true, actionPerformed: {
                                })
                        }
                    }
                    tr {
                        td {
                              label(text: ' ')       
                        }
                        td {
                            radioButton (text: 'Ausgefallen', name: "_SCHADAUSFALL", buttonGroup: btnGrpSchadenArt, actionPerformed: {                            
                                })
                        }
                    }
                    tr {
                        td {
                              label(text: ' ')      
                        }
                        td {
                            radioButton (text: 'Überbucht / Boarding verweigert', name: "_SCHADUEBERBUCHT", buttonGroup: btnGrpSchadenArt, actionPerformed: {                            
                                })
                        }
                    }
                    tr {
                        td {
                            label(text: 'Mitreisende:')
                        }
                        td {
                            spnVorbesitzer = spinner(name: "_MITREISENDE", 
                                model:spinnerNumberModel(minimum:1, 
                                    maximum: 30,
                                    value:1,
                                    stepSize:1))
                        }
                        td {
                                    
                        }
                    }
               
                }  
            }
        }

        return SCRIPTPANEL;

    }
    

}


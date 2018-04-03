/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlawyer.backupmgr.controller;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

/**
 * FXML Controller class
 *
 * @author jens
 */
public class BackupMgrController implements Initializable {

    @FXML
    private Button cmdBrowseBackupDir;
    @FXML
    private TextField txtBackupDir;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    @FXML
    private void cmdBrowseBackupDirClicked(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
                File selectedDirectory = 
                        directoryChooser.showDialog(this.txtBackupDir.getScene().getWindow());
                
                if(selectedDirectory == null){
                    txtBackupDir.setText("No Directory selected");
                }else{
                    txtBackupDir.setText(selectedDirectory.getAbsolutePath());
                }
    }
    
}

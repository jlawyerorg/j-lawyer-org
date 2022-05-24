/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlawyer.backupmgr.controller;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import org.jlawyer.backupmgr.BackupProgressUiCallback;
import org.jlawyer.backupmgr.impl.RestoreExecutor;

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
    @FXML
    private Button cmdRestore;
    @FXML
    private TextField txtEncryptionPwd;
    @FXML
    private TextField txtMysqlPwd;
    @FXML
    private ProgressBar prgRestore;
    @FXML
    private Label lblProgress;
    @FXML
    private Button cmdBrowseDataDir;
    @FXML
    private TextField txtDataDir;

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
        File selectedDirectory
                = directoryChooser.showDialog(this.txtBackupDir.getScene().getWindow());

        if (selectedDirectory == null) {
            txtBackupDir.setText("");
        } else {
            txtBackupDir.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    private void cmdRestoreClicked(ActionEvent event) {
        RestoreExecutor re = new RestoreExecutor(this.txtDataDir.getText(), this.txtBackupDir.getText(), this.txtEncryptionPwd.getText(), this.txtMysqlPwd.getText());
        BackupProgressUiCallback callback=new BackupProgressUiCallback(lblProgress);

        new Thread(() -> {
            Platform.runLater(() -> {
                cmdRestore.disableProperty().set(true);
                prgRestore.setProgress(0.0d);
                lblProgress.setText("Prüfe Datensicherung...");
            });
            System.out.println("validate");
            boolean failed=false;
            try {
                re.validate(callback);
            } catch (Exception ex) {
                ex.printStackTrace();
                failed=true;
                Platform.runLater(() -> {
                    lblProgress.setText(ex.getMessage());
                    cmdRestore.disableProperty().set(false);
                });
                
            }
            System.out.println("validate: " + failed);
            if(failed)
                return;
            
            Platform.runLater(() -> {
                lblProgress.setText("Prüfung erfolgreich...");
            });
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(BackupMgrController.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            System.out.println("restore");
            try {
                re.restore(callback);
            } catch (Exception ex) {
                ex.printStackTrace();
                failed=true;
                Platform.runLater(() -> {
                    lblProgress.setText(ex.getMessage());
                    cmdRestore.disableProperty().set(false);
                });

            }
            if(failed)
                return;
            
            Platform.runLater(() -> {
                prgRestore.setProgress(1.0d);
                lblProgress.setText("Wiederherstellung abgeschlossen.");
                cmdRestore.disableProperty().set(false);
            });
        }).start();

    }

    @FXML
    private void cmdBrowseDataDirClicked(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory
                = directoryChooser.showDialog(this.txtBackupDir.getScene().getWindow());

        if (selectedDirectory == null) {
            txtDataDir.setText("");
        } else {
            txtDataDir.setText(selectedDirectory.getAbsolutePath());
        }
    }

}

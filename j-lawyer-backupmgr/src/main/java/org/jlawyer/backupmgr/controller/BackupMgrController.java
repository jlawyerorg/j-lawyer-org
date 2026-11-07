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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
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

    private final UiQuestion databaseErrorQuestion = new UiQuestion(Platform::runLater);

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
        RestoreExecutor re = new RestoreExecutor(this.txtDataDir.getText(), this.txtBackupDir.getText(), this.txtEncryptionPwd.getText(), "localhost", "3306", "jlawyerdb", "root", this.txtMysqlPwd.getText());
        re.setDatabaseErrorHandler(this::continueAfterDatabaseError);
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
            
            String databaseErrors = re.getDatabaseErrorMessage();
            Platform.runLater(() -> {
                prgRestore.setProgress(1.0d);
                if (databaseErrors == null) {
                    lblProgress.setText("Wiederherstellung abgeschlossen.");
                } else {
                    lblProgress.setText("Wiederherstellung mit Fehlern abgeschlossen.");
                    showDatabaseErrors(databaseErrors);
                }
                cmdRestore.disableProperty().set(false);
            });
        }).start();

    }

    /**
     * Asks on the JavaFX thread whether the restore goes on after an error in the database dump, and waits for the
     * answer. Closing the dialog or the application aborts.
     */
    private boolean continueAfterDatabaseError(String message) {
        return this.databaseErrorQuestion.ask(() -> {
            ButtonType abort = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType proceed = new ButtonType("Fehler ignorieren und fortsetzen", ButtonBar.ButtonData.OK_DONE);
            Alert alert = new Alert(Alert.AlertType.WARNING, message, abort, proceed);
            alert.setTitle("Fehler beim Einspielen der Datenbank");
            alert.setHeaderText(null);
            alert.setResizable(true);
            return alert.showAndWait().orElse(abort) == proceed;
        });
    }

    /**
     * Called when the application stops: a restore waiting for an answer is aborted instead of waiting forever.
     */
    public void shutdown() {
        this.databaseErrorQuestion.close();
    }

    /**
     * Shows all errors of a restore that skipped failing statements of the database dump.
     */
    private void showDatabaseErrors(String message) {
        TextArea text = new TextArea(message);
        text.setEditable(false);
        text.setWrapText(true);
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Wiederherstellung mit Fehlern abgeschlossen");
        alert.setHeaderText(null);
        alert.getDialogPane().setContent(text);
        alert.setResizable(true);
        alert.show();
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

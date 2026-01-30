/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.editors.files;

import com.jdimension.jlawyer.client.plugins.form.FormInstancePanel;
import com.jdimension.jlawyer.client.plugins.form.FormPlugin;
import com.jdimension.jlawyer.client.processing.ProgressIndicator;
import com.jdimension.jlawyer.client.processing.ProgressableAction;
import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.persistence.ArchiveFileFormsBean;
import com.jdimension.jlawyer.persistence.FormTypeBean;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 * Background action to load form data for a case in a separate thread.
 * This prevents blocking the Event Dispatch Thread during form loading.
 *
 * @author jens
 */
public class ArchiveFileFormsLoadAction extends ProgressableAction {

    private static final Logger log = Logger.getLogger(ArchiveFileFormsLoadAction.class.getName());

    private ArchiveFileBean caseDto;
    private JComboBox cmbFormType;
    private JTabbedPane tabPaneForms;
    private JPanel pnlAddForms;
    private ArchiveFilePanel owner;

    public ArchiveFileFormsLoadAction(ProgressIndicator i, ArchiveFilePanel owner, ArchiveFileBean caseDto, JComboBox cmbFormType, JTabbedPane tabPaneForms, JPanel pnlAddForms) {
        super(i, false);
        this.owner = owner;
        this.caseDto = caseDto;
        this.cmbFormType = cmbFormType;
        this.tabPaneForms = tabPaneForms;
        this.pnlAddForms = pnlAddForms;
    }

    @Override
    public int getMax() {
        return 3;
    }

    @Override
    public int getMin() {
        return 0;
    }

    @Override
    public String getErrorMessageAndHints(String rootCause) {
        return "Laden der Falldaten fehlgeschlagen!" + System.lineSeparator() + "Ursache: " + rootCause;
    }

    @Override
    public boolean execute() throws Exception {

        this.progress("Lade Falldaten...");

        ClientSettings settings = ClientSettings.getInstance();
        JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());

        // Create thread pool for parallel execution
        ExecutorService executor = Executors.newFixedThreadPool(2);

        List<FormTypeBean> formTypes = null;
        List<ArchiveFileFormsBean> caseForms = null;

        try {
            this.progress("Lade Falldaten: Starte parallele Abfragen...");

            // Submit independent server calls in parallel
            Future<List<FormTypeBean>> futureFormTypes = executor.submit(() ->
                locator.lookupFormsServiceRemote().getAllFormTypes()
            );

            Future<List<ArchiveFileFormsBean>> futureCaseForms = executor.submit(() ->
                locator.lookupFormsServiceRemote().getFormsForCase(this.caseDto.getId())
            );

            // Wait for and retrieve all results
            formTypes = futureFormTypes.get();
            caseForms = futureCaseForms.get();

            // Shutdown executor
            executor.shutdown();

            this.progress("Lade Falldaten: Verarbeite Formulare...");

            // Populate form types combobox on EDT
            final List<FormTypeBean> finalFormTypes = formTypes;
            SwingUtilities.invokeLater(() -> {
                this.cmbFormType.removeAllItems();
                for (FormTypeBean ftb : finalFormTypes) {
                    if (ftb.getUsageType().equals(FormTypeBean.TYPE_PLUGIN)) {
                        this.cmbFormType.addItem(ftb);
                    }
                }
                if (this.cmbFormType.getItemCount() > 0) {
                    this.cmbFormType.setSelectedIndex(0);
                }
            });

            // Load and initialize form instances
            SimpleDateFormat dayFormat = new SimpleDateFormat("dd.MM.yyyy");
            for (ArchiveFileFormsBean affb : caseForms) {
                this.progress("Lade Falldaten: " + affb.getFormType().getName() + " (" + affb.getPlaceHolder() + ")");

                FormPlugin plugin = new FormPlugin();
                plugin.setId(affb.getFormType().getId());
                plugin.setCaseDto(this.caseDto);
                plugin.setPlaceHolder(affb.getPlaceHolder());
                FormInstancePanel formInstance = new FormInstancePanel(this.tabPaneForms, plugin);

                Dimension maxDimension = this.pnlAddForms.getSize();
                maxDimension.setSize(maxDimension.getWidth() - 100, maxDimension.getHeight() - 60);
                formInstance.setMaximumSize(maxDimension);
                formInstance.setPreferredSize(maxDimension);
                formInstance.setDescription(affb.getDescription());
                formInstance.setForm(affb);

                try {
                    formInstance.initialize();
                    String tabTitle = "<html><p style=\"text-align: left; width: 130px\"><b>" + affb.getFormType().getName() + "</b><br/>" + dayFormat.format(affb.getCreationDate()) + "<br/>" + affb.getPlaceHolder() + "</p></html>";

                    // Add tab on EDT
                    final FormInstancePanel finalFormInstance = formInstance;
                    final String finalTabTitle = tabTitle;
                    SwingUtilities.invokeLater(() -> {
                        tabPaneForms.insertTab(finalTabTitle, null, finalFormInstance, null, tabPaneForms.getTabCount() - 1);
                    });

                } catch (Throwable t) {
                    log.error("Error loading form plugin", t);
                    final String errorMsg = t.getMessage();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this.owner, "Fehler beim Laden des Falldatenblattes: " + errorMsg, com.jdimension.jlawyer.client.utils.DesktopUtils.POPUP_TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
                    });
                }
            }

            // select first form tab if any forms were loaded
            SwingUtilities.invokeLater(() -> {
                if (tabPaneForms.getTabCount() > 1) {
                    tabPaneForms.setSelectedIndex(0);
                }
            });

        } catch (Exception ex) {
            log.error("Error loading forms", ex);
            executor.shutdownNow();
            final String errorMsg = ex.getMessage();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this.owner, "Fehler beim Laden der Falldaten: " + errorMsg, com.jdimension.jlawyer.client.utils.DesktopUtils.POPUP_TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
            });
            return false;
        }

        return true;
    }
}

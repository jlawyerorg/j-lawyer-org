package com.jdimension.jlawyer.client.configuration;

import com.jdimension.jlawyer.client.print.PrinterFavorites;
import com.jdimension.jlawyer.client.print.PrinterServiceRegistry;
import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.client.utils.DesktopUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

/**
 * Manages printer favourites for this client installation.
 */
public class PrinterFavoritesDialog extends javax.swing.JDialog {

    private final PrinterServiceRegistry printerRegistry = PrinterServiceRegistry.getInstance();
    private final PrinterFavoritesTableModel tableModel;

    public PrinterFavoritesDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        ClientSettings settings = ClientSettings.getInstance();
        tableModel = new PrinterFavoritesTableModel(printerRegistry.getSnapshot(),
                PrinterFavorites.load(settings));
        tblPrinters.setModel(tableModel);
        tblPrinters.getColumnModel().getColumn(0).setPreferredWidth(60);
        tblPrinters.getColumnModel().getColumn(0).setMaxWidth(80);
        tblPrinters.getColumnModel().getColumn(1).setPreferredWidth(260);
        tblPrinters.getColumnModel().getColumn(2).setPreferredWidth(180);
        tblPrinters.getColumnModel().getColumn(3).setPreferredWidth(170);
        DefaultTableCellRenderer availabilityRenderer = new AvailabilityRenderer();
        tblPrinters.setDefaultRenderer(String.class, availabilityRenderer);
        tblPrinters.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                int viewRow = tblPrinters.rowAtPoint(event.getPoint());
                int viewColumn = tblPrinters.columnAtPoint(event.getPoint());
                if (viewRow < 0 || viewColumn < 0
                        || tblPrinters.convertColumnIndexToModel(viewColumn) == 0) {
                    return;
                }
                stopCellEditing();
                tableModel.selectOnly(tblPrinters.convertRowIndexToModel(viewRow));
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                int viewRow = tblPrinters.rowAtPoint(event.getPoint());
                int viewColumn = tblPrinters.columnAtPoint(event.getPoint());
                if (event.getClickCount() == 2 && viewRow >= 0 && viewColumn >= 0
                        && tblPrinters.convertColumnIndexToModel(viewColumn) == 2) {
                    int modelRow = tblPrinters.convertRowIndexToModel(viewRow);
                    if (tableModel.isCellEditable(modelRow, 2)
                            && tblPrinters.editCellAt(viewRow, viewColumn)) {
                        Component editor = tblPrinters.getEditorComponent();
                        if (editor != null) {
                            editor.requestFocusInWindow();
                        }
                    }
                }
            }
        });
        refreshPrinters();
    }

    private void refreshPrinters() {
        cmdRefresh.setEnabled(false);
        lblRefreshStatus.setText("Druckerliste wird aktualisiert...");
        printerRegistry.refreshAsync(snapshot -> SwingUtilities.invokeLater(() -> {
            stopCellEditing();
            tableModel.updateSnapshot(snapshot);
            lblRefreshStatus.setText(snapshot.getPrinterNames().size() + " Drucker verfügbar");
            cmdRefresh.setEnabled(true);
        }));
    }

    private void stopCellEditing() {
        TableCellEditor editor = tblPrinters.getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblDescription = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblPrinters = new javax.swing.JTable();
        lblRefreshStatus = new javax.swing.JLabel();
        cmdRefresh = new javax.swing.JButton();
        cmdSave = new javax.swing.JButton();
        cmdClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Druckerfavoriten");
        setMinimumSize(new java.awt.Dimension(720, 360));

        lblDescription.setText("<html>Nur ausgewählte und aktuell verfügbare Drucker erscheinen zusätzlich im Dokumentmenü.<br>"
            + "Anzeigename ändern: Drucker markieren, dann unter „Anzeigename“ doppelt anklicken.</html>");

        tblPrinters.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Favorit", "Drucker", "Anzeigename", "Status"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblPrinters.setFillsViewportHeight(true);
        tblPrinters.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tblPrinters);

        lblRefreshStatus.setText(" ");

        cmdRefresh.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons16/reload.png"))); // NOI18N
        cmdRefresh.setText("Aktualisieren");
        cmdRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdRefreshActionPerformed(evt);
            }
        });

        cmdSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/agt_action_success.png"))); // NOI18N
        cmdSave.setText("Übernehmen");
        cmdSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdSaveActionPerformed(evt);
            }
        });

        cmdClose.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/cancel.png"))); // NOI18N
        cmdClose.setText("Schließen");
        cmdClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdCloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblDescription, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 696, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRefreshStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmdRefresh)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmdSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmdClose)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblDescription)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRefreshStatus)
                    .addComponent(cmdRefresh)
                    .addComponent(cmdSave)
                    .addComponent(cmdClose))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cmdRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdRefreshActionPerformed
        stopCellEditing();
        refreshPrinters();
    }//GEN-LAST:event_cmdRefreshActionPerformed

    private void cmdSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdSaveActionPerformed
        stopCellEditing();
        try {
            ClientSettings settings = ClientSettings.getInstance();
            PrinterFavorites.save(settings, tableModel.getSelectedFavorites());
            settings.saveConfiguration();
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Druckerfavoriten konnten nicht gespeichert werden: "
                    + ex.getMessage(), DesktopUtils.POPUP_TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_cmdSaveActionPerformed

    private void cmdCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdCloseActionPerformed
        dispose();
    }//GEN-LAST:event_cmdCloseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cmdClose;
    private javax.swing.JButton cmdRefresh;
    private javax.swing.JButton cmdSave;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblDescription;
    private javax.swing.JLabel lblRefreshStatus;
    private javax.swing.JTable tblPrinters;
    // End of variables declaration//GEN-END:variables

    private final class AvailabilityRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            Component component = super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            boolean available = tableModel.isAvailable(table.convertRowIndexToModel(row));
            component.setEnabled(available);
            if (!selected) {
                component.setForeground(available ? table.getForeground() : Color.GRAY);
            }
            return component;
        }
    }
}

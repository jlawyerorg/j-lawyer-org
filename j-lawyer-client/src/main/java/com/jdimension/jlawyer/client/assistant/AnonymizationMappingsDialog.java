package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.ai.AnonymizationMapping;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Shows the replacements made by the anonymization of an assistant conversation: which
 * placeholder stands for which original value.
 */
public class AnonymizationMappingsDialog extends JDialog {

    private static final Map<String, String> TYPE_LABELS = new HashMap<>();

    static {
        TYPE_LABELS.put(AnonymizationMapping.TYPE_PERSON, "Person");
        TYPE_LABELS.put(AnonymizationMapping.TYPE_ORGANIZATION, "Organisation");
        TYPE_LABELS.put(AnonymizationMapping.TYPE_ADDRESS, "Adresse");
        TYPE_LABELS.put(AnonymizationMapping.TYPE_CITY, "Ort");
        TYPE_LABELS.put(AnonymizationMapping.TYPE_POSTAL_CODE, "Postleitzahl");
        TYPE_LABELS.put(AnonymizationMapping.TYPE_DATE_OF_BIRTH, "Geburtsdatum");
        TYPE_LABELS.put(AnonymizationMapping.TYPE_CASE_NUMBER, "Aktenzeichen");
        TYPE_LABELS.put(AnonymizationMapping.TYPE_IBAN, "IBAN");
        TYPE_LABELS.put(AnonymizationMapping.TYPE_EMAIL, "E-Mail");
        TYPE_LABELS.put(AnonymizationMapping.TYPE_PHONE, "Telefon");
        TYPE_LABELS.put(AnonymizationMapping.TYPE_OTHER, "Sonstiges");
    }

    public AnonymizationMappingsDialog(Window owner, List<AnonymizationMapping> mappings) {
        super(owner, "Anonymisierte Angaben", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String summary = mappings.size() == 1 ? "1 Angabe wurde" : mappings.size() + " Angaben wurden";
        JLabel lblInfo = new JLabel("<html>" + summary + " vor dem Versand an das KI-Modell durch Platzhalter ersetzt "
                + "und in der Antwort wiederhergestellt.</html>");
        java.net.URL iconUrl = getClass().getResource("/icons16/material/policy_20dp_0E72B5.png");
        if (iconUrl != null) {
            lblInfo.setIcon(new ImageIcon(iconUrl));
        }
        content.add(lblInfo, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new Object[][]{}, new String[]{"Platzhalter", "Originalwert", "Typ"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (AnonymizationMapping m : mappings) {
            model.addRow(new Object[]{m.getPlaceholder(), m.getOriginalValue(), typeLabel(m.getEntityType())});
        }
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(600, 320));
        if (mappings.isEmpty()) {
            JLabel lblEmpty = new JLabel("Es wurden keine personenbezogenen Angaben ersetzt.");
            lblEmpty.setForeground(new Color(153, 153, 153));
            lblEmpty.setHorizontalAlignment(JLabel.CENTER);
            content.add(lblEmpty, BorderLayout.CENTER);
        } else {
            content.add(scroll, BorderLayout.CENTER);
        }

        JButton cmdClose = new JButton("Schliessen");
        java.net.URL closeUrl = getClass().getResource("/icons/cancel.png");
        if (closeUrl != null) {
            cmdClose.setIcon(new ImageIcon(closeUrl));
        }
        cmdClose.addActionListener(e -> dispose());
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlButtons.add(cmdClose);
        content.add(pnlButtons, BorderLayout.SOUTH);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        getRootPane().setDefaultButton(cmdClose);

        setContentPane(content);
        pack();
    }

    private static String typeLabel(String entityType) {
        if (entityType == null) {
            return TYPE_LABELS.get(AnonymizationMapping.TYPE_OTHER);
        }
        String label = TYPE_LABELS.get(entityType);
        return label != null ? label : entityType;
    }

}

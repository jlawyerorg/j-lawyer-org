package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.ai.ToolDefinition;
import com.jdimension.jlawyer.ai.ToolParameter;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;

/**
 * Modal dialog for approving or denying tool calls from the AI assistant.
 * Displays tool parameters in a human-readable format with German descriptions.
 */
public class ToolApprovalDialog extends JDialog {

    private String approvalResult = "deny";

    public ToolApprovalDialog(JDialog parent, String toolSummary, String toolName,
            String riskLevel, ToolDefinition toolDef, String argumentsJson) {
        super(parent, "Werkzeugaufruf genehmigen", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(950, 600));

        // Use Scrollable panel so width always tracks the viewport (shrinks back when dialog is narrowed)
        JPanel contentPanel = new ScrollablePanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // --- Risk header ---
        Color headerBg;
        Color headerBorder;
        Color indicatorColor;
        String riskLabel;
        if (ToolDefinition.RISK_HIGH.equals(riskLevel)) {
            headerBg = new Color(255, 240, 240);
            headerBorder = new Color(220, 150, 150);
            indicatorColor = new Color(244, 67, 54);
            riskLabel = "Hoch";
        } else if (ToolDefinition.RISK_MEDIUM.equals(riskLevel)) {
            headerBg = new Color(255, 248, 220);
            headerBorder = new Color(220, 180, 100);
            indicatorColor = new Color(255, 152, 0);
            riskLabel = "Mittel";
        } else {
            headerBg = new Color(240, 255, 240);
            headerBorder = new Color(150, 200, 150);
            indicatorColor = new Color(76, 175, 80);
            riskLabel = "Niedrig";
        }

        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBackground(headerBg);
        headerPanel.setBorder(new FlatLineBorder(new Insets(10, 14, 10, 14), headerBorder, 1, 10));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 6);

        JLabel dot = new JLabel("\u25CF");
        dot.setForeground(indicatorColor);
        dot.setFont(dot.getFont().deriveFont(14f));
        headerPanel.add(dot, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel headerTextPanel = new JPanel();
        headerTextPanel.setLayout(new BoxLayout(headerTextPanel, BoxLayout.Y_AXIS));
        headerTextPanel.setOpaque(false);
        JLabel headerLine1 = new JLabel("Ingo m\u00f6chte ein Werkzeug verwenden");
        headerLine1.setFont(headerLine1.getFont().deriveFont(Font.BOLD, headerLine1.getFont().getSize2D()));
        headerTextPanel.add(headerLine1);
        JLabel headerLine2 = new JLabel("Risiko: " + riskLabel);
        headerLine2.setFont(headerLine2.getFont().deriveFont(headerLine2.getFont().getSize2D() - 1f));
        headerLine2.setForeground(indicatorColor);
        headerTextPanel.add(headerLine2);
        headerPanel.add(headerTextPanel, gbc);

        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, headerPanel.getPreferredSize().height));
        contentPanel.add(headerPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // --- Tool summary (description from tool definition, no raw arguments) ---
        String displayText = (toolDef != null && toolDef.getDescription() != null)
                ? toolDef.getDescription() : toolSummary;
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));
        summaryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        summaryPanel.setOpaque(false);
        JLabel summaryLabel = new JLabel("<html><body>" + displayText + "</body></html>");
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.BOLD, summaryLabel.getFont().getSize2D() + 2f));
        summaryPanel.add(summaryLabel);
        summaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, summaryPanel.getPreferredSize().height));
        contentPanel.add(summaryPanel);
        contentPanel.add(Box.createVerticalStrut(6));

        // --- Parameters table ---
        JPanel paramsSection = buildParametersPanel(toolDef, argumentsJson);
        if (paramsSection != null) {
            paramsSection.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(paramsSection);
            contentPanel.add(Box.createVerticalStrut(10));
        }

        // Wrap in a scroll pane for tools with many parameters
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        // --- Buttons (outside scroll pane, fixed at bottom) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        JButton btnAllow = new JButton("Erlauben");
        btnAllow.setBackground(new Color(76, 175, 80));
        btnAllow.setForeground(Color.WHITE);
        btnAllow.addActionListener(e -> {
            approvalResult = "allow";
            dispose();
        });

        JButton btnSession = new JButton("F\u00fcr Sitzung erlauben");
        btnSession.addActionListener(e -> {
            approvalResult = "session";
            dispose();
        });

        JButton btnAlways = new JButton("Immer erlauben");
        btnAlways.addActionListener(e -> {
            approvalResult = "always";
            dispose();
        });

        JButton btnDeny = new JButton("Ablehnen");
        btnDeny.addActionListener(e -> {
            approvalResult = "deny";
            dispose();
        });

        buttonPanel.add(btnAllow);
        buttonPanel.add(btnSession);
        buttonPanel.add(btnAlways);
        buttonPanel.add(btnDeny);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Default button and key bindings
        getRootPane().setDefaultButton(btnAllow);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "deny");
        getRootPane().getActionMap().put("deny", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                approvalResult = "deny";
                dispose();
            }
        });

        // Fixed width, height adapts to content but never exceeds screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int dialogWidth = Math.min(950, screenSize.width - 40);
        // Layout at target width to get proper wrapped height
        setSize(dialogWidth, 700);
        validate();
        int contentHeight = contentPanel.getPreferredSize().height
                + getInsets().top + getInsets().bottom + 40;
        int dialogHeight = Math.max(600, Math.min(contentHeight, screenSize.height - 80));
        setSize(dialogWidth, dialogHeight);
        setLocationRelativeTo(parent);
    }

    private JPanel buildParametersPanel(ToolDefinition toolDef, String argumentsJson) {
        JsonObject args;
        try {
            args = (JsonObject) Jsoner.deserialize(argumentsJson);
        } catch (Exception ex) {
            return null;
        }
        if (args == null || args.isEmpty()) {
            return null;
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));

        JLabel paramTitle = new JLabel("Parameter:");
        paramTitle.setFont(paramTitle.getFont().deriveFont(Font.BOLD));
        paramTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        wrapper.add(paramTitle, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new GridBagLayout());
        tablePanel.setBackground(new Color(245, 245, 245));
        tablePanel.setBorder(new FlatLineBorder(new Insets(6, 10, 6, 10), new Color(200, 200, 200), 1, 8));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(3, 4, 3, 10);

        for (Map.Entry<String, Object> entry : args.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (val == null || val.toString().isEmpty()) {
                continue;
            }

            String displayKey = resolveDisplayKey(toolDef, key);
            String displayValue = val.toString();
            if (displayValue.length() > 200) {
                displayValue = displayValue.substring(0, 200) + "\u2026";
            }

            gc.gridx = 0;
            gc.weightx = 0;
            gc.fill = GridBagConstraints.NONE;
            JLabel keyLabel = new JLabel(displayKey);
            keyLabel.setFont(keyLabel.getFont().deriveFont(Font.PLAIN, keyLabel.getFont().getSize2D() - 1f));
            keyLabel.setForeground(new Color(100, 100, 100));
            tablePanel.add(keyLabel, gc);

            gc.gridx = 1;
            gc.weightx = 1.0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            JTextArea valArea = new JTextArea(displayValue);
            valArea.setFont(valArea.getFont().deriveFont(Font.PLAIN, valArea.getFont().getSize2D() - 1f));
            valArea.setLineWrap(true);
            valArea.setWrapStyleWord(true);
            valArea.setEditable(false);
            valArea.setOpaque(false);
            valArea.setBorder(null);
            tablePanel.add(valArea, gc);

            gc.gridy++;
        }

        wrapper.add(tablePanel, BorderLayout.CENTER);
        return wrapper;
    }

    private String resolveDisplayKey(ToolDefinition toolDef, String paramName) {
        if (toolDef != null) {
            List<ToolParameter> params = toolDef.getParameters();
            if (params != null) {
                for (ToolParameter tp : params) {
                    if (tp.getName().equals(paramName) && tp.getDescription() != null
                            && !tp.getDescription().isEmpty()) {
                        return tp.getDescription();
                    }
                }
            }
        }
        return paramName;
    }

    public String getApprovalResult() {
        return approvalResult;
    }

    /**
     * A JPanel that implements Scrollable to track the viewport width,
     * preventing the panel from staying wider than the scroll pane after resizing.
     */
    private static class ScrollablePanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}

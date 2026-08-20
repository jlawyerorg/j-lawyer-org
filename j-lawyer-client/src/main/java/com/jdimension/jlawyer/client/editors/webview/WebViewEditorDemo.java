package com.jdimension.jlawyer.client.editors.webview;

import javax.swing.*;
import java.awt.*;

/**
 * Demo application to test the WebViewHtmlEditorPanel (SunEditor integration).
 * This standalone application allows testing all EditorImplementation methods.
 *
 * @author j-lawyer.org
 */
public class WebViewEditorDemo extends JFrame {

    private WebViewHtmlEditorPanel editorPanel;
    private JTextArea outputArea;

    public WebViewEditorDemo() {
        super("SunEditor WebView Demo - j-lawyer.org PoC");
        initComponents();

        // CRITICAL: Add window listener to cleanup resources on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cleanup();
            }
        });
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Main layout: editor on left, controls on right
        setLayout(new BorderLayout());

        // Create editor panel
        editorPanel = new WebViewHtmlEditorPanel();

        // Create control panel
        JPanel controlPanel = createControlPanel();

        // Split pane: editor (left) and controls (right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(editorPanel);
        splitPane.setRightComponent(controlPanel);
        splitPane.setDividerLocation(800);

        add(splitPane, BorderLayout.CENTER);

        // Status bar
        JLabel statusBar = new JLabel("SunEditor PoC - Testing EditorImplementation interface");
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel title = new JLabel("Test Controls");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(title, BorderLayout.NORTH);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Test buttons
        addButton(buttonPanel, "Get Text", this::testGetText);
        addButton(buttonPanel, "Set Sample HTML", this::testSetText);
        addButton(buttonPanel, "Clear Content", this::testClearText);
        addButton(buttonPanel, "Insert Text at Cursor", this::testInsert);
        addButton(buttonPanel, "Get Caret Position", this::testGetCaretPosition);
        addButton(buttonPanel, "Set Caret Position (10)", this::testSetCaretPosition);
        addButton(buttonPanel, "Get Selected Text", this::testGetSelectedText);
        addSeparator(buttonPanel);
        addButton(buttonPanel, "Test Font Size (SHEF Bug Test)", this::testFontSize);
        addButton(buttonPanel, "Test Format Preservation", this::testFormatPreservation);
        addButton(buttonPanel, "Load Complex HTML", this::testComplexHtml);
        addSeparator(buttonPanel);
        addButton(buttonPanel, "Clear Output", this::clearOutput);

        JScrollPane buttonScroll = new JScrollPane(buttonPanel);
        buttonScroll.setBorder(BorderFactory.createTitledBorder("Actions"));

        // Output area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Output"));

        // Split: buttons on top, output on bottom
        JSplitPane controlSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        controlSplit.setTopComponent(buttonScroll);
        controlSplit.setBottomComponent(outputScroll);
        controlSplit.setDividerLocation(400);

        panel.add(controlSplit, BorderLayout.CENTER);

        return panel;
    }

    private void addButton(JPanel panel, String text, Runnable action) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(e -> {
            try {
                action.run();
            } catch (Exception ex) {
                logError("Error: " + ex.getMessage());
            }
        });
        panel.add(button);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
    }

    private void addSeparator(JPanel panel) {
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        panel.add(sep);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    // ========== Test Methods ==========

    private void testGetText() {
        String text = editorPanel.getText();
        log("getText() returned:");
        log(text);
        log("Length: " + text.length() + " characters");
    }

    private void testSetText() {
        String html = "<h2>Sample HTML Content</h2>" +
                "<p>This is a <strong>bold</strong> and <em>italic</em> text.</p>" +
                "<p style=\"color: red; font-size: 18px;\">Colored and sized text</p>" +
                "<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>";
        editorPanel.setText(html);
        log("setText() called with sample HTML");
    }

    private void testClearText() {
        editorPanel.setText("");
        log("setText(\"\") called - editor cleared");
    }

    private void testInsert() {
        String textToInsert = "<strong>INSERTED TEXT</strong>";
        editorPanel.insert(textToInsert, 0);
        log("insert() called with: " + textToInsert);
    }

    private void testGetCaretPosition() {
        int pos = editorPanel.getCaretPosition();
        log("getCaretPosition() returned: " + pos);
    }

    private void testSetCaretPosition() {
        editorPanel.setCaretPosition(10);
        log("setCaretPosition(10) called");
        int newPos = editorPanel.getCaretPosition();
        log("New position: " + newPos);
    }

    private void testGetSelectedText() {
        String selected = editorPanel.getSelectedText();
        log("getSelectedText() returned: \"" + selected + "\"");
        if (selected.isEmpty()) {
            log("(No text selected - please select some text in the editor first)");
        }
    }

    private void testFontSize() {
        log("Testing SHEF font size bug fix...");
        // This HTML has font size="5" which SHEF would corrupt to size="12px"
        String html = "<p><font size=\"5\">This text has font size 5</font></p>" +
                "<p style=\"font-size: 18px;\">This text has font-size: 18px</p>";
        editorPanel.setText(html);
        log("Set HTML with font sizes");

        // Wait a bit for editor to process
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        String result = editorPanel.getText();
        log("Retrieved HTML:");
        log(result);

        // Check if SHEF bug is present
        if (result.contains("size=\"12px\"") || result.contains("size=\"18pxpx\"")) {
            log("WARNING: SHEF mutation bug detected!");
        } else {
            log("SUCCESS: No SHEF mutation bug detected");
        }
    }

    private void testFormatPreservation() {
        log("Testing format preservation on line breaks...");
        String html = "<p><strong>Bold text</strong></p>" +
                "<p><em>Italic text</em></p>" +
                "<p style=\"color: blue;\">Colored text</p>";
        editorPanel.setText(html);
        log("Set formatted HTML - try adding line breaks and check if formatting is preserved");
    }

    private void testComplexHtml() {
        String html = "<!DOCTYPE html><html><body>" +
                "<h1>Complex HTML Test</h1>" +
                "<table border=\"1\">" +
                "<tr><th>Header 1</th><th>Header 2</th></tr>" +
                "<tr><td>Cell 1</td><td>Cell 2</td></tr>" +
                "</table>" +
                "<p>Text with <a href=\"https://j-lawyer.org\">link</a></p>" +
                "<blockquote>This is a quote</blockquote>" +
                "</body></html>";
        editorPanel.setText(html);
        log("Loaded complex HTML with table, links, and blockquote");
    }

    // ========== Logging ==========

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append(message + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    private void logError(String message) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append("ERROR: " + message + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    private void clearOutput() {
        outputArea.setText("");
        log("Output cleared");
    }

    /**
     * Cleanup resources before closing.
     * CRITICAL: This prevents memory leaks when closing the window.
     */
    private void cleanup() {
        log("Cleaning up resources...");

        if (editorPanel != null) {
            editorPanel.dispose();
            log("Editor panel disposed");
        }

        log("Cleanup complete");
    }

    // ========== Main Method ==========

    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // do nothing
        }

        // Create and show demo
        SwingUtilities.invokeLater(() -> {
            WebViewEditorDemo demo = new WebViewEditorDemo();
            demo.setVisible(true);
        });
    }
}

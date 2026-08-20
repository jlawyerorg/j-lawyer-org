package com.jdimension.jlawyer.client.utils;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A reusable popup-like dialog with a filter text field at the top.
 * Replaces JPopupMenu when a searchable list of items is needed.
 */
public class FilterablePopupDialog extends JDialog {

    private final JTextField filterField;
    private final JPanel itemsPanel;
    private final JScrollPane scrollPane;
    private final List<FilterableItem> allItems = new ArrayList<>();

    private static final int MAX_HEIGHT = 400;
    private static final int PREF_WIDTH = 500;

    public FilterablePopupDialog(Window owner) {
        super(owner);
        setUndecorated(true);
        setType(Window.Type.POPUP);
        setModal(false);

        filterField = new JTextField();
        filterField.putClientProperty("JTextField.placeholderText", "Filter...");
        filterField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground") != null ? UIManager.getColor("Separator.foreground") : Color.GRAY),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        Color popupBg = UIManager.getColor("PopupMenu.background");
        if (popupBg != null) {
            itemsPanel.setBackground(popupBg);
        }

        scrollPane = new JScrollPane(itemsPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        setLayout(new BorderLayout());
        add(filterField, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        Color borderColor = UIManager.getColor("PopupMenu.borderColor");
        if (borderColor == null) {
            borderColor = UIManager.getColor("Component.borderColor");
        }
        if (borderColor == null) {
            borderColor = Color.GRAY;
        }
        getRootPane().setBorder(BorderFactory.createLineBorder(borderColor));

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                rebuildItems();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                rebuildItems();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                rebuildItems();
            }
        });

        filterField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                }
            }
        });

        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                dispose();
            }
        });
    }

    /**
     * Adds an item to the popup list.
     *
     * @param displayText the text to display
     * @param action the action to execute when the item is clicked
     */
    public void addItem(String displayText, ActionListener action) {
        allItems.add(new FilterableItem(displayText, action));
    }

    private void rebuildItems() {
        itemsPanel.removeAll();
        String filter = filterField.getText().toLowerCase();
        for (FilterableItem item : allItems) {
            if (filter.isEmpty() || item.displayText.toLowerCase().contains(filter)) {
                JComponent comp = createItemButton(item);
                itemsPanel.add(comp);
            }
        }
        itemsPanel.revalidate();
        itemsPanel.repaint();
        adjustSize();
    }

    private JComponent createItemButton(FilterableItem item) {
        JLabel label = new JLabel(item.displayText);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        Color popupBg = UIManager.getColor("PopupMenu.background");
        if (popupBg == null) {
            popupBg = label.getBackground();
        }
        label.setBackground(popupBg);

        Font menuFont = UIManager.getFont("MenuItem.font");
        if (menuFont != null) {
            label.setFont(menuFont);
        }

        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));

        final Color normalBg = label.getBackground();
        final Color normalFg = label.getForeground();

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Color selBg = UIManager.getColor("MenuItem.selectionBackground");
                Color selFg = UIManager.getColor("MenuItem.selectionForeground");
                label.setBackground(selBg != null ? selBg : normalBg);
                label.setForeground(selFg != null ? selFg : normalFg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setBackground(normalBg);
                label.setForeground(normalFg);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                item.action.actionPerformed(new ActionEvent(label, ActionEvent.ACTION_PERFORMED, item.displayText));
            }
        });

        return label;
    }

    private void adjustSize() {
        // temporarily remove preferred size constraint so pack() calculates natural size
        scrollPane.setPreferredSize(null);
        pack();
        int naturalHeight = getHeight();
        int naturalWidth = getWidth();
        int width = Math.max(PREF_WIDTH, naturalWidth);
        int height = Math.min(MAX_HEIGHT, naturalHeight);
        setSize(width, height);
    }

    /**
     * Shows the popup at the given position relative to the invoker component.
     *
     * @param invoker the component that triggered the popup
     * @param x x offset relative to invoker
     * @param y y offset relative to invoker
     */
    public void showAt(Component invoker, int x, int y) {
        rebuildItems();

        Point invokerScreen = invoker.getLocationOnScreen();
        int posX = invokerScreen.x + x;
        int posY = invokerScreen.y + y;

        GraphicsConfiguration gc = invoker.getGraphicsConfiguration();
        if (gc != null) {
            Rectangle screenBounds = gc.getBounds();
            if (posX + getWidth() > screenBounds.x + screenBounds.width) {
                posX = screenBounds.x + screenBounds.width - getWidth();
            }
            if (posY + getHeight() > screenBounds.y + screenBounds.height) {
                posY = screenBounds.y + screenBounds.height - getHeight();
            }
        }

        setLocation(posX, posY);
        setVisible(true);
        SwingUtilities.invokeLater(() -> filterField.requestFocusInWindow());
    }

    private static class FilterableItem {

        final String displayText;
        final ActionListener action;

        FilterableItem(String displayText, ActionListener action) {
            this.displayText = displayText;
            this.action = action;
        }
    }
}

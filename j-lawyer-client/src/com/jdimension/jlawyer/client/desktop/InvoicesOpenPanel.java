/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

import com.jdimension.jlawyer.client.modulebar.BadgeIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jlawyer.themes.ServerColorTheme;

/**
 * Desktop panel displaying open invoice statistics as badge cards.
 * Shows two categories: invoices not yet due (green) and overdue invoices (red),
 * each with count and total amount.
 *
 * @author jens
 */
public class InvoicesOpenPanel extends JPanel {

    private static final Color CARD_BACKGROUND = new Color(
            ServerColorTheme.COLOR_DARK_GREY.getRed(),
            ServerColorTheme.COLOR_DARK_GREY.getGreen(),
            ServerColorTheme.COLOR_DARK_GREY.getBlue(),
            190).darker().darker();

    private final JLabel lblNotDueBadge;
    private final JLabel lblNotDueAmount;
    private final JLabel lblOverdueBadge;
    private final JLabel lblOverdueAmount;
    private final JLabel lblLoading;
    private final JPanel pnlCards;
    private final JPanel cardNotDue;
    private final JPanel cardOverdue;
    private final JButton cmdRefresh;

    private final NumberFormat currencyFormat;

    public InvoicesOpenPanel() {
        super(new BorderLayout(0, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);

        // Header
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);

        JLabel lblTitle = new JLabel("Offene Rechnungen");
        lblTitle.setFont(lblTitle.getFont().deriveFont(lblTitle.getFont().getStyle() | Font.BOLD, lblTitle.getFont().getSize() + 2));
        lblTitle.setForeground(Color.WHITE);
        pnlHeader.add(lblTitle, BorderLayout.WEST);

        cmdRefresh = new JButton();
        cmdRefresh.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons16/material/baseline_refresh_white_36dp.png")));
        cmdRefresh.setToolTipText("Aktualisieren");
        cmdRefresh.setBorder(null);
        cmdRefresh.setContentAreaFilled(false);
        cmdRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JPanel pnlRefresh = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlRefresh.setOpaque(false);
        pnlRefresh.add(cmdRefresh);
        pnlHeader.add(pnlRefresh, BorderLayout.EAST);

        add(pnlHeader, BorderLayout.NORTH);

        // Content area
        pnlCards = new JPanel();
        pnlCards.setOpaque(false);
        pnlCards.setLayout(new BoxLayout(pnlCards, BoxLayout.Y_AXIS));

        // Loading placeholder
        lblLoading = new JLabel("Rechnungen werden geladen...");
        lblLoading.setForeground(Color.WHITE);
        pnlCards.add(lblLoading);

        // Badge font
        Font badgeFont = lblLoading.getFont().deriveFont(Font.BOLD, 13f);

        // Card: Not yet due
        cardNotDue = createCard();
        lblNotDueBadge = new JLabel("0");
        lblNotDueBadge.setIcon(new BadgeIcon("0", ServerColorTheme.COLOR_LOGO_GREEN, badgeFont));
        lblNotDueBadge.setText("Noch nicht fällig");
        lblNotDueBadge.setFont(lblNotDueBadge.getFont().deriveFont(Font.BOLD, lblNotDueBadge.getFont().getSize() + 1f));
        lblNotDueBadge.setForeground(Color.WHITE);
        lblNotDueBadge.setIconTextGap(10);

        lblNotDueAmount = new JLabel(currencyFormat.format(0));
        lblNotDueAmount.setForeground(new Color(200, 200, 200));
        lblNotDueAmount.setFont(lblNotDueAmount.getFont().deriveFont(Font.PLAIN, lblNotDueAmount.getFont().getSize() + 1f));

        cardNotDue.add(lblNotDueBadge, createGbc(0));
        cardNotDue.add(Box.createRigidArea(new Dimension(0, 4)), createGbc(1));
        cardNotDue.add(lblNotDueAmount, createGbc(2));
        cardNotDue.setVisible(false);

        // Card: Overdue
        cardOverdue = createCard();
        lblOverdueBadge = new JLabel("0");
        lblOverdueBadge.setIcon(new BadgeIcon("0", ServerColorTheme.COLOR_LOGO_RED, badgeFont));
        lblOverdueBadge.setText("Fällig / Überfällig");
        lblOverdueBadge.setFont(lblOverdueBadge.getFont().deriveFont(Font.BOLD, lblOverdueBadge.getFont().getSize() + 1f));
        lblOverdueBadge.setForeground(Color.WHITE);
        lblOverdueBadge.setIconTextGap(10);

        lblOverdueAmount = new JLabel(currencyFormat.format(0));
        lblOverdueAmount.setForeground(new Color(200, 200, 200));
        lblOverdueAmount.setFont(lblOverdueAmount.getFont().deriveFont(Font.PLAIN, lblOverdueAmount.getFont().getSize() + 1f));

        cardOverdue.add(lblOverdueBadge, createGbc(0));
        cardOverdue.add(Box.createRigidArea(new Dimension(0, 4)), createGbc(1));
        cardOverdue.add(lblOverdueAmount, createGbc(2));
        cardOverdue.setVisible(false);

        pnlCards.add(cardNotDue);
        pnlCards.add(Box.createRigidArea(new Dimension(0, 8)));
        pnlCards.add(cardOverdue);

        add(pnlCards, BorderLayout.CENTER);
    }

    private JPanel createCard() {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BACKGROUND);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        Dimension maxSize = new Dimension(Integer.MAX_VALUE, 80);
        card.setMaximumSize(maxSize);
        return card;
    }

    private GridBagConstraints createGbc(int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        return gbc;
    }

    /**
     * Updates the panel with fresh invoice data.
     *
     * @param notDueCount number of open invoices not yet due
     * @param notDueTotal total gross amount of invoices not yet due
     * @param overdueCount number of overdue invoices
     * @param overdueTotal total gross amount of overdue invoices
     */
    public void updateData(long notDueCount, BigDecimal notDueTotal, long overdueCount, BigDecimal overdueTotal) {
        lblLoading.setVisible(false);

        Font badgeFont = lblNotDueBadge.getFont().deriveFont(Font.BOLD, 13f);

        lblNotDueBadge.setIcon(new BadgeIcon(String.valueOf(notDueCount), ServerColorTheme.COLOR_LOGO_GREEN, badgeFont));
        lblNotDueAmount.setText("Summe: " + currencyFormat.format(notDueTotal));
        cardNotDue.setVisible(true);

        lblOverdueBadge.setIcon(new BadgeIcon(String.valueOf(overdueCount), ServerColorTheme.COLOR_LOGO_RED, badgeFont));
        lblOverdueAmount.setText("Summe: " + currencyFormat.format(overdueTotal));
        cardOverdue.setVisible(true);

        revalidate();
        repaint();
    }

    /**
     * Returns the refresh button so the parent can wire up action listeners.
     *
     * @return the refresh button
     */
    public JButton getRefreshButton() {
        return cmdRefresh;
    }
}

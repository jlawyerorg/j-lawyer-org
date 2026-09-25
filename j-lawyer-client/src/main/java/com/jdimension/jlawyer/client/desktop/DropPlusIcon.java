/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/**
 * Drop indicator shown on desktop entries while files are dragged over them:
 * a white outlined square with a white "+" in its center.
 */
public class DropPlusIcon implements Icon {

    private final int size;

    public DropPlusIcon(int size) {
        this.size = size;
    }

    /**
     * Creates an icon showing the given icon followed by a drop indicator, for
     * labels that carry the icon of the entry themselves.
     *
     * @param icon the icon to show first
     * @param size size of the drop indicator
     * @param gap space between the icon and the drop indicator
     * @return the combined icon
     */
    public static Icon appendedTo(Icon icon, int size, int gap) {
        DropPlusIcon plus = new DropPlusIcon(size);
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                int h = getIconHeight();
                icon.paintIcon(c, g, x, y + (h - icon.getIconHeight()) / 2);
                plus.paintIcon(c, g, x + icon.getIconWidth() + gap, y + (h - size) / 2);
            }

            @Override
            public int getIconWidth() {
                return icon.getIconWidth() + gap + size;
            }

            @Override
            public int getIconHeight() {
                return Math.max(icon.getIconHeight(), size);
            }
        };
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setColor(Color.WHITE);

            float frameStroke = Math.max(1.5f, size / 13f);
            float inset = frameStroke / 2f;
            float arc = size / 5f;
            g2.setStroke(new BasicStroke(frameStroke));
            g2.draw(new RoundRectangle2D.Float(x + inset, y + inset, size - frameStroke, size - frameStroke, arc, arc));

            float half = size * 0.25f;
            float cx = x + size / 2f;
            float cy = y + size / 2f;
            g2.setStroke(new BasicStroke(Math.max(2f, size / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Float(cx - half, cy, cx + half, cy));
            g2.draw(new Line2D.Float(cx, cy - half, cx, cy + half));
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}

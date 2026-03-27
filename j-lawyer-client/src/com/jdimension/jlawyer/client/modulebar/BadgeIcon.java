/*
                    GNU AFFERO GENERAL PUBLIC LICENSE
                       Version 3, 19 November 2007

 Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.modulebar;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/**
 * A pill-shaped badge icon for displaying notification counts. Renders a
 * rounded capsule with centered white text on a colored background.
 *
 * @author jens
 */
public class BadgeIcon implements Icon {

    private final String text;
    private final Color backgroundColor;
    private final Font font;
    private final int width;
    private final int height;

    private static final int H_PAD = 7;
    private static final int V_PAD = 3;
    private static final int RIGHT_MARGIN = 2;

    public BadgeIcon(String text, Color backgroundColor, Font font) {
        this.text = text;
        this.backgroundColor = backgroundColor;
        this.font = font;

        FontMetrics fm = new Canvas().getFontMetrics(font);
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent() + fm.getDescent();
        this.height = textHeight + V_PAD * 2;
        this.width = Math.max(textWidth + H_PAD * 2, this.height);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        g2d.setColor(backgroundColor);
        g2d.fillRoundRect(x, y, width, height, height, height);

        g2d.setColor(Color.WHITE);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int textX = x + (width - fm.stringWidth(text)) / 2;
        int textY = y + ((height - fm.getHeight()) / 2) + fm.getAscent();
        g2d.drawString(text, textX, textY);

        g2d.dispose();
    }

    @Override
    public int getIconWidth() {
        return width + RIGHT_MARGIN;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}

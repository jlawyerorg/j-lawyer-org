/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.utils;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * An Icon implementation that renders two icons side by side (left and right).
 * If either icon is null, only the other icon is rendered without a gap.
 */
public class CompoundIcon implements Icon {

    private final Icon leftIcon;
    private final Icon rightIcon;
    private final int gap;

    public CompoundIcon(Icon leftIcon, Icon rightIcon) {
        this(leftIcon, rightIcon, 2);
    }

    public CompoundIcon(Icon leftIcon, Icon rightIcon, int gap) {
        this.leftIcon = leftIcon;
        this.rightIcon = rightIcon;
        this.gap = gap;
    }

    @Override
    public int getIconWidth() {
        if (leftIcon == null && rightIcon == null) {
            return 0;
        }
        if (leftIcon == null) {
            return rightIcon.getIconWidth();
        }
        if (rightIcon == null) {
            return leftIcon.getIconWidth();
        }
        return leftIcon.getIconWidth() + gap + rightIcon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        int leftHeight = leftIcon != null ? leftIcon.getIconHeight() : 0;
        int rightHeight = rightIcon != null ? rightIcon.getIconHeight() : 0;
        return Math.max(leftHeight, rightHeight);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        int totalHeight = getIconHeight();

        if (leftIcon != null) {
            int leftY = y + (totalHeight - leftIcon.getIconHeight()) / 2;
            leftIcon.paintIcon(c, g, x, leftY);
        }

        if (rightIcon != null) {
            int rightX = x;
            if (leftIcon != null) {
                rightX += leftIcon.getIconWidth() + gap;
            }
            int rightY = y + (totalHeight - rightIcon.getIconHeight()) / 2;
            rightIcon.paintIcon(c, g, rightX, rightY);
        }
    }
}

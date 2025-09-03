package de.costache.calendar.ui;

/**
 * Copyright 2013 Theodor Costache
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License. 
 */
import javax.swing.JPanel;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import de.costache.calendar.JCalendar;
import de.costache.calendar.ui.strategy.DisplayStrategy;
import de.costache.calendar.ui.strategy.DisplayStrategy.Type;
import de.costache.calendar.ui.strategy.DisplayStrategyFactory;

/**
 * 
 * @author theodorcostache
 * 
 */
public class ContentPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private DisplayStrategy strategy;

    private final JCalendar owner;

    /**
     * Creates a new instance of {@link ContentPanel}
     */
    public ContentPanel(JCalendar owner) {
        super(true);
        setOpaque(false);
        this.owner = owner;
        this.strategy = DisplayStrategyFactory.getStrategy(this, Type.WEEK);
        this.strategy.display();

        // Add mouse wheel listener to handle scrolling for specified type
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Allow child panels to consume wheel events (e.g., month day cell overflow scrolling)
                if (e.isConsumed()) {
                    return;
                }
                if (strategy.getType() == Type.MONTH) {
                    if (e.getWheelRotation() < 0) {
                        strategy.moveIntervalLeft();
                    } else {
                        strategy.moveIntervalRight();
                    }
                }
                if (strategy.getType() == Type.WEEK) {
                    if (e.getWheelRotation() < 0) {
                        strategy.moveIntervalLeft();
                    } else {
                        strategy.moveIntervalRight();
                    }
                }
                
                if (strategy.getType() == Type.DAY) {
                    if (e.getWheelRotation() < 0) {
                        strategy.moveIntervalLeft();
                    } else {
                        strategy.moveIntervalRight();
                    }
                }
                
                // Update the interval label in the header panel
                owner.getHeaderPanel().getIntervalLabel().setText(strategy.getDisplayInterval());
            }
        });
    }

    public JCalendar getOwner() {
        return owner;
    }

    /**
     * Sets the display strategy and updates the display.
     * 
     * @param strategy the strategy to set
     */
    public void setStrategy(final DisplayStrategy strategy) {
        this.strategy = strategy;
        this.strategy.display();
    }

    /**
     * Gets the current display strategy.
     * 
     * @return the strategy
     */
    public DisplayStrategy getStrategy() {
        return strategy;
    }
}

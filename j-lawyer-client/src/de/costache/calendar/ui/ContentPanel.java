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

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private DisplayStrategy strategy;

	private final JCalendar owner;

	/**
	 * 
	 */
	public ContentPanel(JCalendar owner) {
		super(true);
		setOpaque(false);
		this.owner = owner;
		this.strategy = DisplayStrategyFactory.getStrategy(this, Type.WEEK);
		this.strategy.display();
	}

	public JCalendar getOwner() {
		return owner;
	}

	/**
	 * @param strategy
	 *            the strategy to set
	 */
	public void setStrategy(final DisplayStrategy strategy) {
		this.strategy = strategy;
		this.strategy.display();
	}

	/**
	 * @return the strategy
	 */
	public DisplayStrategy getStrategy() {
		return strategy;
	}
}

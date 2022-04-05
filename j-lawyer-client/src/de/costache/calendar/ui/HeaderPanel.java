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
package de.costache.calendar.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;

import javax.imageio.ImageIO;
import javax.swing.*;


/**
 * 
 * @author theodorcostache
 * 
 */
public class HeaderPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private JButton scrollLeftButton;

	private JButton scrollRightButton;

	private JLabel intervalLabel;

	private JButton dayButton;

	private JButton weekButton;

	private JButton monthButton;
        
        private JButton todayButton;

	/**
	 * Creates a new instance of {@link HeaderPanel}
	 */
	public HeaderPanel() {
		init();
	}

	private void init() {

		this.setOpaque(false);
               
                String strDay = "Tag";
		String strWeek = "Woche";
		String strMonth = "Monat";

		dayButton = new JButton();
                dayButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons16/calendar_day.png")));
		weekButton = new JButton();
                weekButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons16/calendar_week.png")));
		monthButton = new JButton();
                monthButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons16/calendar_month.png")));
                todayButton=new JButton();
                todayButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons16/calendar_today.png")));

		scrollLeftButton = new JButton();
		scrollRightButton = new JButton();

		intervalLabel = new JLabel();

		dayButton.setText(strDay);
		weekButton.setText(strWeek);
		monthButton.setText(strMonth);
                todayButton.setText("Heute");
                

		scrollLeftButton.setBorderPainted(false);
		scrollLeftButton.setFocusPainted(false);
		scrollLeftButton.setContentAreaFilled(false);

		scrollRightButton.setBorderPainted(false);
		scrollRightButton.setFocusPainted(false);
		scrollRightButton.setContentAreaFilled(false);

		Image left = null;
		Image right = null;
		try {
                        
                        left = ImageIO.read(getClass().getClassLoader().getResource("icons/1leftarrow.png"));
			right = ImageIO.read(getClass().getClassLoader().getResource("icons/1rightarrow.png"));
                        
			scrollLeftButton.setIcon(new ImageIcon(left));
			scrollRightButton.setIcon(new ImageIcon(right));
		} catch (final Exception e) {
			scrollLeftButton.setText("<");
			scrollRightButton.setText(">");
		}

		dayButton.setOpaque(false);
		weekButton.setOpaque(false);
		monthButton.setOpaque(false);
                todayButton.setOpaque(false);

		this.setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		this.add(scrollLeftButton, c);
		c.gridx = 1;
		c.gridy = 0;
		this.add(scrollRightButton, c);
		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 1.0;
		c.insets = new Insets(10, 10, 10, 10);
		this.add(intervalLabel, c);
		c.gridx = 3;
		c.gridy = 0;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(10, 10, 10, 0);
		this.add(dayButton, c);
		c.gridx = 4;
		c.gridy = 0;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(10, 0, 10, 0);
		this.add(weekButton, c);
		c.gridx = 5;
		c.gridy = 0;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(10, 0, 10, 10);
		this.add(monthButton, c);
                c.gridx = 6;
		c.gridy = 0;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(10, 0, 10, 10);
                JSeparator sep=new JSeparator();
                sep.setOrientation(SwingConstants.VERTICAL);
		this.add(sep, c);
                c.gridx = 7;
		c.gridy = 0;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(10, 0, 10, 10);
		this.add(todayButton, c);
	}

	/**
	 * @return the scrollLeft
	 */
	public JButton getScrollLeft() {
		return scrollLeftButton;
	}

	/**
	 * @return the scrollRight
	 */
	public JButton getScrollRight() {
		return scrollRightButton;
	}

	/**
	 * @return the dayButton
	 */
	public JButton getDayButton() {
		return dayButton;
	}

	/**
	 * @return the weekButton
	 */
	public JButton getWeekButton() {
		return weekButton;
	}

	/**
	 * @return the monthButton
	 */
	public JButton getMonthButton() {
		return monthButton;
	}
        
        public JButton getTodayButton() {
		return todayButton;
	}

	/**
	 * @return the intervalLabel
	 */
	public JLabel getIntervalLabel() {
		return intervalLabel;
	}

}

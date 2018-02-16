/*******************************************************************************
 * Bizcal is a component library for calendar widgets written in java using swing.
 * Copyright (C) 2007  Frederik Bertilsson 
 * Contributors:       Martin Heinemann martin.heinemann(at)tudor.lu
 * 
 * http://sourceforge.net/projects/bizcal/
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc. 
 * in the United States and other countries.]
 * 
 *******************************************************************************/
package bizcal.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bizcal.swing.util.ErrorHandler;
import bizcal.swing.util.TableLayoutPanel;
import bizcal.swing.util.TableLayoutPanel.Row;
import bizcal.util.BizcalException;
import bizcal.util.DateUtil;
import bizcal.util.LocaleBroker;
import bizcal.util.StreamCopier;
import bizcal.util.TextUtil;

public class MonthStepper
{
	private TableLayoutPanel panel;
	private Calendar cal;
	private JComboBox yearCombo;
	private JComboBox monthCombo;
	private List listeners = new ArrayList();
	private String fastRewindArrow = "/bizcal/res/go_fb.gif";
	private String prevArrow = "/bizcal/res/go_back.gif";
	private String nextArrow = "/bizcal/res/go_forward.gif";
	private String fastForwardArrow = "/bizcal/res/go_ff.gif";
	
	public MonthStepper()
		throws Exception
	{
		cal = Calendar.getInstance(LocaleBroker.getLocale());
		cal.setTime(DateUtil.round2Day(new Date()));
		panel = new TableLayoutPanel();
		panel.createColumn();
		panel.createColumn();
		panel.createColumn(TableLayoutPanel.FILL);
		panel.createColumn();
		panel.createColumn(10);
		panel.createColumn();
		panel.createColumn(TableLayoutPanel.FILL);
		panel.createColumn();
		panel.createColumn();
		Row row = panel.createRow();
		ActionListener listener;
		listener = new ActionListener() {
			public void actionPerformed(ActionEvent event) {	
				try { previousYear(); }
				catch (Exception e) { ErrorHandler.handleError(e); }
			}
		};
		row.createCell(createButton(fastRewindArrow, listener));
		listener = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				try { previous(); }
				catch (Exception e) { ErrorHandler.handleError(e); }
				
			}
		};
		row.createCell(createButton(prevArrow, listener));
		row.createCell();
		initYearCombo();
		initMonthCombo();
		setCombos();
		row.createCell(yearCombo);
		row.createCell();
		row.createCell(monthCombo);
		row.createCell();
				
		listener = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				try { next(); }
				catch (Exception e) { ErrorHandler.handleError(e); }				
			}
		};
		row.createCell(createButton(nextArrow, listener));
		listener = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				try { nextYear(); }
				catch (Exception e) { ErrorHandler.handleError(e); }				
			}
		};
		row.createCell(createButton(fastForwardArrow, listener));
	}
	
	private void initYearCombo()
		throws Exception
	{
		yearCombo = new JComboBox();
		int year = cal.get(Calendar.YEAR);
		for (int i=year-1; i < year+4; i++)
			yearCombo.addItem(Integer.valueOf(i));
		yearCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				try {
					int year = ((Integer) yearCombo.getSelectedItem()).intValue();
					cal.set(Calendar.YEAR, year);
				} catch (Exception e) {
					throw BizcalException.create(e);
				}
			}
		});
	}
	
	private void initMonthCombo()
		throws Exception
	{
		monthCombo = new JComboBox();
		DateFormat format = new SimpleDateFormat("MMMMM", LocaleBroker.getLocale());
		int orgMonth = cal.get(Calendar.MONTH);
		for (int i=0; i < 12; i++) {
			cal.set(Calendar.MONTH, i);
			MonthWrapper wrapper = new MonthWrapper(i, 
					TextUtil.formatCase(format.format(cal.getTime())));
			monthCombo.addItem(wrapper);
		}
		cal.set(Calendar.MONTH, orgMonth);
		monthCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				try {
					int month = ((MonthWrapper) monthCombo.getSelectedItem()).getValue();
					cal.set(Calendar.MONTH, month);
				} catch (Exception e) {
					throw BizcalException.create(e);
				}
			}
		});
	}
		
	private void setCombos()
	{
		int year = cal.get(Calendar.YEAR);
		yearCombo.setSelectedItem(Integer.valueOf(year));
		int month = cal.get(Calendar.MONTH);
		monthCombo.setSelectedIndex(month);
	}
		
	public JComponent getComponent()
	{
		return panel;
	}
	
	public Date getDate()
	{
		return cal.getTime();
	}
	
	private void nextYear()
		throws Exception
	{
		cal.add(Calendar.YEAR, +1);
		setCombos();
		fireStateChanged();
	}

	private void previousYear()
	throws Exception
	{
		cal.add(Calendar.YEAR, -1);
		setCombos();
		fireStateChanged();
	}

	private void next()
	throws Exception
	{
		cal.add(Calendar.MONTH, +1);
		setCombos();
		fireStateChanged();
	}

	private void previous()
	throws Exception
	{
		cal.add(Calendar.MONTH, -1);
		setCombos();
		fireStateChanged();
	}
	
	public void addChangeListener(ChangeListener listener)
	{
		listeners.add(listener);
	}
	
	private void fireStateChanged()
		throws Exception
	{
		ChangeEvent event = new ChangeEvent(this);
		Iterator i = listeners.iterator();
		while (i.hasNext()) {
			ChangeListener l = (ChangeListener) i.next();
			l.stateChanged(event);
		}
	}

	private Icon getIcon(String filename)
		throws Exception
	{
		byte[] bytes = StreamCopier.copyToByteArray(getClass().getResourceAsStream(filename));
        return new ImageIcon(bytes);        
	}
	
	private JComponent createButton(String filename, ActionListener listener)
		throws Exception
	{
		JButton button = new JButton(getIcon(filename));
		button.addActionListener(listener);
		return button;
	}
	
	public void setDate(Date date)
		throws Exception
	{
		cal.setTime(DateUtil.round2Month(date));
		setCombos();
		fireStateChanged();
	}
	
	private class MonthWrapper
	{
		private int value;
		private String caption;
		
		public MonthWrapper(int value, String caption)
		{
			this.value = value;
			this.caption = caption;
		}
		
		public String toString()
		{
			return caption;
		}
		
		public int getValue()
		{
			return value;
		}
	}
}

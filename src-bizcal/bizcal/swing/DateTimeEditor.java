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

import java.text.DateFormat;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;

import bizcal.swing.util.TableLayoutPanel;
import bizcal.swing.util.TableLayoutPanel.Row;
import bizcal.util.DateUtil;
import bizcal.util.LocaleBroker;
import bizcal.util.TimeOfDay;

public class DateTimeEditor 
{
	private TableLayoutPanel panel;
	private JFormattedTextField dateField;
	private JFormattedTextField timeField;
	
	public DateTimeEditor()
		throws Exception
	{
		panel = new TableLayoutPanel();
		panel.createColumn();
		panel.createColumn(10);
		panel.createColumn();
		Row row = panel.createRow();
		
		DateFormat dateFormat = 
			DateFormat.getDateInstance(DateFormat.SHORT, LocaleBroker.getLocale());
		DateFormat timeFormat =
			DateFormat.getTimeInstance(DateFormat.SHORT, LocaleBroker.getLocale());		
		dateField = new JFormattedTextField(dateFormat);
		timeField = new JFormattedTextField(timeFormat);
		row.createCell(dateField);
		row.createCell();
		row.createCell(timeField);
	}
	
	public void setValue(Date date)
	{
		dateField.setValue(date);
		timeField.setValue(date);
	}
	
	public Date getValue()
		throws Exception
	{
		Date date = (Date) dateField.getValue();
		Date time = (Date) timeField.getValue();
		TimeOfDay timeOfDay = DateUtil.getTimeOfDay(time);
		date = timeOfDay.getDate(date);
		return date;
	}
	
	public JComponent getComponent()
	{
		return panel;
	}

}

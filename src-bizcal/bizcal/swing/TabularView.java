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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import bizcal.common.CalendarViewConfig;
import bizcal.common.Event;
import bizcal.util.DateUtil;
import bizcal.util.LocaleBroker;

/**
 * @author Fredrik Bertilsson
 */
public class TabularView extends CalendarView {
//	private JPanel panel;

//	private int dayCount = 14;
	private JTable table;
	private JScrollPane scroll;

	public TabularView(CalendarViewConfig desc) throws Exception {
		super(desc);
		table = new JTable();
		scroll = new JScrollPane(table);
	}
	
	public JComponent getComponent()
	{
		return scroll;
	}


	public long getTimeInterval() throws Exception {
		return 0;
	}
	

	public void refresh0() throws Exception {
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, LocaleBroker.getLocale());
		DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, LocaleBroker.getLocale());
		DefaultTableModel model = new DefaultTableModel();
		model.addColumn(tr("Date"));
		Map eventMap = new HashMap();
		Iterator i = getSelectedCalendars().iterator();
		while (i.hasNext()) {
			bizcal.common.Calendar cal = 
				(bizcal.common.Calendar) i.next();
			model.addColumn(cal.getSummary());
			eventMap.put(cal.getId(), createEventsPerDay(cal.getId()));
		}
		Date date = getInterval().getStartDate();
		while (date.before(getInterval().getEndDate())) {
			Vector row = new Vector();
			row.add(dateFormat.format(date));
			i = getSelectedCalendars().iterator();
			while (i.hasNext()) {
				bizcal.common.Calendar cal = 
					(bizcal.common.Calendar) i.next();
				Map eventsPerDay = (Map) eventMap.get(cal.getId());
				List events = (List) eventsPerDay.get(date);
				StringBuffer str = new StringBuffer();
				if (events != null) {
					Iterator j = events.iterator();
					while (j.hasNext()) {
						Event event = (Event) j.next();
						str.append(timeFormat.format(event.getStart()) + "-");
						str.append(timeFormat.format(event.getEnd()));
						if (j.hasNext())
							str.append(", ");					
					}
				}				
				row.add(str);
			}
			model.addRow(row);
			System.err.println("TabularView: " + row);
			date = DateUtil.getDiffDay(date, +1);
		}
		table.setModel(model);
		model.fireTableDataChanged();
		setColumnWidths();
	}
	
	private void setColumnWidths()
	{
		TableColumnModel model = table.getColumnModel();
		for (int i=0; i < model.getColumnCount(); i++) {
			TableColumn col = model.getColumn(i);
			if (i == 0)
				col.setWidth(50);
			else
				col.setWidth(100);
		}
	}

	public Date getDate(int x, int y) {
		return null;
	}

	private String tr(String str)
	{
		return str;
	}
	
}

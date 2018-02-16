/*******************************************************************************
 * Copyright (c) 2007 by CRP Henri TUDOR - SANTEC LUXEMBOURG 
 * check http://www.santec.tudor.lu for more information
 *  
 * Contributor(s):
 * Johannes Hermen  johannes.hermen(at)tudor.lu                            
 * Martin Heinemann martin.heinemann(at)tudor.lu  
 *  
 * This library is free software; you can redistribute it and/or modify it  
 * under the terms of the GNU Lesser General Public License (version 2.1)
 * as published by the Free Software Foundation.
 * 
 * This software is distributed in the hope that it will be useful, but     
 * WITHOUT ANY WARRANTY; without even the implied warranty of               
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU        
 * Lesser General Public License for more details.                          
 * 
 * You should have received a copy of the GNU Lesser General Public         
 * License along with this library; if not, write to the Free Software      
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 *******************************************************************************/
package lu.tudor.santec.bizcal.views.list;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import lu.tudor.santec.bizcal.NamedCalendar;
import lu.tudor.santec.i18n.Translatrix;
import bizcal.common.Event;
import bizcal.util.DateUtil;

public class ListModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private ArrayList<Event> events = new ArrayList<Event>();
	
	private DateFormat dateTime = new SimpleDateFormat("dd.MM.yy HH:mm", Translatrix.getLocale());
	private DateFormat date = new SimpleDateFormat("EEE dd.MM.yy", Translatrix.getLocale());
	private DateFormat time = new SimpleDateFormat("HH:mm", Translatrix.getLocale());
	/**
	 * static logger for this class
	 */
	private static Logger logger = Logger.getLogger(ListModel.class.getName());
	
	private static String[] columnNames = {
		Translatrix.getTranslationString("bizcal.date"),
		Translatrix.getTranslationString("bizcal.type"),
		Translatrix.getTranslationString("bizcal.calendar"),
		Translatrix.getTranslationString("bizcal.desc")
	};
	
	private static Class[] columnClasses = {
		String.class,
		ImageIcon.class,
		String.class,
		String.class
	};
	
	public ListModel() {
	}

	public int getColumnCount() {
		return columnNames.length;
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columnClasses[columnIndex];
	}

	public int getRowCount() {
		return events.size();
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		
		Event e = events.get(rowIndex);
		if (e == null) 
			return null;
		
		switch (columnIndex) {
		case 0:
			String str = "";
			try {
					if (DateUtil.isSameDay(e.getStart(), e.getEnd())) {
						String d = date.format(e.getStart());
						d = d.substring(0,1).toUpperCase() + d.substring(1);
						str = "<html><b>" + d + "</b><br>" + time.format(e.getStart()) + " - " + time.format(e.getEnd());
					} else {
						str = "<html><b>" + dateTime.format(e.getStart()) + "</b><br>" + dateTime.format(e.getEnd());
					}
					
				} catch (Exception e1) {
					logger.log(Level.WARNING, "listmodel dateformating creation failed", e);
				}
			return str;
		case 1:
			return e.getIcon();
		case 2:
			return e.get(NamedCalendar.CALENDAR_NAME);
		case 3:
			return "<html><b>" 
				+ (e.getSummary()!=null?e.getSummary():"") 
				+ "</b><br>" + (e.getDescription()!=null?e.getDescription():"");
		default:
			break;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public void setEvents(List events) {
		this.events = new ArrayList<Event>(events);
		fireTableDataChanged();
	}

	public Event getEvent(int row) {
		return events.get(row);
	}

}


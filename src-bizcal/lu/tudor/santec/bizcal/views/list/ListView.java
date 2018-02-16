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

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import lu.tudor.santec.bizcal.views.ListViewPanel;
import bizcal.common.CalendarViewConfig;
import bizcal.common.Event;
import bizcal.swing.CalendarView;
import bizcal.util.DateUtil;

public class ListView extends CalendarView implements MouseListener {

	private JPanel panel;
	private JTable table;
	private ListModel listModel;
	private boolean showDeparted = false;
	private int showDays = 7;
	private ListViewPanel parent;
	private Date date;
	private List showEvents;

	public ListView(CalendarViewConfig desc, ListViewPanel parent) throws Exception {
		super(desc);
		
		showEvents = new ArrayList();
		
		this.parent = parent;
		this.panel = new JPanel();
		this.panel.setOpaque(false);
		this.panel.setLayout(new BorderLayout());

		this.listModel = new ListModel();
		this.table = new JTable(this.listModel);

		ListRenderer renderer = new ListRenderer(this.listModel);
		this.table.getColumnModel().getColumn(0).setCellRenderer(renderer);
		this.table.getColumnModel().getColumn(1).setCellRenderer(renderer);
		this.table.getColumnModel().getColumn(2).setCellRenderer(renderer);
		this.table.getColumnModel().getColumn(3).setCellRenderer(renderer);

		this.table.getColumnModel().getColumn(0).setMinWidth(120);
		this.table.getColumnModel().getColumn(0).setMaxWidth(120);
		this.table.getColumnModel().getColumn(1).setMinWidth(30);
		this.table.getColumnModel().getColumn(1).setMaxWidth(30);
		this.table.getColumnModel().getColumn(2).setMinWidth(120);
		this.table.getColumnModel().getColumn(2).setMaxWidth(120);

		this.table.setRowHeight(32);
		this.table.addMouseListener(this);
		this.table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane jsp = new JScrollPane(table);
		jsp.setOpaque(false);
		jsp.getViewport().setOpaque(false);
		panel.add(jsp, BorderLayout.CENTER);

		setDate(new Date());
		
	}

	@Override
	public JComponent getComponent() {
		return panel;
	}

	@Override
	protected Date getDate(int xPos, int yPos) throws Exception {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void refresh0() throws Exception {
		if (broker != null) {
			List<Event> events= broker.getEvents(null);
			if (! showDeparted) {
				Date start = this.date;
				Date end = DateUtil.getDiffDay(start, showDays);
				parent.setTitle(start, end);
				showEvents.clear();
				for (Iterator iter = events.iterator(); iter.hasNext();) {
					Event ev = (Event) iter.next();
					if (ev.getStart().after(start)  && ev.getStart().before(end)) {
						showEvents.add(ev);
					}
				}
			} else {
				showEvents = events;
			}

			this.listModel.setEvents(showEvents);
		}
	}



	/**
	 * @return the showDeparted
	 */
	public boolean isShowDeparted() {
		return showDeparted;
	}

	/**
	 * @param showDeparted the showDeparted to set
	 */
	public void setShowDeparted(boolean showDeparted) {
		this.showDeparted = showDeparted;
		try {
			refresh0();
		} catch (Exception e) {
		}
	}

	/**
	 * @return the showDays
	 */
	public int getShowDays() {
		return showDays;
	}

	/**
	 * @param showDays the showDays to set
	 */
	public void setShowDays(int showDays) {
		this.showDays = showDays;
		try {
			refresh0();
		} catch (Exception e) {
		}
	}

	public void setDate(Date date) {
		Calendar c = new GregorianCalendar();
		c.setTime(date);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND,0 );
		this.date  = c.getTime();
		try {
			refresh0();
		} catch (Exception e) {
		}
	}






	public void mouseClicked(MouseEvent e) {
		/* ====================================================== */
		if (e.getClickCount() >= 2) {
			listener.eventDoubleClick(null, this.listModel.getEvent(this.table.getSelectedRow()), e);
		}
		/* ====================================================== */
	}

	public void mouseEntered(MouseEvent e) {
		/* ====================================================== */
		// TODO Auto-generated method stub
		/* ====================================================== */
	}

	public void mouseExited(MouseEvent e) {
		/* ====================================================== */
		// TODO Auto-generated method stub
		/* ====================================================== */
	}

	public void mousePressed(MouseEvent e) {
		/* ====================================================== */
		// TODO Auto-generated method stub
		/* ====================================================== */
	}

	public void mouseReleased(MouseEvent e) {
		/* ====================================================== */
		// TODO Auto-generated method stub
		/* ====================================================== */
	}

	public List getEvents() {
		return showEvents;
	}

}

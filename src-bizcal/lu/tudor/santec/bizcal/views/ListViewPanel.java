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
package lu.tudor.santec.bizcal.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JToggleButton;

import lu.tudor.santec.bizcal.AbstractCalendarView;
import lu.tudor.santec.bizcal.CalendarIcons;
import lu.tudor.santec.bizcal.EventModel;
import lu.tudor.santec.bizcal.NamedCalendar;
import lu.tudor.santec.bizcal.print.PrintUtilities;
import lu.tudor.santec.bizcal.views.list.ListView;
import lu.tudor.santec.i18n.Translatrix;
import bizcal.common.DayViewConfig;
import bizcal.swing.CalendarListener;
import bizcal.swing.CalendarView;
import bizcal.swing.util.GradientArea;

public class ListViewPanel extends AbstractCalendarView{

	private DateFormat df = new SimpleDateFormat("dd.MM.yyyy", Translatrix.getLocale());
	private static final long serialVersionUID = 1L;
	private JToggleButton button;
	private EventModel eventModel;
	public static final String VIEW_NAME = "LIST_VIEW";
	public ListView listView;
	Color primaryColor = new Color(230, 230, 230);
	Color secondaryColor = Color.WHITE;
	private GradientArea gp;
	
	/**
	 * static logger for this class
	 */
	private static Logger logger = Logger.getLogger(ListViewPanel.class
			.getName());

	public ListViewPanel(EventModel model) {

		this.eventModel =model;

		this.button = new JToggleButton(
				CalendarIcons.getMediumIcon(CalendarIcons.LISTVIEW));
		this.button.setToolTipText(Translatrix.getTranslationString("bizcal.LIST_VIEW"));

		this.setLayout(new BorderLayout());

		gp = new GradientArea(GradientArea.TOP_BOTTOM, secondaryColor, primaryColor);
		gp.setPreferredSize(new Dimension(30,30));

		this.add(gp, BorderLayout.NORTH);

		try {
			listView = new ListView(new DayViewConfig(), this);
			listView.setModel(eventModel);
			eventModel.addCalendarView(listView);

			listView.refresh();
			listView.refresh0();
			this.add(listView.getComponent(), BorderLayout.CENTER);

		} catch (Exception e) {
			logger.log(Level.WARNING, "listView creation failed", e);
		}

	}

	public JToggleButton getButton() {
		return this.button;
	}

	public String getViewName() {
		return VIEW_NAME;
	}

	public void dateChanged(Date date) {
		eventModel.setDate(date);
		listView.setDate(date);
		try {
			eventModel.refresh();
		} catch (Exception e) {
			logger.log(Level.WARNING, "updating listView failed", e);
		}
	}

	public void setTitle(Date start, Date end) {
		try {
			gp.setText("<html><center>" + Translatrix.getTranslationString("bizcal.LIST_VIEW") + "<br>"
					+ df.format(start) + " - "
					+ df.format(end), true);
		} catch (Exception e) {
			logger.log(Level.WARNING, "setTitle failed", e);
		}
	}

	public void activeCalendarsChanged(Collection<NamedCalendar> calendars) {
		/* ====================================================== */
		/* ====================================================== */
	}

	public void selectedCalendarChanged(NamedCalendar selectedCalendar) {
		/* ====================================================== */
		/* ====================================================== */
	}

	public void addCalendarListener(CalendarListener listener) {
		/* ================================================== */
		listView.addListener(listener);
		/* ================================================== */
	}

	public void setShowDays(int showDays) {
		listView.setShowDays(showDays);
	}

	@Override
	public List getEvents() {
		try {
			return listView.getEvents();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void print(boolean showPrinterDialog) {
		PrintUtilities.printComponent(this, showPrinterDialog, false);
	}

	@Override
	public CalendarView getView() {
		/* ====================================================== */
		return this.listView;
		/* ====================================================== */
	}

}

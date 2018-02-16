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
package lu.tudor.santec.bizcal;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lu.tudor.santec.bizcal.listeners.CalendarManagementListener;
import lu.tudor.santec.bizcal.listeners.DateListener;
import lu.tudor.santec.bizcal.listeners.NamedCalendarListener;
import lu.tudor.santec.bizcal.views.DayViewPanel;
import lu.tudor.santec.bizcal.views.ListViewPanel;
import lu.tudor.santec.bizcal.views.MonthViewPanel;
import lu.tudor.santec.bizcal.widgets.BubbleLabel;
import lu.tudor.santec.bizcal.widgets.ButtonPanel;
import lu.tudor.santec.bizcal.widgets.CheckBoxPanel;
import lu.tudor.santec.bizcal.widgets.NaviBar;
import lu.tudor.santec.i18n.Translatrix;
import bizcal.common.Event;
import bizcal.swing.DayView;
import bizcal.swing.PopupMenuCallback;
import bizcal.util.DateUtil;

import com.toedter.calendar.JCalendar;

public class CalendarPanel extends JPanel implements MouseListener {

	private static final long serialVersionUID = 1L;

	Vector<Action> functionsActionsVector = new Vector<Action>();

	LinkedHashMap<String, AbstractCalendarView> calendarViews = new LinkedHashMap<String, AbstractCalendarView>();

	private NaviBar naviBar;
	private JCalendar dayChooser;

	private ButtonPanel viewsButtonPanel;

	private ButtonPanel calendarButtonPanel;

	private JPanel viewsPanel;

	private CardLayout viewsCardLayout;

	private transient ActionListener viewListener;

	private Vector<DateListener> dateListeners = new Vector<DateListener>();

	private Vector<NamedCalendarListener> calendarListeners = new Vector<NamedCalendarListener>();

	private List<CalendarManagementListener> calendarManagementListeners = new ArrayList<CalendarManagementListener>();

	private Date date = new Date();

	private LinkedHashMap<NamedCalendar, CheckBoxPanel> namedCalendars = new LinkedHashMap<NamedCalendar, CheckBoxPanel>();

	private JPopupMenu popup;

	private AbstractAction modifyCalendarAction;

	private AbstractAction newCalendarAction;

	private AbstractAction deleteCalendarAction;

	private ButtonPanel functionsButtonPanel;

	protected AbstractCalendarView currentView;

	private JSlider slider;

	private NamedCalendar lastShowingCalendarBeforeShowAll = null;

	private static Color headerColor = new Color(153,204,255);
	
	private ButtonGroup calendarButtonGroup = new ButtonGroup();
	

	/**
	 *
	 */
	public CalendarPanel() {

		// load translations
//		Translatrix.loadSupportedLocales("lu.tudor.santec.bizcal.resources.supportedLocales");
//		Translatrix.setLocale(Locale.US);
//		Locale.setDefault(Translatrix.getLocale());
		Translatrix.addBundle("lu.tudor.santec.bizcal.resources.Translatrix");

		this.setLayout(new BorderLayout(3, 3));
		this.setBackground(Color.WHITE);

		createMainPanel();
		this.add(this.viewsPanel, BorderLayout.CENTER);

		createNaviBar();
		this.add(this.naviBar, BorderLayout.EAST);

		initPopup();

	}

	/**
	 * creates the main panel with the differen view
	 */
	private void createMainPanel() {
		this.viewsCardLayout = new CardLayout();
		this.viewsPanel = new JPanel(viewsCardLayout);
		this.viewsPanel.setOpaque(false);
	}

	/**
	 * creates the right toolbar
	 */
	private void createNaviBar() {
		this.naviBar = new NaviBar(110);
		/* ================================================== */
		
		// create the view buttons
		this.viewsButtonPanel = new ButtonPanel(Translatrix
				.getTranslationString("bizcal.views"), headerColor, 4,
				new Vector<AbstractButton>());
		this.naviBar.addButtonPanel(this.viewsButtonPanel, NaviBar.TOP);
		/* ------------------------------------------------------- */
		this.viewListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewsCardLayout.show(viewsPanel, e.getActionCommand());
				currentView = calendarViews.get(e.getActionCommand());
			}
		};

		// create the functions buttons
		functionsButtonPanel = new ButtonPanel(Translatrix
				.getTranslationString("bizcal.functions"), headerColor, 1,
				functionsActionsVector, false, true);
		functionsButtonPanel.setContentLayout(new BorderLayout());
		this.naviBar.addButtonPanel(functionsButtonPanel, NaviBar.FILL);

		// create the calendar buttons
		this.calendarButtonPanel = new ButtonPanel(Translatrix
				.getTranslationString("bizcal.calendar"), headerColor, 1,
				new Vector<AbstractButton>());

		this.naviBar.addButtonPanel(calendarButtonPanel, NaviBar.BOTTOM);

		// this.calendarButtonPanel.addComponent(new CheckBoxPanel("text",
		// Color.RED));

		// create the datechooser
		JPanel daychooserPanel = new JPanel();
		daychooserPanel.setLayout(new BorderLayout(0, 2));
		daychooserPanel.setOpaque(false);
		daychooserPanel.setBackground(Color.WHITE);
		JLabel label = new BubbleLabel(" "
				+ Translatrix.getTranslationString("bizcal.chooseDay") + ":");
//		Color color = headerColor;
//		color = new Color(color.getRed(), color.getGreen(), color.getBlue(),
//				200);
		label.setBackground(Color.YELLOW);
		label.setPreferredSize(new Dimension(22, 22));
		daychooserPanel.add(label, BorderLayout.NORTH);

		this.dayChooser = new JCalendar(date);
//		this.dayChooser.setOpaque(false);
//		this.dayChooser.getDayChooser().setOpaque(false);

		for (int i = 0; i < this.dayChooser.getDayChooser().getComponents().length; i++) {
			this.dayChooser.getDayChooser().getComponent(i).setBackground(
					Color.WHITE);
		}
		// this.dayChooser.getDayChooser().setDecorationBackgroundColor(new
		// Color(243,243,243));
		this.dayChooser.getDayChooser().setDayBordersVisible(false);
		this.dayChooser.getDayChooser().setDecorationBackgroundVisible(false);
		this.dayChooser.getDayChooser().setDecorationBordersVisible(false);

//		this.dayChooser.getDayChooser().getDayPanel().setOpaque(false);




		// manage date changes on daychooser and fire dateChanged
		this.dayChooser.addPropertyChangeListener(new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent evt) {
				/* ================================================== */
				if ("calendar".equals(evt.getPropertyName())
						|| "date".equals(evt.getPropertyName())) {
					date = dayChooser.getDate();
					for (Iterator iter = dateListeners.iterator(); iter
							.hasNext();) {
						DateListener listener = (DateListener) iter.next();
						listener.dateChanged(date);
					}
				}
				/* ================================================== */
			}
		});

		daychooserPanel.add(this.dayChooser, BorderLayout.CENTER);

		JButton todayButton = new JButton(Translatrix
				.getTranslationString("bizcal.gotoToday"), CalendarIcons
				.getMediumIcon(CalendarIcons.TODAY));
		todayButton.setHorizontalAlignment(JLabel.LEFT);
		todayButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setDate(new Date());
			}
		});
		daychooserPanel.add(todayButton, BorderLayout.SOUTH);

		this.naviBar.addButtonPanel(daychooserPanel, NaviBar.BOTTOM);

		/* ------------------------------------------------------- */
		// slider for zoom
		this.slider = new JSlider();
		try {
			/* --------------------------------------------- */
			this.slider.setMinimum(30);
			this.slider.setMaximum(500);
			this.slider.setValue(DayView.PIXELS_PER_HOUR);
			/* --------------------------------------------- */
		} catch (Exception e) {
			/* --------------------------------------------- */
			e.printStackTrace();
			/* --------------------------------------------- */
		}
		
		slider.setOpaque(false);

		slider.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				/* ====================================================== */
				int pos = (((JSlider) (e.getSource())).getValue());
				for (AbstractCalendarView acv : calendarViews.values()) {
					/* ------------------------------------------------------- */
					if (acv instanceof DayViewPanel) {
						DayViewPanel dvp = (DayViewPanel) acv;
						dvp.setZoomFactor(pos);
					}
					/* ------------------------------------------------------- */
				}
				/* ====================================================== */
			}
		});

		slider.addMouseWheelListener(new MouseWheelListener() {

			public void mouseWheelMoved(MouseWheelEvent e) {
				/* ====================================================== */
				slider.setValue(slider.getValue() + e.getWheelRotation() * 6);
				/* ====================================================== */
			}
		});

		this.naviBar.addButtonPanel(slider, NaviBar.BOTTOM);
	}

	/**
	 * show the specified view
	 *
	 * @param panelName
	 */
	public void showView(String panelName) {
		if (panelName == null)
			return;
		for (Iterator iter = calendarViews.values().iterator(); iter.hasNext();) {
			AbstractCalendarView panel = (AbstractCalendarView) iter.next();
			if (panelName.equals(panel.getButton().getActionCommand())) {
				panel.getButton().doClick();
				return;
			}
		}
	}

	/**
	 * adds a new CalendarView to the CalendarPanel
	 *
	 * @param calendarView
	 *            a CalendarView Object
	 */
	public void addCalendarView(AbstractCalendarView calendarView) {
		// add the panel
		this.viewsPanel.add(calendarView, calendarView.getViewName());
		// add the button for the panel
		viewsButtonPanel.addToggleButton(calendarView.getButton());
		calendarView.getButton().setActionCommand(calendarView.getViewName());
		calendarView.getButton().addActionListener(viewListener);
		// add a datelistener to the panel
		addDateListener(calendarView);
		addNamedCalendarListener(calendarView);
		this.calendarViews.put(calendarView.getViewName(), calendarView);

//		calendarView.getView().setPopupMenuCallback(popupCallBack);

		// select the first Panel
		if (this.calendarViews.size() == 1) {
			calendarView.getButton().doClick();
		}
	}

	/**
	 * sets the date of the CalendarPanel
	 *
	 * @param date
	 *            the new Date
	 */
	public void setDate(Date date) {
		this.dayChooser.setDate(date);
	}

	/**
	 * gets the date of the CalendarPanel
	 *
	 * @return the current Date
	 */
	public Date getDate() {
		return this.date;
	}

	/**
	 * adds a DateListener to the Panel
	 *
	 * @param dateListener
	 *            the DateListener
	 */
	public void addDateListener(DateListener dateListener) {
		this.dateListeners.add(dateListener);
	}

	/**
	 * removes a DateListener to the Panel
	 *
	 * @param dateListener
	 *            the DateListener
	 */
	public void removeDateListener(DateListener dateListener) {
		this.dateListeners.remove(dateListener);
	}

	/**
	 * adds a CalendarListener to the Panel
	 *
	 * @param calendarListener
	 *            the CalendarListener
	 */
	public void addNamedCalendarListener(NamedCalendarListener calendarListener) {
		this.calendarListeners.add(calendarListener);
	}

	/**
	 * removes a CalendarListener to the Panel
	 *
	 * @param calendarListener
	 *            the CalendarListener
	 */
	public void removeNamedCalendarListener(
			NamedCalendarListener calendarListener) {
		this.calendarListeners.remove(calendarListener);
	}

	/**
	 * Add a CalendarManagementListener
	 *
	 * @param listener
	 */
	public void addCalendarManagementListener(
			CalendarManagementListener listener) {
		/* ================================================== */
		this.calendarManagementListeners.add(listener);
		/* ================================================== */
	}

	/**
	 * Remove a CalendarManagementListener
	 *
	 * @param listener
	 */
	public void removeCalendarManagementListener(
			CalendarManagementListener listener) {
		/* ================================================== */
		this.calendarManagementListeners.remove(listener);
		/* ================================================== */
	}

	/**
	 * Remove a NamedCalendar.
	 *
	 * @param namedCalendar
	 */
	public void removeNamedCalendar(NamedCalendar namedCalendar) {
		/* ================================================== */
		if (namedCalendar == null)
			return;
		/* ------------------------------------------------------- */
		// remove from hash
		this.namedCalendars.remove(namedCalendar);
		/* ------------------------------------------------------- */
		// remove button
		try {
			this.calendarButtonPanel.removeComponent(namedCalendar
					.getCheckBox());
			this.calendarButtonPanel.validate();
			this.calendarButtonPanel.updateUI();
		} catch (Exception e) {
			e.printStackTrace();
		}
		/* ================================================== */
	}

	/**
	 * Add a calendar.
	 *
	 * @param namedCalendar
	 */
	public void addNamedCalendar(final NamedCalendar namedCalendar) {
		/* ================================================== */
		if (!namedCalendars.containsKey(namedCalendar)) {

			final CheckBoxPanel calendarToggler = new CheckBoxPanel(
					namedCalendar.getName(), namedCalendar.getColor(), calendarButtonGroup);
			
			calendarToggler.setToolTipText(namedCalendar.getDescription());
			calendarToggler.setActiv(namedCalendar.isActive());
			namedCalendar.setCheckBox(calendarToggler);
			/* ------------------------------------------------------- */
			// mouselistener for contextmenu
			calendarToggler.addMouseListener(this);
			/* ------------------------------------------------------- */
			// actionlistener for the calender itself
			calendarToggler.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					/* ------------------------------------------------------- */
					// notify calendar listeners if a calendar has been
					// de/activated
					if (namedCalendar.isActive() != calendarToggler.isActiv()) {
						/* ------------------------------------------------------- */
						// save the new state of the calendar
						namedCalendar.setActive(calendarToggler.isActiv());
						/* ------------------------------------------------------- */
						// inform the listeners
						for (Iterator iter = calendarListeners.iterator(); iter.hasNext();) {
							NamedCalendarListener listener = (NamedCalendarListener) iter.next();
							listener.activeCalendarsChanged(namedCalendars.keySet());
						}
						/* ------------------------------------------------------- */
					} else {
						/* ------------------------------------------------------- */
						// =========================================================
						// set the namedcalendar to the selected state of its
						// calendar toggle button.
						// =========================================================
						namedCalendar.setSelected(calendarToggler.isSelected());
						// =========================================================
						// the namedcalendar that is in charge of this
						// togglebutton
						// is set as the current calendar. There can be only one
						// selected calendar to work on!
						// Unselect the rest.
						// =========================================================
						if (calendarToggler.isSelected()) {
							/* ------------------------------------------------------- */
							// deselect all calendars, except the current
							// selected
							for (NamedCalendar cal : namedCalendars.keySet()) {
								if (!namedCalendars.get(cal).equals(
										calendarToggler)) {
//									namedCalendars.get(cal).setSelected(false);
									cal.setSelected(false);
								}
							}
							/* ------------------------------------------------------- */
						}
						// =========================================================
						// send the current selected calendar to the listeners
						// =========================================================
						for (Iterator iter = calendarListeners.iterator(); iter
								.hasNext();) {
							NamedCalendarListener listener = (NamedCalendarListener) iter
									.next();
							listener
									.selectedCalendarChanged(getSelectedCalendar());
						}
						/* ------------------------------------------------------- */
					}
				}
			});
			// end of actionlistener
			/* ------------------------------------------------------- */
			this.calendarButtonPanel.addComponent(calendarToggler);
			this.namedCalendars.put(namedCalendar, calendarToggler);
		}
		// =========================================================
		// inform all listeners of the new calendar
		// =========================================================
		// for (Iterator iter = calendarListeners.iterator(); iter.hasNext();) {
		// /* ------------------------------------------------------- */
		// NamedCalendarListener listener = (NamedCalendarListener) iter.next();
		// listener.activeCalendarsChanged(namedCalendars.keySet());
		// /* ------------------------------------------------------- */
		// }
		/* ================================================== */
	}

	/**
	 * Triggers update of calendars and calendar buttons
	 */
	public void triggerUpdate() {
		/* ================================================== */
		for (Iterator iter = calendarListeners.iterator(); iter.hasNext();) {
			/* ------------------------------------------------------- */
			NamedCalendarListener listener = (NamedCalendarListener) iter
					.next();
			listener.activeCalendarsChanged(namedCalendars.keySet());
			/* ------------------------------------------------------- */
		}

		calendarButtonPanel.validate();
		calendarButtonPanel.updateUI();
		/* ================================================== */
	}

	/**
	 * step to next date according of the current view
	 */
	public void next() {
		/* ================================================== */
		stepDate(true);
		/* ================================================== */
	}

	/**
	 * step to previous date according of the current view
	 */
	public void pevious() {
		/* ================================================== */
		stepDate(false);
		/* ================================================== */
	}

	/**
	 * @param forward
	 */
	private void stepDate(boolean forward) {
		/* ================================================== */
		try {
			String currentName = this.getCurrentView().getViewName();

			int step = 0;

			if (MonthViewPanel.VIEW_NAME.equals(currentName)) {
				/* ------------------------------------------------------- */
				step = 31;
				/* ------------------------------------------------------- */
		} else
				if (DayViewPanel.VIEW_NAME_DAY.equals(currentName)) {
					step = 1;
				} else
					if (DayViewPanel.VIEW_NAME_WEEK.equals(currentName)) {
						step = 7;
					} else if (ListViewPanel.VIEW_NAME.equals(currentName)) {
						try {
							ListViewPanel listView = (ListViewPanel) calendarViews.get(ListViewPanel.VIEW_NAME);
							step = listView.listView.getShowDays();
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
			if (!forward)
				step = step * (-1);
			/* ------------------------------------------------------- */
			this.dayChooser.setDate(DateUtil.getDiffDay(dayChooser.getDate(),
					step));
			this.date = dayChooser.getDate();

			for (Iterator iter = dateListeners.iterator(); iter.hasNext();) {
				DateListener listener = (DateListener) iter.next();
				listener.dateChanged(date);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// FICKEN
		}
		/* ================================================== */
	}

	/**
	 * Returns the current selected calendar.
	 *
	 * @return
	 */
	public NamedCalendar getSelectedCalendar() {
		/* ================================================== */
		if (namedCalendars.keySet() != null) {
			for (NamedCalendar nc : namedCalendars.keySet()) {
				/* ------------------------------------------------------- */
				if (nc.isSelected())
					return nc;
				/* ------------------------------------------------------- */
			}
		}
		/* ------------------------------------------------------- */
		// if we are here, no calendar is selected
		// if there are active calendars, we will select the first
		if (getActiveCalendars() != null && getActiveCalendars().size() > 0) {
			NamedCalendar nc = (NamedCalendar) getActiveCalendars().toArray()[0];
			nc.setSelected(true);
			// triggerUpdate();

			// inform listeners
			informListeners();

			// setSelectedCalendar(nc);
			return nc;
		} 
		/* ------------------------------------------------------- */
		// if there are no selected and no active calendars,
		// we will select and activate the first in the list
		if (getActiveCalendars() == null || getActiveCalendars().size() < 1) {
			/* ------------------------------------------------------- */
			List<NamedCalendar> allCals = getCalendars();
			if (allCals != null && allCals.size() > 0) {
				/* ------------------------------------------------------- */
				NamedCalendar nc = allCals.get(0);
				nc.setActive(true);
				nc.getCheckBox().setSelected(true);
				nc.setSelected(true);

				// triggerUpdate();
				// inform listeners
				informListeners();

				// setSelectedCalendar(nc);
				return nc;
				/* ------------------------------------------------------- */
			}
			/* ------------------------------------------------------- */
		}
		/* ------------------------------------------------------- */

		return null;
		/* ================================================== */
	}

	/**
	 * Sets the selected calendar
	 *
	 * @param cal
	 */
	public void setSelectedCalendar(NamedCalendar cal) {
		/* ================================================== */
		// if the to selected calendar is not active, do so
		//
		// ... hmm, some kind of weird....
		/* ------------------------------------------------------- */
		if (!cal.isActive()) {
			cal.setActive(true);
		}
		/* ------------------------------------------------------- */
		// try to set selected
		/* ------------------------------------------------------- */
		if (!cal.isSelected()) {
			/* ------------------------------------------------------- */
			// select the new one
			/* ------------------------------------------------------- */
			cal.setSelected(true);
			cal.getCheckBox().setSelected(true);
			if (this.lastShowingCalendarBeforeShowAll != null)
				this.lastShowingCalendarBeforeShowAll = null;
			/* ------------------------------------------------------- */
		}
		/* ================================================== */
	}
	
	
	/**
	 * Selects the next calendar in the list
	 */
	public void selectNextCalendar() {
		/* ================================================== */
		NamedCalendar nc = this.getSelectedCalendar();
		/* ------------------------------------------------------- */
		if (nc == null)
			return;
		/* ------------------------------------------------------- */
		ArrayList<NamedCalendar> list = new ArrayList<NamedCalendar>(namedCalendars.keySet());
		/* ------------------------------------------------------- */
		int size = list.size();
		int pos = list.indexOf(nc);
		pos++;
		/* ------------------------------------------------------- */
		// if this is the last element, take the first
		if (pos > size-1)
			pos = 0;
		/* ------------------------------------------------------- */
		NamedCalendar currCal = list.get(pos);
		currCal.setActive(true);
		namedCalendars.get(currCal).setActiv(true);
		
		this.setSelectedCalendar(list.get(pos));
		deactivateOthers(currCal);
		
		informListeners();
		/* ================================================== */
	}
	
	
	/**
	 * Selects the next calendar in the list
	 */
	public void selectPreviousCalendar() {
		/* ================================================== */
		NamedCalendar nc = this.getSelectedCalendar();
		/* ------------------------------------------------------- */
		if (nc == null)
			return;
		/* ------------------------------------------------------- */
		ArrayList<NamedCalendar> list = new ArrayList<NamedCalendar>(namedCalendars.keySet());
		/* ------------------------------------------------------- */
		int size = list.size();
		int pos = list.indexOf(nc);
		pos--;
		/* ------------------------------------------------------- */
		// if this is the last element, take the first
		if (pos < 0)
			pos = size-1;
		/* ------------------------------------------------------- */
		NamedCalendar currCal = list.get(pos);
		currCal.setActive(true);
		// activate the button
		namedCalendars.get(currCal).setActiv(true);
		
		this.setSelectedCalendar(list.get(pos));
		
		deactivateOthers(currCal);
		informListeners();
		/* ================================================== */
	}
	
	
	/**
	 * Deactivates all but the given calendar
	 * 
	 * @param activeCal
	 */
	private void deactivateOthers(NamedCalendar activeCal) {
		/* ================================================== */
		ArrayList<NamedCalendar> list = new ArrayList<NamedCalendar>(namedCalendars.keySet());
		
		for (NamedCalendar nc : list) {
			/* ------------------------------------------------------- */
			if (!activeCal.equals(nc)) {
				nc.setActive(false);
				// activate the button
				namedCalendars.get(nc).setActiv(false);
			}
			/* ------------------------------------------------------- */
		}
		this.lastShowingCalendarBeforeShowAll = null;
		/* ================================================== */
	}
	
	
	/**
	 * Shows all calendars or hides all but the last selected
	 */
	public void toggleShowAllCalendars() {
		/* ================================================== */
		// =====================================================================
		// show all calendars 
		// 
		// =====================================================================
		if (lastShowingCalendarBeforeShowAll == null) {
			/* ------------------------------------------------------- */
			lastShowingCalendarBeforeShowAll = getSelectedCalendar();
			/* ------------------------------------------------------- */
			ArrayList<NamedCalendar> list = new ArrayList<NamedCalendar>(namedCalendars.keySet());
			
			for (NamedCalendar nc : list) {
				/* ------------------------------------------------------- */
					nc.setActive(true);
					// activate the button
					namedCalendars.get(nc).setActiv(true);
				}
				/* ------------------------------------------------------- */
			/* ------------------------------------------------------- */
		} else {  
			// ===================================================================
			// hide all but the last selected
			// 
			// ===================================================================
			NamedCalendar cal = lastShowingCalendarBeforeShowAll;
			/* ------------------------------------------------------- */
			cal.setSelected(true);
			cal.setActive(true);
			namedCalendars.get(cal).setActiv(true);
			
			ArrayList<NamedCalendar> list = new ArrayList<NamedCalendar>(namedCalendars.keySet());
			
			for (NamedCalendar nc : list) {
				/* ------------------------------------------------------- */
				if (!cal.equals(nc)) {
					nc.setActive(false);
					// activate the button
					namedCalendars.get(nc).setActiv(false);
				}
				/* ------------------------------------------------------- */
			}
			lastShowingCalendarBeforeShowAll = null;
			/* ------------------------------------------------------- */
		}
		
		informListeners();
		/* ================================================== */
	}
	
	/**
	 * Returns all active calendars
	 *
	 * @return
	 */
	public Collection<NamedCalendar> getActiveCalendars() {
		/* ================================================== */
		Collection<NamedCalendar> activeCalendars = new ArrayList<NamedCalendar>(
				0);
		/* ------------------------------------------------------- */
		if (namedCalendars != null) {
			for (NamedCalendar nc : namedCalendars.keySet()) {
				/* ------------------------------------------------------- */
				if (nc.isActive())
					activeCalendars.add(nc);
				/* ------------------------------------------------------- */
			}
		}
		return activeCalendars;
		/* ================================================== */
	}

	
	/**
	 * inform active calendar listeners
	 */
	private void informListeners() {
		/* ================================================== */
//		System.out.println("called update");
		for (Iterator iter = calendarListeners.iterator(); iter.hasNext();) {
			NamedCalendarListener listener = (NamedCalendarListener) iter
					.next();
			listener.activeCalendarsChanged(namedCalendars.keySet());
		}
		/* ================================================== */
	}
	
	
	/**
	 * Returns all calendars, selected and unselected, acitve and inactive
	 *
	 * @return
	 */
	public List<NamedCalendar> getCalendars() {
		/* ================================================== */
		return new ArrayList<NamedCalendar>(namedCalendars.keySet());
		/* ================================================== */
	}

	/**
	 * Init the popup menu
	 */
	private void initPopup() {
		/* ================================================== */
		this.popup = new JPopupMenu();
		/* ------------------------------------------------------- */
		// ===============================================
		// N E W Action
		// ===============================================
		this.newCalendarAction = new AbstractAction(Translatrix
				.getTranslationString("calendar.core.new"), CalendarIcons
				.getMediumIcon(CalendarIcons.NEW)) {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				/* ====================================================== */
				// inform the listeners
				for (CalendarManagementListener lis : calendarManagementListeners)
					lis.newCalendar();
				/* ====================================================== */
			}
		};

		// ===============================================
		// M O D I F Y Action
		// ===============================================
		this.modifyCalendarAction = new AbstractAction(Translatrix
				.getTranslationString("calendar.core.modify"), CalendarIcons
				.getMediumIcon(CalendarIcons.EDIT)) {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				/* ====================================================== */
				// find the named calendar for the clicked toggleButton
				// the clicked calendar should be the currentSelectedCalendar
				// =======================================================
				// inform the listeners
				for (CalendarManagementListener lis : calendarManagementListeners)
					lis.modifyCalendar(getSelectedCalendar());
				/* ====================================================== */
			}
		};
		// ===============================================
		// D E L E T E Action
		// ===============================================
		this.deleteCalendarAction = new AbstractAction(Translatrix
				.getTranslationString("calendar.core.remove"), CalendarIcons
				.getMediumIcon(CalendarIcons.DELETE)) {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				/* ====================================================== */
				// find the named calendar for the clicked toggleButton
				// the clicked calendar should be the currentSelectedCalendar
				// =======================================================
				// inform the listeners
				for (CalendarManagementListener lis : calendarManagementListeners)
					lis.deleteCalendar(getSelectedCalendar());
				/* ====================================================== */
			}
		};
		/* ------------------------------------------------------- */
		// ==================================================
		// assemble the popup menu
		// ==================================================
		this.popup.add(newCalendarAction);
		/* ------------------------------------------------------- */
		this.popup.add(new JSeparator());
		/* ------------------------------------------------------- */
		this.popup.add(modifyCalendarAction);
		/* ------------------------------------------------------- */
		this.popup.add(new JSeparator());
		/* ------------------------------------------------------- */
		this.popup.add(deleteCalendarAction);
		/* ================================================== */
	}

	// /**
	// * Tries to find the owner of the toggler button
	// *
	// * @param toggler
	// * @return
	// */
	// private NamedCalendar findNamedCalendarByButton(CheckBoxPanel toggler) {
	// /* ================================================== */
	// if (namedCalendars.keySet() != null) {
	// for(NamedCalendar nc : namedCalendars.keySet()) {
	// if (nc.getCheckBox().equals(toggler))
	// return nc;
	// }
	// }
	// return null;
	// /* ================================================== */
	// }

	// ============================================================
	// MouseListener
	// ============================================================

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		/* ====================================================== */
		showContextMenu(e);
		/* ====================================================== */
	}

	public void mouseReleased(MouseEvent e) {
		/* ====================================================== */
		showContextMenu(e);
		/* ====================================================== */
	}

	/**
	 * Show the pop menu
	 *
	 * @param e
	 */
	private void showContextMenu(MouseEvent e) {
		/* ================================================== */
		if (e.isPopupTrigger()) {
			/* ------------------------------------------------------- */
			try {
				((CheckBoxPanel)((JToggleButton)e.getSource()).getParent()).setSelected(true);
			} catch (Exception ee) {}
			/* ------------------------------------------------------- */
			this.popup.show((Component) e.getSource(), e.getX(), e.getY());
			/* ------------------------------------------------------- */
		}
		/* ================================================== */
	}

	/**
	 * @return the functionsButtonPanel
	 */
	public ButtonPanel getFunctionsButtonPanel() {
		return functionsButtonPanel;
	}

	/**
	 * @return the currentView
	 */
	public AbstractCalendarView getCurrentView() {
		return currentView;
	}

	// public void removeNamedCalendar(NamedCalendar namedCalendar) {
	// this.calendarButtonPanel.remove(this.namedCalendars .get(namedCalendar));
	// this.namedCalendars .remove(namedCalendar);
	// for (Iterator iter = calendarListeners.iterator(); iter.hasNext();) {
	// NamedCalendarListener listener = (NamedCalendarListener) iter.next();
	// listener.calendarsChanged(namedCalendars.keySet());
	// }
	// }



	class PopupCallBack implements PopupMenuCallback {

		public JPopupMenu getCalendarPopupMenu(Object calId) throws Exception {
			/* ====================================================== */
			// TODO Auto-generated method stub
			return null;
			/* ====================================================== */
		}

		public JPopupMenu getEmptyPopupMenu(Object calId, Date date) throws Exception {
			/* ====================================================== */
			// TODO Auto-generated method stub
			return null;
			/* ====================================================== */
		}

		public JPopupMenu getEventPopupMenu(Object calId, Event event) throws Exception {
			/* ====================================================== */
			return popup;
			/* ====================================================== */
		}

		public JPopupMenu getProjectPopupMenu(Object calId) throws Exception {
			/* ====================================================== */
			// TODO Auto-generated method stub
			return null;
			/* ====================================================== */
		}

	}



}

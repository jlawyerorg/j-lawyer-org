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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import bizcal.common.Calendar;
import bizcal.common.CalendarModel;
import bizcal.swing.util.ErrorHandler;
import bizcal.swing.util.GradientArea;
import bizcal.swing.util.TableLayoutPanel;
import bizcal.swing.util.TableLayoutPanel.Cell;
import bizcal.swing.util.TableLayoutPanel.Row;


/**
 * @author Fredrik Bertilsson
 */
public class CalendarList	
{
	private static int GRADIENT_TOP_HEIGHT = 30;
    private CalendarModel _broker;
    private TableLayoutPanel _panel;
    private JSplitPane _splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    private Map _checkBoxes;
    private Set _listeners = new HashSet();
    private Object _currCalId;
    private int width;
	private Font font = new Font("Arial", 12, 10);
	private Color primaryColor;
	private Color secondaryColor;
	private PopupMenuCallback popupMenuCallback;
	private JLabel header;
	private JLabel groupName;
	private Object projectId;
	private List calendarList;
    
    public CalendarList(Object projId, CalendarModel broker)
    	throws Exception
    { 
    	projectId = projId;
    	primaryColor = new Color(200, 200, 200);
        secondaryColor = Color.WHITE;
		_broker = broker;
        init();
    }
    
    public void setBroker(CalendarModel broker)
    	throws Exception
    {
    	_broker = broker;
    	init();
    }
    
    private void init()
    	throws Exception
    {
        if (_panel != null)
            return;
        _panel = new TableLayoutPanel();
        _panel.addComponentListener(new ThisComponentListener());
    
        build();
    }
    
    public void refresh()
    	throws Exception
    {
    	_panel.deleteColumns();
    	_panel.deleteRows();
    	_panel.clear();
    	build();
    	fireCalendarsSelected();
    }
    
    private void build()
    	throws Exception
    {
    	_panel.setBackground(Color.WHITE);
        _panel.createColumn();
        _panel.createColumn(TableLayoutPanel.FILL);
        _panel.createColumn(5);
        Row ro = _panel.createRow(30);
        Cell cell = ro.createCell();
		cell.setColumnSpan(3);
               
        GradientArea headerGradientArea = new GradientArea(GradientArea.TOP_BOTTOM, secondaryColor, primaryColor);
		headerGradientArea.setGradientLength(0.5);
		if(header==null)
		{
			header = new JLabel("Planeringsgrupp", JLabel.CENTER);//(monthFormat.format(cal.getTime()).toUpperCase() + " " + yearFormat.format(cal.getTime()).toUpperCase(), JLabel.CENTER);
			header.setFont(font.deriveFont(Font.BOLD, 12f));
			header.setForeground(Color.WHITE);
		}
		headerGradientArea.add(header);
		headerGradientArea.setBorderWidth(0.2f);
        headerGradientArea.setBorderColor(Color.LIGHT_GRAY);
        cell.put(headerGradientArea);
        
        ro = _panel.createRow(20);
        cell = ro.createCell();
		cell.setColumnSpan(3);
        GradientArea topGradientArea = new GradientArea(GradientArea.TOP_BOTTOM, new Color(
                255, 255, 255), new Color(245, 245, 245));
        topGradientArea.setOpaque(true);
        cell.put(topGradientArea);
        String projectName = "Unknown";
        if (projectId != null)
        	projectName = "Calendars";
        if(groupName==null)
        	groupName = new JLabel(projectName);
        
        groupName.addMouseListener(new ProjectLabelMouseListener(projectId));
        
        groupName.setText(projectName);
        groupName.setFont(font);
        groupName.setForeground(Color.BLACK);
        
		topGradientArea.add(groupName);        
        
		Map oldCheckBoxes = _checkBoxes;
        _checkBoxes = new HashMap();
        TableLayoutPanel checkboxPanel = new TableLayoutPanel();
        checkboxPanel.setBackground(Color.WHITE);
        checkboxPanel.createColumn(TableLayoutPanel.FILL);
        checkboxPanel.createColumn(15);
        calendarList = _broker.getCalendars();
        Iterator i = calendarList.iterator();
        boolean first = true;
        while (i.hasNext()) {
        	Calendar cal = (Calendar) i.next();
        	Object id = cal.getId();
        	String name = cal.getSummary();
        	Row row;
            row = checkboxPanel.createRow();
            JCheckBox checkBox = new JCheckBox(name);
            checkBox.addMouseListener(new CalMouseListener(id));
            checkBox.setFont(font);
            checkBox.setOpaque(false);
            checkBox.setActionCommand("select");            
            CheckBoxListener listener = new CheckBoxListener(checkBox, cal.getId());
            checkBox.addActionListener(listener);
            _checkBoxes.put(id, checkBox);
            if (first && checkBox.isEnabled()) {
        	    checkBox.setSelected(true);
                first = false;
        	}
            if (oldCheckBoxes != null) {
	            JCheckBox oldCheckBox = (JCheckBox) oldCheckBoxes.get(id);
	            if (oldCheckBox != null)
	            	checkBox.setSelected(oldCheckBox.isSelected());
            }
            row.createCell(checkBox);
            if(!cal.isEnabled())
            {
            	checkBox.setEnabled(cal.isEnabled());
            	checkBox.setFont(font.deriveFont(Font.ITALIC));
            }    
        }    	
        JScrollPane scrollPanel = new JScrollPane(checkboxPanel, 
        		JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPanel.setBorder(null);        
        
        _splitPanel.setBackground(Color.WHITE);
        _splitPanel.setBorder(null); 
        _splitPanel.setDividerSize(0);
    	_splitPanel.setTopComponent(_panel);
        _splitPanel.setBottomComponent(scrollPanel);
    }
    
    public JComponent getComponent()
    {
    	return _splitPanel;
    }
        
    @SuppressWarnings("unchecked")
	public void addListener(CalendarSelectionListener listener)
    {
    	_listeners.add(listener);
    }
    
    private class CheckBoxListener
		implements ActionListener
	{    	
    	public CheckBoxListener(JCheckBox checkBox, Object id)
    	{
    	}
    	
    	public void actionPerformed(ActionEvent event)
    	{    		
    		try {
    			fireCalendarsSelected();
    		} catch (Exception e) {
    			ErrorHandler.handleError(e);
    		}
    	}
	}
    
    private void fireCalendarsSelected()
    	throws Exception
    {
		List selectedCals = getSelectedCalendars();		
		Iterator i = _listeners.iterator();
		while (i.hasNext()) {
			CalendarSelectionListener listener = 
				(CalendarSelectionListener) i.next();
			listener.calendarSelected(selectedCals);
		}    
    }
    
    public List getSelectedCalendars()
    	throws Exception
    {
		List selectedCals = new ArrayList();
		Iterator i = calendarList.iterator();
		while (i.hasNext()) {
			Calendar cal = (Calendar) i.next();
			JCheckBox checkBox = (JCheckBox) _checkBoxes.get(cal.getId());
			if (checkBox != null && checkBox.isSelected())
				selectedCals.add(cal);
		}    	
		return selectedCals;
    }
    
    private class CalMouseListener
		extends MouseAdapter
	{
    	private Object _calId;
    	
    	public CalMouseListener(Object id)
    	{
    		_calId = id;
    	}
    	
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }   	   	
        
    	private void maybeShowPopup(MouseEvent e) {
    		try {
				if (e.isPopupTrigger()) {
					_currCalId = _calId;
					JPopupMenu popup;
					if (popupMenuCallback != null)
						popup = popupMenuCallback.getCalendarPopupMenu(_calId);
					else {
						popup = new JPopupMenu();
						JMenuItem item = new JMenuItem("Ta bort");
						popup.add(item);
						item.addActionListener(new DeleteListener());						
					}					
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
    		} catch (Exception exc) {
    			ErrorHandler.handleError(exc);
    		}
		}	
	}
    
    private class ProjectLabelMouseListener extends MouseAdapter {

		public ProjectLabelMouseListener(Object id) {
		}

		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
			try {
				if (e.isPopupTrigger()) {
					JPopupMenu popup;
					if (popupMenuCallback != null)
						popup = popupMenuCallback
								.getProjectPopupMenu(projectId);
					else {
						popup = new JPopupMenu();
					}
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			} catch (Exception exc) {
				ErrorHandler.handleError(exc);
			}
		}
	}

    
    private class DeleteListener
		implements ActionListener
	{
    	public void actionPerformed(ActionEvent event)
    	{
    		try {
    			_broker.deleteCalendar(_currCalId);
	    		refresh();
    		} catch (Exception e) {
    			ErrorHandler.handleError(e);
    		}

    	}
	}
    
    private class ThisComponentListener
	extends ComponentAdapter
	{
		
		public void componentResized(ComponentEvent e)
		{
//			SwingUtilities.invokeLater(new Runnable() {
//				public void run() {
//					/* ================================================== */
					try {				
						width = _panel.getWidth();
						header.setBounds(0,0,width,GRADIENT_TOP_HEIGHT);
						groupName.setBounds(5,0,width-5,20);
						//refresh();			
					} catch (Exception exc) {
						exc.printStackTrace();
						ErrorHandler.handleError(exc);
					}
					/* ================================================== */
//				}
//			});
		}		
	}
    
	public void setPopupMenuCallback(PopupMenuCallback popupMenuCallback) {
		this.popupMenuCallback = popupMenuCallback;
	}

	public void setProjectId(Object projectId) {
		this.projectId = projectId;
	}
}

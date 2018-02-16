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
import java.util.Collections;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import bizcal.common.Bundle;
import bizcal.common.Event;
import bizcal.swing.util.ErrorHandler;

/**
 * @author wmfrbre
 */
public class EventPopupMenu
	extends JPopupMenu
{
	private static final long serialVersionUID = 1L;

	private Object _calId;
	private Event _event;
	
	public EventPopupMenu(Object calId, Event event, CalendarListener calListener)
		throws Exception
	{
		
		//ANV�NDS EJ?????????
		_calId = calId;
		_event = event;
		ActionListener listener = new ThisActionListener(calListener);
		JMenuItem item = new JMenuItem(Bundle.translate("Copy"));
		item.setActionCommand("copy");
		item.addActionListener(listener);
		add(item);
		addSeparator();
		item = new JMenuItem(Bundle.translate("Delete"));
		item.setActionCommand("delete");
		item.addActionListener(listener);
		add(item);
		addSeparator();
		item = new JMenuItem(Bundle.translate("Properties"));
		item.setActionCommand("properties");
		item.addActionListener(listener);
		add(item);
	}
	
	private class ThisActionListener
		implements ActionListener
	{
		private CalendarListener _listener;
		
		public ThisActionListener(CalendarListener listener)
		{
			_listener = listener;
		}
		
		public void actionPerformed(ActionEvent event)
		{
			try {
				if ("copy".equals(event.getActionCommand())) {
					_listener.copy(Collections.nCopies(1, _event));
					return;
				}
				if ("delete".equals(event.getActionCommand())) {
					_listener.deleteEvent(_event);
					return;
				}
				if ("properties".equals(event.getActionCommand())) {
					_listener.showEvent(_calId, _event);
				}
			} catch (Exception e) {
				ErrorHandler.handleError(e);
			}
				
		}
	}
	
}

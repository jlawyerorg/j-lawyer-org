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

import java.awt.Image;
import java.net.URL;

import javax.swing.ImageIcon;

public class CalendarIcons {

//	***************************************************************************
//	* Class Constants			                                              *
//	***************************************************************************

		protected static final int LARGEPIX = 64;

		protected static final int MEDIPIX = 24;

		protected static final int SMALLPIX = 18;

		protected static final int MINIPIX = 12;

		public static String AGENDA 				= "agendamodule.png";
		public static final String DAYVIEW 				= "cal_day.png";
		public static final String WEEKVIEW 				= "cal_week.png";
		public static final String MONTHVIEW 				= "cal_month.png";
		public static final String LISTVIEW 				= "cal_list.png";

		public static final String TODAY 					= "today.png";


		// day views
		public static final String DAY_FULL 		= MONTHVIEW;
		public static final String DAY_MORNING 		= "cal_day_morning.png";
		public static final String DAY_AFTERNOON 	= "cal_day_afternoon.png";


		public static String NEW 					= "add_line.png";
		public static String EDIT 					= "edit.png";
		public static String DELETE 				= "close.png";

		public static String COPY 			= "copy.png";
		public static String PASTE			= "paste.png";

//	---------------------------------------------------------------------------

	public static ImageIcon getIcon (String p_IconName)
		{
		String 		l_IconPath;
		URL 			l_Location;
		ImageIcon 	l_Icon = null;

		l_IconPath = "resources/icons/" + p_IconName;
		l_Location = CalendarIcons.class.getResource(l_IconPath);

		if (l_Location != null) l_Icon = new ImageIcon(l_Location);

		if ((l_Icon == null) || (l_Icon.getIconHeight() <= 0))
			{
			System.out.println("Couldn't find Icon: " + l_IconPath);
			}
		return l_Icon;
		}

//	---------------------------------------------------------------------------

	public static ImageIcon getScaledIcon(String p_IconName, int size)
		{
		return new ImageIcon(getIcon(p_IconName).getImage().getScaledInstance(
				size, size, Image.SCALE_SMOOTH));
		}

//	---------------------------------------------------------------------------

		public static ImageIcon getMiniIcon(String p_IconName) {
			return getScaledIcon(p_IconName, MINIPIX);
		}

//	---------------------------------------------------------------------------

		public static ImageIcon getSmallIcon(String p_IconName) {
			return getScaledIcon(p_IconName, SMALLPIX);
		}

//	---------------------------------------------------------------------------

		public static ImageIcon getMediumIcon(String p_IconName) {
			return getScaledIcon(p_IconName, MEDIPIX);
		}

//	---------------------------------------------------------------------------

		public static ImageIcon getBigIcon(String p_IconName) {
			return getScaledIcon(p_IconName, LARGEPIX);
		}


}

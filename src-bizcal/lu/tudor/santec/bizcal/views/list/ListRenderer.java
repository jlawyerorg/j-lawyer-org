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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import bizcal.common.Event;
import bizcal.util.DateUtil;

public class ListRenderer extends DefaultTableCellRenderer {

	private static final long serialVersionUID = 1L;

	private ListModel model;
	
	private Border newDayBorder = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK);

	private Border border;
	
	public ListRenderer(ListModel listModel) {
		this.model = listModel;
		this.setFont(new Font("Arial", Font.PLAIN, 12));
	}

	public Component getTableCellRendererComponent(JTable p_Table, Object value, boolean p_IsSelected, boolean hasFocus, int p_Row, int p_Column) {
		Event e = model.getEvent(p_Row); 
		Color c = Color.WHITE;
		try {
			c = e.getColor();		
			c = new Color(c.getRed(),c.getGreen(),c.getBlue(),60);
			if (e.getStart().before(new Date()))
				c = new Color(230,230,230);
			
			if (! DateUtil.isSameDay(e.getStart(), model.getEvent(p_Row-1).getStart()))
					border = newDayBorder;
			else 
					border = null;
		} catch (Exception ee) {
			border = null;
		}
		
		switch (p_Column) {
		case 0:
			this.setVerticalAlignment(JLabel.TOP);
			this.setHorizontalAlignment(JLabel.LEFT);
			this.setText((String) value);
			this.setIcon(null);
			break;
		case 1:
			this.setVerticalAlignment(JLabel.CENTER);
			this.setHorizontalAlignment(JLabel.CENTER);
			this.setText(null);
			this.setIcon((ImageIcon) value);
			break;
		default:
			this.setVerticalAlignment(JLabel.TOP);
			this.setHorizontalAlignment(JLabel.LEFT);
			this.setText((String) value);
			this.setIcon(null);
		    if (p_Table.getRowHeight(p_Row) < this.getPreferredSize().height) {
		    	p_Table.setRowHeight(p_Row, this.getPreferredSize().height + 4);
		    }
		    break;
		}

		if (p_IsSelected)
			this.setBackground(p_Table.getSelectionBackground());
		else
			this.setBackground(c);
		this.setBorder(border);
		return this;
	}
	
	

}

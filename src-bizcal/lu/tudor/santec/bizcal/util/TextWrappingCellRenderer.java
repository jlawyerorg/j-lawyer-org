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
package lu.tudor.santec.bizcal.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableCellRenderer;



public class TextWrappingCellRenderer extends JTextArea implements TableCellRenderer 
	{

//***************************************************************************
//* Class Constants                                                         *
//***************************************************************************
	
	private static final long serialVersionUID = 1L;
	    
	private static final Color c_OddColor  = new Color (237,243,254);
	private static final Color c_EvenColor = new Color (255,255,255);
    private static final int   c_Gap   = 2;
	
	
public TextWrappingCellRenderer() 
	{
    setLineWrap(true);
    setWrapStyleWord(true);
	this.setBorder(BorderFactory.createEmptyBorder (c_Gap,c_Gap,c_Gap,c_Gap));
	}

public Component getTableCellRendererComponent(JTable p_Table, Object
          value, boolean p_IsSelected, boolean hasFocus, int p_Row, int p_Column) 	
	{
	Color	l_Background;
	
	l_Background = ( p_Row % 2 == 0) ? c_OddColor : c_EvenColor;
	if (p_IsSelected) 
		 this.setBackground(p_Table.getSelectionBackground());
	else this.setBackground(l_Background);

	this.setFont (new Font (getFont().getName(), Font.PLAIN, 10));	

	
    setText((String)value);//or something in value, like value.getNote()...
    setSize(p_Table.getColumnModel().getColumn(p_Column).getWidth(),
              getPreferredSize().height);
    
    if (p_Table.getRowHeight(p_Row) < getPreferredSize().height) {
    	p_Table.setRowHeight(p_Row, getPreferredSize().height);
      }
      return this;
  }
} 
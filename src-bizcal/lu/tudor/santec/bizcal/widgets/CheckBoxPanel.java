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
package lu.tudor.santec.bizcal.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.plaf.UIResource;

public class CheckBoxPanel extends JPanel implements ActionListener {




	private static final long serialVersionUID = 1L;
	
	private JCheckBox checkBox;
	private JToggleButton button;
	private Color bgColor;
	private ColorBoxIcon icon;
	private Vector<ActionListener> listeners = new Vector<ActionListener>();

	public CheckBoxPanel(String text, Color c, ButtonGroup group) {

		this.setOpaque(true);
		
		this.setLayout(new BorderLayout());
		this.checkBox = new JCheckBox();
		this.checkBox.setOpaque(true);
		this.checkBox.requestFocus(false);
		this.add(checkBox, BorderLayout.EAST);
		
		this.button = new JToggleButton(text);
		group.add(this.button);

		this.button.addActionListener(this);
//		this.button.setForeground(c);

		this.icon = new ColorBoxIcon(c);
		this.button.setIcon(this.icon);
		this.button.setHorizontalAlignment(SwingConstants.LEFT);
		this.add(this.button, BorderLayout.CENTER);
		
	}

	public void setActiv(boolean b) {
		this.checkBox.setSelected(b);
	}

	public synchronized void addActionListener(ActionListener listener) {
		this.checkBox.addActionListener(listener);
		this.listeners .add(listener);
	}

	public synchronized void removeActionListener(ActionListener listener) {
		this.checkBox.removeActionListener(listener);
		this.listeners .remove(listener);
	}

	public boolean isActiv() {
		return checkBox.isSelected();
	}

	/**
	 * @return the isSelected
	 */
	public boolean isSelected() {
		return this.button.isSelected();
	}

	/**
	 * @param isSelected the isSelected to set
	 */
	public void setSelected(boolean isSelected) {
		/* ================================================== */
		this.button.setSelected(isSelected);
		asynchInformActionListeners();
		/* ================================================== */
	}

	public void setColor(Color c) {
		this.bgColor = c;
//		this.button.setForeground(bgColor);
		this.icon.setColor(bgColor);
		this.button.updateUI();
	}

	/**
	 * Set the text of the button
	 *
	 * @param text
	 */
	public void setText(String text) {
		/* ================================================== */
		this.button.setText(text);
		/* ================================================== */
	}
	
	
	/**
	 * Inform action listeners asynchronous to speedup the gui
	 */
	private synchronized void asynchInformActionListeners() {
		/* ================================================== */
//		Thread t = new Thread() {
//			public void run() {
				for (Iterator iter = listeners.iterator(); iter.hasNext();) {
					ActionListener element = (ActionListener) iter.next();
					element.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "selection changed"));
				}
//			}
//		};
//		t.start();
		/* ================================================== */
	}

	public void actionPerformed(ActionEvent e) {
		/* ====================================================== */
		if (this.button.equals(e.getSource())) {
			/* ------------------------------------------------------- */
			asynchInformActionListeners();
			/* ------------------------------------------------------- */
		}
		/* ====================================================== */
	}
	
	/* (non-Javadoc)
	 * @see java.awt.Component#addMouseListener(java.awt.event.MouseListener)
	 */
	@Override
	public synchronized void addMouseListener(MouseListener l) {
		/* ====================================================== */

		this.button.addMouseListener(l);
		/* ====================================================== */
	}

	
	/**
	 * @author martin.heinemann@tudor.lu
	 * 21.10.2008
	 * 15:32:27
	 *
	 *
	 * @version
	 * <br>$Log: CheckBoxPanel.java,v $
	 * <br>Revision 1.5  2008/10/21 16:05:06  heine_
	 * <br>*** empty log message ***
	 * <br>
	 * <br>Revision 1.4  2008/10/21 15:08:31  heine_
	 * <br>*** empty log message ***
	 * <br>
	 *   
	 */
	public class ColorBoxIcon implements Icon, UIResource, Serializable {

		private Color color;
		
		private static final double FAQ = 1.5;
		
		public ColorBoxIcon(Color color) {
			this.color = color;
		}

		private static final long serialVersionUID = 1L;

		protected int getControlSize() { return 16; }

	    public void paintIcon(Component c, Graphics g, int x, int y) {

			int controlSize = getControlSize();
	
			g.setColor(color);
			g.fillRect(x, y+1, (int) ((controlSize-1)*FAQ), controlSize-1);
			g.setColor(Color.BLACK);
			g.drawRect(x, y+1, (int) ((controlSize-1)*FAQ), controlSize-1);
		}

	    public int getIconWidth() {
	        return (int) (getControlSize()*FAQ);
	    }

	    public int getIconHeight() {
	        return getControlSize();
	    }
	    
	    public void setColor(Color c) {
	    	this.color = c;
	    }
	 }
	
	
}

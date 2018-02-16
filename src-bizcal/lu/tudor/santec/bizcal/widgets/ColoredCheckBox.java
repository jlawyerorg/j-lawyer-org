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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class ColoredCheckBox extends JCheckBox {

	private boolean drawBackground;
	private Color bgColor;
	private ColorCheckBoxIcon cbi;
	private boolean big;
	private static final long serialVersionUID = 1L;

	public ColoredCheckBox(String text, Color c, boolean big, boolean drawBackground) {
		super(text);
		this.drawBackground = drawBackground;
		this.big = big;
		setOpaque(false);

		this.setColor(c);


//		this.bgColor = new Color(c.getRed(),c.getGreen(),c.getBlue(),50);
//		this.cbi = new ColorCheckBoxIcon(c, big);
//		setIcon(cbi);
	}

	public ColoredCheckBox(String text, Color c, boolean big) {
		this(text, c, big, false);
	}

	public ColoredCheckBox(String text, Color c) {
		this(text, c, false, false);
	}

	public void setColor(Color c) {
		/* ================================================== */
		this.bgColor = new Color(c.getRed(),c.getGreen(),c.getBlue(),50);
		this.cbi = new ColorCheckBoxIcon(c, this.big);
		setIcon(cbi);
		/* ================================================== */
	}
	
	
	/* (non-Javadoc)
	 * @see javax.swing.AbstractButton#setSelected(boolean)
	 */
	@Override
	public void setSelected(boolean b) {
		/* ====================================================== */
		super.setSelected(b);
		this.model.setSelected(b);
		/* ====================================================== */
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	protected void paintComponent(Graphics g) {
//		if (hasFocus()) {
//			g.setColor(new Color(200,200,200));
//			g.fillRect(0,0, getWidth(), getHeight());
//		}
		if (drawBackground) {
			g.setColor(bgColor);
			g.fillRoundRect(1, 1, getWidth()-2, getHeight()-2, 8,8);
		}
		super.paintComponent(g);
	}


	/**
	 * 
	 * 12.09.2007
	 * 09:40:04
	 *
	 *
	 * @version
	 * <br>$Log: ColoredCheckBox.java,v $
	 * <br>Revision 1.2  2007/09/20 07:23:16  heine_
	 * <br>new version commit
	 * <br>
	 * <br>Revision 1.4  2007-09-18 09:52:32  heinemann
	 * <br>*** empty log message ***
	 * <br>
	 *   
	 */
	public class ColorCheckBoxIcon implements Icon, UIResource, Serializable {

		private Color color;
		private boolean big;

		public ColorCheckBoxIcon(Color color, boolean bigCheck) {
			this.color = new Color(color.getRed(),color.getGreen(),color.getBlue(),120);
			this.big = bigCheck;
		}

		private static final long serialVersionUID = 1L;

		protected int getControlSize() { return 16; }

	    public void paintIcon(Component c, Graphics g, int x, int y) {

		     JCheckBox cb = (JCheckBox)c;
			ButtonModel model = cb.getModel();
			int controlSize = getControlSize();
	
		    boolean drawCheck = model.isSelected();
		    
		    
//		    System.out.println("drawCheck -> " + drawCheck);
		    
			if (model.isEnabled()) {
				g.setColor(color);
				g.fillRoundRect(x, y+1, controlSize-1, controlSize-1 ,3,3);
			} else {
				g.setColor(MetalLookAndFeel.getControlShadow());
				g.drawRect(x, y, controlSize - 1, controlSize - 1);
			}
	
				if(drawCheck) {
					g.setColor(Color.DARK_GRAY);
				    if (big)
				    	drawBigCheck(c,g,x,y);
				    else
				    	drawCheck(c,g,x,y);
				}
		}

	    protected void drawCheck(Component c, Graphics g, int x, int y) {
		int controlSize = getControlSize();
		g.fillRect( x+3, y+5, 2, controlSize-8 );
		g.drawLine( x+(controlSize-4), y+3, x+5, y+(controlSize-6) );
		g.drawLine( x+(controlSize-4), y+4, x+5, y+(controlSize-5) );
	    }

	    protected void drawBigCheck(Component c, Graphics g, int x, int y) {
			int controlSize = getControlSize();
			x+=2;

			g.fillRect( x+2, y+3, 3, controlSize-4 );
			g.drawLine( x+(controlSize), y-2, x+5, y+(controlSize-7) );
			g.drawLine( x+(controlSize), y-2, x+5, y+(controlSize-6) );
			g.drawLine( x+(controlSize), y-2, x+5, y+(controlSize-5) );
			g.drawLine( x+(controlSize), y-2, x+5, y+(controlSize-4) );
		    }

	    public int getIconWidth() {
	        return getControlSize();
	    }

	    public int getIconHeight() {
	        return getControlSize();
	    }
	 }


}

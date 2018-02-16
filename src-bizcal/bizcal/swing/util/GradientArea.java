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
package bizcal.swing.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * Creates an area which fade between two given colors.
 * 
 * Example use:
  	GradientArea gradientArea = new GradientArea(GradientArea.TOP_BOTTOM, white, grey);
 	gradientArea.setOpaque(true);
	gradientArea.setBounds(0, 0, x, y);
	panel.add(gradientArea);
	
 * @author Jonas Karlsson
 */
public class GradientArea
	extends JComponent
{
	private static final long serialVersionUID = 1L;
	
	public final static String TOP_BOTTOM = "TOP_BOTTOM";
	public final static String BOTTOM_TOP = "BOTTOM_TOP";
	public final static String LEFT_RIGHT = "LEFT_RIGHT";
	public final static String RIGHT_LEFT = "RIGHT_LEFT";
	public final static String TOPLEFT_BOTTOMRIGHT = "TOPLEFT_BOTTOMRIGHT";
	public final static String IN_OUT = "IN_OUT";
	public final static String BORDER_RECTANGLE = "BORDER_RECTANGLE";
	public final static String BORDER_TOP = "BORDER_TOP";
	
	private String itsFadeDirection;
	private double itsGradientLength;
	private Color itsStartColor;
	private Color itsEndColor;
	private float itsBorderWidth;
	private Color itsBorderColor;
	private boolean itsBorder;
	private String itsBorderType;
//	private String text;
//	private JComponent component;
	private JLabel _label;
	
	/**
	 * Constructs a default GradientArea which fades in top-to-bottom
	 * direction from white to a bright grey tone
	 *
	 */
	public GradientArea()
	{
		this(GradientArea.TOP_BOTTOM, 
				new Color(255, 255, 255),
				new Color(245, 245, 245));
	}
	
	public GradientArea(String aFadeDirection, Color aStartColor, Color aEndColor)
	{
		itsFadeDirection = aFadeDirection;
		itsStartColor = aStartColor;
		itsEndColor = aEndColor;
		itsBorderWidth = 4.0f;
		itsBorderColor = itsEndColor;
		itsGradientLength = 1;
		itsBorder = true;
		itsBorderType = GradientArea.BORDER_RECTANGLE;
	}
	
	/**
	 * Sets position of the break point of the two colors
	 * 
	 * @param length
	 */
	public void setGradientLength(double length)
	{
		itsGradientLength = length;
	}
	/**
	 * Sets width of this component border
	 * @param aWidth
	 */
	public void setBorderWidth(float aWidth)
	{
		itsBorderWidth = aWidth;
	}
	
	/**
	 * Sets color of the border of this component
	 * @param aColor
	 */
	public void setBorderColor(Color aColor)
	{
		itsBorderColor = aColor;
	}
	
	public void setBorder(boolean aBorder)
	{
		itsBorder = aBorder;
	}
	
	public void setColors(Color startColor, Color endColor)
	{
		itsStartColor = startColor;
		itsEndColor = endColor;
	}
	
	public void setBorderType(String aType)
	{
		itsBorderType = aType;		
	}
	
	/**
	 * The toolkit will invoke this method when it's time to paint
	 */
	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		
		int width = (int) getWidth();
		int height = (int) getHeight();
		
		GradientPaint gradient;
		if(itsFadeDirection.equals(GradientArea.TOP_BOTTOM))
		{
			gradient = new GradientPaint(0, 0, itsStartColor,0, (int) (height*itsGradientLength), itsEndColor);
			g2.setPaint(gradient);
		}
		if(itsFadeDirection.equals(GradientArea.BOTTOM_TOP))
		{
			gradient = new GradientPaint(0, 0, itsEndColor,0, (int) (height*itsGradientLength), itsStartColor);
			g2.setPaint(gradient);
		}
		if(itsFadeDirection.equals(GradientArea.LEFT_RIGHT))
		{
			gradient = new GradientPaint(0, 0, itsStartColor,(int) (width*itsGradientLength), 0, itsEndColor);
			g2.setPaint(gradient);
		}
		if(itsFadeDirection.equals(GradientArea.RIGHT_LEFT))
		{
			gradient = new GradientPaint((int) (width*itsGradientLength), 0, itsStartColor, 0, 0, itsEndColor);
			g2.setPaint(gradient);
		}
		if(itsFadeDirection.equals(GradientArea.TOPLEFT_BOTTOMRIGHT))
		{
			gradient = new GradientPaint(0, 0, itsStartColor, (int) (width*itsGradientLength), (int) (height*itsGradientLength), itsEndColor);
			g2.setPaint(gradient);
		}
		if(itsFadeDirection.equals(GradientArea.IN_OUT))
		{
			gradient = new GradientPaint(0, 10, Color.RED,(int) (width*itsGradientLength), 0, itsEndColor);
			g2.setPaint(gradient);
			gradient = new GradientPaint(0, 0, Color.BLUE, (int) (width*itsGradientLength), 0, itsEndColor);
			g2.setPaint(gradient);
		}
        			
        g2.fill(new Rectangle2D.Double(0, 0, width, height));
        
        if(itsBorder)
        {
	        g2.setStroke(new BasicStroke(itsBorderWidth));
	        g2.setPaint(itsBorderColor);
	        if(itsBorderType.equals(GradientArea.BORDER_RECTANGLE))
	        	g2.draw(new Rectangle2D.Double(0, 0, width-1,height-1));
	        if(itsBorderType.equals(GradientArea.BORDER_TOP))
	        	g2.draw(new Line2D.Double(new Point(0,0), new Point(width,0)));
        }
	        
        super.paintComponent(g2);
	}  
	
	public void setText(String text, boolean center)
	{
		initLabel();
		_label.setText(text);
		if(center)
			_label.setHorizontalAlignment(JLabel.CENTER);
	}
	
	private void initLabel()
	{
		if (_label == null) {
			_label = new JLabel();
			addComponentListener(new ThisComponentListner());
			add(_label);		
		}
	}
	
	public void setFont(Font font)
	{
		super.setFont(font);
		initLabel();
		_label.setFont(font);
	}
	
	public void setForeground(Color color)
	{
		initLabel();
		_label.setForeground(color);
		super.setForeground(color);
	}
	
	public void setHorizontalAlignment(int align)
	{
		_label.setHorizontalAlignment(align);
	}
	
	private class ThisComponentListner
		extends ComponentAdapter
	{
		public void componentResized(final ComponentEvent e)
		{	
//			SwingUtilities.invokeLater(new Runnable() {
//				public void run() {
					_label.setBounds(10,0, e.getComponent().getWidth(), e.getComponent().getHeight());
//				}
//			});
		}
	}
	
	public ComponentListener getComponentListener()
	{
		return new ThisComponentListner();
	}
	
	
		
}

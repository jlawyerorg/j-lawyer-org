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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class GradientPanel extends JPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel panel;
	private GradientArea area;
	
	/**
	 * @param area
	 * @param panel
	 */
	public GradientPanel(GradientArea area, JPanel panel) {
		/* ================================================== */
		panel.setOpaque(false);
		setLayout(new Layout());
		
		add(panel);
		add(area);
		this.panel = panel;
		this.area = area;
		/* ================================================== */
	}
	
	/**
	 * @param area
	 * @param component
	 */
	public GradientPanel (GradientArea area, JComponent component) {
		/* ================================================== */
		this.panel = new JPanel(new BorderLayout());
		setLayout(new Layout());
		
		panel.setOpaque(false);
		panel.add(component);
		
		add(panel);
		add(area);
		this.area = area;
		/* ================================================== */
	}
	
	
	
	public Dimension getPreferredSize()
	{
		return panel.getPreferredSize();
	}
	
	public void setPreferredSize(Dimension dim)
	{
		panel.setPreferredSize(dim);
	}
	
	public void revalidate()
	{
		if (panel != null)
			panel.revalidate();
	}
	
	/*public void setBounds(int x, int y, int width, int height)
	{
		panel.setBounds(0, 0, width, height);
		area.setBounds(0, 0, width, height);
	}*/
	
	
	private class Layout implements LayoutManager {
		public void addLayoutComponent(String name, Component comp) {
		}

		public void removeLayoutComponent(Component comp) {
		}

		public Dimension preferredLayoutSize(Container parent) {
			return parent.getPreferredSize();
		}

		public Dimension minimumLayoutSize(Container parent) {
//			return parent.getMinimumSize();
			return panel.getMinimumSize();
		}

		public void layoutContainer(Container parent) {
			area.setBounds(0, 0, parent.getWidth(), parent.getHeight());
			panel.setBounds(0, 0, parent.getWidth(), parent.getHeight());
		}
	}
	
	

}

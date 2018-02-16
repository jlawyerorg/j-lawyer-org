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
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * creates a Navigationbar with PatientButtonPanels
 * 
 * @author Johannes Hermen johannes.hermen(at)tudor.lu
 *
 */
public class NaviBar extends JPanel {

	private static final long serialVersionUID = 1L;
	
	// inner constraints
	public static final int TOP 	= 1;
	public static final int BOTTOM 	= 2;
	public static final int FILL 	= 3;
	
	private CellConstraints cc;
	private FormLayout layout;
	private int pos = 2;
	
	/**
	 * 
	 */
	public NaviBar() {
		this.setOpaque(false);
		this.layout = new FormLayout("1dlu, 60dlu, 1dlu", "1dlu, pref:grow");
		this.setLayout(layout);	
		this.cc = new CellConstraints();
		
		
		this.setBorder(BorderFactory.createRaisedBevelBorder());
		
	}
	
	/**
	 * @param width
	 */
	public NaviBar(int width) {
		this.setOpaque(false);
		this.layout = new FormLayout("1dlu, " + width + "dlu, 1dlu", "1dlu, pref");
		
		this.setLayout(layout);	
		this.cc = new CellConstraints();
		
		
		this.setBorder(BorderFactory.createRaisedBevelBorder());
		
	}
	
	/**
	 * @param buttonPanel
	 * @param alignment
	 */
	public void addButtonPanel(JComponent buttonPanel, int alignment) {
		/* ================================================== */
		if (alignment == TOP) {
			/* ------------------------------------------------------- */
			this.layout.insertRow(pos, new RowSpec("pref"));
			this.add(buttonPanel, cc.xy(2, pos));
			pos++;
			this.layout.insertRow(pos, new RowSpec("3dlu"));
			pos++;
			/* ------------------------------------------------------- */
		} else 
		if (FILL == alignment) {
			/* ------------------------------------------------------- */
//			this.layout.insertRow(pos, new RowSpec("fill:pref:grow"));
//			this.add(new JLabel("f "+pos), cc.xy(2, pos));
//			pos++;
			this.layout.insertRow(pos, new RowSpec("fill:pref:grow"));
			this.add(buttonPanel, cc.xy(2, pos));
			pos++;
			this.layout.insertRow(pos, new RowSpec("3dlu"));
			pos++;
			/* ------------------------------------------------------- */
		}
		else {
			this.layout.appendRow(new RowSpec("pref"));
			this.add(buttonPanel, cc.xy(2, layout.getRowCount()));
			this.layout.appendRow(new RowSpec("3dlu"));
		}
		/* ================================================== */
	}
	
	
	/**
	 * creates a Demo PatientNaviBar 
	 * @param args
	 */
	public static void main(String[] args) {
		JFrame jf = new JFrame("PatientNaviBar Demo");
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		NaviBar bar = new NaviBar();
		
		Vector<AbstractButton> v = new Vector<AbstractButton>();
		v.add(new JToggleButton("A"));
		v.add(new JToggleButton("B"));
		v.add(new JToggleButton("C"));
		v.add(new JToggleButton("D"));
		v.add(new JToggleButton("E"));
		v.add(new JToggleButton("F"));
		bar.addButtonPanel(new ButtonPanel("Navigation", Color.CYAN, 3,  v), TOP);
		
		Vector<AbstractButton> v1 = new Vector<AbstractButton>();
		v1.add(new JButton("Verschreibung"));
		v1.add(new JButton("Trait Chron."));
		v1.add(new JButton("Transfer"));
		bar.addButtonPanel(new ButtonPanel("Funktionen", Color.GREEN, 1, v1), TOP);
		
		Vector<AbstractButton> v2 = new Vector<AbstractButton>();
		v2.add(new JButton("C1"));
		v2.add(new JButton("V1"));
		v2.add(new JButton("345"));
		v2.add(new JButton("456"));
		v2.add(new JButton("dfg"));
		v2.add(new JButton("768"));
		v2.add(new JButton("gfh"));
		v2.add(new JButton("234"));
		v2.add(new JButton("klö"));
		v2.add(new JButton("fgh"));
		v2.add(new JButton("sdf"));
		v2.add(new JButton("gh"));
		bar.addButtonPanel(new ButtonPanel("Facturation", Color.RED, 2, v2), BOTTOM);
		
		Vector<AbstractButton> v3 = new Vector<AbstractButton>();
		v3.add(new JToggleButton("1 Peter Maier"));
		v3.add(new JToggleButton("2 Karl Hans"));
		v3.add(new JToggleButton("3 Ranseier Karl"));
		v3.add(new JToggleButton("4 Graf Steffi"));
		bar.addButtonPanel(new ButtonPanel("Clients", Color.YELLOW, 1, v3), BOTTOM);
		
		jf.add(bar);
		jf.setSize(160,600);
		jf.setVisible(true);
	}
	
	
	
	
}

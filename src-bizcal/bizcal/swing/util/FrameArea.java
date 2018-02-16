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

import static java.lang.Math.pow;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EventListener;
import java.util.List;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import bizcal.common.Event;

public class FrameArea extends JComponent implements ComponentListener {
	private static final long serialVersionUID = 1L;

	private String itsHeadLine;

	private String itsDescription;

	private Color fontColor;

	private List<Listener> listeners = new ArrayList<Listener>();

	private boolean border;

	private boolean roundedRectangle;

	private boolean selected;

	private ImageIcon icon;
	
	private ImageIcon upperRightIcon;

	// private Color selectionColor = new Color(196, 0, 0);
	private Color selectionColor = Color.BLACK;

	private Date endTime = null;

	private Date startTime = null;

	private boolean isMoving = false;

	private String movingString = "";

	private Date moveDate;

	private Event event = null;

	private List<FrameArea> children = null;

	private Color bgColor;

	public double xPosition = 0.0;

	public double yPosition = 54.0;

	private double angle = 0.0;

	private Color alphaFontColor;

	private Font normalFont;

	private float ALPHA_DEFAULT = 0.6f;

	private float SELECT_OFFSET = 0.2f;


	private float alphaValue = ALPHA_DEFAULT;

	
	private int lineDistance = 4;
	
	private static int HEADER_HEIGHT = 20;
	
	private boolean showHeader = true;

	private int lineWrap = -1;

	private boolean isBackgroundMarker 	= false;

	private int boundX;

	private int boundY;

	private int boundWidth;

	private int boundHeight;

	private static Color   backgroundMarkColor = new Color(205, 207, 255);
	
	public final DateFormat timeFormat = new SimpleDateFormat("HH:mm",
			Locale.getDefault());

	/**
	 */
	public FrameArea() {
		/* ================================================== */
		this.normalFont = new Font("Verdana", Font.PLAIN, 10);
		this.setFont(normalFont);
		// change color for drag
		this.setBackground(new Color(100, 100, 245));
		this.fontColor = Color.WHITE;
		this.alphaFontColor = fontColor;

		this.border = true;
		this.roundedRectangle = true;
		this.selected = false;
		
		this.addComponentListener(this);
		/* ================================================== */
	}
	
	
	/**
	 * Paints just a light blue background, no round corners
	 * no time, no borders
	 * 
	 * @param backgroundMarker
	 */
	public FrameArea(boolean backgroundMarker) {
		/* ================================================== */
		this();
		this.isBackgroundMarker  = backgroundMarker;
		if (this.isBackgroundMarker) {
			/* ------------------------------------------------------- */
			setBackground(backgroundMarkColor);
			
			/* ------------------------------------------------------- */
		}
		/* ================================================== */
	}
	
	/**
	 * Set the Event object for the FrameArea
	 * @param event
	 */
	public void setEvent(Event event) {
		/* ================================================== */
		this.event = event;
		// compute colors
		if (this.event != null)
			this.bgColor = event.getColor();

		if (this.event != null && this.event.isBackground()) {
			/* ------------------------------------------------------- */
			this.fontColor = Color.BLACK;
			/* ------------------------------------------------------- */
		}
		else {
			/* ------------------------------------------------------- */
			this.fontColor = computeForeground(this.bgColor);
			/* ------------------------------------------------------- */
		}
		/* ------------------------------------------------------- */
		this.alphaFontColor = new Color(fontColor.getRed(), fontColor.getGreen(), fontColor.getBlue(), 220);
		try {
			setBackground(event.getColor());
		} catch (Exception e) {
			e.printStackTrace();
		}
		/* ================================================== */
	}

	/**
	 * @return
	 */
	public Event getEvent() {
		/* ================================================== */
		return this.event;
		/* ================================================== */
	}

	/**
	 * @param fa
	 */
	public void addChild(FrameArea fa) {
		/* ================================================== */
		if (this.children == null)
			this.children = Collections.synchronizedList(new ArrayList<FrameArea>(1));
		/* ------------------------------------------------------- */
		children.add(fa);
		/* ================================================== */
	}

	/**
	 * @param fa
	 */
	public void removeChild(FrameArea fa) {
		/* ================================================== */
		if (this.children != null)
			this.children.remove(fa);
		/* ================================================== */
	}

	/**
	 * @return
	 */
	public List<FrameArea> getChildren() {
		/* ================================================== */
		if (this.children == null)
			this.children =  Collections.synchronizedList(new ArrayList<FrameArea>(0));
		return this.children;
		/* ================================================== */
	}

	/**
	 * @param b
	 */
	public void setIsMoving(boolean b) {
		/* ================================================== */
		this.isMoving = b;
		/* ================================================== */
	}

	/**
	 * Sets the string that is displayed while moving
	 *
	 */
	public synchronized void setMovingTimeString(Date moveStartDate, Date moveEndDate) {
		/* ================================================== */
//		System.out.println("FrameArea::setMovingTimeString " + moveStartDate + " -" + moveEndDate);
		this.moveDate = (Date) moveStartDate.clone();
		this.movingString = timeFormat.format(moveDate) + " - "
				+ timeFormat.format(moveEndDate);
		/* ================================================== */
	}

	/**
	 * @param endTime
	 */
	public void setEndTime(Date endTime) {
		/* ================================================== */
		this.endTime = (Date) endTime.clone();
		/* ================================================== */
	}

	/**
	 * @param startTime
	 */
	public void setStartTime(Date startTime) {
		/* ================================================== */
		this.startTime = (Date) startTime.clone();
		/* ================================================== */
	}


	/**
	 * @param aValue
	 */
	@Deprecated
	public void setAlphaValue(float aValue) {
		if (aValue > 1.0f)
			aValue = 1.0f;
//		this.alphaValue = aValue - SELECT_OFFSET;
	}

	/**
	 * @return
	 */
	public float getAlphaValue() {
		return this.alphaValue;
	}
	
	private synchronized String getMovingTimeString() {
		/* ================================================== */
		return this.movingString;
		/* ================================================== */
	}
	
	
	// The toolkit will invoke this method when it's time to paint
	public void paint(Graphics g) {
		/* ================================================== */
		
		Color backGroundColor = this.getBackground();
		Color headerColor     = this.getBackground();
		/* ------------------------------------------------------- */
		if (event != null) {
			/* ------------------------------------------------------- */
			backGroundColor = event.getColor();
			headerColor     = event.getColor();
			/* ------------------------------------------------------- */
		}
		
//		System.out.println(event);
		/* ------------------------------------------------------- */
		// increase the alpha value if the event is a background event
		/* ------------------------------------------------------- */
		
		/* ------------------------------------------------------- */
		// background settings
		/* ------------------------------------------------------- */
		if (event != null && event.isBackground()) {
			/* ------------------------------------------------------- */
			headerColor = Color.LIGHT_GRAY;
			
			this.alphaValue = ALPHA_DEFAULT + SELECT_OFFSET - 0.55f;
			/* ------------------------------------------------------- */
		}
		/* ------------------------------------------------------- */
		if (!this.getBackground().equals(backGroundColor))
			setBackground(backGroundColor);
		/* ------------------------------------------------------- */
		
		
		/* ------------------------------------------------------- */
		Graphics2D g2 = (Graphics2D) g;
		// makes the graphics smoother
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		/* ------------------------------------------------------- */
		int width  = getWidth();
		int height = getHeight();
		BufferedImage buffImg = null;
		
		/* ------------------------------------------------------- */
		buffImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		/* ------------------------------------------------------- */
		Graphics2D gbi = buffImg.createGraphics();
		gbi.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
									RenderingHints.VALUE_ANTIALIAS_ON);
		gbi.setStroke(new BasicStroke(1.0f));
		
		// ===========================================================
		// draw backgorund marker event
		//
		// ===========================================================
		if (isBackgroundMarker) {
			/* ------------------------------------------------------- */
			// do some modifications
			event.setSelectable(false);
			event.setBackground(true);
			/* ------------------------------------------------------- */
			AlphaComposite ac = null;
			ac = AlphaComposite.getInstance(AlphaComposite.DST_OVER, alphaValue);
			gbi.setComposite(ac);
			gbi.setPaint(this.getBackground());
			/* ------------------------------------------------------- */
			
			gbi.fill(new Rectangle2D.Double(0, 0, width, height));
			/* ------------------------------------------------------- */
//			if (this.border && (this.event == null || !this.event.isBackground())) {
//				gbi.setPaint(Color.black);
//				gbi.draw(new Rectangle2D.Double(1, 1, width - 2, height - 2));
//			}
			
			
			g2.drawImage(buffImg, null, 0, 0);
			super.paint(g2);
			super.paint(gbi);
			return;
			/* ------------------------------------------------------- */
		}
		
		
		// ===========================================================
		// draw round rectangle
		//
		// ===========================================================
		if (this.roundedRectangle) {
			AlphaComposite ac = AlphaComposite.getInstance(
					AlphaComposite.DST_OVER, alphaValue);
			gbi.setComposite(ac);
			gbi.setPaint(this.getBackground());
			/* ------------------------------------------------------- */
//			if (this.event != null && this.event.isBackground())
//				drawHatchedRect(gbi, 0, 0, width, height, false);
//			else
				gbi.fill(new RoundRectangle2D.Double(0, 0, width, height, 20, 20));
			/* ------------------------------------------------------- */
			if (this.border && (this.event == null || !this.event.isBackground())) {
				gbi.setPaint(Color.black);
				gbi.draw(new RoundRectangle2D.Double(1, 1, width - 2,
						height - 2, 17, 17));
			}
		}
		// ============================================================
		// draw non-round rectangle
		// ============================================================
		else {
			/* ------------------------------------------------------- */
			AlphaComposite ac = null;
			ac = AlphaComposite.getInstance(AlphaComposite.DST_OVER, alphaValue);
			gbi.setComposite(ac);
			gbi.setPaint(this.getBackground());
			/* ------------------------------------------------------- */
//			if (this.event != null && this.event.isBackground())
//				gbi.setPaint(Color.LIGHT_GRAY);
//				drawHatchedRect(gbi, 0, 0, width, height, false);
//			else
				gbi.fill(new Rectangle2D.Double(0, 0, width, height));
			/* ------------------------------------------------------- */
//			if (this.border && (this.event == null || !this.event.isBackground())) {
			if (this.border && (this.event != null)) {
//				if (this.event.isBackground())
				gbi.setPaint(Color.black);
				gbi.draw(new Rectangle2D.Double(1, 1, width - 2, height - 2));
			}
			/* ------------------------------------------------------- */
		}

		g2.drawImage(buffImg, null, 0, 0);
		/* ------------------------------------------------------- */
		// ==============================================================
		// creation of the darker header starts here
		//
		// background events do not have a darker header
		// ==============================================================
		Graphics2D gbiHeader = null;

		if (showHeader)
//			if (event == null || !event.isBackground()) {
			if (event != null) {
			/* ------------------------------------------------------- */
			// create darker header
			BufferedImage buffImgHeader = new BufferedImage(width, HEADER_HEIGHT,
					BufferedImage.TYPE_INT_ARGB);
			gbiHeader = buffImgHeader.createGraphics();
			/* ------------------------------------------------------- */
			gbiHeader.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			gbiHeader.setStroke(new BasicStroke(1.0f));
			/* ------------------------------------------------------- */
//			if (event.isBackground())
//				gbiHeader.setPaint(Color.LIGHT_GRAY);
//			else
				gbiHeader.setPaint(headerColor);
			/* ------------------------------------------------------- */
			if (isRoundedRectangle())
				gbiHeader.fill(new RoundRectangle2D.Double(0, 0, width, 20, 20, 20));
			else
				gbiHeader.fill(new Rectangle2D.Double(0, 0, width, 20));
			/* ------------------------------------------------------- */
			// paint
			g2.drawImage(buffImgHeader, null, 0, 0);
			/* ------------------------------------------------------- */
		}

		/* ------------------------------------------------------- */
		int xpos = 5;
		if (icon != null) {
			g2.drawImage(icon.getImage(), xpos, 2, this);
			xpos += icon.getIconWidth() + 3;
		}
		/* ------------------------------------------------------- */
		// upper right icon
		if (upperRightIcon != null) {
			/* ------------------------------------------------------- */
			g2.drawImage(upperRightIcon.getImage(), getWidth() - 20, 2, this);
//			xpos += icon.getIconWidth() + 3;
			/* ------------------------------------------------------- */
		}
		
		
		// =================================================================================
		// actions below this point will be placed on top of the "colored glass"
		//
		// =================================================================================

		// ============================================================
		// paint the header, normally the time periode
		// ============================================================
		Font timeFont = this.getFont().deriveFont(Font.BOLD);
		/* ------------------------------------------------------- */
		// set alpha fontColor for background events
		if (this.event == null || !this.event.isBackground())
			g2.setPaint(fontColor);
		else
			g2.setPaint(alphaFontColor);
		/* ------------------------------------------------------- */
		g2.setFont(timeFont);
		int ypos = 15;
		if (itsHeadLine != null) {
			g2.drawString(itsHeadLine, xpos, ypos);
			ypos += 15;
			xpos = 5;
		}
		Font descriptionFont = this.getFont();
		g2.setFont(descriptionFont);


		// ============================================================
		// paint the summary
		//
		// if the event is a background event, the summary is painted
		// in a diagonale
		// ============================================================
		if (itsDescription != null) {
			if (showHeader && itsHeadLine == null)
				ypos = HEADER_HEIGHT + 15;
//			if (this.event != null && this.event.isBackground()) {
//				/* ------------------------------------------------------- */
//				// compute angle
//				// first the diagonale, normal Pythagoras
//				try {
//					double diagonale =
//						Math.sqrt(Math.pow(this.getBounds().width, 2)
//								+ Math.pow(this.getBounds().height-ypos-10, 2));
//					/* ------------------------------------------------------- */
//					Double temp = Math.asin(this.getBounds().height / diagonale);
//
//						if (!temp.equals(Double.NaN)) {
//						this.angle = temp;
//					}
//				} catch (Exception e) {}
//				/* ------------------------------------------------------- */
//				g2.rotate(angle-0.3, this.xPosition, this.yPosition);
//				g2.drawString(itsDescription, xpos, ypos+10);
//				// back
//				g2.rotate(-angle+0.3, this.xPosition, this.yPosition);
//				/* ------------------------------------------------------- */
//			} else {
				/* ------------------------------------------------------- */
				int fontHeight =	(int) this.getFont().getSize2D();
				// get the optimal width
				int itsW = g2.getFontMetrics().stringWidth(itsDescription);
				FontMetrics fm = g2.getFontMetrics();
				if (itsW > this.getWidth()) {
					/* ------------------------------------------------------- */
					// check if the width of the frame area has changed and we
					// must find a new fitting length to split the string
					int splitWidth = getWidth()-15;
					if (lineWrap < 0 
							|| fm.stringWidth(itsDescription.substring(0, lineWrap)) < splitWidth - 15 
							|| fm.stringWidth(itsDescription.substring(0, lineWrap)) > splitWidth) {
						/* ------------------------------------------------------- */
						// wrap the lines
						String s = itsDescription;
						this.lineWrap = s.length()-1;
						/* ------------------------------------------------------- */
						// shorten the string as often as its painted length
						// fits into the framearea
						while (fm.stringWidth(s)> splitWidth && lineWrap > -1) {
							s = itsDescription.substring(0, lineWrap);
							lineWrap--;
						}
						if (lineWrap < 0) {
							lineWrap = 0;
						}
						/* ------------------------------------------------------- */
					}
					/* ------------------------------------------------------- */
					// paint the string
					int pos 		= 0;
					int yposString  = ypos;
					
					while (pos < itsDescription.length() && lineWrap > 0) {
						if (pos+lineWrap >= itsDescription.length())
							g2.drawString(itsDescription.substring(pos, itsDescription.length()-1).trim(), xpos, yposString);
						else
							try {
							g2.drawString(itsDescription.substring(pos, pos+lineWrap).trim(), xpos, yposString);
							} catch (Exception e) {
								e.printStackTrace();
							}
						pos = pos+lineWrap;
						yposString = yposString + fontHeight + 5;
					}
					
					/* ------------------------------------------------------- */
				} else {
					// just print
					/* ------------------------------------------------------- */
					g2.drawString(itsDescription, xpos, ypos);
					/* ------------------------------------------------------- */
				}
				/* ------------------------------------------------------- */
//			}
		}
		/* ------------------------------------------------------- */
		// draw end time at the bottom
		Date eTime = null;
		if (this.endTime != null)
			eTime = endTime;
		else {
			try {
				eTime = this.event.getEnd();
			} catch (Exception e) {
			}

		}
		if (eTime != null) {
			/* ------------------------------------------------------- */
			g2.setFont(timeFont);
			g2.drawString(timeFormat.format(eTime) + " ",
					xpos
					+ this.getBounds().width - 40, ypos
					+ this.getBounds().height - 20);
			/* ------------------------------------------------------- */
		}
		/* ------------------------------------------------------- */

		/* ------------------------------------------------------- */
		// draw start time at the top
		if (this.startTime != null) {
			/* ------------------------------------------------------- */
			g2.setFont(timeFont);
			g2.drawString(timeFormat.format(startTime) + " ",
					xpos,
					ypos);
			/* ------------------------------------------------------- */
		}
		/* ------------------------------------------------------- */
		// if an event is moving, draw the new time period at the bottom
		if (this.isMoving) {
			/* ------------------------------------------------------- */

			g2.setFont(timeFont);
//			g2.drawString(movingString, xpos + this.getBounds().width - 85,
//					ypos + this.getBounds().height - 35);
			g2.drawString(getMovingTimeString(), xpos + this.getBounds().width - 85,
					ypos + this.getBounds().height - 35);
			/* ------------------------------------------------------- */
		}

		// ===============================================================
		// draw selection border on this frame area
		//
		// background events do not get this
		// ===============================================================
		if (this.selected && (this.event == null || !this.event.isBackground())) {
			/* ------------------------------------------------------- */
			// ??
			// float dash1[] = { 1.0f };
			// BasicStroke dashed = new BasicStroke(10.0f, BasicStroke.CAP_BUTT,
			// BasicStroke.JOIN_BEVEL, 10.0f, dash1, 0.0f);

			g2.setPaint(selectionColor);
			g2.setStroke(new BasicStroke(1.5f));
			if (this.roundedRectangle)
				g2.draw(new RoundRectangle2D.Double(1, 1, width - 2,
						height - 2, 17, 17));
			else
				g2.draw(new Rectangle2D.Double(1, 1, width - 2, height - 2));
			/* ------------------------------------------------------- */
		}
		if (gbiHeader != null)
			super.paint(gbiHeader);
		super.paint(g2);
		super.paint(gbi);
	}

	/**
	 * @return Returns the border.
	 */
	public boolean isBorder() {
		return border;
	}

	/**
	 * @param border
	 *            The border to set.
	 */
	public void setBorder(boolean border) {
		this.border = border;
	}

	public void setRoundedRectangle(boolean rounded) {
		this.roundedRectangle = rounded;
	}

	public boolean isRoundedRectangle() {
		return this.roundedRectangle;
	}

	/**
	 * Label text placed on the first line in the FrameArea. Example value:
	 * "08:00-11.30"
	 *
	 * @param aHeadLine
	 */
	public void setHeadLine(String aHeadLine) {
		itsHeadLine = aHeadLine;
		if (aHeadLine == null)
			itsHeadLine = "";
	}

	/**
	 * Label text placed below HeadLine in the FrameArea. Example value:
	 * "Meeting with group C"
	 *
	 * @param aDescription
	 */
	public void setDescription(String aDescription) {
		itsDescription = aDescription;
		if (aDescription == null)
			itsDescription = "";
	}

	public void setFontColor(Color aColor) {
		fontColor = aColor;
	}

	public Color getFontColor() {
		return fontColor;
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	
	
	// ========================================================================
	// Methods for the component listener
	// ------------------------------------------------------------------------

	public void componentResized(ComponentEvent e) {
		/* ====================================================== */
		
//		this.lineWrap = -1;
		/* ====================================================== */
	}

	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	
	
	/**
	 * @author martin.heinemann@tudor.lu
	 * 18.07.2007
	 * 09:45:33
	 *
	 *
	 * @version
	 * <br>$Log: FrameArea.java,v $
	 * <br>Revision 1.13  2008/10/27 13:59:50  heine_
	 * <br>fixed invite loop.
	 * <br>happens when the paint width of a character is bigger than the width of the frame area
	 * <br>
	 * <br>Revision 1.12  2008/10/21 15:08:31  heine_
	 * <br>*** empty log message ***
	 * <br>
	 * <br>Revision 1.11  2008/08/12 12:47:28  heine_
	 * <br>fixed some bugs and made code improvements
	 * <br>
	 * <br>Revision 1.10  2008/06/10 13:16:36  heine_
	 * <br>*** empty log message ***
	 * <br>
	 * <br>Revision 1.9  2008/06/09 14:10:09  heine_
	 * <br>*** empty log message ***
	 * <br>
	 * <br>Revision 1.8  2008/05/30 11:36:47  heine_
	 * <br>*** empty log message ***
	 * <br>
	 * <br>Revision 1.7  2007/09/20 07:23:16  heine_
	 * <br>new version commit
	 * <br>
	 * <br>Revision 1.25  2007-09-18 09:52:33  heinemann
	 * <br>*** empty log message ***
	 * <br>
	 * <br>Revision 1.24  2007/08/22 11:58:23  heinemann
	 * <br>*** empty log message ***
	 * <br>
	 * <br>Revision 1.23  2007/08/06 11:21:59  heinemann
	 * <br>*** empty log message ***
	 * <br>
	 * <br>Revision 1.22  2007/07/18 07:27:19  heinemann
	 * <br>line wrap for description string
	 * <br>
	 *   
	 */
	public static interface Listener extends EventListener {
		public void selected(FrameArea source) throws Exception;

		public void mouseOver(FrameArea source) throws Exception;

		public void mouseOut(FrameArea source) throws Exception;

		public void popupMenu(FrameArea source) throws Exception;

		public void moved(Point pos1, Point pos2) throws Exception;
	}

	public void setSelected(boolean flag) {
		this.selected = flag;
		this.setBrightness(flag);
		repaint();
	}

	public boolean isSelected() {
		return this.selected;
	}

	/**
	 * Switches the brightnes of a frame area
	 * to bright or darker
	 *
	 * true - brighter
	 * false - darker
	 * @param b
	 */
	public void setBrightness(boolean b) {
		/* ================================================== */
		if (b)
			this.alphaValue = ALPHA_DEFAULT + SELECT_OFFSET;
		else
			this.alphaValue = ALPHA_DEFAULT - SELECT_OFFSET;
		/* ================================================== */
	}


	/**
	 * Get the icon that is painted in the upper left corner
	 * 
	 * @return
	 */
	public ImageIcon getIcon() {
		return icon;
	}

	/**
	 * Set the icon for the upper left corner
	 * 
	 * @param icon
	 */
	public void setIcon(ImageIcon icon) {
		this.icon = icon;
	}

	
	/**
	 * Get the icon that is painted in the upper right corner
	 * 
	 * @return
	 */
	public ImageIcon getUpperRightIcon() {
		return upperRightIcon;
	}

	/**
	 * Set an icon for the upper right corner.
	 * 
	 * @param upperRightIcon
	 */
	public void setUpperRightIcon(ImageIcon upperRightIcon) {
		this.upperRightIcon = upperRightIcon;
	}
	
	/**
	 * Set the line distance for the diagonal lined background of
	 * background events
	 * 
	 * @param dst
	 */
	public void setLineDistance (int dst) {
		this.lineDistance = dst;
	}
	
	/**
	 * Get the line distance for the diagonal lined background of
	 * background events
	 * @return
	 */
	public int getLineDistance() {
		return this.lineDistance;
	}
	
	
	/**
	 * Show the darker header
	 * 
	 * @param b
	 */
	public void showHeader(boolean b) {
		/* ================================================== */
		this.showHeader = b;
		/* ================================================== */
	}
	
//	@Override
//	public void setBounds(int x, int y, int width, int height) {
//		/* ================================================== */
//		super.setBounds(x, y, width, height);
//		System.out.println(this.toString() + " - " + getBounds());
//		this.boundX 		= x;
//		this.boundY 		= y;
//		this.boundWidth 	= width;
//		this.boundHeight 	= height;
//		if (this.autoCommitBounds)
//			commitBounds();
		/* ================================================== */
//	}
	
	public void commitBounds() {
		/* ================================================== */
		super.setBounds(boundX, boundY, boundWidth, boundHeight);
		/* ================================================== */
	}
	
	  /**
	   * Draw nice lines in the area
	   *
	 * @param g
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param round round rectangle or not
	 */
	public void drawHatchedRect(Graphics g, int x, int y, int width, int height, boolean round)
	  {
		final int DST = this.lineDistance;
		if (round)
			g.drawRoundRect(x, y, width, height, 20, 20);
		else
			g.drawRect(x, y, width, height);

		for (int i = DST; i < width + height; i += DST) {
		  int p1x = (i <= height) ? x : x + i - height;
		  int p1y = (i <= height) ? y + i : y + height;
		  int p2x = (i <= width) ? x + i : x + width;
		  int p2y = (i <= width) ? y : y + i - width;
		  g.drawLine(p1x, p1y, p2x, p2y);
		}

//		for (int i = DST; i < width + height; i += DST) {
//			int p1x = (i <= height) ? x : x + i -height;
//			int p1y = (i <= height) ? y + height - i : y;
//
//			int p2x = (i <= width) ? x + i : x + width;
//			int p2y = (i <= width) ? y + height : y + height - i;
//			g.drawLine(p1x, p1y, p2x, p2y);
//		}


//		// ||||||
//		for (int i = DST; i < width; i +=DST) {
//			int p1x = x + i;
//			int p1y = y;
//			int p2x = p1x;
//			int p2y = y + height;
//			g.drawLine(p1x, p1y, p2x, p2y);
//		}
//		// ----
//		// ----
//		// ----
//		for (int i = DST; i < height; i +=DST) {
//			int p1x = x;
//			int p1y = y + i;
//			int p2x = x + width;
//			int p2y = p1y;
//			g.drawLine(p1x, p1y, p2x, p2y);
//		}


	  }






	/* (non-Javadoc)
	 * @see javax.swing.JComponent#isOptimizedDrawingEnabled()
	 */
	@Override
	public boolean isOptimizedDrawingEnabled() {
		/* ====================================================== */
		return false;
		/* ====================================================== */
	}

	/**
	 * Computes the color of the font. If the bg color is to dark, white is
	 * choosen, otherwise its black.
	 *
	 * @param bg
	 * @return
	 */
	public static Color computeForeground(Color bg) {
		/* ================================================== */
		if (bg == null) {
			return Color.WHITE;
		}
		// Δe = sqrt(pow(ΔL) + pow(Δa) + pow(Δb))
		//
		// a well trained human can detect colors that have a Δe=2
		// so we must choose a Δe that fits the capability of John Doe
		// ============================================================
		// first we must convert the RGB colors to LAB

		// LAB of white
		int[] labWhite = new int[3];
		int[] labBlack = new int[3];
		int[] labBg = new int[3];
		/* ------------------------------------------------------- */
		// lab of white
		rgb2lab(Color.WHITE.getRed(), Color.WHITE.getGreen(), Color.WHITE
				.getBlue(), labWhite);
		// lab of black
		rgb2lab(Color.BLACK.getRed(), Color.BLACK.getGreen(), Color.BLACK
				.getBlue(), labBlack);
		// lab of bg
		rgb2lab(bg.getRed(), bg.getGreen(), bg.getBlue(), labBg);
		/* ------------------------------------------------------- */

		int deltaBgWhite = deltaE(labBg, labWhite);
		int deltaBgBlack = deltaE(labBg, labBlack);

		// choose the biggest deltaE
		if (deltaBgBlack > deltaBgWhite)
			return Color.BLACK;
		
		return Color.WHITE;
		/* ================================================== */
	}

	private static int deltaE(int[] lab1, int[] lab2) {
		/* ================================================== */
		int deltaE = 0;

		double deltaL = lab2[0] - lab1[0];
		double deltaA = lab2[1] - lab1[1];
		double deltaB = lab2[2] - lab1[2];

		deltaE = (int) Math.sqrt(pow(deltaL, 2.0) + pow(deltaA, 2.0)
				+ pow(deltaB, 2.0));

		return deltaE;
		/* ================================================== */
	}

	/**
	 * Convert RGB color to LAB color mode taken from
	 * http://www.f4.fhtw-berlin.de/~barthel/ImageJ/ColorInspector//HTMLHelp/farbraumJava.htm
	 *
	 * @param R
	 * @param G
	 * @param B
	 * @param lab
	 */
	public static void rgb2lab(int R, int G, int B, int[] lab) {
		// http://www.brucelindbloom.com

		float r, g, b, X, Y, Z, fx, fy, fz, xr, yr, zr;
		float Ls, as, bs;
		float eps = 216.f / 24389.f;
		float k = 24389.f / 27.f;

		float Xr = 0.964221f; // reference white D50
		float Yr = 1.0f;
		float Zr = 0.825211f;

		// RGB to XYZ
		r = R / 255.f; // R 0..1
		g = G / 255.f; // G 0..1
		b = B / 255.f; // B 0..1

		// assuming sRGB (D65)
		if (r <= 0.04045)
			r = r / 12;
		else
			r = (float) Math.pow((r + 0.055) / 1.055, 2.4);

		if (g <= 0.04045)
			g = g / 12;
		else
			g = (float) Math.pow((g + 0.055) / 1.055, 2.4);

		if (b <= 0.04045)
			b = b / 12;
		else
			b = (float) Math.pow((b + 0.055) / 1.055, 2.4);

		X = 0.436052025f * r + 0.385081593f * g + 0.143087414f * b;
		Y = 0.222491598f * r + 0.71688606f * g + 0.060621486f * b;
		Z = 0.013929122f * r + 0.097097002f * g + 0.71418547f * b;

		// XYZ to Lab
		xr = X / Xr;
		yr = Y / Yr;
		zr = Z / Zr;

		if (xr > eps)
			fx = (float) Math.pow(xr, 1 / 3.);
		else
			fx = (float) ((k * xr + 16.) / 116.);

		if (yr > eps)
			fy = (float) Math.pow(yr, 1 / 3.);
		else
			fy = (float) ((k * yr + 16.) / 116.);

		if (zr > eps)
			fz = (float) Math.pow(zr, 1 / 3.);
		else
			fz = (float) ((k * zr + 16.) / 116);

		Ls = (116 * fy) - 16;
		as = 500 * (fx - fy);
		bs = 200 * (fy - fz);

		lab[0] = (int) (2.55 * Ls + .5);
		lab[1] = (int) (as + .5);
		lab[2] = (int) (bs + .5);
	}
}

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

import java.awt.Font;
import java.awt.FontMetrics;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.swing.JLabel;

import bizcal.util.TextUtil;


/**
 * @author wmjnkal
 *
*/

public class StringLengthFormater
{

	public static String formatDateString(Date aDate, Font aFont, int aWidth, ArrayList aPatternList)
	throws Exception
	{
		boolean fontFits = false;
		String string = "";
		if(aPatternList==null)
		{
			aPatternList = new ArrayList();
			aPatternList.add("EEEE");
			aPatternList.add("EEE");
			aPatternList.add("EE");
			aPatternList.add("E");
		}
		FontMetrics currentMetrics = new JLabel().getFontMetrics(aFont);
		DateFormat format = new SimpleDateFormat();		
		
		int i = 0;
		while(!fontFits)
		{
			if(aPatternList.size() > i)
			{
				format = new SimpleDateFormat(aPatternList.
						get(i).toString(), Locale.getDefault());
				string = TextUtil.formatCase(format.format(aDate));
			}
			else
			{
				string = TextUtil.formatCase(format.format(aDate)).substring(0,1);
				fontFits = true;
			}
			if(currentMetrics.stringWidth(string) <= aWidth)
			{
				fontFits = true;
			}
			i++;						
		}	
		return string;
	}
	
	public static String getCommonDateFormat(List aDateList, Font aFont, int aWidth, ArrayList aPatternList)
	throws Exception
	{
		String string = "";
		int minFormat = 0;
		if(aPatternList==null)
		{
			aPatternList = new ArrayList();
			aPatternList.add("EEEE");
			aPatternList.add("EEE");
			aPatternList.add("EE");
			aPatternList.add("E");
		}
		FontMetrics currentMetrics = new JLabel().getFontMetrics(aFont);
		DateFormat format = new SimpleDateFormat();		
		
		Iterator iter = aDateList.iterator();
		while(iter.hasNext())
		{
			Date date = (Date) iter.next();
			int i = 0;
			boolean fontFits = false;
			while(!fontFits)
			{
				if(aPatternList.size() > i)
				{
					format = new SimpleDateFormat((String) aPatternList.
							get(i), Locale.getDefault());
					string = TextUtil.formatCase(format.format(date));
				}
				else
				{
					string = TextUtil.formatCase(format.format(date)).substring(0,1);
					fontFits = true;
				}
				if(currentMetrics.stringWidth(string) <= aWidth)
				{
					fontFits = true;
					if(i>minFormat)
						minFormat = i;
				}
				i++;						
			}	
		}
		if(minFormat < aPatternList.size())
			return (String) aPatternList.get(minFormat);
		else
			return (String) aPatternList.get(aPatternList.size()-1);
	}
	
	/**
	 * Tries combination of name in the following order:
	 * aName:"Per Anders Svensson"
	 * 1."Per Anders Svensson"
	 * 2."Per A Svensson"
	 * 3."Per A S"
	 * 4."PAS" 
	 * 
	 * @param aName
	 * @param aFont
	 * @param aWidth
	 * @return
	 * @throws Exception
	 */
	public static String formatNameString(String aName, Font aFont, int aWidth)
	throws Exception
	{
		if(aName == null || "".equals(aName))
			return "";
		String string = "";
		StringTokenizer tok = new StringTokenizer(aName);
		String first = "";
		String last = "";
		ArrayList<String> middleList = new ArrayList<String>();
//		String middles = new String();
		StringBuffer middles = new StringBuffer();
		int noOfNames = tok.countTokens();
		int i = 1;
		while(tok.hasMoreTokens())
		{
			if(i==1)
				first = tok.nextToken();
			else if(i==noOfNames)
				last = tok.nextToken();
			else
				middleList.add(tok.nextToken());
			i++;
		}
		
		FontMetrics currentMetrics = new JLabel().getFontMetrics(aFont);
		
		string = first;
//		Iterator iter = middleList.iterator();
//		while(iter.hasNext())
//			middles = " " + middles + iter.next();
		/* ------------------------------------------------------- */
		for (String tokenString : middleList) {
			/* ------------------------------------------------------- */
			middles.insert(0, " ");
			middles.append(tokenString);
			/* ------------------------------------------------------- */
		}
		/* ------------------------------------------------------- */
		string = string + middles; 
		string = string + " " + last;
		
		if(currentMetrics.stringWidth(string) <= aWidth)
			return string;
				
		string = first;
		/* ------------------------------------------------------- */
//		middles = "";
		middles = new StringBuffer();
		/* ------------------------------------------------------- */
//		iter = middleList.iterator();
//		while(iter.hasNext())
//			middles = " " + middles + ((String)iter.next()).substring(0,1);
		/* ------------------------------------------------------- */
		for (String tokenString : middleList) {
			/* ------------------------------------------------------- */
			middles.insert(0, " ");
			middles.append(tokenString.substring(0, 1));
			/* ------------------------------------------------------- */
		}
		/* ------------------------------------------------------- */
		string = string + middles; 
		string = string + " " + last;
		
		if(currentMetrics.stringWidth(string) <= aWidth)
			return string;
		/* ------------------------------------------------------- */
		string = first;
		/* ------------------------------------------------------- */
//		middles = "";
		middles = new StringBuffer();
		/* ------------------------------------------------------- */
//		iter = middleList.iterator();
//		while(iter.hasNext())
//			middles = " " + middles + ((String)iter.next()).substring(0,1);
		/* ------------------------------------------------------- */
		for (String tokenString : middleList) {
			/* ------------------------------------------------------- */
			middles.insert(0, " ");
			middles.append(tokenString.substring(0, 1));
			/* ------------------------------------------------------- */
		}
		/* ------------------------------------------------------- */
		
		string = string + middles; 
		if (last.length() > 0)
			string = string + " " + last.substring(0,1);
		
		if(currentMetrics.stringWidth(string) <= aWidth)
			return string;
		
		string = first.substring(0,1);
		/* ------------------------------------------------------- */
//		middles = "";
		middles = new StringBuffer();
		/* ------------------------------------------------------- */
//		iter = middleList.iterator();
//		while(iter.hasNext())
//			middles = middles + ((String)iter.next()).substring(0,1);
		/* ------------------------------------------------------- */
		for (String tokenString : middleList) {
			/* ------------------------------------------------------- */
			middles.insert(0, " ");
			middles.append(tokenString.substring(0, 1));
			/* ------------------------------------------------------- */
		}
		/* ------------------------------------------------------- */
		string = string + middles;
		if (last.length() > 0)
			string = string + last.substring(0,1);
		
		if(currentMetrics.stringWidth(string) <= aWidth)
			return string;
		
		string = aName.substring(0,1);
		return string;
	}
}

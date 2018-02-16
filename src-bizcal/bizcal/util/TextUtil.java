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
package bizcal.util;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Fredrik Bertilsson
 *
 */
public class TextUtil 
{
	public static String formatCase(String txt)
	{
	    if (txt == null)
	        return null;
		if (txt.toUpperCase().equals(txt))
			txt = txt.toLowerCase();
		return txt.substring(0,1).toUpperCase() + txt.substring(1);
	}
	
	public static String translate(String txt)
		throws Exception
	{
		if (txt == null)
			return null;
		return ResourceBundle.getBundle("bizcal.common.Bundle", Locale.getDefault()).getString(txt);
	}

}

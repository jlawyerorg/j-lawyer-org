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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a number of values.
 *
 * @author Fredrik Bertilsson
 */
public class Tuple
    implements Comparable, Serializable
{
    private static final long serialVersionUID = 1L;
    
    private List itsContent;
    private boolean itsNullFlag = false;

    public Tuple()
    {
    	itsContent = new ArrayList();
    }
    
    public Tuple(int size)
    {
        itsContent = new ArrayList(size);
    }

    public Tuple(Tuple tuple)
    {
        itsContent = new ArrayList(tuple.itsContent);
    }

    public Tuple(Object elementA)
    {
        itsContent = new ArrayList(1);
        add(elementA);
    }

    public Tuple(Object aElementA, Object aElementB)
    {
        itsContent = new ArrayList(2);
        add(aElementA);
        add(aElementB);
    }

    public Tuple(Object aElementA, Object aElementB, Object aElementC)
    {
        itsContent = new ArrayList(3);
        add(aElementA);
        add(aElementB);
        add(aElementC);
    }

    public Tuple(Object aElementA,
            Object aElementB,
            Object aElementC,
            Object aElementD)
    {
        itsContent = new ArrayList(4);
        add(aElementA);
        add(aElementB);
        add(aElementC);
        add(aElementD);
    }

    public Tuple(Object aElementA,
            Object aElementB,
            Object aElementC,
            Object aElementD,
            Object aElementE)
    {
        itsContent = new ArrayList(5);
        add(aElementA);
        add(aElementB);
        add(aElementC);
        add(aElementD);
        add(aElementE);
    }
    
    public Tuple(Tuple tuple, Object element)
    {
    	itsContent = new ArrayList(tuple.itsContent);
    	itsContent.add(element);
    }

    private Tuple(Object[] content)
    {
        itsContent = new ArrayList(content.length);
        for (int i=0; i < content.length; i++)
            itsContent.add(content[i]);
    }

    public void setValueAt(int index, Comparable aElement)
    throws Exception
    {
        itsContent.set(index, aElement);
    }

    public boolean equals(Object anOther)
    {
        if (anOther instanceof Tuple) {
            Tuple other = (Tuple) anOther;
            Object element;
            Object otherElement;
            int tmp;
            int iOther = 0;
            for (int i=0; i < size(); i++) {
                element = elementAt(i);
                if (other.size() == iOther)
                    return false;
                otherElement = other.elementAt(iOther);
                if (!NullSafe.equals(element, otherElement))
                    return false;
                iOther++;
            }
            if (iOther < other.size())
                return false;
            return true;
        }
        return false;
    }

    public int compareTo(Object anOther)
    {
        if (anOther instanceof Tuple) {
            Tuple other = (Tuple) anOther;
            Comparable element;
            Comparable otherElement;
            int tmp;
            int iOther = 0;
            for (int i=0; i < size(); i++) {
                element = (Comparable) elementAt(i);
                if (other.size() == iOther)
                    return 1;
                otherElement = (Comparable) other.elementAt(iOther);
                if (element == null && otherElement != null)
                    return -1;
                if (element == null && otherElement == null)
                    return 0;
                if (element != null && otherElement == null)
                    return 1;
                tmp = element.compareTo(otherElement);
                if (tmp != 0) return tmp;
                iOther++;
            }
            if (iOther < other.size())
                return 1;
            return 0;
        }
        return -1;
    }

    public boolean hasNullValues()
    {
        return itsNullFlag;
    }

    public Object elementAt(int aIndex)
    {
        return itsContent.get(aIndex);
    }

    public int size()
    {
        return itsContent.size();
    }

    public void add(Object aElement)
    {
        if (aElement == null)
            itsNullFlag = true;
        itsContent.add(aElement);
    }

    /*public String toString()
    {
        StringBuffer str = new StringBuffer();
        str.append("(");
        for (int i=0; i < size(); i++) {
            str.append(itsContent[i]);
            if (i+1 < size())
                str.append(", ");
        }
        str.append(")");
        return str.toString();
    }*/

    public int hashCode()
    {
        int result = 0;
        for (int i=0; i < size(); i++)
            if (itsContent.get(i) != null)
                result += itsContent.get(i).hashCode();
        return result;
    }

    public String toString()
    {
        StringBuffer str = new StringBuffer();
        for (int i=0; i < itsContent.size(); i++) {
            if (itsContent.get(i) == null)
                str.append("null");
            else
                str.append(itsContent.get(i).toString());
            str.append("#");
        }
        return str.toString();
    }

    public List toList()
    {
        return itsContent;
    }
    
}

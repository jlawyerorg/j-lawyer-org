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
/**
 * @author Martin Heinemann martin.heinemann@tudor.lu
 *
 *
 *
 * @version
 * <br>$Log: ObservableEventList.java,v $
 * <br>Revision 1.3  2008/08/12 12:47:28  heine_
 * <br>fixed some bugs and made code improvements
 * <br>
 * <br>Revision 1.2  2007/09/20 07:23:16  heine_
 * <br>new version commit
 * <br>
 * <br>Revision 1.4  2007/06/20 12:08:24  heinemann
 * <br>*** empty log message ***
 * <br>
 * <br>Revision 1.3  2007/06/14 13:31:25  heinemann
 * <br>*** empty log message ***
 * <br>
 * <br>Revision 1.2  2007/06/04 11:36:42  heinemann
 * <br>first try of synchronisation
 * <br>
 * <br>Revision 1.1  2007/05/25 13:43:59  heinemann
 * <br>pres-weekend checkin
 * <br>
 *
 */
package lu.tudor.santec.bizcal.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Observable;

import bizcal.common.Event;

/**
 * Observable ArrayList for the EventModel
 *
 * @author martin.heinemann@tudor.lu
 * 22.05.2007
 * 14:06:12
 *
 *
 * @version
 * <br>$Log: ObservableEventList.java,v $
 * <br>Revision 1.3  2008/08/12 12:47:28  heine_
 * <br>fixed some bugs and made code improvements
 * <br>
 * <br>Revision 1.2  2007/09/20 07:23:16  heine_
 * <br>new version commit
 * <br>
 * <br>Revision 1.4  2007/06/20 12:08:24  heinemann
 * <br>*** empty log message ***
 * <br>
 * <br>Revision 1.3  2007/06/14 13:31:25  heinemann
 * <br>*** empty log message ***
 * <br>
 * <br>Revision 1.2  2007/06/04 11:36:42  heinemann
 * <br>first try of synchronisation
 * <br>
 * <br>Revision 1.1  2007/05/25 13:43:59  heinemann
 * <br>pres-weekend checkin
 * <br>
 *
 */
public class ObservableEventList extends Observable implements List<Event> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


	private ArrayList<Event> list = new ArrayList<Event>();


	private boolean notifyEnabled = true;


	public synchronized boolean add(Event o) {
		/* ====================================================== */
		boolean b = list.add(o);
		setChanged();
		notifyObservers();
		return b;
		/* ====================================================== */
	}


	public synchronized void add(int index, Event element) {
		/* ====================================================== */
		list.add(index, element);
		setChanged();
		notifyObservers();
		/* ====================================================== */
	}


	public synchronized boolean addAll(Collection<? extends Event> c) {
		/* ====================================================== */
		boolean b =  list.addAll(c);
		setChanged();
		notifyObservers();

		return b;
		/* ====================================================== */
	}


	public synchronized boolean addAll(int index, Collection<? extends Event> c) {
		/* ====================================================== */
		setChanged();
		notifyObservers();
		return list.addAll(index, c);
		/* ====================================================== */
	}


	public synchronized void clear() {
		/* ====================================================== */
		setChanged();
		list.clear();
		notifyObservers();
		/* ====================================================== */
	}


	public synchronized boolean contains(Object o) {
		/* ====================================================== */
		return list.contains(o);
		/* ====================================================== */
	}


	public synchronized boolean containsAll(Collection<?> c) {
		/* ====================================================== */
		return list.containsAll(c);
		/* ====================================================== */
	}


	public synchronized Event get(int index) {
		/* ====================================================== */
		return list.get(index);
		/* ====================================================== */
	}


	public synchronized int indexOf(Object o) {
		/* ====================================================== */
		return list.indexOf(o);
		/* ====================================================== */
	}


	public synchronized boolean isEmpty() {
		/* ====================================================== */
		return list.isEmpty();
		/* ====================================================== */
	}


	public synchronized Iterator<Event> iterator() {
		/* ====================================================== */
		return list.iterator();
		/* ====================================================== */
	}


	public synchronized int lastIndexOf(Object o) {
		/* ====================================================== */
		return list.lastIndexOf(o);
		/* ====================================================== */
	}


	public synchronized ListIterator<Event> listIterator() {
		/* ====================================================== */
		return list.listIterator();
		/* ====================================================== */
	}


	public synchronized ListIterator<Event> listIterator(int index) {
		/* ====================================================== */
		return list.listIterator(index);
		/* ====================================================== */
	}


	public synchronized boolean remove(Object o) {
		/* ====================================================== */
		if (notifyEnabled) {
			setChanged();
			notifyObservers();
		}
		return list.remove(o);
		/* ====================================================== */
	}


	public synchronized Event remove(int index) {
		/* ====================================================== */
		setChanged();
		notifyObservers();
		return list.remove(index);
		/* ====================================================== */
	}


	public synchronized boolean removeAll(Collection<?> c) {
		/* ====================================================== */
		setChanged();
		notifyObservers();
		return list.removeAll(c);
		/* ====================================================== */
	}


	public synchronized boolean retainAll(Collection<?> c) {
		/* ====================================================== */
		setChanged();
		notifyObservers();
		return list.retainAll(c);
		/* ====================================================== */
	}


	public synchronized Event set(int index, Event element) {
		/* ====================================================== */
		setChanged();
		notifyObservers();
		return list.set(index, element);
		/* ====================================================== */
	}


	public synchronized int size() {
		/* ====================================================== */
		return list.size();
		/* ====================================================== */
	}


	public synchronized List<Event> subList(int fromIndex, int toIndex) {
		/* ====================================================== */
		return list.subList(fromIndex, toIndex);
		/* ====================================================== */
	}


	public synchronized Object[] toArray() {
		/* ====================================================== */
		return list.toArray();
		/* ====================================================== */
	}


	public synchronized <T> T[] toArray(T[] a) {
		/* ====================================================== */
		return list.toArray(a);
		/* ====================================================== */
	}


	public synchronized void trigger() {
		/* ====================================================== */
		setChanged();
		notifyObservers();
		/* ====================================================== */
	}


	public synchronized void setEnableNotify(boolean b) {
		/* ================================================== */
		this.notifyEnabled = b;
		/* ================================================== */
	}

}

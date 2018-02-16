package lu.tudor.santec.bizcal.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Class for faster performance tests
 * 
 * @author martin.heinemann@tudor.lu
 * 25.02.2008
 * 12:53:03
 *
 *
 * @version
 * <br>$Log: TimeTracker.java,v $
 * <br>Revision 1.1  2008/06/12 13:04:36  heine_
 * <br>*** empty log message ***
 * <br>
 * <br>Revision 1.1  2008-02-25 15:47:18  heinemann
 * <br>Initial checkin
 * <br>
 *   
 */
public class TimeTracker {
	
	
	private static LinkedHashMap<String, ArrayList<Long>> lapMap = new LinkedHashMap<String, ArrayList<Long>>();
	
	
	/**
	 * 
	 */
	private TimeTracker() {
		/* ================================================== */

		/* ================================================== */
	}
	
	/**
	 * 
	 * @param identifier for the new perfomance race
	 */
	public static void start(String identifier) {
		/* ================================================== */
		ArrayList<Long> laps = new ArrayList<Long>(1);
		laps.add(System.currentTimeMillis());
		lapMap.put(identifier, laps);
		/* ================================================== */
	}
	
	/**
	 * Triggers a lap for the time tracking.
	 * 
	 * @param identifier
	 */
	public static void lapTime(String identifier) {
		/* ================================================== */
		lapTime(identifier, "");
		/* ================================================== */
	}
	
	/**
	 * @param identifier
	 * @param hint text to be placed in the output for simplification
	 */
	public static void lapTime(String identifier, String hint) {
		/* ================================================== */
		ArrayList<Long> laps = lapMap.get(identifier);
		try {
			laps.add(System.currentTimeMillis());
			System.out.println("TimeTrack:: "+identifier +" - " + hint +" : last operation took " 
								+ (laps.get(laps.size()-1) - laps.get(laps.size()-2)) + "ms");
//			if ("17".equals(hint))
//				throw new Exception("Scheisse hier");
		} catch (Exception e) {
//			e.printStackTrace();
//			// is faster than if
//			// TODO: handle exception
//			String f = null;
//			f.substring(7);
		}
		/* ================================================== */
	}
	
	/**
	 * Make a last track and clean the hashmap
	 * 
	 * @param identifier
	 */
	public static void finish(String identifier) {
		/* ================================================== */
		lapTime(identifier);
		ArrayList<Long> laps = lapMap.get(identifier);
		System.out.println("TimeTrack:: "+identifier +": Whole process took " 
				+ (laps.get(laps.size()-1) - laps.get(0))+ "ms");
		System.out.println("-------------------------------------------");
		/* ------------------------------------------------------- */
		// make clean
		/* ------------------------------------------------------- */
		lapMap.remove(identifier);
		/* ================================================== */
	}
	
	
}

/* *********************************************************************** *
 * project: org.matsim.*
 * DpDurControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
/**
 * 
 */
package playground.yu;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;

/**
 * @author ychen
 *
 */
public class DpDurControler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = Gbl.createConfig(args);
		DpDurWriter ddw = new DpDurWriter(config.events().getOutputFile());
		Events events = new Events();
		events.addHandler(ddw);// TODO...

		System.out.println("  reading the eventsfile (TXTv1) ...");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		System.out.println("  done.");
		
		ddw.writeMatrix();
		ddw.closefile();
	}
}

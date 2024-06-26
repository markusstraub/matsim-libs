/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.vsp.openberlinscenario.cemdap.input;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * @author dziemke
 */
public class CommuterFileReaderV2 {
	private static final Logger LOG = LogManager.getLogger(CommuterFileReaderV2.class);
	
	private Map<String, Map<String, CommuterRelationV2>> relationsMap = new HashMap<>();
	
	
	public static void main(String[] args) {
		String commuterFileOutgoing = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/pendlerstatistik_2009/Brandenburg_2009/Teil1BR2009Ga.txt";
		String delimiter = "\t";
		
		CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing, delimiter);
		
		String origin = "12051000"; // "Brandenburg an der Havel, St."
		String destination = "11000000"; // "Berlin, Stadt"
		LOG.info("Simple test: 1513 = " + commuterFileReader.getRelationsMap().get(origin).get(destination).getTrips());
	}
	
	
	public CommuterFileReaderV2(String commuterFileOutgoing, String delimiter) {
		readFile(commuterFileOutgoing, delimiter);
	}
	

	private void readFile(final String filename, String delimiter) {
		LOG.info("Start reading " + filename);
				
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterRegex(delimiter);
		new TabularFileParser().parse(tabFileParserConfig, new TabularFileHandler() {
        	String origin = null;
        	String destination = null;

            @Override
            public void startRow(String[] row) {
            	if (row.length > 2) {
            		if (row[0].length() == 8) { // New origin
            			origin = row[0];
            			LOG.info("New origin set to: " + origin);
            			return;
            			// Next check for destinations
            		} else if (row[2].length() == 8) { // New destiantion
            			destination = row[2];
            			LOG.info("New destination set to: " + destination);
            			LOG.info(origin + " -> " + destination + ": All commuters: " + row[4] + "; males: " + row[5] + "; females: " + row[6]);
            			Integer tripsMale;
            			Integer tripsFemale;

            			if (row[5].equals("X")) {
            				tripsMale = null;
            			} else {
            				tripsMale = Integer.parseInt(row[5]);
            			}

            			if (row[6].equals("X")) {
            				tripsFemale = null;
            			} else {
            				tripsFemale = Integer.parseInt(row[6]);
            			}

            			process(origin, destination, Integer.parseInt(row[4]), tripsMale, tripsFemale);
            			return;
            		} else { // A line that is neither has a new origin nor a new destination
            			return;
            		}
            	}
            };
		});
	}
	
	
	private void process(String origin, String destination, Integer tripsAll, Integer tripsMale, Integer tripsFemale) {
		CommuterRelationV2 commuterRelation = new CommuterRelationV2(origin, destination, tripsAll, tripsMale, tripsFemale);
		
		if (!this.relationsMap.containsKey(origin)) {
			Map<String, CommuterRelationV2> originMap = new HashMap<>();
			this.relationsMap.put(origin, originMap);
		}
		Map<String, CommuterRelationV2> originMap = this.relationsMap.get(origin);
		originMap.put(destination, commuterRelation);
	}
	
	
	public Map<String, Map<String, CommuterRelationV2>> getRelationsMap() {
		return this.relationsMap;
	}
	
	
	public Set<String> getMunicipalities() {
		return this.relationsMap.keySet();
	}
}
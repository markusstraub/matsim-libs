/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.aas;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.andreas.aas.modules.AbstractAnalyisModule;
import playground.andreas.aas.modules.multiAnalyzer.MultiAnalyzer;
import playground.andreas.aas.modules.ptTripAnalysis.BvgTripAnalysisRunnerV4;
import playground.andreas.aas.modules.spatialAveragingLinkDemand.SpatialAveragingForLinkDemand;

/**
 * 
 * @author aneumann
 *
 */
public class AasRunner {
	
	private final static Logger log = Logger.getLogger(AasRunner.class);
	
	private final String baseFolder;
	private final String iterationOutputDir;
	private final String eventsFile;
	private final ScenarioImpl scenario;
	private final Set<Feature> shapeFile;
	
	private final List<AbstractAnalyisModule> anaModules = new LinkedList<AbstractAnalyisModule>();

	

	public AasRunner(ScenarioImpl scenario, String baseFolder, String iterationOutputDir, String eventsFile, Set<Feature> shapeFile) {
		this.baseFolder = baseFolder;
		this.iterationOutputDir = iterationOutputDir;
		this.eventsFile = eventsFile;
		this.scenario = scenario;
		this.shapeFile = shapeFile;
	}

	public void init(String aasRunnerConfigFile){
		log.info("This is currently not implemented. Initializing all modules with defaults.");
		String ptDriverPrefix = "pt_";
		
		
		// END of configuration file
		
		// TOOD BvgAnalysis is a SpecialCase. Use TTtripAnalysis here... /dr Nov '12
		BvgTripAnalysisRunnerV4 ptAna = new BvgTripAnalysisRunnerV4(ptDriverPrefix);
		ptAna.init(this.scenario, this.shapeFile);
		this.anaModules.add(ptAna);
		
		MultiAnalyzer mA = new MultiAnalyzer(ptDriverPrefix);
		mA.init(this.scenario);
		this.anaModules.add(mA);
		
		SpatialAveragingForLinkDemand sAD = new SpatialAveragingForLinkDemand(ptDriverPrefix);
		sAD.init(this.scenario, this.shapeFile, 1);
		this.anaModules.add(sAD);
		
		
		// END ugly code - Initialization needs to be configurable
	}

	public void preProcess(){
		for (AbstractAnalyisModule module : this.anaModules) {
			module.preProcessData();
		}
	}
	
	public void run(){
		EventsManager eventsManager = EventsUtils.createEventsManager();
		for (AbstractAnalyisModule module : this.anaModules) {
			for (EventHandler handler : module.getEventHandler()) {
				eventsManager.addHandler(handler);
			}
		}
		
		// TODO: what if there are no events in the directory? bk oct'12
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
		reader.parse(this.eventsFile);
	}
	
	public void postProcess(){
		for (AbstractAnalyisModule module : this.anaModules) {
			module.postProcessData();
		}
	}
	
	public void writeResults(){
		String outputDir = this.iterationOutputDir + "aasRunner" + "/";
		log.info("Generating output directory " + outputDir);
		new File(outputDir).mkdir();
		
		for (AbstractAnalyisModule module : this.anaModules) {
			String moduleOutputDir = outputDir + module.getName() + "/";
			log.info("Writing results of module " + module.getName() + " to " + moduleOutputDir + "...");
			new File(moduleOutputDir).mkdir();
			module.writeResults(moduleOutputDir);
			log.info("... finished");
		}
		
		log.info("All output written");
		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();
	}
}

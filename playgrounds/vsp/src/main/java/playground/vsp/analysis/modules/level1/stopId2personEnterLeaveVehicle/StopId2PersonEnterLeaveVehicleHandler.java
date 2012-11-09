/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.vsp.analysis.modules.level1.stopId2personEnterLeaveVehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;

/**
 * Collects <code>PersonEntersVehicleEvent</code> and <code>PersonLeavesVehicleEventHandler</code> for each stop id.
 *
 * @author ikaddoura, aneumann
 *
 */
public class StopId2PersonEnterLeaveVehicleHandler implements VehicleArrivesAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{

	private final Logger log = Logger.getLogger(StopId2PersonEnterLeaveVehicleHandler.class);
	private final Level logLevel = Level.WARN;
	private String ptDriverPrefix;
	
	private Map<Id, Id> vehId2stopIdMap = new TreeMap<Id, Id>();
	private Map<Id, List<PersonEntersVehicleEvent>> stopId2PersonEnterEventMap = new TreeMap<Id, List<PersonEntersVehicleEvent>>();
	private Map<Id, List<PersonLeavesVehicleEvent>> stopId2PersonLeaveEventMap = new TreeMap<Id, List<PersonLeavesVehicleEvent>>();

	public StopId2PersonEnterLeaveVehicleHandler(String ptDriverPrefix) {
		this.log.setLevel(this.logLevel);
		this.ptDriverPrefix = ptDriverPrefix;
	}

	/**
	 * @return A map containing all <code>PersonEntersVehicleEvent</code> sorted by stop id
	 */
	public Map<Id, List<PersonEntersVehicleEvent>> getStopId2PersonEnterEventMap() {
		return this.stopId2PersonEnterEventMap;
	}

	/**
	 * @return A map containing all <code>PersonLeavesVehicleEvent</code> sorted by stop id
	 */
	public Map<Id, List<PersonLeavesVehicleEvent>> getStopId2PersonLeaveEventMap() {
		return this.stopId2PersonLeaveEventMap;
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehId2stopIdMap.put(event.getVehicleId(), event.getFacilityId());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (event.getPersonId().toString().startsWith(ptDriverPrefix)) {
			// pt driver
		} else {
			if (this.vehId2stopIdMap.containsKey(event.getVehicleId())){
				// entering a public vehicle
				if(this.stopId2PersonEnterEventMap.get(this.vehId2stopIdMap.get(event.getVehicleId())) == null){
					this.stopId2PersonEnterEventMap.put(this.vehId2stopIdMap.get(event.getVehicleId()), new ArrayList<PersonEntersVehicleEvent>());
				}
				this.stopId2PersonEnterEventMap.get(this.vehId2stopIdMap.get(event.getVehicleId())).add(event);
				this.log.debug("Added event to stop " + this.vehId2stopIdMap.get(event.getVehicleId()) + " event " + event);
				
			} else {
				// no public vehicle
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (event.getPersonId().toString().startsWith(ptDriverPrefix)) {
			// pt driver
		} else {
			if (this.vehId2stopIdMap.containsKey(event.getVehicleId())){
				// entering a public vehicle
				if(this.stopId2PersonLeaveEventMap.get(this.vehId2stopIdMap.get(event.getVehicleId())) == null){
					this.stopId2PersonLeaveEventMap.put(this.vehId2stopIdMap.get(event.getVehicleId()), new ArrayList<PersonLeavesVehicleEvent>());
				}
				this.stopId2PersonLeaveEventMap.get(this.vehId2stopIdMap.get(event.getVehicleId())).add(event);
				this.log.debug("Added event to stop " + this.vehId2stopIdMap.get(event.getVehicleId()) + " event " + event);
				
			} else {
				// no public vehicle
			}
		}
	}
	
	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");
	}

}

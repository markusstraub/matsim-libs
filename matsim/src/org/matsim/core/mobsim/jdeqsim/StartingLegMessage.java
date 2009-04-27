/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.jdeqsim;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.events.ActEndEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.BasicEventImpl;

/**
 * The micro-simulation internal handler for starting a leg.
 *
 * @author rashid_waraich
 */
public class StartingLegMessage extends EventMessage {

	public StartingLegMessage(Scheduler scheduler, Vehicle vehicle) {
		super(scheduler, vehicle);
		priority = SimulationParameters.PRIORITY_DEPARTUARE_MESSAGE;
	}

	@Override
	public void handleMessage() {
		// if current leg is in car mode, then enter request in first road
		if (vehicle.getCurrentLeg().getMode().equals(TransportMode.car)) {
			Road road = Road.getRoad(vehicle.getCurrentLink().getId().toString());
			road.enterRequest(vehicle, getMessageArrivalTime());
		} else {
			// move to first link in next leg and schedule an end leg message
			vehicle.moveToFirstLinkInNextLeg();
			Road road = Road.getRoad(vehicle.getCurrentLink().getId().toString());

			vehicle.scheduleEndLegMessage(getMessageArrivalTime() + vehicle.getCurrentLeg().getTravelTime(),
					road);
		}
	}

	public void processEvent() {
		BasicEventImpl event = null;

		// schedule ActEndEvent
		event = new ActEndEvent(this.getMessageArrivalTime(), vehicle.getOwnerPerson(),
				vehicle.getCurrentLink(), vehicle.getPreviousActivity());
		SimulationParameters.getProcessEventThread().processEvent(event);

		// schedule AgentDepartureEvent
		event = new AgentDepartureEvent(this.getMessageArrivalTime(), vehicle.getOwnerPerson(), vehicle.getCurrentLink(), vehicle.getCurrentLeg());

		SimulationParameters.getProcessEventThread().processEvent(event);

	}

}

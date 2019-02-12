/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.dvrp;

import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.ev.data.ElectricVehicle;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.OhdeSlaskiAuxEnergyConsumption;

import com.google.inject.Injector;

public class DvrpAuxConsumptionFactory implements AuxEnergyConsumption.Factory {
	@Inject
	private Injector injector;

	private final String mode;
	private final DoubleSupplier temperatureProvider;
	private final Predicate<DvrpVehicle> turnedOnPredicate;

	public DvrpAuxConsumptionFactory(String mode, DoubleSupplier temperatureProvider,
			Predicate<DvrpVehicle> turnedOnPredicate) {
		this.mode = mode;
		this.temperatureProvider = temperatureProvider;
		this.turnedOnPredicate = turnedOnPredicate == null ? v -> true : turnedOnPredicate;
	}

	@Override
	public AuxEnergyConsumption create(ElectricVehicle electricVehicle) {
		Fleet fleet = injector.getInstance(DvrpModes.key(Fleet.class, mode));
		DvrpVehicle vehicle = fleet.getVehicles().get(electricVehicle.getId());
		return new OhdeSlaskiAuxEnergyConsumption(electricVehicle, temperatureProvider,
				ev -> turnedOnPredicate.test(vehicle));
	}
}

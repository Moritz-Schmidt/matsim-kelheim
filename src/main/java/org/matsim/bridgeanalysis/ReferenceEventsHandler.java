package org.matsim.bridgeanalysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.HashSet;
import java.util.Set;

public class ReferenceEventsHandler implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler {

	/*
	Events for the reference case.

	Find any vehicle that enters one of the (bridge) links that will later be blocked.
	Then resolve person associated with that vehicle.
	 */

	private final Set<String> bridgeLinkIds = Set.of(
			"-830829241",
			 "24744482#2",
			 "-974548442",
			 "161285417",
			 "-8599673",
			 "8599673",
			 "-585581112",
			 "23987639",
			 "-27575929",
			 "167639282",
			 "318235724",
			 "-11810468" );
	private final Set<Id<Vehicle>> affectedVehicles = new HashSet<>();
	private final Set<Id<Person>> affectedAgents = new HashSet<>();

	// LinkEnterEvent
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(bridgeLinkIds.contains(event.getLinkId().toString())) {
			affectedVehicles.add(event.getVehicleId());
		}
	}

	public Set<Id<Vehicle>> getAffectedVehicles() {
		return affectedVehicles;
	}

	// VehicleEntersTrafficEvent
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if(affectedVehicles.contains(event.getVehicleId())) {
			affectedAgents.add(event.getPersonId());
		}
	}

	public Set<Id<Person>> getAffectedAgents() {
		return affectedAgents;
	}


	// Cleanup function for event handlers
	// Not really sure when this would be used, but its provided by the handler sooo
	@Override
	public void reset(int iteration) {
		affectedVehicles.clear();
		affectedAgents.clear();
	}

}

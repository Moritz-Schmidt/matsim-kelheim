package org.matsim.bridgeanalysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TripDataHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	/*
	Compare departure and arrival times to calculate trip time.
	Also store leg mode to find out if someone switched their mode of transportation.

	I really really hope (and kinda doubt) this works this way, cause we sorta rely on the departure and arrival events being called
	in chronological order.

	Let's say an agent does two trips in a day:
		- Departure D1 from home
		- Arrival A1 at work
		- Departure D2 from work
		- Arrival A2 at home

	The idea is to store departure D1 and hope that A1 is the first thing coming up from the ArrivalEvent, then subtract and store the difference.
	Continue with departure D2 and A2 and this time add the difference onto the duration already stored, since we are interested in the total
	time spent in traffic as per the assignment.
	 */

	private final Set<Id<Person>> affectedAgents;
	private final Map<Id<Person>, Double> departureTimes = new HashMap<>();
	private final Map<Id<Person>, Double> timeInTraffic = new HashMap<>();

	// Constructor to pass affected agents to this handler.
	public TripDataHandler(Set<Id<Person>> affectedAgents) {
		this.affectedAgents = affectedAgents;
	}

	// PersonDepartureEvent

	// Store times and mode of transportation
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(affectedAgents.contains(event.getPersonId())) {
			departureTimes.put(event.getPersonId(), event.getTime());
		}
	}


	// PersonArrivalEvent

	// Get stored departure time and subtract from arrival to get the trip duration - then store that.
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(affectedAgents.contains(event.getPersonId())) {
			Double departure = departureTimes.get(event.getPersonId());

			if (!(departure == null)) {
				//timeInTraffic.put(event.getPersonId(), event.getTime() - departure);
				timeInTraffic.merge(event.getPersonId(), event.getTime() - departure, Double::sum);
			}
		}
	}


	public Map<Id<Person>, Double> getTimeInTraffic() {
		return timeInTraffic;
	}


	@Override
	public void reset(int iteration) {
		departureTimes.clear();
		timeInTraffic.clear();
	}

}

package org.matsim.bridgeanalysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RunEventAnalysis {

	public static void main(String[] args) {

		EventsManager eventsManager = EventsUtils.createEventsManager();

		// Prepare event handlers
		ReferenceEventsHandler bridgeAgents = new ReferenceEventsHandler();
		TripDataHandler tripDataBase = new TripDataHandler(bridgeAgents.getAffectedAgents());
		TripDataHandler tripDataPolicy = new TripDataHandler(bridgeAgents.getAffectedAgents());

		eventsManager.addHandler(bridgeAgents);
		eventsManager.addHandler(tripDataBase);

		// Base case

		EventsUtils.readEvents(eventsManager, "output/output-kelheim-v3.1-1pct-iter_50-BASE/kelheim-v3.1-1pct-iter_50.output_events.xml.gz");

		Set<Id<Person>> affectedAgents = bridgeAgents.getAffectedAgents();
		System.out.println("Total affected agents: " + affectedAgents.size());

		Map<Id<Person>, Double> timeInTrafficBase = tripDataBase.getTimeInTraffic();
		System.out.println("Base time in traffic : " + timeInTrafficBase);

		Map<Id<Person>, Double> distanceBase = PlansDistanceAnalyzer.extractTripDistances("output/output-kelheim-v3.1-1pct-iter_50-BASE/kelheim-v3.1-1pct-iter_50.output_plans.xml.gz", affectedAgents);
		System.out.println("Base Distance: " + distanceBase);


		// Policy case

		eventsManager.removeHandler(bridgeAgents);
		eventsManager.removeHandler(tripDataBase);
		eventsManager.resetHandlers(0);

		// using a new handler solves the issue of a shared state between runs
		eventsManager.addHandler(tripDataPolicy);


		EventsUtils.readEvents(eventsManager, "output/output-kelheim-v3.1-1pct-iter_100-POLICY/kelheim-v3.1-1pct-iter_100.output_events.xml.gz");

		Map<Id<Person>, Double> timeInTrafficPolicy = tripDataPolicy.getTimeInTraffic();
		System.out.println("Policy time in traffic : " + timeInTrafficPolicy);

		Map<Id<Person>, Double> distancePolicy = PlansDistanceAnalyzer.extractTripDistances("output/output-kelheim-v3.1-1pct-iter_100-POLICY/kelheim-v3.1-1pct-iter_100.output_plans.xml.gz", affectedAgents);
		System.out.println("Policy Distance: " + distancePolicy);


		// Get delta to visualize changes

		Map<Id<Person>, Double> timeInTrafficDelta = new HashMap<>();
		Map<Id<Person>, Double> distanceDelta = new HashMap<>();

		for (Id<Person> key : timeInTrafficBase.keySet()) {
			//System.out.println("Key: " + key.toString() + " - Base: " + timeInTrafficBase.get(key).toString() + " - Policy: " + timeInTrafficPolicy.get(key).toString());
			timeInTrafficDelta.put(key, timeInTrafficPolicy.get(key) - timeInTrafficBase.get(key));
		}

		for (Id<Person> key : distanceBase.keySet()) {
			distanceDelta.put(key, distancePolicy.get(key) - distanceBase.get(key));
		}

		System.out.println("Time delta: " + timeInTrafficDelta);
		System.out.println("Distance delta " + distanceDelta);
	}

}

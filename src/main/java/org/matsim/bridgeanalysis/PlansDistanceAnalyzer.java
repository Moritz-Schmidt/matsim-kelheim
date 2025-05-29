package org.matsim.bridgeanalysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PlansDistanceAnalyzer {

	/*
	Since not every mode of transportation actually uses the network,
	we need to get the trip distance from the plans file.

	Luckily, as per the assignment, we are interested in the total amount of kilometers traveled and not per-trip-data.
	 */


	public static Map<Id<Person>, Double> extractTripDistances(String plansFile, Set<Id<Person>> affectedAgents) {

		// Found this in the matsim-code-examples repo, honestly not entirely sure how it works o.o
		// makes the data available tho so thats nice
		// https://github.com/matsim-org/matsim-code-examples/blob/0022241a4223f2893e12c7f4d378ab7e3e0cb820/src/main/java/org/matsim/codeexamples/programming/multipleJvmMatsimRuns/MultipleJvmMatsimCallable.java#L103
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(plansFile);

		Map<Id<Person>, Double> distances = new HashMap<>();

		// Loop over every person in the scenario, check if they're part of our affected agents and add together the leg distances
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Id<Person> personId = person.getId();

			if(affectedAgents.contains(personId)) {
				double totalDistance = 0.0;

				Plan plan = person.getSelectedPlan();

				for (PlanElement element : plan.getPlanElements()) {
					if(element instanceof Leg leg) {
						if(leg.getRoute().getDistance() > 0) {
							totalDistance += leg.getRoute().getDistance();
						}
					}
				}

				distances.put(personId, totalDistance);
			}
		}

		return distances;
	}
}

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

import java.util.*;

public class PlansModeAnalyzer {
	public static Map<Id<Person>, Map<String, Double>> extractTripTime(String plansFile, Set<Id<Person>> affectedAgents) {

		String[] modes = {"car", "bike", "walk", "ride", "pt"};
		// Found this in the matsim-code-examples repo, honestly not entirely sure how it works o.o
		// makes the data available tho so thats nice
		// https://github.com/matsim-org/matsim-code-examples/blob/0022241a4223f2893e12c7f4d378ab7e3e0cb820/src/main/java/org/matsim/codeexamples/programming/multipleJvmMatsimRuns/MultipleJvmMatsimCallable.java#L103
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(plansFile);

		Map<Id<Person>, Map<String, Double>> ModeTimes = new HashMap<>();

		// Loop over every person in the scenario, check if they're part of our affected agents and add together the leg times for each mode of transportation.
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Id<Person> personId = person.getId();

			if(affectedAgents.contains(personId)) {

				Map<String, Double> times = new HashMap<>();

				for (String mode : modes) {
					times.put(mode, 0.0);
				}

				Plan plan = person.getSelectedPlan();

				for (PlanElement element : plan.getPlanElements()) {
					if(element instanceof Leg leg) {
						double time = leg.getRoute().getTravelTime().seconds();
						if(time > 0) {
							double new_dist = times.get(leg.getMode()) + time;
							times.put(leg.getMode(), new_dist);
						}
					}
				}

				ModeTimes.put(personId, times);
			}
		}

		return ModeTimes;
	}
}

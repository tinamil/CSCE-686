package pavlik.john;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * After instantiation, the thread must be executed with a Genetic.start() command. After the thread
 * terminates execution then the fitness value and route can be read.
 * 
 * @author John
 *
 */
public class Genetic extends Thread implements Comparable<Genetic> {

	Windmill	instance;
	boolean[]	windmills;
	Integer[]	route;
	Random		rand	= new Random();
	int			fitness;

	public Genetic(Windmill test, boolean[] windmills2) {
		this.instance = test;
		this.windmills = windmills2;
	}

	@Override
	public void run() {
		super.run();
		route = generateRandomRouteSolution();
		fitness = calculateFitness();
	}

	public int calculateFitness() {
		return instance.calculateFitness(windmills, route);
	}

	@Override
	public int compareTo(Genetic o) {
		if (o == null) return 0;
		return Integer.compare(fitness, o.fitness);
	}

	public Integer[] generateRandomRouteSolution() {
		List<Integer> routeList = new ArrayList<>();
		Set<Integer> windmillSet = new TreeSet<>();

		// Maintaining a java.util.Set of all the nodes where a Windmill is located, because this
		// particular problem requires hitting all the Windmills but not every city or empty node
		for (int i = 0; i < windmills.length; ++i) {
			if (windmills[i]) windmillSet.add(i);
		}

		// Must start at the maintenance depot
		routeList.add(instance.startCity);

		// Create a backtrack marker to eliminate redundant cycles
		int progressMarker = routeList.size() - 1;

		// Do not stop until every windmill has been hit and arrived back at the start city
		do {
			// Find all the valid routes from the current node
			List<Integer> validRoutes = new ArrayList<>();
			for (int j = 0; j < instance.adjacencyMatrix.length; ++j) {
				if (instance.adjacencyMatrix[routeList.get(routeList.size() - 1)][j] > 0) {
					validRoutes.add(j);
				}
			}
			// Pick a route at random
			int nextNode = validRoutes.get(rand.nextInt(validRoutes.size()));
			// If that route creates a cycle without finding any progress in the interim,
			// throw away the cycle
			for (int i = progressMarker; i < routeList.size(); ++i) {
				if (routeList.get(i) == nextNode) {
					while (routeList.size() > i) {
						routeList.remove(routeList.size() - 1);
					}
				}
			}
			routeList.add(nextNode);
			if (windmillSet.contains(nextNode)) {
				progressMarker = routeList.size() - 1;
			}
		} while (!routeList.containsAll(windmillSet)
				|| routeList.get(routeList.size() - 1) != instance.startCity);
		return routeList.toArray(new Integer[0]);
	}
}

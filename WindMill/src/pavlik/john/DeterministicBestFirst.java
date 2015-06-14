package pavlik.john;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

public class DeterministicBestFirst {
	Windmill		instance;
	boolean[]		bestWindmills;
	List<Integer>	bestRoute;
	int				bestFitness;

	private class State implements Comparable<State> {
		double	f, g, h;

		@Override
		public int compareTo(State o) {
			if (o.f - f < 0) {
				return 1;
			} else if (o.f == f) return 0;
			else return -1;
		}

		BitSet	unvisited;
		List<Integer>	cities;
		Integer			progressMarker	= -1;

		public State(List<Integer> init) {
			cities = new ArrayList<>(init);
		}

		/**
		 * Copy constructor
		 * 
		 * @param current
		 */
		public State(State current) {
			this.cities = new ArrayList<>(current.cities);
			this.progressMarker = current.progressMarker;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof State) {
				State other = (State) obj;
				return (cities.equals(other.cities));
			}
			return super.equals(obj);
		}
	}

	/**
	 * Calculates an estimate
	 * 
	 * @param current
	 * @param start
	 * @param unvisited
	 * @return
	 */
	private double estimate(List<Integer> currentRoute, Set<Integer> windmillSet) {
		int currentNode = currentRoute.get(currentRoute.size() - 1);

		Set<Integer> remaining = new HashSet<>(windmillSet);
		remaining.removeAll(currentRoute);

		// If we are at the start city and there are no windmills left then we are done
		if (currentNode == instance.startCity && remaining.isEmpty()) return 0;

		// Have to get back to the start city
		remaining.add(instance.startCity);

		int bestCurrent = Integer.MAX_VALUE;
		for (int i = 0; i < instance.adjacencyMatrix.length; ++i) {
			int distance = instance.adjacencyMatrix[currentNode][i];

			if (distance > 0 && remaining.contains(i)) {
				// The currentNode is adjacent to a windmill or the start city, so it's distance will
				// be factored in later
				bestCurrent = 0;
			} else if (distance < bestCurrent && distance > 0) {
				bestCurrent = distance;
			}
		}

		int totalDistance = bestCurrent;
		for (Integer windmill : remaining) {
			bestCurrent = Integer.MAX_VALUE;
			for (int i = 0; i < instance.adjacencyMatrix.length; ++i) {
				int distance = instance.adjacencyMatrix[windmill][i];

				if (distance < bestCurrent && distance > 0) {
					bestCurrent = distance;
				}
			}
			totalDistance += bestCurrent;
		}
		return totalDistance;
	}

	public DeterministicBestFirst(Windmill instance) {
		this.instance = instance;
		bestWindmills = Main.generateAllWindmillSolution(instance);
		bestRoute = Main.generateRandomSolution(instance, bestWindmills);
		bestFitness = instance.calculateFitness(bestWindmills, bestRoute);
	}

	public DeterministicBestFirst(Windmill instance, boolean[] windmills, List<Integer> route) {
		this.instance = instance;
		bestWindmills = windmills;
		bestRoute = route;
		bestFitness = instance.calculateFitness(bestWindmills, bestRoute);
	}

	public void search() {
		System.out.println("Starting best fitness: " + bestFitness);
		int windmillCount = 0;

		for (int i = 0; i < instance.windspeed.length; ++i) {
			if (instance.windspeed[i] > 0) windmillCount += 1;
		}

		long windmillSolutionSetSize = Math.round(Math.pow(2, windmillCount));
		// There are 2^n-1 possible windmill solutions (no windmills is not a valid solution because
		// then powerline costs are infinity), every solution is unique and must be checked because
		// adding a new windmill both adds and subtracts from the fitness function.
		for (long i = 1; i < windmillSolutionSetSize; ++i) {
			System.out.println(i + " of " + (windmillSolutionSetSize - 1)
					+ " windmill sets remaining");
			boolean[] windmills = new boolean[instance.windspeed.length];
			long copy = i;
			for (int j = 0; j < windmills.length; ++j) {
				// Only count nodes that are windmill capable
				if (instance.windspeed[j] > 0) {
					windmills[j] = (copy & 1) == 1;
					copy >>= 1;
				}
			}

			State result = bestFirst(windmills);
			if (result != null) {
				int fitness = instance.calculateFitness(windmills, result.cities);
				if (fitness > bestFitness) {
					bestFitness = fitness;
					bestRoute = result.cities;
					bestWindmills = windmills;
					System.out.println("New best found: " + bestFitness);
				}
			}
		}
	}

	private State bestFirst(boolean[] windmills) {
		int windmillFitness = instance.calculateFitness(windmills, new ArrayList<Integer>());
		// System.out.println("Starting windmill fitness: " + windmillFitness);
		Set<Integer> windmillSet = new TreeSet<>();

		// Maintaining a java.util.Set of all the nodes where a Windmill is located, because this
		// particular problem requires hitting all the Windmills but not every city or empty node
		for (int i = 0; i < instance.windspeed.length; ++i) {
			if (windmills[i]) windmillSet.add(i);
		}

		// Setup the open and closed priority queue and set.
		Set<State> closed = new HashSet<>();
		PriorityQueue<State> open = new PriorityQueue<>();

		// Initialize the starting state
		State start = new State(new ArrayList<Integer>(1));
		start.cities.add(instance.startCity);
		start.progressMarker = 0;
		start.g = 0;
		start.unvisited = new BitSet(instance.adjacencyMatrix.length);
		for (int i = 0; i < instance.adjacencyMatrix.length; ++i) {
			if (i != instance.startCity && instance.adjacencyMatrix[instance.startCity][i] > 0) start.unvisited.set(i);
		}
		start.h = estimate(start.cities, windmillSet);
		start.f = start.g + start.h;

		// Add the starting state to the open set
		open.add(start);

		while (!open.isEmpty()) {
			// Take the current best solution out of the open set
			State current = open.poll();

			// The current State is always the lowest possible solution cost to the problem and
			// adding to the route will only make the fitness worse, if this route is too costly
			// then all routes are too costly to beat the previous best and it
			// is time to backtrack
			if (windmillFitness - current.f < bestFitness) return null;
			// System.out.println("Estimated gap: " + (windmillFitness - current.f - bestFitness));
			// If that solution is complete (i.e. it has all the cities and ends back at the start),
			// then output and exit
			if (current.cities.containsAll(windmillSet)
					&& current.cities.get(current.cities.size() - 1).equals(instance.startCity)) {
				return current;
			}
			// Add the current solution to the closed set
			closed.add(current);

			// Generate a neighbor instance for each city not yet visited in the current state
			int index = -1;
			while ((index = current.unvisited.nextSetBit(index+1)) != -1) {
				State neighbor = new State(current);
				boolean progress = windmillSet.contains(index) && !neighbor.cities.contains(index);
				neighbor.cities.add(index);
				if (progress) {
					neighbor.progressMarker = neighbor.cities.size() - 1;
				}

				// Find all the nodes that can be reached from this new node, but that won't create
				// a cycle since the last time progress was made (because creating such a cycle
				// would mean an infinite search).
				neighbor.unvisited = new BitSet(instance.adjacencyMatrix.length);
				Set<Integer> cycles = new HashSet<>();
				for (int j = neighbor.progressMarker; j < neighbor.cities.size(); ++j) {
					cycles.add(neighbor.cities.get(j));
				}
				for (int i = 0; i < instance.adjacencyMatrix.length; ++i) {
					if (instance.adjacencyMatrix[index][i] > 0 && !cycles.contains(i)) {
						neighbor.unvisited.set(i);
					}
				}
				// Check if that neighbor is already in the closed set
				if (closed.contains(neighbor)) continue;

				// Calculate the distance from the start city to the new city, and every city in
				// between
				double possible = current.g
						+ instance.adjacencyMatrix[current.cities.get(current.cities.size() - 1)][index];

				neighbor.g = possible;
				neighbor.h = estimate(neighbor.cities, windmillSet);
				neighbor.f = neighbor.g + neighbor.h;
				open.add(neighbor);
			}
		}
		return null;
	}
}

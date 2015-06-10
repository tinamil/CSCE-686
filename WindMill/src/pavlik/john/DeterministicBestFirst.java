package pavlik.john;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
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

		List<Integer>	unvisited;
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
			this.f = current.f;
			this.g = current.g;
			this.h = current.h;
			this.unvisited = new ArrayList<>(current.unvisited);
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
	private double estimate(Integer current) {
		// If we are at the start city then it's possible that we are done and we must not
		// over-estimate remaining distance
		if (current == instance.startCity) return 0;

		int bestCurrent = Integer.MAX_VALUE;

		for (int i = 0; i < instance.adjacencyMatrix.length; ++i) {
			int cdistance = instance.adjacencyMatrix[current][i];

			if (cdistance < bestCurrent && cdistance > 0) {
				bestCurrent = cdistance;
			}
		}
		return bestCurrent;
	}

	public DeterministicBestFirst(Windmill instance) {
		this.instance = instance;
		bestWindmills = Main.generateRandomWindmillSolution(instance);
		bestRoute = Arrays.asList(Main.generateRandomSolution(instance, bestWindmills));
		bestFitness = instance.calculateFitness(bestWindmills, bestRoute.toArray(new Integer[0]));
	}

	public void search() {
		System.out.println("Starting best fitness (from randomly generated solution): " + bestFitness);
		int windmillCount = 0;

		for (int i = 0; i < instance.windspeed.length; ++i) {
			if (instance.windspeed[i] > 0) windmillCount += 1;
		}
		long windmillSolutionSetSize = Math.round(Math.pow(2, windmillCount));
		// There are 2^n-1 possible windmill solutions (no windmills is not a valid solution because
		// then powerline costs are infinity), every solution is unique and must be checked because
		// adding a new windmill both adds and subtracts from the fitness function.
		for(long i = 1; i < windmillSolutionSetSize; ++i){ //Count up
		//for (long i = windmillSolutionSetSize - 1; i > 0; --i) { //Count down
			System.out.println(i + " windmill sets remaining of " + windmillSolutionSetSize);
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
				int fitness = instance.calculateFitness(windmills, result.cities
						.toArray(new Integer[0]));
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
		int windmillFitness = instance.calculateFitness(windmills, new Integer[0]);
		System.out.println("Starting windmill fitness: " + windmillFitness);
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
		State start = new State(new LinkedList<Integer>());
		start.cities.add(instance.startCity);
		start.progressMarker = 0;
		start.g = 0;
		start.unvisited = new LinkedList<>();
		for (int i = 0; i < instance.adjacencyMatrix.length; ++i) {
			if (i != instance.startCity && instance.adjacencyMatrix[instance.startCity][i] > 0) start.unvisited
					.add(i);
		}
		start.h = estimate(start.cities.get(start.cities.size() - 1));
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
			if (windmillFitness - current.g < bestFitness) return null;

			// If that solution is complete (i.e. it has all the cities and ends back at the start),
			// then output and exit
			if (current.cities.containsAll(windmillSet)
					&& current.cities.get(current.cities.size() - 1).equals(instance.startCity)) {
				return current;
			}
			// Add the current solution to the closed set
			closed.add(current);

			// Generate a neighbor instance for each city not yet visited in the current state
			for (Integer n : current.unvisited) {
				State neighbor = new State(current);
				boolean progress = windmillSet.contains(n) && !neighbor.cities.contains(n);
				neighbor.cities.add(n);
				if (progress) {
					neighbor.progressMarker = neighbor.cities.size() - 1;
				}

				// Find all the nodes that can be reached from this new node, but that won't create
				// a cycle since the last time progress was made (because creating such a cycle
				// would mean an infinite search).
				neighbor.unvisited = new LinkedList<>();
				Set<Integer> cycles = new HashSet<>();
				for (int j = neighbor.progressMarker; j < neighbor.cities.size(); ++j) {
					cycles.add(neighbor.cities.get(j));
				}
				for (int i = 0; i < instance.adjacencyMatrix.length; ++i) {
					if (instance.adjacencyMatrix[n][i] > 0 && !cycles.contains(i)) {
						neighbor.unvisited.add(i);
					}
				}
				// Check if that neighbor is already in the closed set
				if (closed.contains(n)) continue;

				// Calculate the distance from the start city to the new city, and every city in
				// between
				double possible = current.g
						+ instance.adjacencyMatrix[current.cities.get(current.cities.size() - 1)][n];

				neighbor.g = possible;
				neighbor.h = estimate(n);
				neighbor.f = neighbor.g + neighbor.h;
				open.add(neighbor);
			}
		}
		return null;
	}
}

package pavlik.john;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DepthFirstAStar {
	Windmill	instance;
	boolean[]	windmills;	// TODO initialize with DFS search

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

		public State(List<Integer> init) {
			cities = init;
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

	public DepthFirstAStar(Windmill instance) {
		this.instance = instance;
	}

	public void search() {
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
		start.g = 0;
		start.unvisited = new LinkedList<>();
		for (int i = 0; i < instance.windspeed.length; ++i) {
			if (i != instance.startCity && instance.adjacencyMatrix[instance.startCity][i] > 0) start.unvisited
					.add(i);
		}
		start.h = estimate(start.cities.get(start.cities.size() - 1), start.cities.get(0),
				start.unvisited.toArray(new Integer[0]));
		start.f = start.g + start.h;

		// Add the start state to the open set
		open.add(start);

		while (!open.isEmpty()) {
			// Take the current best solution out of the open set
			State current = open.poll();

			// If that solution is complete (i.e. it has all the cities and ends back at the start),
			// then output and exit
			if (current.cities.containsAll(windmillSet)
					&& current.cities.get(current.cities.size() - 1).equals(instance.startCity)) {
				System.out.println("Done");
				int cost = 0;
				for (int i = 0; i < current.cities.size() - 1; ++i) {
					cost += instance.adjacencyMatrix[current.cities.get(i)][current.cities
							.get(i + 1)];
				}
				System.out.println(cost);
				return;
			}
			// Add the current solution to the closed set
			closed.add(current);

			// If the current solution has visited every city, then send it back to the start city
			if (current.unvisited.isEmpty()) current.unvisited.add(state.cities.get(0));

			// Generate a neighbor instance for each city not yet visited in the current state
			for (Integer n : current.unvisited) {
				State neighbor = new State(current);
				neighbor.cities.add(n);
				neighbor.unvisited.remove(n);
				// Check if that neighbor is already in the closed set (not actually possible with
				// this problem domain)
				if (closed.contains(n)) continue;

				// Calculate the distance from the start city to the new city, and every city in
				// between
				double possible = current.g
						+ instance.adjacencyMatrix[current.cities.get(current.cities.size() - 1)][n];

				if (!open.contains(neighbor) || possible < neighbor.g) {
					neighbor.g = possible;
					// Estimate the best case solution from this state
					neighbor.h = estimate(n, instance.startCity, neighbor.unvisited
							.toArray(new Integer[0]));
					neighbor.f = neighbor.g + neighbor.h;
					open.add(neighbor);
				}
			}
		}

	}

	/**
	 * Calculates an estimate for TSP using minimum spanning tree, it is the sum of the distance to
	 * the closest unvisited city, the minimum spanning tree cost, and the distance from the closest
	 * unvisited back to the start node.
	 * 
	 * @param current
	 * @param start
	 * @param unvisited
	 * @return
	 */
	private static double estimate(Integer current, Integer start, Integer[] unvisited) {
		int bestCurrent = Integer.MAX_VALUE;
		int bestStart = Integer.MAX_VALUE;

		Prims newPrim = new Prims(unvisited.length);
		if (unvisited.length > 0) newPrim.primsAlgorithm(Prims.getAdjacencyMatrix(unvisited));

		for (Node n : unvisited) {
			int cdistance = distance(current, n);
			int sdistance = distance(start, n);

			if (cdistance < bestCurrent) {
				bestCurrent = cdistance;
			}
			if (sdistance < bestStart) {
				bestStart = sdistance;
			}
		}
		if (unvisited.length > 0) return newPrim.getCost() + bestCurrent + bestStart;
		else return bestCurrent + bestStart;
	}
}

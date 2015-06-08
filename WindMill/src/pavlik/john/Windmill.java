package pavlik.john;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Windmill {
	int[][]		adjacencyMatrix;
	int[]		windspeed;
	boolean[]	cities;
	int			startCity;

	int calculateFitness(boolean[] windmills, Integer[] route) {
		int sum = 0;
		for (int i = 0; i < windmills.length; ++i) {
			if (windmills[i]) sum += windspeed[i];
			if (cities[i]) sum -= calculateMST(i, windmills);
		}
		for (int i = 0; i < route.length - 1; ++i) {
			sum -= adjacencyMatrix[route[i]][route[i + 1]];
		}
		return sum;
	}

	/**
	 * Calculates the cost of the distance from the specified city to the closest windmill utilizing
	 * Prim's Algorithm to generate a Minimum Spanning Tree with a termination condition of reaching
	 * any node containing a windmill, then tracing parent pointers to only include the costs
	 * directly between the city and the windmill.
	 */
	int calculateMST(int city, boolean[] windmills) {
		List<Integer> reached = new ArrayList<>();
		reached.add(city);
		int[] parents = new int[windmills.length];
		for (int i = 0; i < parents.length; ++i) {
			parents[i] = -1;
		}
		while (reached.size() < windmills.length
				&& windmills[reached.get(reached.size() - 1)] == false) {
			int cheapestEdgeCost = Integer.MAX_VALUE;
			int cheapestEdgeIndexStart = -1;
			int cheapestEdgeIndexDestination = -1;
			for (Integer node : reached) {
				for (int j = 0; j < adjacencyMatrix.length; ++j) {
					if (!reached.contains(j)) {
						int cost = adjacencyMatrix[node][j];
						if (cost > 0 && cost < cheapestEdgeCost) {
							cheapestEdgeCost = cost;
							cheapestEdgeIndexStart = node;
							cheapestEdgeIndexDestination = j;
						}
					}
				}
			}
			if (cheapestEdgeIndexDestination == -1) throw new RuntimeException(
					"Graph has unreachable nodes");
			reached.add(cheapestEdgeIndexDestination);
			parents[cheapestEdgeIndexDestination] = cheapestEdgeIndexStart;
		}
		if (windmills[reached.get(reached.size() - 1)]) {
			int cost = 0;
			int index = reached.size() - 1;
			while (parents[index] != -1) {
				cost += adjacencyMatrix[parents[index]][index];
				index = parents[index];
			}
			return cost;
		} else {
			throw new RuntimeException("Failed to find a single windmill");
		}
	}

	public static Windmill generateRandom(int size, int maxspeed, double edgeChance, int maxcost,
			double cityChance, double windmillChance) {
		String[] strings = new String[size + 1];
		strings[0] = ("size:" + size);
		Random rand = new Random();
		StringBuilder builder;
		for (int i = 1; i < size + 1; ++i) {
			builder = new StringBuilder();
			if (rand.nextDouble() < cityChance) {
				builder.append("c");
			} else if (rand.nextDouble() < windmillChance) {
				builder.append("w:");
				builder.append(rand.nextInt(maxspeed));
			} else { // Site is empty
				builder.append("e");
			}
			builder.append(" ");
			for (int j = 0; j < size; ++j) {
				builder.append(rand.nextDouble() >= edgeChance ? "0"
						: rand.nextInt(maxcost - 1) + 1);
				builder.append(" ");
			}
			strings[i] = builder.toString();
		}
		return loadWindmill(strings);
	}

	public static Windmill loadWindmill(String[] inputFile) {
		Windmill newmill = new Windmill();
		newmill.startCity = -1;
		int linecount = 0;
		for (String line : inputFile) {
			if (line.startsWith("size:")) {
				int size = Integer.parseInt(line.substring("size:".length()));
				newmill.windspeed = new int[size];
				newmill.adjacencyMatrix = new int[size][size];
				newmill.cities = new boolean[size];
				for (int i = 0; i < size; ++i) {
					newmill.cities[i] = false;
					newmill.windspeed[i] = 0;
					for (int j = 0; j < size; ++j) {
						newmill.adjacencyMatrix[i][j] = 0;
					}
				}
			} else {
				String[] words = line.split("\\s"); // Split on whitespace
				if (words[0].equalsIgnoreCase("c")) {
					newmill.cities[linecount] = true;
					if (newmill.startCity == -1) newmill.startCity = linecount;
				} else if (words[0].equalsIgnoreCase("e")) {
					// Do nothing, not a city or windmill node
				} else {
					int wind = Integer.parseInt(words[0].split(":")[1].trim()); // w:int
					newmill.windspeed[linecount] = wind;
				}
				for (int j = 1; j < words.length; ++j) {
					newmill.adjacencyMatrix[linecount][j - 1] = Integer.parseInt(words[j]);
					if(linecount == (j-1)) {
						newmill.adjacencyMatrix[linecount][j-1] = 0;
					}
				}
				linecount++;
			}
		}
		if (newmill.startCity == -1) {
			return null;
		}
		return newmill;
	}
}

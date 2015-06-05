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

	static Windmill	instance;
	boolean[]		windmills;
	Integer[]		route;
	Random			rand	= new Random();
	int				fitness;

	public Genetic(boolean[] windmills2) {
		this.windmills = windmills2;
	}

	@Override
	public void run() {
		super.run();
		route = generateAntColonySolution();
		fitness = calculateFitness();
	}

	public Integer[] generateRandomSolution() {
		int[][] uniformProbability = new int[instance.adjacencyMatrix.length][instance.adjacencyMatrix.length];

		for (int i = 0; i < uniformProbability.length; ++i) {
			for (int j = 0; j < uniformProbability[i].length; ++i) {
				uniformProbability[i][j] = 1;
			}
		}
		return generateRouteSolution(uniformProbability);
	}

	public int calculateFitness() {
		return instance.calculateFitness(windmills, route);
	}

	@Override
	public int compareTo(Genetic o) {
		if (o == null) return 0;
		return Integer.compare(fitness, o.fitness);
	}

	public Integer[] generateAntColonySolution() {
		Long start = System.currentTimeMillis();
		Ant[] ants = new Ant[Ant.NUM_ANTS];
		// Initialize all pheromones to 1 for equal probabilities
		for (int i = 0; i < Ant.pheromoneMatrix.length; ++i) {
			for (int j = 0; j < Ant.pheromoneMatrix[i].length; ++j) {
				Ant.pheromoneMatrix[i][j] = 1;
			}
		}
		Integer[] shortestSolution = null;
		long shortestSolutionLength = Long.MAX_VALUE;
		int convergence = Ant.CONVERGENCE;
		while (convergence-- > 0) {
			// Begin finding a route using pheromone matrix
			for (int i = 0; i < Ant.NUM_ANTS; ++i) {
				ants[i] = new Ant(this);
				ants[i].start();
			}
			for (int i = 0; i < Ant.NUM_ANTS; ++i) {
				// Wait for each Ant to finish finding a route
				try {
					ants[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// Calculate the length of the route
				long length = 0;
				for (int j = 0; j < ants[i].antRoute.length - 1; ++j) {
					Integer currentNode = ants[i].antRoute[j];
					Integer nextNode = ants[i].antRoute[j + 1];
					length += instance.adjacencyMatrix[currentNode][nextNode];
				}
				// Check if this route is the global best
				if (length < shortestSolutionLength || shortestSolution == null) {
					convergence = Ant.CONVERGENCE;
					shortestSolutionLength = length;
					shortestSolution = ants[i].antRoute;
				}
				// Add pheromone according to the inverse of the length for each successful route
				for (int j = 0; j < ants[i].antRoute.length - 1; ++j) {
					Integer currentNode = ants[i].antRoute[j];
					Integer nextNode = ants[i].antRoute[j + 1];
					Ant.pheromoneMatrix[currentNode][nextNode] += 1 / length;
				}
			}
			// Evaporate all of the pheromone
			for (int i = 0; i < Ant.pheromoneMatrix.length; ++i) {
				for (int j = 0; j < Ant.pheromoneMatrix[i].length; ++j) {
					Ant.pheromoneMatrix[i][j] *= Ant.pheromoneMatrix[i][j]
							* Ant.PHEROMONE_EVAPORATION_COEFFICIENT;
				}
			}
		}
		System.out.println("Ant Colony: " + ((System.currentTimeMillis() - start)/1000));
		return shortestSolution;
	}

	private static class Ant extends Thread {
		public Integer[]	antRoute;
		static final int	NUM_ANTS							= 10;
		// The number of iterations of no improvement before giving up
		static final int	CONVERGENCE							= 10;
		Genetic				parent;
		// % of pheromone that is retained aftery each iteration
		static final double	PHEROMONE_EVAPORATION_COEFFICIENT	= 0.95;
		static int[][]		pheromoneMatrix						= new int[instance.adjacencyMatrix.length][instance.adjacencyMatrix.length];

		Ant(Genetic parent) {
			this.parent = parent;
		}

		@Override
		public void run() {
			super.run();
			antRoute = parent.generateRouteSolution(pheromoneMatrix);
		}
	}

	/**
	 * 
	 * @param probabilityMatrix
	 *            a 2-dimensional array providing a weight for generating a probability of choosing
	 *            any given edge.
	 * @return
	 */
	public Integer[] generateRouteSolution(int[][] probabilityMatrix) {
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
			Integer current = routeList.get(routeList.size() - 1);
			for (int j = 0; j < instance.adjacencyMatrix.length; ++j) {
				if (instance.adjacencyMatrix[current][j] > 0) {
					validRoutes.add(j);
				}
			}
			// Sum up the total probability number, which is a multiplication of the probability
			// matrix and inverse of the cost
			int totalProbability = 0;
			for (Integer nextNode : validRoutes) {
				totalProbability += probabilityMatrix[current][nextNode]
						* (1.0 / instance.adjacencyMatrix[current][nextNode]);
			}
			// Generate a uniform random number between 0 and 1.0 in order to choose a probability
			double selectedProbability = rand.nextDouble();
			// Check the probability of the current value, then subtract that from the selected
			// probability and move onto the next value.
			int nextNode = 0;
			double currentProbability;
			while (selectedProbability > (currentProbability = (probabilityMatrix[current][nextNode] * (1.0 / instance.adjacencyMatrix[current][nextNode]))
					/ totalProbability)) {
				selectedProbability -= currentProbability;
				nextNode += 1;
			}
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
			// Mark progress so that we never backtrack by throwing away a cycle that included a
			// node we needed
			if (windmillSet.contains(nextNode)) {
				progressMarker = routeList.size() - 1;
			}
		} while (!routeList.containsAll(windmillSet)
				|| routeList.get(routeList.size() - 1) != instance.startCity);
		return routeList.toArray(new Integer[0]);
	}
}
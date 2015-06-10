package pavlik.john;

import java.util.Random;

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
		try {
			fitness = calculateFitness();
		} catch (RuntimeException e) {
			fitness = Integer.MIN_VALUE;
		}
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
		Ant[] ants = new Ant[Main.NUM_ANTS];
		// Initialize all pheromones to 1 for equal probabilities
		for (int i = 0; i < Ant.pheromoneMatrix.length; ++i) {
			for (int j = 0; j < Ant.pheromoneMatrix[i].length; ++j) {
				Ant.pheromoneMatrix[i][j] = 1;
			}
		}
		Integer[] shortestSolution = null;
		long shortestSolutionLength = Long.MAX_VALUE;
		int convergence = Main.CONVERGENCE;
		while (convergence-- > 0) {
			// Begin finding a route using pheromone matrix
			for (int i = 0; i < Main.NUM_ANTS; ++i) {
				ants[i] = new Ant(this);
				ants[i].start();
			}
			for (int i = 0; i < Main.NUM_ANTS; ++i) {
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
					convergence = Main.CONVERGENCE;
					shortestSolutionLength = length;
					shortestSolution = ants[i].antRoute;
				}
				// Add pheromone according to the inverse of the length for each successful route
				for (int j = 0; j < ants[i].antRoute.length - 1; ++j) {
					Integer currentNode = ants[i].antRoute[j];
					Integer nextNode = ants[i].antRoute[j + 1];
					Ant.pheromoneMatrix[currentNode][nextNode] += Main.PHEROMONE_PLACEMENT / length;
				}
			}
			// Evaporate all of the pheromone, not letting it go below 1.0 (at which point there is
			// no effect from pheromones and it's pure heuristic)
			for (int i = 0; i < Ant.pheromoneMatrix.length; ++i) {
				for (int j = 0; j < Ant.pheromoneMatrix[i].length; ++j) {
					Ant.pheromoneMatrix[i][j] = Math.max(1.0, Ant.pheromoneMatrix[i][j]
							* Main.PHEROMONE_EVAPORATION_COEFFICIENT);
				}
			}
		}
		return shortestSolution;
	}

	private static class Ant extends Thread {
		public Integer[]	antRoute;
		Genetic				parent;
		static double[][]	pheromoneMatrix	= new double[instance.adjacencyMatrix.length][instance.adjacencyMatrix.length];

		Ant(Genetic parent) {
			this.parent = parent;
		}

		@Override
		public void run() {
			super.run();
			antRoute = Main.generateRouteSolution(instance, pheromoneMatrix, parent.windmills);
		}
	}
}

package pavlik.john;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Main {

	// Random Instance Generation Variables
	static final int	size								= 50;
	static final int	maxspeed							= 10;
	static final double	edgeChance							= .1;
	static final int	maxcost								= 5;
	static final double	cityChance							= .5;
	static final double	windmillChance						= .5;

	// Genetic Algorithm Variables
	static final int	populationSize						= 20;
	static final int	iterations							= 50;
	static final Random	rand								= new Random();

	// Ant algorithm variables
	static final int	NUM_ANTS							= 25;
	// The number of iterations of no improvement before giving up
	static final int	CONVERGENCE							= 100;
	// % of pheromone that is retained aftery each iteration
	static final double	PHEROMONE_EVAPORATION_COEFFICIENT	= 0.9;
	// Amount of pheromone to lay down after each iteration, will be divided by length of route
	static final double	PHEROMONE_PLACEMENT					= 50;

	public static void main(String[] args) {
		testGeneticAlgorithm();
	}

	public static void testGeneticAlgorithm() {
		// Generate random problem
		Windmill test = generateRandom();
		Genetic.instance = test;
		long start = System.currentTimeMillis();
		Genetic bestSolution = null;

		// Generate random solutions
		Genetic[] solutions = new Genetic[populationSize];
		for (int i = 0; i < populationSize; ++i) {
			boolean[] windmills = generateRandomWindmillSolution(test);
			Genetic solution = new Genetic(windmills);
			solutions[i] = solution;
			solution.start();
		}

		for (int i = 0; i < iterations; ++i) {
			System.out.println("Generation " + i);
			// Wait for all the solutions to finish calculating their fitness function

			int average = 0;
			for (int j = 0; j < populationSize; ++j) {
				try {
					solutions[j].join();
					average += solutions[j].fitness;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			average /= populationSize;
			// Sort the solutions into ascending order of fitness function
			Arrays.sort(solutions);
			System.out.println("Best: " + solutions[solutions.length - 1].fitness);
			System.out.println("Average: " + average);
			// If the current generation best solution is better than the previous best, keep it.
			if (bestSolution == null
					|| bestSolution.fitness < solutions[solutions.length - 1].fitness) {
				bestSolution = solutions[solutions.length - 1];
				System.out.println("Overall Best Solution: " + bestSolution.fitness);
			}
			// Replace the worst 20% of the solutions with new random solutions
			for (int j = 0; j < (int) Math.round(populationSize * 0.2); ++j) {
				boolean[] windmills = generateRandomWindmillSolution(test);
				Genetic solution = new Genetic(windmills);
				solutions[j] = solution;
				solution.start();
			}
			// Save the first solution so that it can be used to combine with the last solution
			// later
			Genetic solutionZero = solutions[(int) Math.round(populationSize * 0.2)];

			// Perform cross-over / mutation of windmill placement, maintaining a static population
			// size
			for (int j = (int) Math.round(populationSize * 0.2); j < populationSize; ++j) {
				// Every parent combines with the parent of one relative fitness level better, with
				// the highest fitness parent combining with the lowest
				Genetic parentLeft = solutions[j];
				Genetic parentRight;
				if (j + 1 < populationSize) {
					parentRight = solutions[j + 1];
				} else {
					parentRight = solutionZero;
				}

				// Crossover the windmills into the child
				boolean[] newWindmills = new boolean[test.adjacencyMatrix.length];
				for (int k = 0; k < newWindmills.length; ++k) {
					// If both parents have it or don't have it, then set it to the parents
					newWindmills[k] = parentRight.windmills[k] && parentLeft.windmills[k];
					// If only one parent has it, then 50/50 chance of getting it in the child
					if (parentRight.windmills[k] ^ parentLeft.windmills[k]) {
						newWindmills[k] = rand.nextBoolean();
					}
				}
				// Randomly create a route to match the new windmill locations
				Genetic child = new Genetic(newWindmills);
				solutions[j] = child;
				child.start();
			}
		}

		// Output best result

		System.out.println("Cost: " + bestSolution.fitness);
		System.out.print("Windmills: ");
		StringBuilder windmills = new StringBuilder();
		for (int i = 0; i < size; ++i) {
			if (bestSolution.windmills[i]) {
				windmills.append(i);
				windmills.append(" ");
			}
		}
		System.out.println(windmills.toString());
		System.out.print("Route: ");
		StringBuilder route = new StringBuilder();
		for (int i = 0; i < bestSolution.route.length; ++i) {
			route.append(bestSolution.route[i]);
			route.append(" ");
		}
		System.out.println(route.toString());
		System.out.println("Total time: " + ((System.currentTimeMillis() - start) / 1000));

	}

	public static boolean[] generateRandomWindmillSolution(Windmill instance) {
		boolean[] windmills = new boolean[size];
		for (int i = 0; i < size; ++i) {
			if (instance.windspeed[i] > 0 && rand.nextDouble() < windmillChance) windmills[i] = true;
		}
		return windmills;
	}

	public static Windmill generateRandom() {
		Windmill mill = null;
		while (mill == null) {
			mill = Windmill.generateRandom(size, maxspeed, edgeChance, maxcost, cityChance,
					windmillChance);
		}
		return mill;
	}

	public static void testStaticAlgorithm() {
		int size = 50;
		Windmill test = generateRandom();
		boolean[] windmills = new boolean[size];
		List<Integer> routeList = new ArrayList<>();
		int startCity = -1;
		for (int i = 0; i < size; ++i) {
			if (startCity == -1 && test.cities[i]) startCity = i;
			if (test.windspeed[i] > 0) windmills[i] = true;
			routeList.add(i);
		}
		routeList.add(startCity);
		routeList.add(0, startCity);
		System.out.println("Cost: "
				+ test.calculateFitness(windmills, routeList.toArray(new Integer[0])));
		System.out.print("Windmills: ");
		for (int i = 0; i < size; ++i) {
			if (windmills[i]) System.out.print(i + " ");
		}
		System.out.println();
		System.out.print("Route: ");
		for (int i = 0; i < routeList.size(); ++i) {
			System.out.print(routeList.get(i) + " ");
		}
	}
}

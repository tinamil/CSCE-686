package pavlik.john;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.directory.InvalidAttributesException;

/**
 * Run with LOTS of RAM, 10GB for a size 30 isn't always enough depending on the edges -Xmx10g
 * 
 * @author John
 *
 */
public class Main {

	// Random Instance Generation Variables
	static final int	size								= 30;
	static final int	maxspeed							= 10;
	static final double	edgeChance							= .3;
	static final int	maxcost								= 10;
	static final double	cityChance							= .2;
	static final double	windmillChance						= .5;

	// Genetic Algorithm Variables
	static final int	populationSize						= 20;
	static final int	iterations							= 20;
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
		Windmill test = generateRandomInstance();
		
		System.out.println("Instance Size: " + size);
		System.out.println("Max Speed: " + maxspeed);
		System.out.println("Edge Chance: " + edgeChance);
		System.out.println("Max Cost: " + maxcost);
		System.out.println("City Chance: " + cityChance);
		System.out.println("Windmill Chance: " + windmillChance);
		System.out.println();
		System.out.println("Genetic Size: " + populationSize);
		System.out.println("Iterations: " + iterations);
		System.out.println("Number of ants: " + NUM_ANTS);
		System.out.println("Iterations without improvement: " + CONVERGENCE);
		System.out.println("Pheromone Evaporation: " + PHEROMONE_EVAPORATION_COEFFICIENT);
		System.out.println("Pheromone Placement: " + PHEROMONE_PLACEMENT);
		System.out.println();
		System.out.println("Adjacency Cost Matrix:");
		for (int i = 0; i < test.adjacencyMatrix.length; ++i) {
			for (int j = 0; j < test.adjacencyMatrix[i].length; ++j) {
				System.out.print(test.adjacencyMatrix[i][j]);
				System.out.print(" ");
			}
			System.out.println();
		}
		System.out.println();
		System.out.println("Cities: ");
		for (int i = 0; i < test.cities.length; ++i) {
			if (test.cities[i]) {
				System.out.print(i + " ");
			}
		}
		System.out.println();
		System.out.println();
		System.out.println("Windspeeds: ");
		for (int i = 0; i < test.windspeed.length; ++i) {
			System.out.println(i + ": " + test.windspeed[i]);
		}
		System.out.println();
		System.out.println("Starting City: " + test.startCity);

		Genetic solution = stochasticGeneticAntSearch(test);
		deterministicSearch(test, solution.windmills, Arrays.asList(solution.route));
	}

	public static Genetic stochasticGeneticAntSearch(Windmill instance) {
		// Generate random problem
		Genetic.instance = instance;
		long start = System.currentTimeMillis();
		Genetic bestSolution = null;

		// Generate random solutions
		Genetic[] solutions = new Genetic[populationSize];
		for (int i = 0; i < populationSize; ++i) {
			boolean[] windmills = generateRandomWindmillSolution(instance);
			Genetic solution = new Genetic(windmills);
			solutions[i] = solution;
			solution.start();
		}

		for (int i = 0; i < iterations; ++i) {
			System.out.println("Generation " + i);
			// Wait for all the solutions to finish calculating their fitness function

			int average = 0;
			int count = 0;
			for (int j = 0; j < populationSize; ++j) {
				try {
					solutions[j].join();
					if (solutions[j].fitness > Integer.MIN_VALUE) {
						average += solutions[j].fitness;
						count += 1;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			average /= count;
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
				boolean[] windmills = generateRandomWindmillSolution(instance);
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
				boolean[] newWindmills = new boolean[instance.adjacencyMatrix.length];
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
		return bestSolution;
	}

	public static boolean[] generateRandomWindmillSolution(Windmill instance) {
		boolean[] windmills = new boolean[size];
		for (int i = 0; i < size; ++i) {
			windmills[i] = (instance.windspeed[i] > 0 && rand.nextDouble() < 0.5);
		}
		return windmills;
	}

	public static boolean[] generateAllWindmillSolution(Windmill instance) {
		boolean[] windmills = new boolean[size];
		for (int i = 0; i < size; ++i) {
			windmills[i] = instance.windspeed[i] > 0;
		}
		return windmills;
	}

	public static Windmill generateRandomInstance() {
		Windmill mill = null;
		while (mill == null) {
			mill = Windmill.generateRandom(size, maxspeed, edgeChance, maxcost, cityChance,
					windmillChance);
			try {
				mill.validate();
			} catch (InvalidAttributesException e) {
				e.printStackTrace();
				mill = null;
			}
		}
		return mill;
	}

	public static void deterministicSearch(Windmill instance, boolean[] windmills, List<Integer> route) {
		long start = System.currentTimeMillis();
		DeterministicBestFirst bestSolution = new DeterministicBestFirst(instance, windmills, route);
		bestSolution.search();

		// Output best result
		System.out.println("Cost: " + bestSolution.bestFitness);
		System.out.print("Windmills: ");
		StringBuilder windmillString = new StringBuilder();
		for (int i = 0; i < size; ++i) {
			if (bestSolution.bestWindmills[i]) {
				windmillString.append(i);
				windmillString.append(" ");
			}
		}
		System.out.println(windmillString.toString());
		System.out.print("Route: ");
		StringBuilder routeString = new StringBuilder();
		for (int i = 0; i < bestSolution.bestRoute.size(); ++i) {
			routeString.append(bestSolution.bestRoute.get(i));
			routeString.append(" ");
		}
		System.out.println(routeString.toString());
		System.out.println("Total time: " + ((System.currentTimeMillis() - start) / 1000));
		System.out.println();
	}

	public static List<Integer> generateRandomSolution(Windmill instance, boolean[] windmills) {
		double[][] uniformProbability = new double[instance.adjacencyMatrix.length][instance.adjacencyMatrix.length];

		for (int i = 0; i < uniformProbability.length; ++i) {
			for (int j = 0; j < uniformProbability[i].length; ++j) {
				uniformProbability[i][j] = 1;
			}
		}
		return generateRouteSolution(instance, uniformProbability, windmills);
	}

	/**
	 * 
	 * @param probabilityMatrix
	 *            a 2-dimensional array providing a weight for generating a probability of choosing
	 *            any given edge.
	 * @return
	 */
	public static List<Integer> generateRouteSolution(Windmill instance, double[][] probabilityMatrix,
			boolean[] windmills) {
		Random rand = new Random();
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
			if (validRoutes.isEmpty()) throw new RuntimeException(
					"No valid routes from current node");
			// Sum up the total probability number, which is a multiplication of the probability
			// matrix and inverse of the cost
			double totalProbability = 0;
			for (Integer nextNode : validRoutes) {
				totalProbability += (probabilityMatrix[current][nextNode] * (1.0 / instance.adjacencyMatrix[current][nextNode]));
			}
			// Generate a uniform random number between 0 and 1.0 in order to choose a probability
			double selectedProbability = rand.nextDouble();
			// Check the probability of the current value, then subtract that from the selected
			// probability and move onto the next value.
			double currentProbability = 0;
			int nextNode;
			int nextNodeIndex = 0;
			do {
				nextNode = validRoutes.get(nextNodeIndex++);
				selectedProbability -= currentProbability;
				currentProbability = (probabilityMatrix[current][nextNode] * 1.0 / instance.adjacencyMatrix[current][nextNode])
						/ totalProbability;
			} while (selectedProbability > currentProbability && validRoutes.size() > nextNodeIndex);
			// If that route creates a cycle without finding any progress in the interim,
			// throw away the cycle
			for (int i = progressMarker; i < routeList.size(); ++i) {
				if (routeList.get(i) == nextNode) {
					while (routeList.size() > i) {
						if (routeList.size() > 1) {
							int last = routeList.get(routeList.size() - 1);
							int previous = routeList.get(routeList.size() - 2);
							// If we are going in circles, reduce probability of that path
							probabilityMatrix[previous][last] = Math.max(1.0,
									probabilityMatrix[previous][last]
											* Main.PHEROMONE_EVAPORATION_COEFFICIENT);
						}
						routeList.remove(routeList.size() - 1);
					}
				}
			}
			boolean firstMatch = !routeList.contains(nextNode);
			routeList.add(nextNode);
			// Mark progress so that we never backtrack by throwing away a cycle that included a
			// node we needed
			if (windmillSet.contains(nextNode) && firstMatch) {
				progressMarker = routeList.size() - 1;
			}
		} while (!routeList.containsAll(windmillSet)
				|| routeList.get(routeList.size() - 1) != instance.startCity);
		return routeList;
	}
}

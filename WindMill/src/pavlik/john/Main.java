package pavlik.john;

import java.util.ArrayList;
import java.util.List;

public class Main {

	// Random Instance Generation Variables
	static int		size			= 500;
	static int		maxspeed		= 10;
	static double	edgeChance		= .1;
	static int		maxcost			= 5;
	static double	cityChance		= .1;
	static double	windmillChance	= .5;

	// Genetic Algorithm Variables
	static int		populationSize	= 500;
	static int 		iterations = 5000;

	public static void main(String[] args) {
		testGeneticAlgorithm();
	}

	public static void testGeneticAlgorithm() {
		Windmill test = generateRandom();
		//Generate random solutions
		
		//for each iteration:
		//Perform cross-over / mutation
		//Check fitness
		
		//Output best result
	}

	public static Windmill generateRandom() {
		return Windmill.generateRandom(size, maxspeed, edgeChance, maxcost, cityChance,
				windmillChance);
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

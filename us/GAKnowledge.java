package grif1252;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import spacewar2.clients.ImmutableTeamInfo;
import spacewar2.simulator.Toroidal2DPhysics;

class ChromosomeComparator implements Comparator<Chromosome>
{
	@Override
	public int compare(Chromosome o1, Chromosome o2)
	{
		return Double.compare((o1.fitness()), (o2.fitness()));
	}
}

public class GAKnowledge
{
	// I imagined this file would be our GA.
	// containing things like current performance measure, current population, current chromosomes, etc
	// and would be stored in the file we write out so that it persists across runs.
	
	private Chromosome current_chromosome;
	
	private ArrayList<Chromosome> population = new ArrayList<Chromosome>();
	
	public static int people_in_generation = 10;
	public static int number_to_breed = 5;
	
	private int generation_number = 1;
	private int individual_number = 0;
	
	private ArrayList<Double> performance_measure = new ArrayList<Double>();
	
	public GAKnowledge()
	{
		// first run through
		
		if (population.isEmpty())
		{
			for (int i = 0; i < people_in_generation; i++)
			{
				population.add(this.generate_random_chromosome());
			}
		}
	}
	
	public void initialize()
	{
		current_chromosome(population.get(individual_number));
	}
	
	/**
	 * first calculate a fitness to see how well a chromosome did then update all values and see if we need a new generation or not
	 * 
	 * @param space
	 */
	public void shutDown(Toroidal2DPhysics space)
	{
		current_chromosome.fitness(this.calculateIndividualFitness(space));
		
		individual_number++;
		if (individual_number == people_in_generation)
		{
			// new generation needed
			generation_number++;
			individual_number = 0;
			
			ArrayList<Chromosome> new_population = new ArrayList<Chromosome>();
			
			// rank selection
			// sort our people by fitness and then make the top ones copulate for a new generation
			// this sorts ascendingly
			Collections.sort(population, new ChromosomeComparator());
			
			// make a new generation
			for (int i = 0; i < people_in_generation; i++)
			{
				// pick two parents
				// subtract one to account for index
				int parent_index_1 = (int) randomDouble((people_in_generation - number_to_breed) - 1, people_in_generation - 1);
				int parent_index_2 = (int) randomDouble((people_in_generation - number_to_breed) - 1, people_in_generation - 1);
				
				Chromosome child = Chromosome.sexytime(population.get(parent_index_2), population.get(parent_index_1));
				child = Chromosome.hiroshima(child);
				new_population.add(child);
			}
			
			// and add in our performance
			this.performance_measure.add(this.performace(population));
		}
		
		current_chromosome = null;
	}
	
	/**
	 * calculate a fitness for a chromosome
	 * 
	 * @param space
	 * @return
	 */
	public int calculateIndividualFitness(Toroidal2DPhysics space)
	{
		ArrayList<ImmutableTeamInfo> teams = new ArrayList<ImmutableTeamInfo>();
		teams.addAll(space.getTeamInfo());
		for (ImmutableTeamInfo team : teams)
		{
			if (team.getTeamName().equals(Project4Client.team_name))
			{
				int current_money = team.getTotalMoney();
				return current_money;
			}
		}
		
		return 0;
	}
	
	/**
	 * calculate the average performance of a population as a measure
	 * 
	 * @param population
	 * @return
	 */
	public double performace(ArrayList<Chromosome> population)
	{
		double total = 0;
		for (Chromosome c : population)
		{
			total += c.fitness();
		}
		return total / population.size();
	}
	
	/**
	 * make an individual with some absolutely random values
	 * 
	 * @return
	 */
	public Chromosome generate_random_chromosome()
	{
		return new Chromosome((int) randomDouble(0, 100), // max iterations
				(int) randomDouble(0, 100), // magic multiplier
				(int) randomDouble(0, 100), // FCMin increase
				(int) randomDouble(0, 100), // FCMin decrease
				(int) randomDouble(0, 100) // Fmin angle
		);
	}
	
	/**
	 * random between two values
	 * 
	 * @param min
	 * @param max
	 * @return
	 */
	public static final double randomDouble(double min, double max)
	{
		if (min == max)
			return max;
		
		return min + (Math.random() * (max - min));
	}
	
	public int getGeneration_number()
	{
		return generation_number;
	}
	
	public void setGeneration_number(int generation_number)
	{
		this.generation_number = generation_number;
	}
	
	public int getIndividual_number()
	{
		return individual_number;
	}
	
	public void setIndividual_number(int individual_number)
	{
		this.individual_number = individual_number;
	}
	
	public ArrayList<Double> getPerformance_measure()
	{
		return performance_measure;
	}
	
	public void setPerformance_measure(ArrayList<Double> performance_measure)
	{
		this.performance_measure = performance_measure;
	}
	
	public ArrayList<Chromosome> population()
	{
		return population;
	}
	
	public void population(ArrayList<Chromosome> population)
	{
		this.population = population;
	}
	
	/**
	 * @return the current_chromosome
	 */
	public Chromosome current_chromosome()
	{
		return current_chromosome;
	}
	
	/**
	 * @param current_chromosome
	 *            the current_chromosome to set
	 */
	public void current_chromosome(Chromosome current_chromosome)
	{
		this.current_chromosome = current_chromosome;
	}
}

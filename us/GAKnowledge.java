package grif1252;

import java.util.ArrayList;

public class GAKnowledge
{
	// I imagined this file would be our GA.
	// containing things like current performance measure, current population, current chromosomes, etc
	// and would be stored in the file we write out so that it persists across runs.
	
	public GAKnowledge()
	{
		setCurrent_chromosome(new Chromosome());
	}
	
	/**
	 * @return the current_chromosome
	 */
	public Chromosome getCurrent_chromosome()
	{
		return current_chromosome;
	}

	/**
	 * @param current_chromosome the current_chromosome to set
	 */
	public void setCurrent_chromosome(Chromosome current_chromosome)
	{
		this.current_chromosome = current_chromosome;
	}
	
	public double performace(ArrayList<Chromosome> population){
		double total = 0;
		for(Chromosome c : population){
			total += c.fitness();
		}
		return total;
	}
	
	private Chromosome current_chromosome;
}

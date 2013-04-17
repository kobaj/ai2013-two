package grif1252;

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

	private Chromosome current_chromosome;
}

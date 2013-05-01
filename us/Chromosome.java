package grif1252;

public class Chromosome{
	
	private double fitness; // the fitness of this individual
	private int maxIterations = 20; // how many iterations before recalculating astar
	private int magnitude_vector = 2500; // when calculating a multiplication vector (so we fly faster) how far out should the vector be placed relative to our goal?
	private int FCMinIncrease = -10; // the minimum increase in future distance between asteroid and ship for a futile chase
	private int FCMinDecrease = 5; // minimum decrease in current asteroid position and future ship position for a futile chase
	private int FCMinRadius = 30; // minimum distance between ship and asteroid for futile chase
	private double FCMaxAngle = 0.3; // maximum difference in direction of ship and asteroid for futil chase
	
	/**
	 * gets the fitness of this individual. This is effectively "the fitness function"
	 * 
	 */
	public double fitness(){
		return fitness;
	}
	
	/** 
	 * sets the fitness of this individual for later use
	 * @param score
	 */
	public void fitness(double score){
		this.fitness = score;
	}
	
	/**
	 * gets the maximum iterations for A* 
	 **/
	public int maxIterations(){
		return maxIterations;
	}
	
	/**
	 * sets the max iterations
	 * @param maxIterations
	 */
	public void maxIterations(int maxIterations){
		this.maxIterations = maxIterations;
	}

	/**
	 * gets the magnitude vector 
	 **/
	public int magnitude_vector(){
		return magnitude_vector;
	}
	/**
	 * sets the magnitude vector
	 * @param magnitude_vector
	 */
	public void magnitude_vector(int magnitude_vector){
		this.magnitude_vector = magnitude_vector;
	}
	
	/**
	 * gets the maximum angle
	 */
	public double FCMaxAngle(){
		return FCMaxAngle;
	}
	/**
	 * sets the maximum angle
	 * @param input
	 */
	public void FCMaxAngle(Double input){
		FCMaxAngle = input;
	}
	/**
	 * gets the minimum increase
	 */
	public int FCMinIncrease(){
		return FCMinIncrease;
	}
	/**
	 * sets the minimum increase
	 * @param FCMinIncrease
	 */
	public void FCMinIncrease(int FCMinIncrease){
		this.FCMinIncrease = FCMinIncrease;
	}
	/**
	 * gets the minimum decrease
	 */
	public int FCMinDecrease(){
		return FCMinDecrease;
	}
	/**
	 * sets the minimum decrease
	 * @param FCMinDecrease
	 */
	public void FCMinDecrease(int FCMinDecrease){
		this.FCMinDecrease = FCMinDecrease;
	}	
	
	/**
	 * gets the minimum radius
	 * @return
	 */
	public int FCMinRadius(){
		return FCMinRadius;
	}
	/**
	 * sets the minimum radius
	 * @param FCMinAngle
	 */
	public void FCMinRadius(int FCMinAngle){
		this.FCMinRadius = FCMinAngle;
	}

	/**
	 * Constructor. Sets all of the fields of the chromosome
	 * @param maxIterations
	 * @param magnitude_vector
	 * @param FCMinIncrease
	 * @param FCMinDecrease
	 * @param FCMinRadius
	 * @param FCMaxAngle
	 */
	public Chromosome(int maxIterations, int magnitude_vector, int FCMinIncrease, int FCMinDecrease, int FCMinRadius, double FCMaxAngle){
		this.maxIterations = maxIterations;
		this.magnitude_vector = magnitude_vector;
		this.FCMinIncrease = FCMinIncrease;
		this.FCMinDecrease = FCMinDecrease;
		this.FCMinRadius = FCMinRadius;
		this.FCMaxAngle = FCMaxAngle;
	}
	
	/**
	 * Copy constructor. Duplicates a chromosome
	 * @param a
	 */
	public Chromosome(Chromosome a){
		this(a.maxIterations(),a.magnitude_vector(), a.FCMinIncrease(), a.FCMinDecrease(), a.FCMinRadius(), a.FCMaxAngle);
	}
	
	
	/**
	 * The completely safe for work crossbreeding function. Combines two chromosomes with a 50%
	 * probability of taking a given field from each parent.
	 * @param a
	 * @param b
	 * @return
	 */
	public static Chromosome crossbreed(Chromosome a, Chromosome b){
		
		int maxIterations;
		int magnitude_vector;
		int FCMinIncrease;
		int FCMinDecrease;
		int FCMinRadius;
		double FCMaxAngle;
				
		if(Math.random() < 0.5){
			maxIterations = a.maxIterations();
		}else{
			maxIterations = b.maxIterations();
		}
		
		if(Math.random() < 0.5){
			magnitude_vector = a.magnitude_vector();
		}else{
			magnitude_vector = b.magnitude_vector();
		}
	
		if(Math.random() < 0.5){
			FCMinIncrease = a.FCMinIncrease();
		}else{
			FCMinIncrease = b.FCMinIncrease();
		}
		
		if(Math.random() < 0.5){
			FCMinDecrease = a.FCMinDecrease();
		}else{
			FCMinDecrease = b.FCMinDecrease();
		}
		if(Math.random() < 0.5){
			FCMinRadius = a.FCMinRadius();
		}else{
			FCMinRadius = b.FCMinRadius();
		}
		if(Math.random() < 0.5){
			FCMaxAngle = a.FCMaxAngle();
		}else{
			FCMaxAngle = b.FCMaxAngle();
		}
		
		return new Chromosome(maxIterations, magnitude_vector,  FCMinIncrease, FCMinDecrease, FCMinRadius, FCMaxAngle);
	}

	/**
	 * The politically correct mutation function.
	 * 
	 * one percent probability of decreasing each field by one
	 * one percent probability of increasing each field by one
	 * 
	 * @param a
	 * @return
	 */
	public static Chromosome mutate(Chromosome a){
	
		double sensitivity = 0.01 ;

		if(Math.random() < sensitivity){
			a.maxIterations(a.maxIterations() + (int)GAKnowledge.randomDouble(1,10));
		}else if(Math.random() < sensitivity){
			a.maxIterations(a.maxIterations() - (int)GAKnowledge.randomDouble(1,10));
		}

		if(Math.random() < sensitivity){
			a.magnitude_vector(a.magnitude_vector() + (int)GAKnowledge.randomDouble(1,10));
		}else if(Math.random() < sensitivity){
			a.magnitude_vector(a.magnitude_vector() - (int)GAKnowledge.randomDouble(1,10));
		}

		if(Math.random() < sensitivity){
			a.FCMinRadius(a.FCMinRadius() + (int)GAKnowledge.randomDouble(1,10));
		}else if(Math.random() < sensitivity){
			a.FCMinRadius(a.FCMinRadius() - (int)GAKnowledge.randomDouble(1,10));
		}

		if(Math.random() < sensitivity){
			a.FCMinIncrease(a.FCMinIncrease() + (int)GAKnowledge.randomDouble(1,10));
		}else if(Math.random() < sensitivity){
			a.FCMinIncrease(a.FCMinIncrease() - (int)GAKnowledge.randomDouble(1,10));
		}
		
		if(Math.random() < sensitivity){
			a.FCMinDecrease(a.FCMinDecrease() + (int)GAKnowledge.randomDouble(1,10));
		}else if(Math.random() < sensitivity){
			a.FCMinDecrease(a.FCMinDecrease() - (int)GAKnowledge.randomDouble(1,10));
		}
		
		if(Math.random() < sensitivity){
			a.FCMaxAngle(a.FCMaxAngle() + GAKnowledge.randomDouble(0,1));
		}else if(Math.random() < sensitivity){
			a.FCMaxAngle(a.FCMaxAngle() - GAKnowledge.randomDouble(0,1));
		}
		
		return new Chromosome(a);
		
	}
	
}

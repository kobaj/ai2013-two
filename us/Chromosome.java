package grif1252;

public class Chromosome{
	
	private double fitness;
	public double fitness(){
		return fitness;
	}
	public void fitness(double score){ // include any numbers necessary to calculate fitneess if we change the metric later
		this.fitness = score;
	}
	
	// how many iterations before recalculating astar
	private int maxIterations = 20;
	public int maxIterations(){
		return maxIterations;
	}
	public void maxIterations(int maxIterations){
		this.maxIterations = maxIterations;
	}

	// when calculating a multiplication vector (so we fly faster) how far out should the vector be placed relative to our goal?
	private int magnitude_vector = 2500;
	public int magnitude_vector(){
		return magnitude_vector;
	}
	public void magnitude_vector(int magnitude_vector){
		this.magnitude_vector = magnitude_vector;
	}
	
	// Tom's patriotic variables for futile chase detection
	private int FCMinIncrease = -10;
	private int FCMinDecrease = 5;
	private int FCMinRadius = 30;
	private double FCMaxAngle = 0.3;
	public double FCMaxAngle(){
		return FCMaxAngle;
	}
	public void FCMaxAngle(Double input){
		FCMaxAngle = input;
	}
	public int FCMinIncrease(){
		return FCMinIncrease;
	}
	public void FCMinIncrease(int FCMinIncrease){
		this.FCMinIncrease = FCMinIncrease;
	}
	public int FCMinDecrease(){
		return FCMinDecrease;
	}
	public void FCMinDecrease(int FCMinDecrease){
		this.FCMinDecrease = FCMinDecrease;
	}	
	public int FCMinRadius(){
		return FCMinRadius;
	}
	public void FCMinRadius(int FCMinAngle){
		this.FCMinRadius = FCMinAngle;
	}

	// some kind of unGodly spawning function, unless it's called as the result of two chromosomes 
	// combining in a loving act of algorithmic computation
	public Chromosome(int maxIterations, int magnitude_vector, int FCMinIncrease, int FCMinDecrease, int FCMinRadius, double FCMaxAngle){
		this.maxIterations = maxIterations;
		this.magnitude_vector = magnitude_vector;
		this.FCMinIncrease = FCMinIncrease;
		this.FCMinDecrease = FCMinDecrease;
		this.FCMinRadius = FCMinRadius;
		this.FCMaxAngle = FCMaxAngle;
	}
	
	public Chromosome(Chromosome a){
		this(a.maxIterations(),a.magnitude_vector(), a.FCMinIncrease(), a.FCMinDecrease(), a.FCMinRadius(), a.FCMaxAngle);
	}
	
	public static Chromosome sexytime(Chromosome a, Chromosome b){
		
		// foreplay
		int maxIterations;
		int magnitude_vector;
		int FCMinIncrease;
		int FCMinDecrease;
		int FCMinRadius;
		double FCMaxAngle;
		
		// do it
		
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
		
		// lights cigarette
	}

	// bomb the fuck out of those damned japs
	public static Chromosome hiroshima(Chromosome a){
	
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

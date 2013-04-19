package grif1252;

public class Chromosome{
	
	// good old fashioned American source code right here
	private int maxIterations = 20;
	public int maxIterations(){
		return maxIterations;
	}
	public void maxIterations(int maxIterations){
		this.maxIterations = maxIterations;
	}

	// Jakob's standard speed regulation code from the hood
	private int jakobsMagicMultiplier = 10;
	public int jakobsMagicMultiplier(){
		return jakobsMagicMultiplier;
	}
	public void jakobsMagicMultiplier(int jakobsMagicMultiplier){
		this.jakobsMagicMultiplier = jakobsMagicMultiplier;
	}
	
	// Tom's patriotic variables for futile chase detection
	private int FCMinIncrease = -10;
	private int FCMinDecrease = 5;
	private int FCMinAngle = 30;
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
	public int FCMinAngle(){
		return FCMinAngle;
	}
	public void FCMinAngle(int FCMinAngle){
		this.FCMinAngle = FCMinAngle;
	}

	// some kind of unGodly spawning function, unless it's called as the result of two chromosomes 
	// combining in a loving act of algorithmic computation
	public Chromosome(int maxIterations, int jakobsMagicMultiplier, int FCMinIncrease, int FCMinDecrease, int FCMinAngle){
		this.maxIterations = maxIterations;
		this.jakobsMagicMultiplier = jakobsMagicMultiplier;
		this.FCMinIncrease = FCMinIncrease;
		this.FCMinDecrease = FCMinDecrease;
		this.FCMinAngle = FCMinAngle;
	}
	
	public Chromosome sexytime(Chromosome a, Chromosome b){
		
		// foreplay
		int maxIterations;
		int jakobsMagicMultiplier;
		int FCMinIncrease;
		int FCMinDecrease;
		int FCMinAngle ;
		
		// do it
		
		if(Math.random() < 0.5){
			maxIterations = a.maxIterations();
		}else{
			maxIterations = b.maxIterations();
		}
		
		if(Math.random() < 0.5){
			jakobsMagicMultiplier = a.jakobsMagicMultiplier();
		}else{
			jakobsMagicMultiplier = b.jakobsMagicMultiplier();
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
			FCMinAngle = a.FCMinAngle();
		}else{
			FCMinAngle = b.FCMinAngle();
		}
		
		return new Chromosome(maxIterations, jakobsMagicMultiplier,  FCMinIncrease, FCMinDecrease, FCMinAngle);
		
		// lights cigarette
	}
	
}
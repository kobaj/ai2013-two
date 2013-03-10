package grif1252;

import java.awt.Color;
import java.util.ArrayList;

import spacewar2.shadows.CircleShadow;
import spacewar2.simulator.Toroidal2DPhysics;
import spacewar2.simulator.Toroidal2DPhysics;
import spacewar2.utilities.Position;
import spacewar2.utilities.Vector2D;
import spacewar2.objects.*;

// A directed path from a to b with no properties
class Relation{
	
	// A and B - the objects/edges this relation connects
	protected SpacewarObject a;
	protected SpacewarObject b;
	
	// make a relation between A and B if possible. Always return null in the base class
	public static Relation make(SpacewarObject a, SpacewarObject b, Toroidal2DPhysics space){
		return null;
	}
	
	// constructor
	public Relation(SpacewarObject a, SpacewarObject b){
		this.a = a;
		this.b = b;
	}
	
	// setters and getters for A
	public SpacewarObject A(){
		return a;
	}
	public void A(SpacewarObject a){
		this.a = a ;
	}
	
	// setters and getters for B
	public SpacewarObject B(){
		return b;
	}
	public void B(SpacewarObject b){
		this.b = b ;
	}
	
}

// Relation: A is approaching the current position of B
class ApproachingCurrentPosition extends Relation{

	// make the relation if a will approximately reach B's location in some number of steps
	public static ApproachingCurrentPosition make(SpacewarObject a, SpacewarObject b, Toroidal2DPhysics space){

		// set constants
		int radius = 20;
		int steps = 30;
		int resolution = 3;

		// get velocity vector and position for A
		Vector2D v = a.getPosition().getTranslationalVelocity();
		Position futurePosition = a.getPosition().deepCopy();

		// see if A will arrive at current position of B within given number  of frames
		int i = 0;
		while(i < steps){
			// return relation if approximate collision found at this step
			if(space.findShortestDistance(b.getPosition(), futurePosition) < radius){
				ApproachingCurrentPosition r = new ApproachingCurrentPosition(a,b,i);
				return r;
			}
			
			
			// increment step and future position
			futurePosition.setX(futurePosition.getX() + (v.getXValue() * resolution));
			futurePosition.setY(futurePosition.getY() + (v.getYValue() * resolution));
			i += resolution;
			
		}

		return null ;
	}

	// the number of steps before B occupies the approximate current position of A
	protected int steps;
	public int steps(){
		return steps;
	}
	public void steps(int steps){
		this.steps = steps;
	}
	
	
	// the constructor
	public ApproachingCurrentPosition(SpacewarObject a, SpacewarObject b, int steps) {
		super(a, b);
		this.steps = steps;
	}
	
}

public class KnowledgeGraph{

	
	protected ArrayList<SpacewarObject> vertices;
	protected ArrayList<Relation> edges;
	
	public KnowledgeGraph(Toroidal2DPhysics space, ShadowManager shadow_manager){
		
		edges = new ArrayList<Relation>();
		vertices = new ArrayList<SpacewarObject>();
		
		vertices.addAll(space.getAsteroids());
		vertices.addAll(space.getShips());
		vertices.addAll(space.getWeapons());
		
		
		for(SpacewarObject a : vertices){
			for(SpacewarObject b : vertices){
				
				// Add relations for Ships and asteroids approaching eachother 
				if( !a.equals(b) && ( (a.getClass().isAssignableFrom(Bullet.class) && b.getClass().isAssignableFrom(Ship.class) ) || (a.getClass().isAssignableFrom(Ship.class) && b.getClass().isAssignableFrom(Asteroid.class)))){
					// add relations between asteroids and ships where one is headed towards
					// the other one's current position
					Relation r = ApproachingCurrentPosition.make(a, b, space);
					if(r != null ){
						edges.add(r);
					}
				}
				
				
			}
		}
		
	}
	
	// get the list of directed relations between two SpacewarObjects of a particular type
	public ArrayList<Relation> getRelations(SpacewarObject a,SpacewarObject b, Class c ){
		
		ArrayList<Relation> result = new ArrayList<Relation>();
		
		for(Relation e : edges){
			if(e.A().equals(a) && e.B().equals(b) && e.getClass() == c){
				result.add(e);
			}
		}
		
		return result;
	}
	
	// get the list of relations of a particular type starting from a particular object
	public ArrayList<Relation> getRelationsFrom(SpacewarObject a, Class c){
		
		ArrayList<Relation> result = new ArrayList<Relation>();
		
		for(Relation e : edges){
			if(e.A().equals(a) && e.getClass() == c){
				result.add(e);
			}
		}
		
		return result;
	}
	
	// get the list of relations starting from a particular object
	public ArrayList<Relation> getRelationsTo(SpacewarObject b, Class c){
		
		ArrayList<Relation> result = new ArrayList<Relation>();
		
		for(Relation e : edges){
			if(e.B().equals(b) && e.getClass() == c){
				result.add(e);
			}
		}
		
		return result;
	}
	
	
	
}

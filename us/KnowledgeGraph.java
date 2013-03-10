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
class ShipApproachingAsteroid extends Relation{

	// make the relation if a will approximately reach B's location in some number of steps
	public static ShipApproachingAsteroid make(Ship a, Asteroid b, Toroidal2DPhysics space){

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
				ShipApproachingAsteroid r = new ShipApproachingAsteroid(a,b,i);
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
	public ShipApproachingAsteroid(Ship a, Asteroid b, int steps) {
		super(a, b);
		this.steps = steps;
	}
	
}


//Relation: A is approaching the current position of B
class shipApproachingBase extends Relation{

	// make the relation if a will approximately reach B's location in some number of steps
	public static shipApproachingBase make(Ship a, Base b, Toroidal2DPhysics space){

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
				shipApproachingBase r = new shipApproachingBase(a,b,i);
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
	public shipApproachingBase(Ship a, Base b, int steps) {
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
		vertices.addAll(space.getBases());
		vertices.addAll(space.getWeapons());
		
		
		
		for(SpacewarObject a : vertices){
			for(SpacewarObject b : vertices){
				
				// Ships approaching bases
				if(a.getClass().isAssignableFrom(Ship.class) && b.getClass().isAssignableFrom(Base.class) ){
					Relation r = shipApproachingBase.make((Ship) a, (Base) b, space);
					if(r != null ){
						edges.add(r);
					}		
				}
				
				// Ships approaching asteroids
				if(a.getClass().isAssignableFrom(Ship.class) && b.getClass().isAssignableFrom(Asteroid.class) ){
					Relation r = ShipApproachingAsteroid.make((Ship) a, (Asteroid) b, space);
					if(r != null ){
						edges.add(r);
					}		
				}
				
				// Bullets approaching ships
				//if(a.getClass().isAssignableFrom(Bullet.class) && b.getClass().isAssignableFrom(Ship.class) ){
				//	Relation r = BulletApproachingShip.make((Bullet) a, (Ship) b, space);
				//	if(r != null ){
				//		edges.add(r);
				//	}		
				//}			
				
				
				// Add relations for Ships and asteroids approaching eachother 
				//if( !a.equals(b) && ( (a.getClass().isAssignableFrom(Bullet.class) && b.getClass().isAssignableFrom(Ship.class) ) || (a.getClass().isAssignableFrom(Ship.class) && b.getClass().isAssignableFrom(Asteroid.class)))){
				//	// add relations between asteroids and ships where one is headed towards
				//	// the other one's current position
				//	Relation r = ApproachingCurrentPosition.make(a, b, space);
				//	if(r != null ){
				//		edges.add(r);
				//	}
				//}
				
				
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
			if(e.A().equals(a) && c.isAssignableFrom(e.getClass())){
				result.add(e);
			}
		}
		
		return result;
	}
	public ArrayList<Relation> getRelationsFrom(SpacewarObject a){
		return getRelationsFrom(a,Object.class);
	}

	
	// get the list of relations starting from a particular object
	public ArrayList<Relation> getRelationsTo(SpacewarObject b, Class c){
		
		ArrayList<Relation> result = new ArrayList<Relation>();
		
		for(Relation e : edges){
			if(e.B().equals(b) && c.isAssignableFrom(e.getClass())){
				result.add(e);
			}
		}
		
		return result;
	}
	public ArrayList<Relation> getRelationsTo(SpacewarObject b){
		return getRelationsFrom(b,Object.class);
	}
	
	
	
}

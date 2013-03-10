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
class FutileChase extends Relation{

	// make the relation if a will approximately reach B's location in some number of steps
	public static FutileChase make( Ship a, Asteroid b, Toroidal2DPhysics space){

		// check to see if the ship is getting closer to the asteroid's current position
		// but further from the asteroid's future position
		
		// set constants
		int minIncrease = 1;	// minimum increase in distance to future position of asteroid to register
		int minDecrease = 5;	// minimum decrease in distance to current position to register
		int minRadius = 300;		// minimum distance from asteroid to even bother checking

		// get current radius
		double currentRadius = space.findShortestDistance(a.getPosition(), b.getPosition());
		
		// check radius
		if(currentRadius > minRadius){
			return null ;
		}
		
		// get velocity vector and position for A
		Vector2D vA = a.getPosition().getTranslationalVelocity();
		Position futurePositionA = a.getPosition().deepCopy();
		futurePositionA.setX(futurePositionA.getX() + vA.getXValue());
		futurePositionA.setY(futurePositionA.getY() + vA.getYValue());

		
		// get velocity vector and position for B
		Vector2D vB = b.getPosition().getTranslationalVelocity();
		Position futurePositionB = b.getPosition().deepCopy();
		futurePositionB.setX(futurePositionB.getX() + vB.getXValue());
		futurePositionB.setY(futurePositionB.getY() + vB.getYValue());
		
		// check for min decrease to current position of asteroid
		double futureShipToCurrentAsteroidRadius = space.findShortestDistance(futurePositionA, b.getPosition());
		if(futureShipToCurrentAsteroidRadius  + minDecrease > currentRadius){
			return null;
		}
		
		// check for min increase to current position of asteroid
		double futureShipToFutureAsteroidRadius = space.findShortestDistance(futurePositionA, futurePositionB);
		if(futureShipToFutureAsteroidRadius - minIncrease < currentRadius){
			return null;
		}
		

		return new FutileChase(a,b) ;
	}
	
	
	// the constructor
	public FutileChase(Ship a, Asteroid b) {
		super(a, b);
	}
	
}



//Relation: A is approaching the current position of B
class ShipApproachingBase extends Relation{

	// make the relation if a will approximately reach B's location in some number of steps
	public static ShipApproachingBase make(Ship a, Base b, Toroidal2DPhysics space){

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
				ShipApproachingBase r = new ShipApproachingBase(a,b,i);
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
	public ShipApproachingBase(Ship a, Base b, int steps) {
		super(a, b);
		this.steps = steps;
	}
	
}

//Relation: A is approaching the current position of B
class BulletApproachingShip extends Relation{

	// make the relation if a will approximately reach B's location in some number of steps
	public static BulletApproachingShip make(Bullet a, Ship b, Toroidal2DPhysics space){

		// set constants
		int radius = 100;
		int steps = 60;
		int resolution = 5;

		// get velocity vector and position for A
		Vector2D v = a.getPosition().getTranslationalVelocity();
		Position futurePosition = a.getPosition().deepCopy();

		// see if A will arrive at current position of B within given number  of frames
		int i = 0;
		while(i < steps){
			// return relation if approximate collision found at this step
			if(space.findShortestDistance(b.getPosition(), futurePosition) < radius){
				BulletApproachingShip r = new BulletApproachingShip(a,b,i);
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
	public BulletApproachingShip(Bullet a, Ship b, int steps) {
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
					Relation r = ShipApproachingBase.make((Ship) a, (Base) b, space);
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

				// Ships futily chasing asteroids (they should speed up)
				if(a.getClass().isAssignableFrom(Ship.class) && b.getClass().isAssignableFrom(Asteroid.class) ){
					Relation r = FutileChase.make((Ship) a, (Asteroid) b, space);
					if(r != null ){
						edges.add(r);
					}		
				}
				
				
				// Bullets approaching ships
				if(a.getClass().isAssignableFrom(Bullet.class) && b.getClass().isAssignableFrom(Ship.class) ){
					Relation r = BulletApproachingShip.make((Bullet) a, (Ship) b, space);
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
		return getRelationsTo(b,Object.class);
	}
	
	
	
}

package grif1252;

import java.util.ArrayList;
import java.util.Set;

import spacewar2.objects.Asteroid;
import spacewar2.objects.Base;
import spacewar2.objects.Beacon;
import spacewar2.objects.Missile;
import spacewar2.objects.Ship;
import spacewar2.objects.SpacewarObject;
import spacewar2.simulator.Toroidal2DPhysics;
import spacewar2.utilities.Position;


public class State {

	public enum possibleTasks {mineAsteroid, moveToPosition, getBeacon, destroyShip};
	
	public Toroidal2DPhysics	space;
	public ArrayList<Ship>		shipsDestroyed;
	public ArrayList<Asteroid>	asteroidsGone;
	public ArrayList<Beacon>	beaconsGone;
	public Position				position;
	public int 					money ;
	public double				energy;
	public double 				distanceToBase;
	public double				distanceToAsteroid;
	public double				distanceToBeacon;
	public String				teamName;
	
	public Base					closest_base;
	public Beacon				closest_beacon;
	public Asteroid				closest_asteroid;
	public Asteroid				closest_mineable_asteroid;
	
	public possibleTasks		action;
	public Object				subject;
	public Position				subjectPosition;
	
	// distance from goal necessary to assume it's accomplished
	final public static int SUBGOAL_DISTANCE = 50;
	
	public enum accomplishStates {accomplished, recalculate_plan, not_accomplished};
	
	public accomplishStates isAccomplished(Toroidal2DPhysics space, Ship ship){
		
		System.out.println("checking accomplishment");
		
		// The goal is to mine an asteroid
		if(action == possibleTasks.mineAsteroid){
			System.out.println("mine asteroid");
				
			// The asteroid is right where we expect
			for(Asteroid a : space.getAsteroids()){
				if(a.getPosition().equals(subjectPosition)){
					System.out.println("not accomplished");
					return accomplishStates.not_accomplished;
				}
			}
			
			// The asteroid is gone because we probably collected it
			if(space.findShortestDistance(ship.getPosition(), subjectPosition) < SUBGOAL_DISTANCE){
				System.out.println("accomplished");
				return accomplishStates.accomplished;
			}
			
			// the asteroid is gone, and there's no evidence we collected it
			System.out.println("recalculate");
			return accomplishStates.recalculate_plan;
			
		}
		
		// The goal is to move to a position
		else if(action == possibleTasks.moveToPosition){
						
			// We're there
			if(space.findShortestDistance(ship.getPosition(), subjectPosition) < SUBGOAL_DISTANCE){
				return accomplishStates.accomplished;
			}
			
			// We aren't there
			return accomplishStates.not_accomplished;
		}
		
		// The goal is something else - not yet implemented
		return accomplishStates.not_accomplished;
	}
	
	public String toString(){
		String s	= "State:\n-------------------------------\n";
		s 			+="shipsDestroyed: " + shipsDestroyed.size() + "\n";
		s 			+="asteroidsGone: " + asteroidsGone.size() + "\n";
		s 			+="beaconsGone: " + beaconsGone.size() + "\n";
		s 			+="position: (" + position.getX() + "," + position.getY() +")\n";
		s 			+="money: " + money + "\n";
		s 			+="energy: " + energy + "\n";
		s 			+="distanceToBase: " + distanceToBase + "\n";
		s 			+="distanceToAsteroid: " + distanceToAsteroid + "\n";
		s 			+="distanceToBeacon: " + distanceToBeacon + "\n\n";
		return s;
	}
	
	public State(Ship ship, Toroidal2DPhysics space){
		System.out.println("making new start state");
		this.space = space ;
		this.shipsDestroyed = new ArrayList<Ship>();
		this.asteroidsGone = new ArrayList<Asteroid>();
		this.beaconsGone = new ArrayList<Beacon>();
		this.position = ship.getPosition();
		this.money = ship.getMoney();
		this.energy = ship.getEnergy();
		this.teamName = ship.getTeamName();
		updateNearestAsteroid();
		updateNearestBase();
		updateNearestBeacon();

	}
	
	public State(State prev,possibleTasks action,Object subject){
		// copy over the fields that will be incremented
		this.space = prev.space;
		this.shipsDestroyed = new ArrayList<Ship>(prev.shipsDestroyed);
		this.asteroidsGone = new ArrayList<Asteroid>(prev.asteroidsGone);
		this.beaconsGone = new ArrayList<Beacon>(prev.beaconsGone);
		this.position = prev.position;
		this.money = prev.money;
		this.energy = prev.energy;
		this.distanceToAsteroid = prev.distanceToAsteroid;
		this.distanceToBase = prev.distanceToBase;
		this.distanceToBeacon = prev.distanceToBeacon;
		this.action = action;
		this.subject = subject;
		try{
			if(SpacewarObject.class.isAssignableFrom(subject.getClass())){
				this.subjectPosition = ((SpacewarObject)subject).getPosition();
			}else if(subject.getClass() == Position.class){
				this.subjectPosition = (Position) subject ;
			}else{
				System.out.println("something is wrong");
			}
		}catch(Exception e){
			System.out.println(e);
			System.exit(0);
		}
		
		if(action == possibleTasks.mineAsteroid){
			Asteroid asteroid_subject = (Asteroid) subject;
			asteroidsGone.add(asteroid_subject);
			updateEnergy(asteroid_subject.getPosition());//IT IS VERY IMPORTANT THAT THIS STAY ABOVE THE UPDATING OF POSITOIN
			position = asteroid_subject.getPosition();
			money += ((Asteroid)subject).getMoney();
			updateNearestAsteroid();
			updateNearestBase();
			updateNearestBeacon();
		}
		
		else if(action == possibleTasks.getBeacon){
			Beacon beacon_subject = (Beacon) subject;
			
			beaconsGone.add(beacon_subject);
			updateEnergy(beacon_subject.getPosition());//IT IS VERY IMPORTANT THAT THIS STAY ABOVE THE UPDATING OF POSITOIN
			position = beacon_subject.getPosition();
			energy += Beacon.BEACON_ENERGY_BOOST;
			updateNearestAsteroid();
			updateNearestBase();
			updateNearestBeacon();
		}
		
		else if(action == possibleTasks.destroyShip){
			Ship ship_subject = (Ship) subject;
			
			shipsDestroyed.add(ship_subject);
			money += (ship_subject).getMoney();
			energy -= ((ship_subject).getEnergy() / -Missile.MISSILE_DAMAGE ) * -Missile.MISSILE_COST; // cost of the number of bullets needed to kill
		}
		
		else if(action == possibleTasks.moveToPosition){
			Position position_subject = (Position) subject;
			updateEnergy(position_subject);
			position = position_subject;
			updateNearestAsteroid();
			updateNearestBase();
			updateNearestBeacon();
		}
		
	}
	
	private void updateNearestAsteroid(){
		Asteroid nearestAsteroid = null;
		for(Asteroid a : space.getAsteroids()){
			if(!asteroidsGone.contains(a)){
				if(nearestAsteroid == null || space.findShortestDistance(position, a.getPosition()) < space.findShortestDistance(position, nearestAsteroid.getPosition())){
					nearestAsteroid= a;
					if(a.isMineable())
					{
						this.closest_mineable_asteroid = a;
					}
				}
			}
		}
		this.closest_asteroid = nearestAsteroid;
		this.distanceToAsteroid = space.findShortestDistance(position, nearestAsteroid.getPosition());

	}

	private void updateNearestBeacon(){
		Beacon nearestBeacon = null;
		for(Beacon b : space.getBeacons()){
			if(!beaconsGone.contains(b)){
				if(nearestBeacon == null || space.findShortestDistance(position, b.getPosition()) < space.findShortestDistance(position, nearestBeacon.getPosition()) ){
					nearestBeacon = b;
				}
			}
		}
		this.closest_beacon = nearestBeacon;
		this.distanceToBeacon = space.findShortestDistance(position, nearestBeacon.getPosition());
	}
	
	private void updateNearestBase(){
		Base nearestBase = null;
		for(Base b : space.getBases()){
			if(	nearestBase == null || b.getTeamName().equals(teamName) && (space.findShortestDistance(position, b.getPosition()) < space.findShortestDistance(position, nearestBase.getPosition()))){
				nearestBase = b;
			}
		}
		this.closest_base = nearestBase;
		this.distanceToBase = space.findShortestDistance(position, nearestBase.getPosition());

	}
	
	private void updateEnergy(Position dest){
		int mpg = 5; // this is awesome
		
		double distance = space.findShortestDistance(position, dest);
		double energyUsed = mpg * distance ;
		energy -= energyUsed ;
	}
	
	
}
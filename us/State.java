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

	public enum possibleTasks {mineAsteroid, moveToPosition, getBeacon, destroyShip, goToBase, InitialTask};
	
	public Toroidal2DPhysics	space;
	public ArrayList<Ship>		shipsDestroyed;
	public ArrayList<Asteroid>	asteroidsGone;
	public ArrayList<Beacon>	beaconsGone;
	public Position				position;
	public int					base_money ;
	public int 					ship_money ;
	public double				move_energy;
	public double 				gain_energy;
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
	final public static int SUBGOAL_DISTANCE = 30;
	
	public enum accomplishStates {accomplished, recalculate_plan, not_accomplished};
	
	public static boolean isPreconditionsSatisfied(Toroidal2DPhysics space, Ship ship, State last_state, State possible_state)
	{
		State.possibleTasks action = possible_state.action;
		
		int energy_limit = 300;
		int asteroid_money_requirement = 50;
		int ship_return_base_money_requirement = 300;
		
		//list of all simple pre-conditions.
		if(action == possibleTasks.mineAsteroid)
		{
			double net_energy = possible_state.move_energy + possible_state.gain_energy;
			
			if((net_energy > energy_limit) && //
				//	 (((Asteroid)possible_state.subject).getMoney() > asteroid_money_requirement) && //
					   (true)) //
						return true;
		}
		else if(action == possibleTasks.goToBase)
		{
			double net_energy = last_state.gain_energy + last_state.move_energy + possible_state.move_energy;
			
			if((net_energy > 0) && //
					(last_state.action != possibleTasks.goToBase) && //
					// (possible_state.ship_money > ship_return_base_money_requirement) && //
					   (true)) //
					    return true;
		}
		else if(action == possibleTasks.getBeacon)
		{
			double net_energy = last_state.gain_energy + last_state.move_energy + possible_state.move_energy;
			
			if((net_energy > 0) && //
					   (true)) //
						return true;			
		}
		
		return false;
	}
	
	public accomplishStates isAccomplished(Toroidal2DPhysics space, Ship ship){
		
		
		// The goal is to mine an asteroid
		if(action == possibleTasks.mineAsteroid){
				
			// The asteroid is right where we expect
			for(Asteroid a : space.getAsteroids()){
				if(a.getPosition().equals(subjectPosition)){
					return accomplishStates.not_accomplished;
				}
			}
			
			// The asteroid is gone because we probably collected it
			if(space.findShortestDistance(ship.getPosition(), subjectPosition) < SUBGOAL_DISTANCE){
				return accomplishStates.accomplished;
			}
			
			// the asteroid is gone, and there's no evidence we collected it
			return accomplishStates.recalculate_plan;
			
		}
		else if(action == possibleTasks.getBeacon){
				
				// The beacon is right where we expect
				for(Beacon b : space.getBeacons()){
					if(b.getPosition().equals(subjectPosition)){
						return accomplishStates.not_accomplished;
					}
				}
				
				// The beacon is gone because we probably collected it
				if(space.findShortestDistance(ship.getPosition(), subjectPosition) < SUBGOAL_DISTANCE){
					return accomplishStates.accomplished;
				}
				
				// the beacon is gone, and there's no evidence we collected it
				return accomplishStates.recalculate_plan;
				
			}
		// The goal is to go to the base
		else if(action == possibleTasks.goToBase){
			
			// The base no longer belong to us
			Base theBase = null;
			for(Base b : space.getBases()){
				if(b.getPosition().equals(subjectPosition)){
					theBase = b;
				}
			}
			if(theBase == null){
				return accomplishStates.recalculate_plan;			
			}
			
			// We are within range of the base
			if(space.findShortestDistance(ship.getPosition(), subjectPosition) < SUBGOAL_DISTANCE){
				return accomplishStates.accomplished;
			}
			
			// the base belong to us be we are not there yet
			return accomplishStates.not_accomplished;
			
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
		s 			+="shipmoney: " + ship_money + "\n";
		s 			+="moveenergy: " + move_energy + "\n";
		s 			+="distanceToBase: " + distanceToBase + "\n";
		s 			+="distanceToAsteroid: " + distanceToAsteroid + "\n";
		s 			+="distanceToBeacon: " + distanceToBeacon + "\n";
		
		if(action == possibleTasks.mineAsteroid)
			s += "Action: mineAsteroid"+ "\n\n";
		else if(action == possibleTasks.destroyShip)
			s += "Action: destroyShip"+ "\n\n";
		else if(action == possibleTasks.goToBase)
			s += "Action: goToBase"+ "\n\n";
		else if(action == possibleTasks.getBeacon)
			s += "Action: getBeacon"+ "\n\n";
		else if(action == possibleTasks.moveToPosition)
			s += "Action: moveToPosition"+ "\n\n";
		else if(action == possibleTasks.InitialTask)
			s += "action: initialTask" + "\n\n";
		
		return s;
	}
	
	public State(Ship ship, Toroidal2DPhysics space){
		this.space = space ;
		this.shipsDestroyed = new ArrayList<Ship>();
		this.asteroidsGone = new ArrayList<Asteroid>();
		this.beaconsGone = new ArrayList<Beacon>();
		this.position = ship.getPosition();
		this.ship_money = ship.getMoney();
		this.move_energy = ship.getEnergy();
		this.teamName = ship.getTeamName();
		this.action = possibleTasks.InitialTask;
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
		this.ship_money = prev.ship_money;
		this.base_money = prev.base_money;
		this.move_energy = prev.move_energy;
		this.gain_energy = prev.gain_energy;
		this.distanceToAsteroid = prev.distanceToAsteroid;
		this.distanceToBase = prev.distanceToBase;
		this.distanceToBeacon = prev.distanceToBeacon;
		this.action = action;
		this.subject = subject;
		
		if(SpacewarObject.class.isAssignableFrom(subject.getClass())){
			this.subjectPosition = ((SpacewarObject)subject).getPosition();
		}else if(subject.getClass() == Position.class){
			this.subjectPosition = (Position) subject ;
		}else{
			System.out.println("something is wrong");
		}

		
		if(action == possibleTasks.mineAsteroid){
			Asteroid asteroid_subject = (Asteroid) subject;
			asteroidsGone.add(asteroid_subject);
			move_energy += updateEnergy(asteroid_subject.getPosition());//IT IS VERY IMPORTANT THAT THIS STAY ABOVE THE UPDATING OF POSITOIN
			position = asteroid_subject.getPosition();
			ship_money += ((Asteroid)subject).getMoney();
			updateNearestAsteroid();
			updateNearestBase();
			updateNearestBeacon();
		}
		
		else if(action == possibleTasks.getBeacon){
			Beacon beacon_subject = (Beacon) subject;
			
			beaconsGone.add(beacon_subject);
			move_energy += updateEnergy(beacon_subject.getPosition());//IT IS VERY IMPORTANT THAT THIS STAY ABOVE THE UPDATING OF POSITOIN
			gain_energy += Beacon.BEACON_ENERGY_BOOST;
			gain_energy = Math.min(gain_energy, Ship.SHIP_MAX_ENERGY);
			position = beacon_subject.getPosition();
			updateNearestAsteroid();
			updateNearestBase();
			updateNearestBeacon();
		}
		
		else if(action == possibleTasks.destroyShip){
			Ship ship_subject = (Ship) subject;
			
			shipsDestroyed.add(ship_subject);
			ship_money += (ship_subject).getMoney();
			move_energy -= ((ship_subject).getEnergy() / -Missile.MISSILE_DAMAGE ) * -Missile.MISSILE_COST; // cost of the number of bullets needed to kill
		}
		
		else if(action == possibleTasks.moveToPosition){
			Position position_subject = (Position) subject;
			move_energy += updateEnergy(position_subject);//IT IS VERY IMPORTANT THAT THIS STAY ABOVE THE UPDATING OF POSITOIN
			position = position_subject;
			updateNearestAsteroid();
			updateNearestBase();
			updateNearestBeacon();
		}
		else if(action == possibleTasks.goToBase){
			Base base_subject = (Base) subject;
			move_energy += updateEnergy(base_subject.getPosition());//IT IS VERY IMPORTANT THAT THIS STAY ABOVE THE UPDATING OF POSITOIN
			gain_energy += (base_subject.getEnergy() / 2);
			gain_energy = Math.min(gain_energy, Ship.SHIP_MAX_ENERGY);
			position = base_subject.getPosition();
			base_money += ship_money;
			ship_money = 0;
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
					if(a.isMineable())
					{
						nearestAsteroid = a;
					}
				}
			}
		}
		this.closest_mineable_asteroid = nearestAsteroid;
		
		nearestAsteroid = null;
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
	
	private double updateEnergy(Position dest){
		int mpg = 5; // this is awesome
		
		double distance = space.findShortestDistance(position, dest);
		double energyUsed = mpg * distance ;
		return -energyUsed;
	}
	
	
}

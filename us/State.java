package grif1252;

import java.util.ArrayList;
import java.util.Set;

import spacewar2.simulator.Toroidal2DPhysics;
import spacewar2.utilities.Position;
import spacewar2.objects.*;


public class State {

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
		System.out.println("making new state");
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
	
	public State(State prev,String action,SpacewarObject subject){
		// copy over the fields that will be incremented
		this.space = prev.space;
		this.shipsDestroyed = new ArrayList<Ship>(prev.shipsDestroyed);
		this.asteroidsGone = new ArrayList<Asteroid>(prev.asteroidsGone);
		this.beaconsGone = new ArrayList<Beacon>(prev.beaconsGone);
		this.position = prev.position;
		this.money = prev.money;
		this.energy = prev.energy;
		this.distanceToAsteroid = prev.distanceToAsteroid;
		this.distanceToBase = prev.distanceToBase ;
		this.distanceToBeacon = prev.distanceToBeacon ;
		
		if(action.equals("mineAsteroid")){
			asteroidsGone.add((Asteroid)subject);
			updateEnergy(subject);//IT IS VERY IMPORTANT THAT THIS STAY ABOVE THE UPDATING OF POSITOIN
			position = subject.getPosition();
			money += ((Asteroid)subject).getMoney();
			updateNearestAsteroid();
			updateNearestBase();
			updateNearestBeacon();
		}
		if(action.equals("getBeacon")){
			beaconsGone.add((Beacon)subject);
			updateEnergy(subject);//IT IS VERY IMPORTANT THAT THIS STAY ABOVE THE UPDATING OF POSITOIN
			position = subject.getPosition();
			energy += ((Beacon)subject).BEACON_ENERGY_BOOST;
			updateNearestAsteroid();
			updateNearestBase();
			updateNearestBeacon();
		}
		
		else if(action.equals("destroyShip")){
			shipsDestroyed.add((Ship)subject);
			money += ((Ship)subject).getMoney();
			energy -= (((Ship) subject).getEnergy() / 200 ) * 50; // cost of the number of bullets needed to kill
		}
		
	}
	
	private void updateNearestAsteroid(){
		Asteroid nearestAsteroid = null;
		for(Asteroid a : space.getAsteroids()){
			if(!asteroidsGone.contains(a)){
				if(nearestAsteroid == null || space.findShortestDistance(position, a.getPosition()) < space.findShortestDistance(position, nearestAsteroid.getPosition())){
					nearestAsteroid= a;
				}
			}
		}
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
		this.distanceToBeacon = space.findShortestDistance(position, nearestBeacon.getPosition());
	}
	
	private void updateNearestBase(){
		Base nearestBase = null;
		for(Base b : space.getBases()){
			if(	nearestBase == null || b.getTeamName().equals(teamName) && (space.findShortestDistance(position, b.getPosition()) < space.findShortestDistance(position, nearestBase.getPosition()))){
				nearestBase = b;
			}
		}
		this.distanceToBase = space.findShortestDistance(position, nearestBase.getPosition());

	}
	
	private void updateEnergy(SpacewarObject dest){
		int mpg = 5;
		
		double distance = space.findShortestDistance(position, dest.getPosition());
		double energyUsed = mpg * distance ;
		energy -= energyUsed ;
	}
	
	
}

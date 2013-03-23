package grif1252;

import java.util.ArrayList;

import spacewar2.simulator.Toroidal2DPhysics;
import spacewar2.utilities.Position;
import spacewar2.objects.*;


public class State {

	public Toroidal2DPhysics	space;
	public ArrayList<Ship>		shipsDestroyed;
	public ArrayList<Asteroid>	asteroidsGone;
	public Position				position;
	public int 					money ;
	public double				energy;
	public double 				distanceToBase;
	public double				distanceToAsteroid;
	public String				teamName;
	
	public State(Ship ship, Toroidal2DPhysics space){
		this.space = space ;
		this.shipsDestroyed = new ArrayList<Ship>();
		this.asteroidsGone = new ArrayList<Asteroid>();
		this.position = ship.getPosition();
		this.money = ship.getMoney();
		this.energy = ship.getEnergy();
		this.teamName = ship.getTeamName();
		updateNearestAsteroid();
		updateNearestAsteroid();

	}
	
	public State(State prev,String action,SpacewarObject subject){
		// copy over the fields that will be incremented
		this.space = prev.space;
		this.shipsDestroyed = new ArrayList<Ship>(prev.shipsDestroyed);
		this.asteroidsGone = new ArrayList<Asteroid>(prev.asteroidsGone);
		this.position = prev.position;
		this.money = prev.money;
		this.energy = prev.energy;
		this.distanceToAsteroid = prev.distanceToAsteroid;
		this.distanceToBase = prev.distanceToBase ;
		
		if(action.equals("mineAsteroid")){
			asteroidsGone.add((Asteroid)subject);
			position = subject.getPosition();
			money += ((Asteroid)subject).getMoney();
			updateNearestAsteroid();
			updateNearestBase();

		}
		
		else if(action.equals("destroyShip")){
			shipsDestroyed.add((Ship)subject);
			money += ((Ship)subject).getMoney();
		}
		
	}
	
	private void updateNearestAsteroid(){
		ArrayList<Asteroid> asteroids = (ArrayList<Asteroid>) space.getAsteroids();
		Asteroid nearestAsteroid = null;
		for(Asteroid a : asteroids){
			if(!asteroidsGone.contains(a)){
				if(space.findShortestDistance(position, a.getPosition()) < space.findShortestDistance(position, nearestAsteroid.getPosition()) || nearestAsteroid == null){
					nearestAsteroid= a;
				}
			}
		}
		this.distanceToAsteroid = space.findShortestDistance(position, nearestAsteroid.getPosition());
	}
	
	private void updateNearestBase(){
		ArrayList<Base> bases = (ArrayList<Base>) space.getBases();
		Base nearestBase = null;
		for(Base b : bases){
			if(	b.getTeamName().equals(teamName) && (space.findShortestDistance(position, b.getPosition()) < space.findShortestDistance(position, nearestBase.getPosition()) || nearestBase == null)){
				nearestBase = b;
			}
		}
		this.distanceToBase = space.findShortestDistance(position, nearestBase.getPosition());

	}
	
	
}

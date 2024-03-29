package grif1252;

import grif1252.Node.NodeType;
import grif1252.State.accomplishStates;
import grif1252.State.possibleTasks;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import spacewar2.actions.MoveAction;
import spacewar2.actions.SpacewarAction;
import spacewar2.actions.SpacewarPurchaseEnum;
import spacewar2.clients.TeamClient;
import spacewar2.objects.Asteroid;
import spacewar2.objects.Base;
import spacewar2.objects.Beacon;
import spacewar2.objects.Ship;
import spacewar2.objects.SpacewarActionableObject;
import spacewar2.objects.SpacewarObject;
import spacewar2.powerups.SpacewarPowerupEnum;
import spacewar2.shadows.CircleShadow;
import spacewar2.shadows.LineShadow;
import spacewar2.shadows.Shadow;
import spacewar2.simulator.Toroidal2DPhysics;
import spacewar2.utilities.Position;
import spacewar2.utilities.Vector2D;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

public class Project4Client extends TeamClient
{
	// ways of searching for nodes
	public static enum NodeConnections
	{
		closest, furthest, random_c
	};
	
	// ship goals are overall goals (base, beacon, or mineable asteroid)
	HashMap<Ship, String> ship_goals;
	// local goals are nodes inbetween the ship and its overall goal
	HashMap<Ship, Position> local_goals;
	
	// where to store our knowledge
	final public static String knowledgeFile = "grif1252/tomandjakobknowledge.xml";
	private GAKnowledge myKnowledge;
	
	// vary the following to determine which to pick (asteroid, beacon, or money).
	// none of these are strickly if/then nor one to one.
	// eg, just because an asteroid has a size of 900 (> 700) does not mean we go after it.
	
	// how big should an asteroid be (money wise) so that we pick it, instead of the closest asteroid
	final public static double ASTEROID_SIZE = 200;
	
	// how much money until a ship searches for a base
	final public static double MONEY_RETURN = 250;
	
	// how low on energy should we be before searching for a beacon
	final public static double BEACON_GET = 1500;
	
	// when calculating nodes randomly, how many should we use (much like resolution)
	public static int MAX_RANDOM_NODES = 10;
	
	// when calculating nodes in the variable grid, how much extra space should we include
	// to compensate for unfindable paths
	final public static int SQUARE_PADDING = 150;
	
	// how many loops should we go through before recalculating astar and nodes
	HashMap<UUID, Integer> current_iterations;
	
	// screen resolution
	public static int X_RES = 1024;
	public static int Y_RES = 768;
	
	// how many nodes we should make (square thise to get total)
	final public static double RES = 10.0;
	
	// how many links should be attempted between nodes
	// (8 would be up down left right and diagonals)
	final public static int MAX_NUM_NODE_CONNECTIONS = 20;
	
	// because we are unable to access the parent random
	Random random;
	
	// global_output = true means it will output to console
	final private boolean global_output = false;
	
	HashSet<Shadow> shadows;
	
	// can we buy a base?
	private HashMap<UUID, Boolean> can_buy_base;
	
	// should we shoot?
	private HashMap<UUID, Boolean> shoot;
	
	private static double NODEGOAL_DISTANCE = 20;
	
	// PROJECT 3 VARIABLES
	// **********************************************
	private HashMap<UUID, LinkedList<State>> object_plans = new HashMap<UUID, LinkedList<State>>();
	
	// some new logic properties
	private final int EMERGENCYMONEY = 1000;
	private final int EMERGENCYENERGY = 300;
	
	// some new shadow holders
	private HashMap<UUID, ArrayList<Shadow>> astar_shadows = new HashMap<UUID, ArrayList<Shadow>>();
	private Shadow projection;
	
	// this has to do with bases
	private HashMap<UUID, Boolean> stop_following = new HashMap<UUID, Boolean>();
	
	// and dividing the map up
	private enum MapDivision
	{
		updown, leftright
	};
	
	private MapDivision current_division = MapDivision.leftright;
	
	// predict movement of moving asteroids
	private ArrayList<Shadow> node_tracking = new ArrayList<Shadow>();
	
	// purchase timeout so it doesn't crash
	private double purchase_timeout = 0;
	private final double MAX_TIMEOUT = 100;
	
	private enum HighLevelGoals
	{
		LotsOfMoney, RefilledEnergy, VisitedBase, None
	};
	
	// keep track of the number of iterations the game loops through.
	private int game_loops = 5;
	
	public static String team_name;
	
	/**
	 * Initialize most variables, we've started moving these outside initialize to be initialized with the constructor.
	 * 
	 */
	@Override
	public void initialize(Toroidal2DPhysics space)
	{
		team_name = this.getTeamName();
		
		ship_goals = new HashMap<Ship, String>();
		local_goals = new HashMap<Ship, Position>();
		
		current_iterations = new HashMap<UUID, Integer>();
		
		shadows = new HashSet<Shadow>();
		
		can_buy_base = new HashMap<UUID, Boolean>();

		shoot = new HashMap<UUID, Boolean>();
		
		random = new Random();
		
		readInFile();
		myKnowledge.initialize();
		System.out.println("************************************************************************");
		System.out.println("************************************************************************");
		System.out.println("************************************************************************");
		System.out.println(this.myKnowledge.current_chromosome().maxIterations());
		System.out.println("************************************************************************");
		System.out.println("************************************************************************");
		System.out.println("************************************************************************");
	}
	
	/**
	 * Demonstrates reading in a xstream file You can save out other ways too. This is a human-readable way to examine the knowledge you have learned.
	 */
	private void readInFile()
	{
		XStream xstream = new XStream();
		xstream.alias("GAKnowledge", GAKnowledge.class);
		
		try
		{
			myKnowledge = (GAKnowledge) xstream.fromXML(new File(knowledgeFile));
		}
		catch (XStreamException e)
		{
			// if you get an error, handle it other than a null pointer because
			// the error will happen the first time you run
			myKnowledge = new GAKnowledge();
		}
	}
	
	/**
	 * Demonstrates saving out to the xstream file You can save out other ways too. This is a human-readable way to examine the knowledge you have learned.
	 */
	private void writeOutFile()
	{
		XStream xstream = new XStream();
		xstream.alias("GAKnowledge", GAKnowledge.class);
		
		try
		{
			// if you want to compress the file, change FileOuputStream to a GZIPOutputStream
			xstream.toXML(myKnowledge, new FileOutputStream(new File(knowledgeFile)));
		}
		catch (XStreamException e)
		{
			// if you get an error, handle it somehow as it means your knowledge didn't save
			// the error will happen the first time you run
			myKnowledge = new GAKnowledge();
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			myKnowledge = new GAKnowledge();
		}
	}
	
	/**
	 * called at the end of the game
	 */
	@Override
	public void shutDown(Toroidal2DPhysics space)
	{
		myKnowledge.shutDown(space);
		writeOutFile();
	}
	
	/**
	 * This is responsible for creating a plan for each ship independently, and then calculating any necessary details such as astar. Additionally for this project we included some base modifications.
	 * 
	 * @param space
	 * @param actionableObjects
	 */
	@Override
	public Map<UUID, SpacewarAction> getMovementStart(Toroidal2DPhysics space, Set<SpacewarActionableObject> actionableObjects)
	{
		try
		{
			if (global_output)
				System.out.println("Begin");
			
			// store necessary variables
			Long time = System.currentTimeMillis();
			X_RES = space.getWidth();
			Y_RES = space.getHeight();
			
			HashMap<UUID, SpacewarAction> actions = new HashMap<UUID, SpacewarAction>();
			Toroidal2DPhysics local_space = space;
			
			ArrayList<Base> my_bases = new ArrayList<Base>();
			ArrayList<Ship> my_ships = new ArrayList<Ship>();
			
			int number_of_ships = 0; // zero based indexing
			for (SpacewarObject actionable : actionableObjects)
				if (actionable instanceof Ship)
					number_of_ships += 1;
			
			// old loop
			for (SpacewarObject actionable : actionableObjects)
				if (actionable instanceof Ship)
				{
					if (space.getCurrentTimestep() > space.getMaxTime() - game_loops)
					{
						continue;
					}
					
					Ship ship = (Ship) actionable;
					my_ships.add(ship);
					int ship_number = my_ships.size() - 1; // zero based indexing
					
					// first get our plan out of storage
					LinkedList<State> ship_plan = object_plans.get(ship.getId());
					
					// if our plan is empty recalculate a new plan
					boolean recalculate_plan = false;
					if (ship_plan == null || ship_plan.isEmpty())
						recalculate_plan = true;
					
					if (recalculate_plan)
					{
						// prepare your PLEASE WATCH THE LANGUAGE, I mean, stack.
						// ship_plan = new LinkedList<State>();
						
						// first set our start state
						State start = new State(ship, local_space);
						
						// get a high level goal based on start
						// Asteroid original_goal = getClosestAsteroid(local_space, ship.getPosition());
						// State next = new State(start, possibleTasks.mineAsteroid, original_goal);
						// ship_plan.add(next);
						
						ship_plan = makePlan(space, ship, start, ship_number, number_of_ships);
						
						// and push it to our storage
						object_plans.put(ship.getId(), ship_plan);
					}
					
					// and draw
					drawPlan(ship_plan);
					
					// see if its been 20 timesteps
					if (!current_iterations.containsKey(ship.getId()))
						current_iterations.put(ship.getId(), 0);
					
					// actual 20 timestep calcs
					int ship_iterations = current_iterations.get(ship.getId());
					if (ship_iterations <= 0)
					{
						this.stop_following.put(ship.getId(), false);
						
						Position a_star_needed = null;
						
						if (ship.getEnergy() < this.EMERGENCYENERGY)
						{
							// recalculate plan
							ship_plan.clear();
							object_plans.put(ship.getId(), ship_plan);
						}
						else if (ship.getMoney() > this.EMERGENCYMONEY)
						{
							// recalculate plan
							ship_plan.clear();
							object_plans.put(ship.getId(), ship_plan);
						}
						else
						{
							// get our next action from the plan
							State next_action = ship_plan.peek();
							
							if (next_action.action == State.possibleTasks.goToBase)
								this.stop_following.put(ship.getId(), true);
							
							accomplishStates is_accomplished = next_action.isAccomplished(local_space, ship);
							if (is_accomplished == State.accomplishStates.accomplished)
							{
								ship_plan.pop();
								object_plans.put(ship.getId(), ship_plan);
							}
							else if (is_accomplished == State.accomplishStates.recalculate_plan)
							{
								ship_plan.clear();
								object_plans.put(ship.getId(), ship_plan);
							}
							else if (is_accomplished == State.accomplishStates.not_accomplished)
							{
								a_star_needed = next_action.position;
							}
						}
						
						if (a_star_needed != null)
						{
							current_iterations.put(ship.getId(), this.myKnowledge.current_chromosome().maxIterations());
							
							// calcualate astar
							ArrayList<Position> subgoals = this.independentAStar(local_space, ship.getPosition(), a_star_needed, ship.getRadius(), ship.getId());
							
							Position original_goal = subgoals.get(1);
							
							// set a speed multiplier, increase it if the ship is in a futile chase
							Vector2D v = space.findShortestDistanceVector(ship.getPosition(), original_goal);
							double jakobs_magic_multiplier = ((double) myKnowledge.current_chromosome().magnitude_vector()) / v.getMagnitude();
							int toms_miracle_multiplier = 5;
							if (futileChase(ship, space, a_star_needed))
							{
								jakobs_magic_multiplier *= toms_miracle_multiplier;
							}
							
							// use the multiplier to fine-tune velocity
							Vector2D distance_unit = v.getUnitVector();
							Position extended_goal = new Position(original_goal.getX() + distance_unit.getXValue() * jakobs_magic_multiplier, original_goal.getY() + distance_unit.getYValue()
									* jakobs_magic_multiplier);
							
							// push!!
							projection = new CircleShadow(3, Color.orange, extended_goal);
							actions.put(ship.getId(), new MoveAction(local_space, ship.getPosition(), extended_goal));
						}
					}
					else
					{
						current_iterations.put(ship.getId(), ship_iterations - 1);
						
						actions.put(ship.getId(), ship.getCurrentAction());
					}
				}
				else if (actionable instanceof Base)
				{
					Base base = (Base) actionable;
					my_bases.add(base);
				}
			
			// drawing things
			for (int i = 0; i < number_of_ships; i++)
			{
				
				if (this.current_division == MapDivision.updown)
				{
					shadows.add(new ColorLineShadow(new Position(0, i * Y_RES / number_of_ships), new Position(X_RES, i * Y_RES / number_of_ships), Color.WHITE));
				}
				else if (this.current_division == MapDivision.leftright)
				{
					shadows.add(new ColorLineShadow(new Position(i * X_RES / number_of_ships, 0), new Position(i * X_RES / number_of_ships, Y_RES), Color.WHITE));
				}
				
			}
			
			shadows.add(projection);
			for (Shadow s : node_tracking)
				shadows.add(s);
			for (Ship ship : my_ships)
				if (astar_shadows.containsKey(ship.getId()))
					for (Shadow shad : astar_shadows.get(ship.getId()))
						shadows.add(shad);
			
			if (global_output)
				System.out.println("TimeSpent in Client: " + (System.currentTimeMillis() - time));
			
			return actions;
		}
		catch (Exception e)
		{
			System.out.println(e);
			e.printStackTrace();
		}
		
		// last (do not delete me)
		System.gc();
		
		return null;
	}
	
	/**
	 * This method will return a properly queued plan that the particular ship should follow.
	 * 
	 * @param space
	 * @param ship
	 * @param sub_previous
	 *            this should be a start state
	 * @param ship_number
	 *            this will be between 0 and number_of_ships
	 * @param number_of_ships
	 *            is the current number of ships on the field.
	 * @return queuedplan
	 */
	private LinkedList<State> makePlan(Toroidal2DPhysics space, Ship ship, State sub_previous, int ship_number, int number_of_ships)
	{
		LinkedList<State> ship_plan = null;
		
		HighLevelGoals plan_goal = HighLevelGoals.LotsOfMoney;
		
		// see what our high level goal will be
		if (ship.getEnergy() < this.EMERGENCYENERGY)
		{
			plan_goal = HighLevelGoals.RefilledEnergy;
		}
		
		if (ship.getMoney() > this.EMERGENCYMONEY)
		{
			plan_goal = HighLevelGoals.VisitedBase;
		}
		
		int max_depth = 10;
		int depth = 1;
		while (depth <= max_depth && ship_plan == null)
		{
			ship_plan = recursiveMakePlan(space, ship, sub_previous, ship_number, number_of_ships, plan_goal, depth);
			depth++;
		}
		
		if (ship_plan == null)
		{
			// plan couldn't be found
			ship_plan = new LinkedList<State>();
			
			Asteroid choice = this.getMapDividedClosestAsteroid(space, sub_previous, ship_number, number_of_ships);
			if (choice != null)
			{
				ship_plan.add(new State(sub_previous, possibleTasks.mineAsteroid, choice));
			}
			else
			{
				ship_plan.add(new State(sub_previous, possibleTasks.goToBase, this.getClosestBase(space, ship.getPosition())));
			}
			
		}
		else
		{
			// System.out.println("High Level Plan Rocks!");
			
			// System.out.println(sub_previous);
			// for (State s : ship_plan)
			// System.out.println(s.toString());
			
		}
		
		// System.exit(0);
		
		return ship_plan;
	}
	
	/**
	 * this is the iterative depth first search that makes the plan
	 * 
	 * @param space
	 * @param ship
	 * @param sub_previous
	 *            this is the previous state that is called recursively
	 * @param ship_number
	 * @param number_of_ships
	 * @param plan_goal
	 *            this is the high level plan goal such as "get asteroid"
	 * @param depth_limit
	 *            this is the current depth number which counts down as recursion occurs.
	 * @return
	 */
	private LinkedList<State> recursiveMakePlan(Toroidal2DPhysics space, Ship ship, State sub_previous, int ship_number, int number_of_ships, HighLevelGoals plan_goal, int depth_limit)
	{
		// base case
		// see if our goal is met
		if (plan_goal == HighLevelGoals.LotsOfMoney && sub_previous.base_money > 500)
		{
			LinkedList<State> tail = new LinkedList<State>();
			tail.add(sub_previous);
			return tail;
		}
		
		else if (plan_goal == HighLevelGoals.VisitedBase && sub_previous.action == State.possibleTasks.goToBase)
		{
			LinkedList<State> tail = new LinkedList<State>();
			tail.add(sub_previous);
			return tail;
		}
		
		else if (plan_goal == HighLevelGoals.RefilledEnergy && sub_previous.action == State.possibleTasks.getBeacon)
		{
			LinkedList<State> tail = new LinkedList<State>();
			tail.add(sub_previous);
			return tail;
		}
		
		// and just so we dont go rampant
		else if (depth_limit <= 0)
		{
			return null;
		}
		
		// recurse
		LinkedList<State> children = null;
		State sub_next;
		
		// first check if we can catch a close node
		Asteroid choice = this.getMapDividedClosestAsteroid(space, sub_previous, ship_number, number_of_ships);
		
		if (choice != null)
		{
			sub_next = new State(sub_previous, possibleTasks.mineAsteroid, choice);
			if (State.isPreconditionsSatisfied(space, ship, sub_previous, sub_next))
			{
				children = recursiveMakePlan(space, ship, sub_next, ship_number, number_of_ships, plan_goal, depth_limit - 1);
				if (children != null)
				{
					children.addFirst(sub_next);
					return children;
				}
			}
		}
		
		// try to catch a base
		sub_next = new State(sub_previous, possibleTasks.goToBase, this.getClosestBase(space, ship.getPosition()));
		if (State.isPreconditionsSatisfied(space, ship, sub_previous, sub_next))
		{
			children = recursiveMakePlan(space, ship, sub_next, ship_number, number_of_ships, plan_goal, depth_limit - 1);
			if (children != null)
			{
				children.addFirst(sub_next);
				return children;
			}
		}
		
		// all else fails lets get a beacon
		sub_next = new State(sub_previous, possibleTasks.getBeacon, this.getClosestBeacon(space, ship.getPosition()));
		if (State.isPreconditionsSatisfied(space, ship, sub_previous, sub_next))
		{
			children = recursiveMakePlan(space, ship, sub_next, ship_number, number_of_ships, plan_goal, depth_limit - 1);
			if (children != null)
			{
				children.addFirst(sub_next);
				return children;
			}
		}
		
		return null;
	}
	
	/**
	 * This is responsible for allowing the ships to work "together" on the map by dividing the map up with dividor lines that the ships wont cross.
	 * 
	 * @param space
	 * @param sub_previous
	 *            this is the current state you want divided via asteroids
	 * @param ship_number
	 *            this number is between 0 and number of ships
	 * @param number_of_ships
	 * @return
	 */
	private Asteroid getMapDividedClosestAsteroid(Toroidal2DPhysics space, State sub_previous, int ship_number, int number_of_ships)
	{
		Toroidal2DPhysics local_space = space.deepClone();
		
		ArrayList<Asteroid> local_asteroids = this.getClosestAsteroids(local_space, sub_previous.position, 10);
		local_asteroids.removeAll(sub_previous.asteroidsGone);
		
		for (Asteroid a : local_asteroids)
		{
			Position asteroid_position = a.getPosition();
			
			double length = 0;
			double pos = 0;
			
			// type of division
			if (this.current_division == MapDivision.updown)
			{
				length = Y_RES / number_of_ships;
				pos = asteroid_position.getY();
			}
			else if (this.current_division == MapDivision.leftright)
			{
				length = X_RES / number_of_ships;
				pos = asteroid_position.getX();
			}
			
			if (ship_number * length <= pos && pos <= (ship_number + 1) * length)
			{
				return a;
			}
		}
		
		return null;
	}
	
	/**
	 * a new way of handling astar is completely independent of implementation. Start and end points go in, a list of positions pops out.
	 * 
	 * @param space
	 * @param start
	 *            position
	 * @param end
	 *            position
	 * @param start_object_size
	 *            this is the ship (or whatever) traversing a star
	 * @param shadow_uuid
	 *            this is the ships_uuid associated with the astar path to draw some nice shadows on
	 * @return
	 */
	private ArrayList<Position> independentAStar(Toroidal2DPhysics space, Position start, Position end, double start_object_size, UUID shadow_uuid)
	{
		ArrayList<Node> outer_nodes = new ArrayList<Node>(); // all nodes
		
		// end, this is ZERO
		int i = 0;
		Node goal_node = new Node(end, i, NodeType.goal, 0);
		outer_nodes.add(goal_node);
		i++;
		
		// start, this is full distance!
		Node start_node = new Node(start, i, NodeType.start, space.findShortestDistance(start, end));
		outer_nodes.add(start_node);
		i++;
		
		ArrayList<Node> fast_path = null;
		ArrayList<Shadow> astar_shadows_array = new ArrayList<Shadow>();
		
		for (int e = 1; e < 2; e++)
		{
			ArrayList<Node> nodes = new ArrayList<Node>();
			for (Node n : outer_nodes)
				nodes.add(n.copy());
			
			// fill a grid full of nodes
			calculateNodesGrid(space, RES, SQUARE_PADDING * e, start, global_output, nodes);
			
			// make all connections
			AdjacencyMatrixGraph matrix_graph = calculateDistanceSetConnections(space, start_object_size, nodes, global_output, (int) (MAX_NUM_NODE_CONNECTIONS), NodeConnections.closest);
			
			// find the fastest way through it
			fast_path = AStar(space, matrix_graph, matrix_graph.getNodes().get(1), global_output);
			
			// draw the solutions
			astar_shadows_array.clear();
			
			// draw all the nodes
			// for(Node n: nodes)
			// drawNodesConnections(space, n, n, 1, astar_shadows_array); // draw all nodes
			
			// draw all possible lines
			// this.drawLines(space, matrix_graph, 0, astar_shadows_array);
			
			if (fast_path != null)
				break;
		}
		
		if (fast_path == null)
		{
			ArrayList<Position> fake_a_star = new ArrayList<Position>();
			fake_a_star.add(start);
			fake_a_star.add(end);
			return fake_a_star;
		}
		
		// fast_path is good to go!
		ArrayList<Position> a_star = new ArrayList<Position>();
		for (Node n : fast_path)
			a_star.add(n.position);
		
		// shadows
		drawSolution(space, fast_path, 0, astar_shadows_array, Color.BLUE); // draw the shortest path
		astar_shadows.put(shadow_uuid, astar_shadows_array);
		
		return a_star;
	}
	
	/**
	 * this will take a queue of states (aka, the plan) and draw it on screen with light blue lines.
	 * 
	 * @param plan
	 */
	private void drawPlan(LinkedList<State> plan)
	{
		int radius = 2;
		
		for (int i = 0; i < plan.size() - 1; i++)
		{
			
			shadows.add(new CircleShadow(radius, Color.red, plan.get(i).position));
			shadows.add(new ColorLineShadow(plan.get(i).position, plan.get(i + 1).position, Color.CYAN));
			
			if (global_output)
			{
				System.out.println(plan.get(i).toString());
			}
		}
		
		shadows.add(new CircleShadow(radius, Color.red, plan.get(plan.size() - 1).position));
		
	}
	
	/**
	 * generate a grid of nodes in the rectangle surrounding the diagonal between start and goal be sure to pad this grid
	 * 
	 * @param space
	 * @param divider
	 *            the relative space between points on the grid (10 is a good value)
	 * @param padding
	 *            how far behind the ship should we also include grids? (150 is a good value)
	 * @param start
	 *            is the position of the ship
	 * @param output
	 *            this is a boolean value as to print to console or not
	 * @param nodes
	 *            this should be populated with 2 nodes, 0 is the goal, 1 is the ships current position.
	 */
	private void calculateNodesGrid(Toroidal2DPhysics space, double divider, int padding, Position start, boolean output, ArrayList<Node> nodes)
	{
		if (output)
		{
			System.out.println("******************************************************");
			System.out.println("*                Calculating Node Grid               *");
			System.out.println("******************************************************");
		}
		
		Position goal = nodes.get(0).position;
		
		// default
		int min_x = 0;
		int max_x = Project4Client.X_RES;
		int min_y = 0;
		int max_y = Project4Client.Y_RES;
		
		Vector2D smallest_distance = space.findShortestDistanceVector(start, goal);
		double radian = smallest_distance.getAngle();
		double degree = Math.toDegrees(radian);
		
		if (output)
			System.out.println("Radian: " + radian + " degree: " + Math.toDegrees(radian));
		
		// focus in the search just a little bit
		if (-90 < degree && degree < 90)
		{
			min_x = (int) (start.getX() - padding);
			max_x = (int) (goal.getX() + padding);
		}
		else
		{
			max_x = (int) (start.getX() + padding);
			min_x = (int) (goal.getX() - padding);
		}
		
		if (180 > degree && degree > 0)
		{
			min_y = (int) (start.getY() - padding);
			max_y = (int) (goal.getY() + padding);
		}
		else
		{
			max_y = (int) (start.getY() + padding);
			min_y = (int) (goal.getY() - padding);
		}
		
		int divider_x = (int) (max_x / divider);
		int divider_y = (int) (max_y / divider);
		
		int e = nodes.size();
		for (int i = min_x; i < max_x; i += divider_x)
			for (int j = min_y; j < max_y; j += divider_y)
			{
				Position position = new Position(i, j);
				
				// find the distance to player
				// dont add it if the player is really close
				if (space.findShortestDistance(position, start) > NODEGOAL_DISTANCE)
				{
					nodes.add(new Node(position, e, NodeType.regular, space.findShortestDistance(position, goal)));
					e++;
				}
			}
	}
	
	/**
	 * calculate n many connections between nodes as possible
	 * 
	 * @param space
	 * @param min_distance
	 *            this is how far away two nodes have to be in order to not be considered for connection ( 400 is good)
	 * @param nodes
	 *            this is just the list of nodes already calculated from the calculate nodes grid
	 * @param output
	 *            boolean value as to whether to print to console or not
	 * @param connections
	 *            the number of connections between each node to make (20 is good)
	 * @param type
	 *            this is closest connection, or furthest connection, or random connection.
	 * @return
	 */
	private AdjacencyMatrixGraph calculateDistanceSetConnections(Toroidal2DPhysics space, double min_distance, ArrayList<Node> nodes, boolean output, int connections, NodeConnections type)
	{
		if (connections < 2)
			connections = 2;
		
		if (output)
		{
			System.out.println("******************************************************");
			System.out.println("*               Calculating Connections              *");
			System.out.println("******************************************************");
		}
		
		// remove beacons as they are actually good
		Toroidal2DPhysics local_space = space.deepClone();
		
		if (output)
			System.out.println("Clear bases");
		ArrayList<Base> my_bases = getMyBases(local_space);
		for (int i = my_bases.size() - 1; i >= 0; i--)
			local_space.removeObject(my_bases.get(i));
		
		if (output)
			System.out.println("Clear beacons");
		ArrayList<Beacon> beacons = new ArrayList<Beacon>();
		beacons.addAll(local_space.getBeacons());
		for (int i = beacons.size() - 1; i >= 0; i--)
			local_space.removeObject(beacons.get(i));
		
		if (output)
			System.out.println("Clear asteroids");
		ArrayList<Asteroid> Asteroids = new ArrayList<Asteroid>();
		Asteroids.addAll(local_space.getAsteroids());
		for (int i = Asteroids.size() - 1; i >= 0; i--)
			if (Asteroids.get(i).isMineable())
				local_space.removeObject(Asteroids.get(i));
		
		// remove good ships
		ArrayList<Ship> Ships = new ArrayList<Ship>();
		Ships.addAll(local_space.getShips());
		for (int i = Ships.size() - 1; i >= 0; i--)
			if (Ships.get(i).getTeamName().equals(getTeamName()))
				local_space.removeObject(Ships.get(i));
		
		// add in more dummy asteroids that are moving
		node_tracking.clear();
		/*
		 * ArrayList<Asteroid> Asteroids_bad = new ArrayList<Asteroid>(); Asteroids_bad.addAll(local_space.getAsteroids()); for (int i = Asteroids_bad.size() - 1; i >= 0; i--) { Asteroid bad =
		 * Asteroids_bad.get(i); if (bad.isMoveable()) { Position original_goal = bad.getPosition(); double direction = original_goal.getTranslationalVelocity().getAngle(); double forward_distance =
		 * -bad.getRadius() * 1.5;
		 * 
		 * double trailing_x = forward_distance * Math.cos(direction); double trailing_y = forward_distance * Math.sin(direction);
		 * 
		 * Position extended_goal = new Position(original_goal.getX() - trailing_x, original_goal.getY() - trailing_y); local_space.addObject(new Asteroid(extended_goal, bad.isMineable(),
		 * bad.getRadius(), true));
		 * 
		 * node_tracking.add(new CircleShadow(bad.getRadius(), Color.YELLOW, extended_goal)); } }
		 */
		
		// put everything into our arraylist for checking
		if (output)
			System.out.println("All object collection");
		ArrayList<SpacewarObject> all_objects = new ArrayList<SpacewarObject>();
		all_objects.addAll(local_space.getAllObjects());
		
		ArrayList<Node> visited_nodes = new ArrayList<Node>();
		
		// now we have our nodes, lets see which ones touch
		AdjacencyMatrixGraph my_graph = new AdjacencyMatrixGraph(nodes.size() + 1);
		my_graph.storeNodes(nodes);
		
		// walk through the nodes and find out which ones can touch
		for (Node n1 : nodes)
		{
			if (output)
				System.out.println("Time to do a walk");
			
			// before immediately walking over ALL nodes
			// we can only search for the x closest amount
			// and walk through them much more efficiently.
			PriorityQueue<Double> distances = new PriorityQueue<Double>(nodes.size() + 1);
			HashMap<Double, Node> distance_relations = new HashMap<Double, Node>();
			
			int y = 0;
			for (Node n2 : nodes)
				if (n1.matrix_id != n2.matrix_id && !visited_nodes.contains(n2))
				{
					double distance = local_space.findShortestDistance(n1.position, n2.position);
					
					if (type == NodeConnections.furthest)
						distance = 1.0 / distance;
					else if (type == NodeConnections.random_c)
						distance = random.nextDouble();
					
					if (output)
						System.out.println("Finding distances: " + distance);
					
					// we dont want duplicates
					while (distances.contains(distance))
						distance += 1;
					
					distances.add(distance);
					distance_relations.put(distance, n2);
					y++;
				}
			
			if (connections > y)
				connections = y;
			
			// now walk through the closest amount q we have found.
			for (int q = 0; q < connections && !distances.isEmpty(); q++)
			{
				Node n2 = distance_relations.get(distances.poll());
				
				double distance = local_space.findShortestDistance(n1.position, n2.position);
				
				if (output)
				{
					System.out.println("Node A: " + n1.matrix_id);
					System.out.println("Node B: " + n2.matrix_id);
					System.out.println("Distance: " + distance);
				}
				
				boolean collision = false;
				
				if (output)
					System.out.println("Just about to do a path check");
				collision = !local_space.isPathClearOfObstructions(n1.position, n2.position, all_objects, (int) min_distance);
				
				if (output)
					System.out.println("path check complete");
				
				// set if we can go between these nodes
				if (!collision)
				{
					my_graph.setConnected(n1, n2, distance);
					
					if (output)
						System.out.println("                                                                  Stored: " + distance);
				}
				
			}
			
			visited_nodes.add(n1);
		}
		
		return my_graph;
	}
	
	/**
	 * finally with all nodes connections found, find a path along the nodes.
	 * 
	 * @param space
	 * @param graph
	 *            graph should already be calculated from calculateDistanceSetConnections
	 * @param start
	 *            is the current ships position, although technically this value is also contained in graph already
	 * @param output
	 *            whether to print to console or not.
	 * @return
	 */
	private ArrayList<Node> AStar(Toroidal2DPhysics space, AdjacencyMatrixGraph graph, Node start, boolean output)
	{
		// have to stick the graph into a tree starting from start
		
		if (output)
		{
			System.out.println("******************************************************");
			System.out.println("*                Shortest path with A*               *");
			System.out.println("******************************************************");
		}
		
		ArrayList<Node> closed_visited = new ArrayList<Node>();
		closed_visited.add(start);
		PriorityQueue<Node> fringe = new PriorityQueue<Node>(10, new NodeComparator());
		
		if (output)
			System.out.println("starting at: " + start.toString());
		
		ArrayList<Node> children = graph.getChildren(start);
		for (Node child : children)
		{
			child.root_to_n_distance = space.findShortestDistance(start.position, child.position);
			fringe.add(child.copy());
			if (output)
				System.out.println("child: " + child.toString());
		}
		
		while (true)
		{
			if (output)
				System.out.println("doing a loop");
			
			if (fringe.isEmpty())
				return null;
			
			Node next = fringe.poll();
			
			if (output)
				System.out.println("next is at: " + next.toString());
			
			if (next.node_type == NodeType.goal)
			{
				if (output)
					System.out.println("found the goal: " + next.toString());
				return next.getPathToRoot();
			}
			else
			{
				closed_visited.add(next.copy());
				ArrayList<Node> sub_children = graph.getChildren(next);
				for (Node child : sub_children)
				{
					child.root_to_n_distance = next.root_to_n_distance + space.findShortestDistance(child.position, next.position);
					
					if (output)
						System.out.println("child: " + child.toString());
					
					boolean inserted = false;
					
					// or visited
					for (Node p : closed_visited)
					{
						if (p.matrix_id == child.matrix_id)
						{
							if (output)
								System.out.println("already visited : " + child.toString());
							inserted = true;
							break;
						}
					}
					
					// already there
					if (!inserted)
						for (Node p : fringe)
							if (p.matrix_id == child.matrix_id)
							{
								if (output)
									System.out.println("already fringed : " + child.toString() + (new Formatter()).format("%n  previous child was: ") + p.toString());
								
								if (p.fn() > child.fn())
								{
									p.root_to_n_distance = child.root_to_n_distance;
									p.parent = next;
									
									if (output)
										System.out.println("  this child is better : " + child.toString());
									
								}
								inserted = true;
								break;
							}
					
					// add to fringe
					if (!inserted)
						fringe.add(child);
				}
			}
		}
	}
	
	/**
	 * get a list of all my bases
	 * 
	 * @param space
	 * @return
	 */
	private ArrayList<Base> getMyBases(Toroidal2DPhysics space)
	{
		ArrayList<Base> bases = new ArrayList<Base>();
		
		for (Base base : space.getBases())
			if (base.getTeamName().equalsIgnoreCase(getTeamName()))
				bases.add(base);
		
		return bases;
	}
	
	/**
	 * get the closest base to a position
	 * 
	 * @param space
	 * @param pos
	 * @return
	 */
	private Base getClosestBase(Toroidal2DPhysics space, Position pos)
	{
		ArrayList<Base> bases = getMyBases(space);
		Base closest = null;
		double closest_distance = Double.MAX_VALUE;
		for (Base b : bases)
		{
			double local_distance = space.findShortestDistance(pos, b.getPosition());
			if (local_distance < closest_distance)
			{
				closest_distance = local_distance;
				closest = b;
			}
		}
		
		return closest;
	}
	
	/**
	 * get the count number of closest mineable asteroids
	 * 
	 * @param space
	 * @param ship
	 *            is the position you want to get close to
	 * @param count
	 * @return
	 */
	private ArrayList<Asteroid> getClosestAsteroids(Toroidal2DPhysics space, Position ship, int count)
	{
		// make a copy of space
		Toroidal2DPhysics local_space = space.deepClone();
		
		ArrayList<Asteroid> asteroids = new ArrayList<Asteroid>();
		
		for (int i = 0; i < count; i++)
		{
			Asteroid temp = getClosestAsteroid(local_space, ship);
			if (temp != null)
			{
				asteroids.add(temp);
				local_space.removeObject(temp);
			}
		}
		
		return asteroids;
	}
	
	/**
	 * just get one closest mineable asteroid
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private Asteroid getClosestAsteroid(Toroidal2DPhysics space, Position ship)
	{
		Asteroid close = null;
		double close_distance = Double.MAX_VALUE;
		for (Asteroid as : space.getAsteroids())
			if (as.isMineable())
			{
				double local_distance = space.findShortestDistance(ship, as.getPosition());
				if (close == null || local_distance < close_distance)
				{
					close_distance = local_distance;
					close = as;
				}
			}
		
		return close;
	}
	
	/**
	 * get the most expensive mineable asteroid on the map
	 * 
	 * @param space
	 * @return
	 */
	@SuppressWarnings("unused")
	private Asteroid getMaxAsteroid(Toroidal2DPhysics space)
	{
		Set<Asteroid> asteroids = space.getAsteroids();
		double max = Double.MIN_VALUE;
		Asteroid max_asteroid = null;
		for (Asteroid a : asteroids)
			if (a.getMoney() > max && a.isMineable())
			{
				max = a.getMoney();
				max_asteroid = a;
			}
		
		return max_asteroid;
	}
	
	/**
	 * get the closest beacon to a position
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private Beacon getClosestBeacon(Toroidal2DPhysics space, Position ship)
	{
		Set<Beacon> beacons = space.getBeacons();
		
		double min = Double.MAX_VALUE;
		Beacon min_beacon = null;
		for (Beacon b : beacons)
		{
			double distance = space.findShortestDistance(b.getPosition(), ship);
			if (distance < min)
			{
				min_beacon = b;
				min = distance;
			}
		}
		
		return min_beacon;
	}
	
	/**
	 * linearly interpolate between two values
	 * 
	 * @param min_x
	 * @param max_x
	 * @param between
	 *            should be between min_x and max_x
	 * @param min_y
	 * @param max_y
	 * @return a value between min_y and max_y
	 */
	private double lerp(double min_x, double max_x, double between, double min_y, double max_y)
	{
		double numerator1 = min_y * (between - max_x);
		double denominator1 = (min_x - max_x);
		double numerator2 = max_y * (between - min_x);
		double denominator2 = (max_x - min_x);
		
		double final_value = numerator1 / denominator1 + numerator2 / denominator2;
		
		return final_value;
	}
	
	/**
	 * take an a star set of nodes and draw them
	 * 
	 * @param space
	 * @param nodes
	 *            from astar
	 * @param min_radius
	 *            the radius of dots, set to 0 to draw lines
	 * @param node_shadows
	 *            this will be returned full of shadows
	 * @param c
	 */
	private void drawSolution(Toroidal2DPhysics space, ArrayList<Node> nodes, double min_radius, ArrayList<Shadow> node_shadows, Color c)
	{
		if (nodes == null)
			return;
		
		for (Node n : nodes)
		{
			if (n.parent != null)
				drawNodesConnections(space, n.parent, n, min_radius, node_shadows, c);
		}
	}
	
	/**
	 * take an astar set of nodes and draw their connections
	 * 
	 * @param space
	 * @param a
	 *            start node
	 * @param b
	 *            end node
	 * @param min_radius
	 *            the radius of dots, set to 0 to draw lines
	 * @param node_shadows
	 * @param c
	 */
	private void drawNodesConnections(Toroidal2DPhysics space, Node a, Node b, double min_radius, ArrayList<Shadow> node_shadows, Color c)
	{
		node_shadows.add(new CircleShadow(1, Color.orange, a.position));
		
		if (min_radius > 0)
		{
			double divisors = Math.ceil(space.findShortestDistance(a.position, b.position) / (min_radius * 2));
			for (int j = 0; j < divisors; j++)
			{
				double next_x = lerp(0, divisors, j, a.position.getX(), b.position.getX());
				double next_y = lerp(0, divisors, j, a.position.getY(), b.position.getY());
				
				node_shadows.add(new CircleShadow((int) (min_radius / 5.0), c, new Position(next_x, next_y)));
			}
		}
		else
		{
			LineShadow s = new LineShadow(b.position, a.position, new Vector2D(a.position.getX() - b.position.getX(), a.position.getY() - b.position.getY()));
			s.setLineColor(c);
			node_shadows.add(s);
		}
	}
	
	/**
	 * this is used in conjunction with drawNodesConnections to draw all nodes from a star and all possible connections
	 * 
	 * @param space
	 * @param temp
	 *            this is the graph generated by astar
	 * @param min_radius
	 *            this is the size of dots, zero for lines
	 * @param node_shadows
	 *            the shadows returned to be drawn
	 * @param c
	 */
	@SuppressWarnings("unused")
	private void drawLines(Toroidal2DPhysics space, AdjacencyMatrixGraph temp, double min_radius, ArrayList<Shadow> node_shadows, Color c)
	{
		ArrayList<Node> nodes = temp.getNodes();
		
		ArrayList<Node> visited_nodes = new ArrayList<Node>();
		
		for (Node n1 : nodes)
		{
			for (Node n2 : nodes)
				if (!n1.equals(n2) && !visited_nodes.contains(n2) && temp.getConnected(n1, n2))
					drawNodesConnections(space, n1, n2, min_radius, node_shadows, c);
			
			visited_nodes.add(n1);
		}
		
	}
	
	/**
	 * not used
	 */
	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<SpacewarActionableObject> actionableObjects)
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * get the missiles fired. currently not used
	 * 
	 */
	@Override
	public Map<UUID, SpacewarPowerupEnum> getPowerups(Toroidal2DPhysics space, Set<SpacewarActionableObject> actionableObjects)
	{
		HashMap<UUID, SpacewarPowerupEnum> powerupMap = new HashMap<UUID, SpacewarPowerupEnum>();
		for (Ship ship : space.getShips())
		{
			if (this.shoot.containsKey(ship.getId()) && shoot.get(ship.getId()))
			{
				powerupMap.put(ship.getId(), SpacewarPowerupEnum.FIRE_MISSILE);
				shoot.put(ship.getId(), false);
			}
		}
		
		return powerupMap;
	}
	
	/**
	 * get the team purchase, currently only gets ships and bases if it can afford either.
	 * 
	 */
	@Override
	public Map<UUID, SpacewarPurchaseEnum> getTeamPurchases(Toroidal2DPhysics space, Set<SpacewarActionableObject> actionableObjects, int availableMoney,
			Map<SpacewarPurchaseEnum, Integer> purchaseCosts)
	{
		if (this.purchase_timeout != 0)
		{
			purchase_timeout--;
			return null;
		}
		
		purchase_timeout = this.MAX_TIMEOUT;
		
		HashMap<UUID, SpacewarPurchaseEnum> purchases = new HashMap<UUID, SpacewarPurchaseEnum>();
		double BASE_BUYING_DISTANCE = 400;
		
		int currentCostOfBase = purchaseCosts.get(SpacewarPurchaseEnum.BASE);
		int currentCostOfShip = purchaseCosts.get(SpacewarPurchaseEnum.SHIP);
		
		Set<Ship> ships = space.getShips();
		for (Ship ship : ships)
		{
			can_buy_base.put(ship.getId(), false);
			if (availableMoney >= currentCostOfBase)
			{
				Set<Base> bases = space.getBases();
				
				// how far away is this ship to a base of my team?
				double maxDistance = Double.MAX_VALUE;
				for (Base base : bases)
				{
					if (base.getTeamName().equalsIgnoreCase(getTeamName()))
					{
						double distance = space.findShortestDistance(ship.getPosition(), base.getPosition());
						if (distance < maxDistance)
						{
							maxDistance = distance;
						}
					}
				}
				
				if (maxDistance > BASE_BUYING_DISTANCE)
				{
					if (global_output)
						System.out.println("Can buy a base");
					can_buy_base.put(ship.getId(), true);
				}
			}
			else if (availableMoney >= currentCostOfShip)
			{
				// System.out.println("buying a ship");
				purchases.put(ship.getId(), SpacewarPurchaseEnum.SHIP);
			}
			
			if (can_buy_base.get(ship.getId()))
			{
				// if (global_output)
				System.out.println("attempgin to buy a base");
				purchases.put(ship.getId(), SpacewarPurchaseEnum.BASE);
			}
		}
		
		return purchases;
	}
	
	/**
	 * set and clear the shadows
	 * 
	 */
	@Override
	public Set<Shadow> getShadows()
	{
		HashSet<Shadow> copy = new HashSet<Shadow>();
		for (Shadow s : shadows)
			copy.add(s);
		
		shadows.clear();
		
		return copy;
	}
	
	/**
	 * check and return a boolean value if the ships is in a futile chase against an asteroid.
	 * 
	 * @param ship
	 *            the current ship with all its features
	 * @param space
	 * @param goalPosition
	 *            where the ship wants to go
	 * @return
	 */
	public boolean futileChase(Ship ship, Toroidal2DPhysics space, Position goalPosition)
	{
		
		// set constants
		int minIncrease = myKnowledge.current_chromosome().FCMinIncrease(); // minimum increase in distance to future position of asteroid to register
		int minDecrease = myKnowledge.current_chromosome().FCMinDecrease(); // minimum decrease in distance to current position to register
		int minRadius = myKnowledge.current_chromosome().FCMinRadius(); // minimum distance from goal position to register as "our asteroid"
		double maxAngleDifference = myKnowledge.current_chromosome().FCMaxAngle(); // trigonometry
		
		for (Asteroid a : space.getAsteroids())
		{
			
			// get current radius from ship
			double currentRadius = space.findShortestDistance(ship.getPosition(), a.getPosition());
			
			// check radius from goal to make sure this isn't just some raondom asteroid that happens
			// to satisfy the other properties but isnt out goal
			if (space.findShortestDistance(goalPosition, a.getPosition()) > minRadius)
			{
				continue;
			}
			
			// get velocity vector and position for ship
			Vector2D vShip = ship.getPosition().getTranslationalVelocity();
			Position futureShipPosition = ship.getPosition().deepCopy();
			futureShipPosition.setX(futureShipPosition.getX() + vShip.getXValue());
			futureShipPosition.setY(futureShipPosition.getY() + vShip.getYValue());
			
			// get velocity vector and position for asteroid
			Vector2D vAsteroid = a.getPosition().getTranslationalVelocity();
			Position futureAsteroidPosition = a.getPosition().deepCopy();
			futureAsteroidPosition.setX(futureAsteroidPosition.getX() + vAsteroid.getXValue());
			futureAsteroidPosition.setY(futureAsteroidPosition.getY() + vAsteroid.getYValue());
			
			// get velocity angles for both asteroid and ship velocity and compare.
			// if the ship and asteroid are not moving approximately parallel,
			// then there isn't really a chase to begin with, let alone a futile one
			double aAngle = Math.tan(a.getPosition().getyVelocity() / a.getPosition().getxVelocity());
			double shipAngle = Math.tan(ship.getPosition().getyVelocity() / ship.getPosition().getxVelocity());
			if (Double.isNaN(aAngle) || Math.abs(aAngle - shipAngle) > maxAngleDifference)
			{
				continue;
			}
			
			// check for min decrease to current position of asteroid
			double futureShipToCurrentAsteroidRadius = space.findShortestDistance(futureShipPosition, a.getPosition());
			if (futureShipToCurrentAsteroidRadius + minDecrease > currentRadius)
			{
				continue;
			}
			
			// check for min increase to current position of asteroid
			double futureShipToFutureAsteroidRadius = space.findShortestDistance(futureShipPosition, futureAsteroidPosition);
			if (futureShipToFutureAsteroidRadius - minIncrease < currentRadius)
			{
				continue;
			}
			
			// if we got here without "continue"ing, then there is a futile chase
			System.out.println("Futile Chase is occuring. Asteroid Angle: " + aAngle + " Ship Angle: " + shipAngle);
			
			return true;
		}
		
		// System.out.println("Not futile");
		return false;
	}
}

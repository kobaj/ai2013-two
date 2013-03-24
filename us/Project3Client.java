package grif1252;

import grif1252.Node.NodeType;
import grif1252.State.accomplishStates;
import grif1252.State.possibleTasks;

import java.awt.Color;
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

public class Project3Client extends TeamClient
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
	
	// when calculating a multiplication vector (so we fly faster) how far out should the vector be placed relative to our goal?
	final public static double magnitude_vector = 2500.0;
	
	// how many loops should we go through before recalculating astar and nodes
	final public static int MAX_ITERATIONS = 20;
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
	private HashMap<UUID, Boolean> buy_a_base;
	
	// should we shoot?
	private HashMap<UUID, Boolean> shoot;
	
	private static double NODEGOAL_DISTANCE = 20;
	
	// PROJECT 3 VARIABLES
	private HashMap<UUID, LinkedList<State>> object_plans;
	
	private final int energycutoff = 600;
	private final int moneycutoff = 600;
	
	@Override
	public void initialize()
	{
		ship_goals = new HashMap<Ship, String>();
		local_goals = new HashMap<Ship, Position>();
		
		current_iterations = new HashMap<UUID, Integer>();
		
		shadows = new HashSet<Shadow>();
		
		can_buy_base = new HashMap<UUID, Boolean>();
		buy_a_base = new HashMap<UUID, Boolean>();
		
		shoot = new HashMap<UUID, Boolean>();
		
		object_plans = new HashMap<UUID, LinkedList<State>>();
		
		random = new Random();
	}
	
	// start here
	@Override
	public Map<UUID, SpacewarAction> getMovementStart(Toroidal2DPhysics space, Set<SpacewarActionableObject> actionableObjects)
	{
		if (global_output)
			System.out.println("Begin");
		
		// store necessary variables
		Long time = System.currentTimeMillis();
		X_RES = space.getWidth();
		Y_RES = space.getHeight();
		
		HashMap<UUID, SpacewarAction> actions = new HashMap<UUID, SpacewarAction>();
		Toroidal2DPhysics local_space = space;
		
		// old loop
		for (SpacewarObject actionable : actionableObjects)
			if (actionable instanceof Ship)
			{
				Ship ship = (Ship) actionable;
				
				// first get our plan out of storage
				LinkedList<State> ship_plan = object_plans.get(ship.getId());
				
				// if our plan is empty recalculate a new plan
				boolean recalculate_plan = false;
				if (ship_plan == null || ship_plan.isEmpty())
					recalculate_plan = true;
				
				if (recalculate_plan)
				{
					System.out.println("recalculate plan");
					
					// prepare your anus...er, I mean, stack.
					ship_plan = new LinkedList<State>();
					
					// first set our start state
					try
					{
						State start = new State(ship, local_space);
						
						// get a high level goal based on start
						Asteroid original_goal = getClosestAsteroid(local_space, ship);
						State next = new State(start, possibleTasks.mineAsteroid, original_goal);
						ship_plan.add(next);
						
						System.out.println("many plans");
						
						State sub_previous = next;
						for (int i = 1; i <= 10; i++)
						{
							State sub_next = null;
							
							if (sub_previous.closest_mineable_asteroid == null || sub_previous.energy < this.energycutoff || sub_previous.money > this.moneycutoff || i % 5 == 0)
							{
								if (sub_previous.action == possibleTasks.moveToPosition)
									break;
								
								sub_next = new State(sub_previous, possibleTasks.moveToPosition, this.getClosestBase(space, sub_previous.position).getPosition());
							}
							else
							{
								sub_next = new State(sub_previous, possibleTasks.mineAsteroid, sub_previous.closest_mineable_asteroid);
							}
							
							if (sub_next != null)
							{
								ship_plan.addLast(sub_next);
								sub_previous = sub_next;
							}
						}
						
						// and push it to our storage
						object_plans.put(ship.getId(), ship_plan);
					}
					catch (Exception e)
					{
						System.out.println(e);
						e.printStackTrace();
					}
					// last (do not delete me)
					System.gc();
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
					current_iterations.put(ship.getId(), Project3Client.MAX_ITERATIONS);
					
					// get our next action
					State next_action = ship_plan.peek();
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
						// calcualate astar
						ArrayList<Position> subgoals = this.independentAStar(local_space, ship.getPosition(), next_action.position, ship.getRadius());
						
						Position original_goal = subgoals.get(1);
						
						// extend the goal for higher velocity
						Vector2D v = space.findShortestDistanceVector(ship.getPosition(), original_goal);
						Vector2D distance_unit = v.getUnitVector();
						
						double jakobs_magic_multiplier = magnitude_vector / v.getMagnitude();
						
						Position extended_goal = new Position(original_goal.getX() + distance_unit.getXValue() * jakobs_magic_multiplier, original_goal.getY() + distance_unit.getYValue()
								* jakobs_magic_multiplier);
						
						// push!!
						actions.put(ship.getId(), new MoveAction(local_space, ship.getPosition(), extended_goal));
					}
				}
				else
				{
					current_iterations.put(ship.getId(), ship_iterations - 1);
					
					actions.put(ship.getId(), ship.getCurrentAction());
				}
			}
		
		for (Shadow s : astar_shadows)
			shadows.add(s);
		
		if (global_output)
			System.out.println("TimeSpent in Client: " + (System.currentTimeMillis() - time));
		return actions;
	}
	
	ArrayList<Shadow> astar_shadows = new ArrayList<Shadow>();
	
	private ArrayList<Position> independentAStar(Toroidal2DPhysics space, Position start, Position end, double start_object_size)
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
			astar_shadows.clear();
			
			// draw all the nodes
			// for(Node n: nodes)
			// drawNodesConnections(space, n, n, 1, astar_shadows); // draw all nodes
			
			// draw all possible lines
			// this.drawLines(space, matrix_graph, 0, astar_shadows);
			
			if (fast_path != null)
				break;
		}
		
		if (fast_path == null)
		{
			System.out.println("a star failed");
			
			ArrayList<Position> fake_a_star = new ArrayList<Position>();
			fake_a_star.add(start);
			fake_a_star.add(end);
			return fake_a_star;
		}
		
		// fast_path is good to go!
		
		// shadows
		drawSolution(space, fast_path, 0, astar_shadows, Color.BLUE); // draw the shortest path
		
		// secondary a star layer makes us go faster!
		AdjacencyMatrixGraph matrix_graph = calculateDistanceSetConnections(space, start_object_size, fast_path, global_output, (int) (MAX_NUM_NODE_CONNECTIONS), NodeConnections.closest);
		
		// find the fastest way through it
		ArrayList<Node> faster_path = AStar(space, matrix_graph, matrix_graph.getNodes().get(1), global_output);
		
		ArrayList<Position> a_star = new ArrayList<Position>();
		if (faster_path == null)
		{
			for (Node n : fast_path)
				a_star.add(n.position);
		}
		else
		{
			System.out.println("Faster path found!");
			
			// shadows
			drawSolution(space, faster_path, 0, astar_shadows, Color.GREEN); // draw the shortest path
			
			for (Node n : faster_path)
				a_star.add(n.position);
		}
		
		return a_star;
	}
	
	private void drawPlan(LinkedList<State> plan)
	{
		for (int i = 0; i < plan.size() - 1; i++)
		{
			int radius = 5;
			if (i == plan.size() - 1)
				radius = 10;
			
			shadows.add(new CircleShadow(radius, Color.red, plan.get(i).position));
			shadows.add(new ColorLineShadow(plan.get(i).position, plan.get(i + 1).position, Color.CYAN));
			
			if (global_output)
			{
				System.out.println(plan.get(i).toString());
			}
		}
		
	}
	
	private ArrayList<Asteroid> get_impending_asteroid_collision_uuids(KnowledgeGraph kg, Ship ship)
	{
		ArrayList<Relation> relations = kg.getRelationsFrom(ship);
		ArrayList<Asteroid> collision_asteroid = new ArrayList<Asteroid>();
		for (Relation r : relations)
		{
			if (ShipApproachingAsteroid.class.isAssignableFrom(r.getClass()))
				if (Asteroid.class.isAssignableFrom(r.B().getClass()))
				{
					Asteroid b = Asteroid.class.cast(r.B());
					if (b.isMineable())
						collision_asteroid.add(b);
				}
		}
		return collision_asteroid;
	}
	
	private SpacewarAction goalHueristic(Toroidal2DPhysics space, Ship ship, KnowledgeGraph kg, SpacewarAction newAction)
	{
		
		// first lets find out if the enemy and our ship are going towards the same goal
		ArrayList<Asteroid> my_asteroids = get_impending_asteroid_collision_uuids(kg, ship);
		
		// see if I can get it to work
		ArrayList<Relation> a = kg.getRelationsFrom(ship, ShipApproachingAsteroid.class);
		if (a.size() > 0)
		{
			ArrayList<Relation> b = kg.getRelationsTo(a.get(0).B(), ShipApproachingAsteroid.class);
			// System.out.println(b.size() - 1 + " Other ships headed towards my asteroid");
			
		}
		else
		{
			// System.out.println("Still A*ing");
		}
		
		for (Asteroid as : my_asteroids)
		{
			// this.my_shadow_manager.put(ship.getId() + "my_collision", new ColorLineShadow(ship.getPosition(), as.getPosition(), Color.cyan));
		}
		
		for (Ship other_ship : space.getShips())
		{
			if (!other_ship.getId().equals(ship.getId()))
			{
				ArrayList<Asteroid> other_asteroids = get_impending_asteroid_collision_uuids(kg, other_ship);
				
				// for (Asteroid as : other_asteroids)
				// this.my_shadow_manager.put(other_ship.getId() + "their_collision", new ColorLineShadow(other_ship.getPosition(), as.getPosition(), Color.LIGHT_GRAY));
				
				// matching ids
				for (Asteroid my_collision : my_asteroids)
					for (Asteroid other_collision : other_asteroids)
						if (other_collision.getId().equals(my_collision.getId()))
						{
							
							// System.out.println("we are headed to the same place... HELL!");
							double my_distance = space.findShortestDistance(ship.getPosition(), other_collision.getPosition());
							double their_distance = space.findShortestDistance(other_ship.getPosition(), other_collision.getPosition());
							
							Color my_color = Color.GREEN;
							Color their_color = Color.RED;
							
							if (my_distance > their_distance)
							{
								my_color = Color.RED;
								their_color = Color.GREEN;
								
								shoot.put(ship.getId(), true);
							}
							
							// draw the impending doom
							// this.my_shadow_manager.put(ship.getId() + "my_collision", new ColorLineShadow(ship.getPosition(), other_collision.getPosition(), my_color));
							// this.my_shadow_manager.put(other_ship.getId() + "their_collision", new ColorLineShadow(other_ship.getPosition(), other_collision.getPosition(), their_color));
						}
			}
		}
		
		return newAction;
	}
	
	// find start and goal nodes
	// start is ship
	// goal is calculated based on priority of asteroids, bases, beacons, etc.
	private SpacewarObject addStartAndGoal(Toroidal2DPhysics space, Ship ship, ArrayList<Node> nodes, boolean output, ArrayList<SpacewarObject> out_goal)
	{
		// we add two nodes, one for start, one for destination
		
		if (output)
		{
			System.out.println("******************************************************");
			System.out.println("*                 Start and Goal Nodes               *");
			System.out.println("******************************************************");
		}
		
		int i = 0;
		
		// this is our intelligent search system
		Asteroid goal_max_asteroid = getMaxAsteroid(space);
		ArrayList<Asteroid> goal_close_asteroids = getClosestAsteroids(space, ship, 6);
		ArrayList<Base> bases = getMyBases(space);
		Set<Beacon> beacons = space.getBeacons();
		
		// we want things to have lower priority depending on things
		// like how much money we are carrying and how much energy we have left
		HashMap<Double, SpacewarObject> relations = new HashMap<Double, SpacewarObject>();
		PriorityQueue<Double> intelligent_select = new PriorityQueue<Double>(4);
		for (Asteroid goal_close_asteroid : goal_close_asteroids)
			if (goal_close_asteroid != null)
			{
				double distance = space.findShortestDistance(goal_close_asteroid.getPosition(), ship.getPosition());
				intelligent_select.add(distance);
				relations.put(distance, goal_close_asteroid);
				
				if (output)
					System.out.println("Adding close astroid, distance of :" + distance);
			}
		for (Base base : bases)
			if (base != null)
			{
				double distance = space.findShortestDistance(base.getPosition(), ship.getPosition());
				
				// lower the distance (aka, priority) if we have lots of money or low energy
				distance = distance / Math.pow((ship.getMoney() / Project3Client.MONEY_RETURN), 2);
				distance = distance * ship.getEnergy() / Project3Client.BEACON_GET;
				
				intelligent_select.add(distance);
				relations.put(distance, base);
				
				if (output)
					System.out.println("Adding close base, distance of :" + distance);
			}
		for (Beacon goal_close_beacon : beacons)
			if (goal_close_beacon != null)
			{
				double distance = space.findShortestDistance(goal_close_beacon.getPosition(), ship.getPosition());
				
				// raise the distance as we get energy
				distance = distance * ship.getEnergy() / Project3Client.BEACON_GET;
				
				intelligent_select.add(distance);
				relations.put(distance, goal_close_beacon);
				
				if (output)
					System.out.println("Adding close beacon, distance of :" + distance);
			}
		if (goal_max_asteroid != null)
		{
			double distance = space.findShortestDistance(goal_max_asteroid.getPosition(), ship.getPosition());
			
			// decrease distance/priority if it is higher value
			distance = distance / Math.pow(goal_max_asteroid.getMoney() / ASTEROID_SIZE, 2);
			
			intelligent_select.add(distance);
			relations.put(distance, goal_max_asteroid);
			
			if (output)
				System.out.println("Adding max astroid, distance of :" + distance);
		}
		
		SpacewarObject goal = null;
		
		while (!intelligent_select.isEmpty() && goal == null)
		{
			double closest_goal = 0;
			try
			{
				closest_goal = intelligent_select.poll();
			}
			catch (NullPointerException e)
			{
				break;
			}
			
			SpacewarObject closest_object = relations.get(closest_goal);
			
			// return if we have some money
			if (Base.class.isAssignableFrom(closest_object.getClass()))
			{
				if (output)
					System.out.println("picking the base");
				goal = closest_object;
				buy_a_base.put(ship.getId(), true);
			}
			else if (Beacon.class.isAssignableFrom(closest_object.getClass()))
			{
				if (output)
					System.out.println("picking the beacon");
				goal = closest_object;
				buy_a_base.put(ship.getId(), true);
			}
			else if (Asteroid.class.isAssignableFrom(closest_object.getClass()))
			{
				if (output)
					System.out.println("picking the asteroid " + closest_goal + " money: " + ((Asteroid) closest_object).getMoney());
				// if (!((Asteroid) closest_object).isMoveable())
				goal = closest_object;
			}
			
			for (SpacewarObject forbidden : out_goal)
				if (goal != null)
					if (goal.getPosition().equals(forbidden.getPosition()))
						goal = null;
		}
		
		if (output)
			System.out.println("Done Picking");
		
		// and just a catch all
		if (goal == null)
		{
			return null;
		}
		
		out_goal.add(goal);
		Node goal_node = new Node(goal.getPosition(), i, NodeType.goal, 0);
		nodes.add(goal_node);
		i++;
		
		// next
		Node start_node = new Node(ship.getPosition(), i, NodeType.start, space.findShortestDistance(ship.getPosition(), goal.getPosition()));
		nodes.add(start_node);
		i++;
		
		// remove from space so we can do collision detection
		space.removeObject(ship);
		if (!Base.class.isAssignableFrom(goal.getClass()))
			space.removeObject(goal);
		
		return goal;
	}
	
	// generate a grid of nodes in the rectangle surrounding the diagonal between start and goal
	// be sure to pad this grid
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
		int max_x = Project3Client.X_RES;
		int min_y = 0;
		int max_y = Project3Client.Y_RES;
		
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
	
	// calculate n many connections between nodes as possible
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
	
	// finally with all nodes connections found, find a path along the nodes.
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
	
	// methods to help find appropriot goals
	
	private ArrayList<Base> getMyBases(Toroidal2DPhysics space)
	{
		ArrayList<Base> bases = new ArrayList<Base>();
		
		for (Base base : space.getBases())
			if (base.getTeamName().equalsIgnoreCase(getTeamName()))
				bases.add(base);
		
		return bases;
	}
	
	private Base getClosestBase(Toroidal2DPhysics space, Position pos)
	{
		ArrayList<Base> bases = getMyBases(space);
		Base closest = null;
		double closest_distance = Double.MAX_VALUE;
		for(Base b: bases)
		{
			double local_distance = space.findShortestDistance(pos, b.getPosition());  
			if(local_distance < closest_distance)
			{
				closest_distance = local_distance;
				closest = b;
			}
		}
		
		return closest;
	}
	
	private ArrayList<Asteroid> getClosestAsteroids(Toroidal2DPhysics space, Ship ship, int count)
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
	
	// returns the closest MINEABLE asteroid
	private Asteroid getClosestAsteroid(Toroidal2DPhysics space, Ship ship)
	{
		Asteroid close = null;
		double close_distance = Double.MAX_VALUE;
		for (Asteroid as : space.getAsteroids())
			if (as.isMineable())
			{
				double local_distance = space.findShortestDistance(ship.getPosition(), as.getPosition());
				if (close == null || local_distance < close_distance)
				{
					close_distance = local_distance;
					close = as;
				}
			}
		
		return close;
	}
	
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
	
	private Beacon getClosestBeacon(Toroidal2DPhysics space, Ship ship)
	{
		Set<Beacon> beacons = space.getBeacons();
		
		double min = Double.MAX_VALUE;
		Beacon min_beacon = null;
		for (Beacon b : beacons)
		{
			double distance = space.findShortestDistance(b.getPosition(), ship.getPosition());
			if (distance < min)
			{
				min_beacon = b;
				min = distance;
			}
		}
		
		return min_beacon;
	}
	
	// methods that are needed to calculated math values
	
	private double lerp(double min_x, double max_x, double between, double min_y, double max_y)
	{
		double numerator1 = min_y * (between - max_x);
		double denominator1 = (min_x - max_x);
		double numerator2 = max_y * (between - min_x);
		double denominator2 = (max_x - min_x);
		
		double final_value = numerator1 / denominator1 + numerator2 / denominator2;
		
		return final_value;
	}
	
	@SuppressWarnings("unused")
	private Position get_half_way_point(Position a, Position b)
	{
		return new Position(((a.getX() + b.getX()) / 2.0), ((a.getY() + b.getY()) / 2.0));
	}
	
	// everything below here is dealing with drawing and shadows
	
	@SuppressWarnings("unused")
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
	
	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<SpacewarActionableObject> actionableObjects)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public Map<UUID, SpacewarPowerupEnum> getPowerups(Toroidal2DPhysics space, Set<SpacewarActionableObject> actionableObjects)
	{
		HashMap<UUID, SpacewarPowerupEnum> powerupMap = new HashMap<UUID, SpacewarPowerupEnum>();
		for (Ship ship : space.getShips())
		{
			if (this.shoot.containsKey(ship.getId()) && shoot.get(ship.getId()))
			{
				powerupMap.put(ship.getId(), SpacewarPowerupEnum.FIRE_EMP);
				shoot.put(ship.getId(), false);
			}
		}
		return powerupMap;
	}
	
	@Override
	public Map<UUID, SpacewarPurchaseEnum> getTeamPurchases(Toroidal2DPhysics space, Set<SpacewarActionableObject> actionableObjects, int availableMoney,
			Map<SpacewarPurchaseEnum, Integer> purchaseCosts)
	{
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
				purchases.put(ship.getId(), SpacewarPurchaseEnum.SHIP);
			}
			
			if (can_buy_base.get(ship.getId()) && this.buy_a_base.get(ship.getId()))
			{
				if (global_output)
					System.out.println("buying a base");
				purchases.put(ship.getId(), SpacewarPurchaseEnum.BASE);
			}
		}
		
		return purchases;
	}
	
	@Override
	public void shutDown()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public Set<Shadow> getShadows()
	{
		HashSet<Shadow> copy = new HashSet<Shadow>();
		for (Shadow s : shadows)
			copy.add(s);
		
		shadows.clear();
		
		return copy;
	}
}

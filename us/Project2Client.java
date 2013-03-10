
package grif1252;

import grif1252.Node.NodeType;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
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
import spacewar2.powerups.SpacewarPowerup;
import spacewar2.shadows.CircleShadow;
import spacewar2.shadows.LineShadow;
import spacewar2.shadows.Shadow;
import spacewar2.simulator.Toroidal2DPhysics;
import spacewar2.utilities.Position;
import spacewar2.utilities.Vector2D;

public class Project2Client extends TeamClient
{
	// ways of searching for nodes
	public static enum NodeConnections
	{
		closest, furthest, random
	};
	
	// ship goals are overall goals (base, beacon, or mineable asteroid)
	HashMap<Ship, String> ship_goals;
	// local goals are nodes inbetween the ship and its overall goal
	HashMap<Ship, Position> local_goals;
	
	// this variable determines how close a ship has to be to its local goal before
	// the goal is considered reached
	final public static int SUBGOAL_DISTANCE = 30;
	
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
	final public static int MAX_ITERATIONS = 60;
	HashMap<Ship, Integer> current_iterations;
	
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
	
	// no_draw = true means draw shadows if shadows are uncommented in getAction
	final private boolean no_draw = false;
	
	// global_output = true means it will output to console (Except heartbeat always outputs)
	final private boolean global_output = false;
	
	public ShadowManager my_shadow_manager;
	
	// can we buy a base?
	private HashMap<UUID, Boolean> can_buy_base;
	private HashMap<UUID, Boolean> buy_a_base;
	
	@Override
	public void initialize()
	{
		ship_goals = new HashMap<Ship, String>();
		local_goals = new HashMap<Ship, Position>();
		
		current_iterations = new HashMap<Ship, Integer>();
		
		my_shadow_manager = new ShadowManager();
		
		can_buy_base = new HashMap<UUID, Boolean>();
		buy_a_base = new HashMap<UUID, Boolean>();
		
		random = new Random();
	}
	
	// basic overview
	// first we check and see if any goals have been gotten or we need to update star because of iterations
	// if yes then nodes are generated in a rectangle grid around the diagonal between the ship and its goal
	// the goal is picked by addStartAndGoal which picks a goal based on priority
	// then connections are made between nodes
	// then an astar path is found between nodes
	
	@Override
	public Map<UUID, SpacewarAction> getMovementStart(Toroidal2DPhysics space, Set<SpacewarActionableObject> actionableObjects)
	{
		// store necessary variables
		Long time = System.currentTimeMillis();
		X_RES = space.getWidth();
		Y_RES = space.getHeight();
		
		// switch shadows
		my_shadow_manager.switchShadows();
		
		HashMap<UUID, SpacewarAction> actions = new HashMap<UUID, SpacewarAction>();
		Toroidal2DPhysics local_space = space.deepClone();

		// new stuff
		KnowledgeGraph kg = new KnowledgeGraph(space,my_shadow_manager);
		ArrayList<Relation> relations = kg.edges;
		for(Relation r : relations){
			my_shadow_manager.put(r.A().getId().toString() + " to " + r.B().getId().toString()  , new LineShadow(r.B().getPosition(), r.A().getPosition(), new Vector2D(r.A().getPosition().getX() - r.B().getPosition().getX(), r.A().getPosition().getY() - r.B().getPosition().getY())));
			//my_shadow_manager.put(r.B().getId().toString(), new CircleShadow(2, new Color(0,0,255), r.B().getPosition()));
		}

		SpacewarObject goal = null;
		
		for (SpacewarObject actionable : actionableObjects)
			if (actionable instanceof Ship)
			{
				Ship ship = (Ship) actionable;
				SpacewarAction current = ship.getCurrentAction();


				// work on iterations
				if (current_iterations.get(ship) == null)
					current_iterations.put(ship, MAX_ITERATIONS);
				current_iterations.put(ship, current_iterations.get(ship) - 1);
				
				// see if the map contains our goal anymore
				String arc_goal = ship_goals.get(ship);
				boolean goal_exists = true;
				if (arc_goal != null)
				{
					goal_exists = false;
					
					for (Asteroid as : local_space.getAsteroids())
						if (as.getPosition().toString().equals(arc_goal))
						{
							goal_exists = true;
						}
					
					for (Beacon be : local_space.getBeacons())
						if (be.getPosition().toString().equals(arc_goal))
						{
							goal_exists = true;
						}
					
					for (Base ba : local_space.getBases())
						if (ba.getPosition().toString().equals(arc_goal))
						{
							goal_exists = true;
						}
				}
				
				// we're close enough. doesnt have to be perfect.
				Position sub_goal = local_goals.get(ship);
				if (sub_goal != null)
					if (local_space.findShortestDistance(ship.getPosition(), sub_goal) < Project2Client.SUBGOAL_DISTANCE)
					{
						// System.out.println("short circuited distance!");
						goal_exists = false;
					}
				
				boolean enemy_closer = false;
				if(goal != null){
					ArrayList<Relation> othersApproachingGoal = kg.getRelationsTo(goal, ApproachingCurrentPosition.class);
					ArrayList<Relation> usApproaching = kg.getRelations(ship, goal, ApproachingCurrentPosition.class) ;
					for(Relation o : othersApproachingGoal){
						if(!o.A().equals(ship)){
							System.out.println("checking enemy distance to goal");
							if(usApproaching.size() == 0){
								enemy_closer = true;
								System.out.println("enemy close to goal, giving up");
							}else if(((ApproachingCurrentPosition) usApproaching.get(0)).steps() > ((ApproachingCurrentPosition) o).steps()){
								enemy_closer = true ;
								System.out.println("enemy close to goal, giving up");
							}
						}
					}
				}
				
				// get next ship action
				if (current == null || current.isMovementFinished(space) || current_iterations.get(ship) <= 0 || !goal_exists || enemy_closer)
				{
					current_iterations.put(ship, MAX_ITERATIONS);
					
					Toroidal2DPhysics sub_local_space = local_space.deepClone();
					
					AdjacencyMatrixGraph matrix_graph = null; // all connections between all nodes
					ArrayList<Node> fast_path = null; // fastest path through nodes
					
					// list of forbidden goals
					ArrayList<SpacewarObject> out_goal = new ArrayList<SpacewarObject>();
					goal = null;
					
					int e = 1;
					int i = 1;
					boolean breakout = false;
					
					while (true)
					{
						ArrayList<Node> outer_nodes = new ArrayList<Node>(); // all nodes
						
						goal = addStartAndGoal(sub_local_space, ship, outer_nodes, false, out_goal);
						
						if (goal == null)
							break;
						
						for (; e < 3; e++)
						{
							ArrayList<Node> nodes = new ArrayList<Node>();
							for (Node n : outer_nodes)
								nodes.add(n.copy());
							
							calculateNodesGrid(sub_local_space, RES, Project2Client.SQUARE_PADDING, ship, false, nodes);
							
							// make all connections
							matrix_graph = calculateDistanceSetConnections(sub_local_space, ship.getRadius(), nodes, false, (int) (MAX_NUM_NODE_CONNECTIONS), NodeConnections.closest, ship);
							
							// find the fastest way through it
							fast_path = AStar(sub_local_space, matrix_graph, matrix_graph.getNodes().get(1), global_output);
							
							// move on
							if (fast_path != null)
							{
								breakout = true;
							}
							
							if (breakout)
								break;
							
						}
						
						if (breakout)
							break;
						
					}
					
					if (global_output)
						System.out.println("LOOP ran for " + e * i + " times. (" + e + ", " + i + ")");
					
					// un-comment to draw lines and shadows
					ArrayList<Shadow> node_shadows = new ArrayList<Shadow>();
					// for(Node n: nodes)
					// drawNodesConnections(space, n, n, ship.getRadius(), node_shadows); // draw all nodes
					// drawLines(space, matrix_graph, 0, node_shadows); // draw all the lines connecting all nodes
					drawSolution(local_space, fast_path, ship.getRadius(), node_shadows); // draw the shortest path
					my_shadow_manager.put(ship.getId() + "sources", node_shadows);
					
					// make the goals
					Position currentPosition = ship.getPosition();
					Position newGoal = null;
					if (fast_path != null && fast_path.get(1) != null)
					{
						newGoal = fast_path.get(1).position;
						
						// store our goal
						if (goal != null)
						{
							ship_goals.put(ship, goal.getPosition().toString());
							// my_shadow_manager.put(ship.getId() + "goal_shadow", new CircleShadow(5, new Color(255, 0, 0), goal.getPosition()));
						}
						
						if (fast_path.get(1).node_type != NodeType.goal)
							local_goals.put(ship, newGoal);
						else
							local_goals.remove(ship);
					}
					else
					{
						if (global_output)
							System.out.println("********Could not find path*********");
						newGoal = this.getClosestAsteroid(local_space, ship).getPosition();// local_space.getRandomFreeLocation(random, ship.getRadius()); // get next movement
						
						// remove our goa
						// ship_goals.put(ship, newGoal.toString());
						ship_goals.remove(ship);
						local_goals.remove(ship);
					}
					
					// extend the goal for higher velocity
					Vector2D v = space.findShortestDistanceVector(ship.getPosition(), newGoal);
					Vector2D distance_unit = v.getUnitVector();
					
					ArrayList<Shadow> goal_shadow = new ArrayList<Shadow>();
					SpacewarAction newAction;
					
					double jakobs_magic_multiplier = magnitude_vector / v.getMagnitude();
					
					if(kg.getRelations(ship, goal, ApproachingCurrentPosition.class).size() > 0){
						
						System.out.println("Final Approach - Increasing multiplier");
						my_shadow_manager.put(ship.getId().toString() + " to " + goal.getId().toString()  , new LineShadow(goal.getPosition(), ship.getPosition(), new Vector2D(ship.getPosition().getX() - goal.getPosition().getX(), ship.getPosition().getY() - goal.getPosition().getY())));

						jakobs_magic_multiplier *= 5;
						
					}
					
					Position extended_goal = new Position(newGoal.getX() + distance_unit.getXValue() * jakobs_magic_multiplier, newGoal.getY() + distance_unit.getYValue() * jakobs_magic_multiplier);
					newAction = new MoveAction(local_space, currentPosition, extended_goal);
					
					goal_shadow.add(new CircleShadow(5, new Color(240, 100, 0), extended_goal));
					
					my_shadow_manager.put(ship.getId() + "destination", goal_shadow);
					
					// finally
					actions.put(ship.getId(), newAction);
					
					if (global_output)
						System.out.println("Finished with ship: " + ship.toString());
					
					//last (do not delete me)
					System.gc();
				}
				else
				{
					// current ship action
					actions.put(ship.getId(), ship.getCurrentAction());
				}
			}
		
		if (global_output)
			System.out.println("TimeSpent in Client: " + (System.currentTimeMillis() - time));
		return actions;
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
		ArrayList<Base> bases = getMyBases(space, ship);
		ArrayList<Beacon> beacons = space.getBeacons();
		
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
				distance = distance / Math.pow((ship.getMoney() / Project2Client.MONEY_RETURN), 2);
				distance = distance * ship.getEnergy() / Project2Client.BEACON_GET;
				
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
				distance = distance * ship.getEnergy() / Project2Client.BEACON_GET;
				
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
	private void calculateNodesGrid(Toroidal2DPhysics space, double divider, int padding, Ship ship, boolean output, ArrayList<Node> nodes)
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
		int max_x = Project2Client.X_RES;
		int min_y = 0;
		int max_y = Project2Client.Y_RES;
		
		Vector2D smallest_distance = space.findShortestDistanceVector(ship.getPosition(), goal);
		double radian = smallest_distance.getAngle();
		double degree = Math.toDegrees(radian);
		
		if (output)
			System.out.println("Radian: " + radian + " degree: " + Math.toDegrees(radian));
		
		// focus in the search just a little bit
		if (-90 < degree && degree < 90)
		{
			min_x = (int) (ship.getPosition().getX() - padding);
			max_x = (int) (goal.getX() + padding);
		}
		else
		{
			max_x = (int) (ship.getPosition().getX() + padding);
			min_x = (int) (goal.getX() - padding);
		}
		
		if (180 > degree && degree > 0)
		{
			min_y = (int) (ship.getPosition().getY() - padding);
			max_y = (int) (goal.getY() + padding);
		}
		else
		{
			max_y = (int) (ship.getPosition().getY() + padding);
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
				if (space.findShortestDistance(position, ship.getPosition()) > SUBGOAL_DISTANCE)
				{
					nodes.add(new Node(position, e, NodeType.regular, space.findShortestDistance(position, goal)));
					e++;
				}
			}
	}
	
	// calculate n many connections between nodes as possible
	private AdjacencyMatrixGraph calculateDistanceSetConnections(Toroidal2DPhysics space, double min_distance, ArrayList<Node> nodes, boolean output, int connections, NodeConnections type, Ship ship)
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
		
		ArrayList<Base> my_bases = getMyBases(local_space, ship);
		for (int i = my_bases.size() - 1; i >= 0; i--)
			local_space.removeObject(my_bases.get(i));
		
		ArrayList<Beacon> beacons = local_space.getBeacons();
		for (int i = beacons.size() - 1; i >= 0; i--)
			local_space.removeObject(beacons.get(i));
		
		ArrayList<Asteroid> asteroids = local_space.getAsteroids();
		for (int i = asteroids.size() - 1; i >= 0; i--)
			if (asteroids.get(i).isMineable())
				local_space.removeObject(asteroids.get(i));
		
		ArrayList<Node> visited_nodes = new ArrayList<Node>();
		
		// now we have our nodes, lets see which ones touch
		AdjacencyMatrixGraph my_graph = new AdjacencyMatrixGraph(nodes.size() + 1);
		my_graph.storeNodes(nodes);
		
		// walk through the nodes and find out which ones can touch
		for (Node n1 : nodes)
		{
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
					else if (type == NodeConnections.random)
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
				
				// see if there is anything between n1 and n2 by lerping from
				// n1 to n2, checking positions inbetween.
				// ideally this will be replaced with a much better circle in
				// rectangle collision check one day.
				int divisors = (int) Math.ceil(distance / min_distance);
				boolean collision = false;
				
				for (int j = 1; j < divisors - 1; j++)
				{
					double next_x = lerp(0, divisors, j, n1.position.getX(), n2.position.getX());
					double next_y = lerp(0, divisors, j, n1.position.getY(), n2.position.getY());
					
					if (!local_space.isLocationFree(new Position(next_x, next_y), (int) (min_distance * 2.0)))
					{
						if (output)
							System.out.println("                                                                  Collision");
						
						collision = true;
						break;
					}
				}
				
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
	
	private ArrayList<Base> getMyBases(Toroidal2DPhysics space, Ship ship)
	{
		ArrayList<Base> bases = new ArrayList<Base>();
		
		for (Base base : space.getBases())
			if (base.getTeamName().equalsIgnoreCase(ship.getTeamName()))
				bases.add(base);
		
		return bases;
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
		ArrayList<Asteroid> asteroids = space.getAsteroids();
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
		ArrayList<Beacon> beacons = space.getBeacons();
		
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
	private void drawSolution(Toroidal2DPhysics space, ArrayList<Node> nodes, double min_radius, ArrayList<Shadow> node_shadows)
	{
		if (nodes == null)
			return;
		
		for (Node n : nodes)
		{
			if (n.parent != null)
				drawNodesConnections(space, n.parent, n, min_radius, node_shadows);
		}
	}
	
	private void drawNodesConnections(Toroidal2DPhysics space, Node a, Node b, double min_radius, ArrayList<Shadow> node_shadows)
	{
		node_shadows.add(new CircleShadow(1, Color.orange, a.position));
		
		if (min_radius > 0)
		{
			double divisors = Math.ceil(space.findShortestDistance(a.position, b.position) / (min_radius * 2));
			for (int j = 0; j < divisors; j++)
			{
				double next_x = lerp(0, divisors, j, a.position.getX(), b.position.getX());
				double next_y = lerp(0, divisors, j, a.position.getY(), b.position.getY());
				
				node_shadows.add(new CircleShadow((int) (min_radius / 5.0), getTeamColor(), new Position(next_x, next_y)));
			}
		}
		else
		{
			node_shadows.add(new LineShadow(b.position, a.position, new Vector2D(a.position.getX() - b.position.getX(), a.position.getY() - b.position.getY())));
		}
	}
	
	@SuppressWarnings("unused")
	private void drawLines(Toroidal2DPhysics space, AdjacencyMatrixGraph temp, double min_radius, ArrayList<Shadow> node_shadows)
	{
		ArrayList<Node> nodes = temp.getNodes();
		
		ArrayList<Node> visited_nodes = new ArrayList<Node>();
		
		for (Node n1 : nodes)
		{
			for (Node n2 : nodes)
				if (!n1.equals(n2) && !visited_nodes.contains(n2) && temp.getConnected(n1, n2))
					drawNodesConnections(space, n1, n2, min_radius, node_shadows);
			
			visited_nodes.add(n1);
		}
		
	}
	
	@Override
	public Set<Shadow> getNewShadows()
	{
		if (no_draw)
			return null;
		
		return my_shadow_manager.getNewShadows();
	}
	
	@Override
	public Set<Shadow> getOldShadows()
	{
		if (no_draw)
			return null;
		
		return my_shadow_manager.getOldShadows();
	}
	
	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<SpacewarActionableObject> actionableObjects)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public Map<UUID, SpacewarPowerup> getPowerups(Toroidal2DPhysics space, Set<SpacewarActionableObject> actionableObjects)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Map<UUID, SpacewarPurchaseEnum> getTeamPurchases(Toroidal2DPhysics space, Set<Ship> ships, int availableMoney, int currentCostOfBase)
	{
		
		HashMap<UUID, SpacewarPurchaseEnum> purchases = new HashMap<UUID, SpacewarPurchaseEnum>();
		double BASE_BUYING_DISTANCE = 400;
		
		for (Ship ship : ships)
		{
			can_buy_base.put(ship.getId(), false);
			if (availableMoney >= currentCostOfBase)
			{
				ArrayList<Base> bases = space.getBases();
				
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
					System.out.println("Can buy a base");
					can_buy_base.put(ship.getId(), true);
				}
				
			}
			
			if (can_buy_base.get(ship.getId()) && this.buy_a_base.get(ship.getId()))
			{
				
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
}

package grif1252;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import spacewar2.shadows.Shadow;

public class ShadowManager
{
	// Benefits of ShadowManager are two fold
	// 1. Shadows are redrawn every frame, this means
	// 1.a. They function more like traditional games where every frame is redrawn.
	// 1.b. Its not up to the programmer to remember which shadows are old or new (as all shadows are 'new').
	// 2. Shadows can be grouped, this means that shadows can be associated with ships, asteroids, etc, and as such
	// entire groups can be 'turned off' or 'on' if needed.
	
	private HashMap<String, ArrayList<Shadow>> managedShadows;
	private ArrayList<Shadow> newShadows;
	private ArrayList<Shadow> oldShadows;
	
	private Random random;
	
	private boolean draw = false;
	
	/**
	 * Setup the shadow manager
	 */
	public ShadowManager()
	{
		managedShadows = new HashMap<String, ArrayList<Shadow>>();
		newShadows = new ArrayList<Shadow>();
		oldShadows = new ArrayList<Shadow>();
		random = new Random();
	}
	
	
	/**
	 * Add a shadow to the manager with an ID association.
	 * 
	 * @param id
	 *            The ID associated with the shadow.
	 * @param shadow
	 *            The shadow to be added.
	 */
	public void put(String id, Shadow shadow)
	{
		if(!draw)
			return;
		
		ArrayList<Shadow> shadows = new ArrayList<Shadow>();
		shadows.add(shadow);
		put(id, shadows);
	}
	
	/**
	 * Add several shadows to the manager with an ID association.
	 * 
	 * @param id
	 *            The ID associated with the shadows.
	 * @param shadows
	 *            The shadows to be added.
	 */
	public void put(String id, ArrayList<Shadow> shadows)
	{
		if(!draw)
			return;
		
		managedShadows.put(id, shadows);
	}
	
	/**
	 * Delete all old shadows and add all new shadows every frame. This must be called every time getMovementStart is called.
	 * 
	 */
	public void switchShadows()
	{
		oldShadows = new ArrayList<Shadow>(newShadows);
		newShadows.clear();
		
		Iterator<Entry<String, ArrayList<Shadow>>> it = managedShadows.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<String, ArrayList<Shadow>> pairs = (Map.Entry<String, ArrayList<Shadow>>) it.next();
			
			ArrayList<Shadow> shadows = pairs.getValue();
			
			for (Shadow s : shadows)
				newShadows.add(s);
		}
	}
	
	/**
	 * Return any old shadows that the team client wants to remove from being drawn
	 * 
	 * @return an array list of objects that extend the Shadow class
	 */
	public Set<Shadow> getOldShadows()
	{
		if (oldShadows.size() > 0)
		{
			Set<Shadow> shadows = new HashSet<Shadow>(oldShadows);
			oldShadows.clear();
			return shadows;
		}
		else
			return null;
	}
	
	/**
	 * Return any new shadows that the team client wants to draw
	 * 
	 * @return an array list of objects that extend the Shadow class
	 */
	public Set<Shadow> getNewShadows()
	{
		Set<Shadow> shadows = new HashSet<Shadow>(newShadows);
		return shadows;
	}
}

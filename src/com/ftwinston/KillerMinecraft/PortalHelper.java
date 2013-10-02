package com.ftwinston.KillerMinecraft;

import java.util.HashMap;
import java.util.LinkedList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TravelAgent;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class PortalHelper
{
	PortalHelper(TravelAgent agent)
	{
		this.agent = agent;
	}
	
	private TravelAgent agent;
	private Location destination = null;
	private boolean useExitPortal = true;
	private static long rePortalDelay = 1000; // set this (automatically?) whenever performing a portalling.
	
	private class QueueEntry
	{
		public QueueEntry(Integer i, Long l)
		{
			key = i; value = l;
		}
		public final Integer key;
		public final Long value;
	}
	
	private static HashMap<Integer, Long> delayedEntitiesMap = new HashMap<Integer, Long>();
	private static LinkedList<QueueEntry> delayedEntitiesList = new LinkedList<QueueEntry>();
	
	static void tidyPortalDelays(long currentTime)
	{
		QueueEntry entry = delayedEntitiesList.peek();
		while ( entry != null && entry.value <= currentTime )
		{
			delayedEntitiesList.pop();
			delayedEntitiesMap.remove(entry.key);
			entry = delayedEntitiesList.peek();
		}
	}
	
	static boolean isAllowedToPortal(Entity entity)
	{
		Long blockedUntil = delayedEntitiesMap.get(entity.getEntityId());
		if ( blockedUntil == null )
			return true;
		
		return blockedUntil.longValue() <= System.currentTimeMillis();
	}

	public Location getDestination() { return destination; }
	public void setDestination(Location loc) { destination = loc; }
	
	public boolean getUseExitPortal() { return useExitPortal; }
	public void setUseExitPortal(boolean use) { useExitPortal = use; }
	
	public int getExitPortalSearchRadius() { return agent.getSearchRadius(); }
	public void setExitPortalSearchRadius(int radius) { agent.setSearchRadius(radius); }
	
	public int getExitPortalCreationRadius() { return agent.getCreationRadius(); }
	public void setExitPortalCreationRadius(int radius) { agent.setCreationRadius(radius); }
	
	public void setupScaledDestination(World destinationWorld, Location entranceLocation, double scaleFactor)
	{
		setDestination(new Location(destinationWorld, (entranceLocation.getX() * scaleFactor), entranceLocation.getY(), (entranceLocation.getZ() * scaleFactor), entranceLocation.getYaw(), entranceLocation.getPitch()));
	}
	
	void performTeleport(TeleportCause cause, Entity entity)
	{
		if ( destination == null )
			return; // null destination means no teleporting
		
		if ( useExitPortal )
		{
			destination = agent.findOrCreate(destination);
			
			// the above returns the northwest corner of the portal.
			// if there's a portal block south or east, return that instead, so that the player exits in the middle of the portal rather than in the wall (and so appears above it)
			Block other = destination.getBlock().getRelative(BlockFace.SOUTH);
			if ( other.getType() == Material.PORTAL )
				destination = other.getLocation().add(0.5, 0, 0);
			else
			{
				other = destination.getBlock().getRelative(BlockFace.EAST);
				if ( other.getType() == Material.PORTAL )
					destination = other.getLocation().add(0, 0, 0.5);
			}
		}
		
		entity.teleport(destination, cause);
		if ( rePortalDelay > 0 )
		{
			delayedEntitiesMap.put(entity.getEntityId(), System.currentTimeMillis() + rePortalDelay);
			delayedEntitiesList.add(new QueueEntry(entity.getEntityId(), System.currentTimeMillis() + rePortalDelay));
		}
	}
}
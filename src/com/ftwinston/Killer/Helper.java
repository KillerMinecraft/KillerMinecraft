package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.ftwinston.Killer.PlayerManager.Info;

public class Helper
{
	private static Random random = new Random();
	
	public static String tidyItemName(Material m)
	{
		return m.name().toLowerCase().replace('_', ' ');
	}

	public static int getTeam(OfflinePlayer player)
	{
		return Killer.instance.playerManager.getTeam(player.getName());
	}
	
	public static void setTeam(OfflinePlayer player, int teamNum)
	{
		Killer.instance.playerManager.setTeam(player, teamNum);
	}
	
	public static Player getTargetOf(OfflinePlayer player)
	{
		Info info = Killer.instance.playerManager.getInfo(player.getName());
		if ( info.target == null )
			return null;
		
		Player target = Killer.instance.getServer().getPlayerExact(info.target);
		
		if ( isAlive(target) && Killer.instance.getGameForPlayer(target) == info.getGame() )
			return target;
		
		return null;
	}
	
	public static void setTargetOf(OfflinePlayer player, OfflinePlayer target)
	{
		Info info = Killer.instance.playerManager.getInfo(player.getName());
		if ( target == null )
			info.target = null;
		else
			info.target = target.getName();
	}
	
	public static boolean isAlive(OfflinePlayer player)
	{
		return Killer.instance.playerManager.isAlive(player.getName());
	}
	
	public static boolean playerCanSeeOther(Player looker, Player target, double maxDistanceSq)
	{
		return Killer.instance.playerManager.canSee(looker, target, maxDistanceSq);
	}

	public static Location getNearestPlayerTo(Player player, List<Player> candidates)
	{
		Location nearest = null;
		double nearestDistSq = Double.MAX_VALUE;
		World playerWorld = player.getWorld();
		for ( Player other : candidates )
		{
			if ( other == player || other.getWorld() != playerWorld || !isAlive(other))
				continue;
				
			double distSq = other.getLocation().distanceSquared(player.getLocation());
			if ( distSq < nearestDistSq )
			{
				nearestDistSq = distSq;
				nearest = other.getLocation();
			}
		}
		
		// if there's no player to point at, point in a random direction
		if ( nearest == null )
		{
			nearest = player.getLocation();
			nearest.setX(nearest.getX() + random.nextDouble() * 32 - 16);
			nearest.setZ(nearest.getZ() + random.nextDouble() * 32 - 16);
		}
		return nearest;
	}
	
	public static <T> List<T> selectRandom(int num, List<T> candidates)
	{
		List<T> results = new ArrayList<T>();
		
		if ( num >= candidates.size() )
		{
			results.addAll(candidates);
			return results;
		}
		
		for ( int i=0; i<num; i++ )
			results.add(candidates.remove(random.nextInt(candidates.size())));
		
		return results;
	}
	
	public static <T> T selectRandom(List<T> candidates)
	{
		return candidates.get(random.nextInt(candidates.size()));
	}
	
	// returns the index with the highest value. If multiple indices share the highest value, picks one of these at random.
	public static int getHighestValueIndex(int[] values)
	{
		int highestVal = Integer.MIN_VALUE;
		ArrayList<Integer> highestIndices = new ArrayList<Integer>();
		
		for ( int i=0; i<values.length; i++ )
			if ( values[i] > highestVal )
			{
				highestVal = values[i];
				highestIndices.clear();
				highestIndices.add(i);
			}
			else if ( values[i] == highestVal )
				highestIndices.add(i);
		
		if ( highestIndices.size() == 0 )
			return 0;
		return selectRandom(highestIndices);
	}

	// returns the index with the lowest value. If multiple indices share the lowest value, picks one of these at random.
	public static int getLowestValueIndex(int[] values)
	{
		int lowestVal = Integer.MAX_VALUE;
		ArrayList<Integer> lowestIndices = new ArrayList<Integer>();
		
		for ( int i=0; i<values.length; i++ )
			if ( values[i] < lowestVal )
			{
				lowestVal = values[i];
				lowestIndices.clear();
				lowestIndices.add(i);
			}
			else if ( values[i] == lowestVal )
				lowestIndices.add(i);
		
		if ( lowestIndices.size() == 0 )
			return 0;
		return selectRandom(lowestIndices);
	}
	
	// Adds a random offset to a location. Each component of the location will be offset by a value at least as different as the corresponding min, and at most as different as the max.
	public static Location randomizeLocation(Location loc, double xMin, double yMin, double zMin, double xMax, double yMax, double zMax)
	{
		double xOff = xMin + (xMax - xMin) * random.nextDouble();
		double yOff = yMin + (yMax - yMin) * random.nextDouble();
		double zOff = zMin + (zMax - zMin) * random.nextDouble();
	
		return loc.clone().add(random.nextBoolean() ? xOff : -xOff, random.nextBoolean() ? yOff : -yOff, random.nextBoolean() ? zOff : -zOff);
	}
	
	public static Location getSafeSpawnLocationNear(Location loc)
	{
		int attempt = 1; boolean locationIsSafe;
		World world = loc.getWorld();
		do
		{
			double range = attempt * 2.5;
			loc = randomizeLocation(loc, 0, 0, 0, range, 0, range);
			
			Chunk chunk = world.getChunkAt(loc);
			if (!world.isChunkLoaded(chunk))
				world.loadChunk(chunk);
			
			loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1);
			attempt ++;
			locationIsSafe = isSafeSpawnLocation(loc);
		}
		while ( !locationIsSafe && attempt < 6 );
		
		if ( !locationIsSafe )
		{// make it safe
			Block floor = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY()-1, loc.getBlockZ());
			floor.setType(Material.DIRT);
		}
		
		return loc;
	}
	
	public static boolean isSafeSpawnLocation(Location loc)
	{
		Block b = loc.getBlock();
		if ( !isBlockSafe(b) )
			return false;

		Block other = b.getRelative(BlockFace.UP);
		if ( !isBlockSafe(other) )
			return false;
		
		other = b.getRelative(BlockFace.DOWN);
		if ( !isBlockSafe(other) )
			return false;
		
		other = b.getRelative(BlockFace.WEST);
		if ( !isBlockSafe(other) )
			return false;
		
		other = b.getRelative(BlockFace.EAST);
		if ( !isBlockSafe(other) )
			return false;
		
		other = b.getRelative(BlockFace.NORTH);
		if ( !isBlockSafe(other) )
			return false;

		other = b.getRelative(BlockFace.SOUTH);
		if ( !isBlockSafe(other) )
			return false;
		
		return true;
	}
	
	private static boolean isBlockSafe(Block b)
	{
		//return !b.isLiquid(); // actually, we don't care about water, only lava
		return b.getType() != Material.LAVA && b.getType() != Material.STATIONARY_LAVA;
	}

	public static int getHighestBlockYAt(Chunk c, int x, int z)
	{
		return c.getWorld().getHighestBlockYAt(c.getX() * 16 + x, c.getZ() * 16 + z);
	}
	
	public static int getHighestGroundYAt(Chunk c, int x, int z)
	{	
		int y = getHighestBlockYAt(c, x, z);
		x = x & 15; z = z & 15;
		Block b = c.getBlock(x, y, z);
		
		int seaLevel = c.getWorld().getSeaLevel();
		while ( y > seaLevel )
		{
			if ( b.getType() == Material.GRASS || b.getType() == Material.DIRT || b.getType() == Material.STONE || b.getType() == Material.SAND || b.getType() == Material.GRAVEL || b.getType() == Material.BEDROCK )
				break;

			y--;
			b = c.getBlock(x, y, z);
		}

		return y;
	}
	
	public static int getHighestGroundYAt(World w, int x, int z)
	{	
		int y = w.getHighestBlockYAt(x, z);
		Block b = w.getBlockAt(x, y, z);
		
		int seaLevel = w.getSeaLevel();
		while ( y > seaLevel )
		{
			if ( b.getType() == Material.GRASS || b.getType() == Material.DIRT || b.getType() == Material.STONE || b.getType() == Material.SAND || b.getType() == Material.GRAVEL || b.getType() == Material.BEDROCK )
				break;

			y--;
			b = w.getBlockAt(x, y, z);
		}

		return y;
	}

	public static Player getAttacker(EntityDamageEvent event)
	{
		Player attacker = null;
		if ( event instanceof EntityDamageByEntityEvent )
		{
			Entity damager = ((EntityDamageByEntityEvent)event).getDamager();
			if ( damager != null )
				if ( damager instanceof Player )
					attacker = (Player)damager;
				else if ( damager instanceof Arrow )
				{
					Arrow arrow = (Arrow)damager;
					if ( arrow.getShooter() instanceof Player )
						attacker = (Player)arrow.getShooter();
				}
		}
		return attacker;
	}
	
	public static Location generatePlinth(World world)
	{
		return generatePlinth(new Location(world, world.getSpawnLocation().getX() + 20,
												  world.getSpawnLocation().getY(),
												  world.getSpawnLocation().getZ()));
	}
	
	public static Location generatePlinth(Location loc)
	{
		World world = loc.getWorld();
		int x = loc.getBlockX(), z = loc.getBlockZ();
		
		int highestGround = world.getSeaLevel();		
		for ( int testX = x-1; testX <= x+1; testX++ )
			for ( int testZ = z-1; testZ <= z+1; testZ++ )
			{
				int groundY = Helper.getHighestGroundYAt(world, testX, testZ);
				if ( groundY > highestGround )
					highestGround = groundY;
			}
		
		int plinthPeakHeight = highestGround + 12, spaceBetweenPlinthAndGlowstone = 4;
		
		// a 3x3 column from bedrock to the plinth height
		for ( int y = 0; y < plinthPeakHeight; y++ )
			for ( int ix = x - 1; ix < x + 2; ix++ )
				for ( int iz = z - 1; iz < z + 2; iz++ )
				{
					Block b = world.getBlockAt(ix, y, iz);
					b.setType(Material.BEDROCK);
				}
		
		// with one block sticking up from it
		int y = plinthPeakHeight;
		for ( int ix = x - 1; ix < x + 2; ix++ )
				for ( int iz = z - 1; iz < z + 2; iz++ )
				{
					Block b = world.getBlockAt(ix, y, iz);
					b.setType(ix == x && iz == z ? Material.BEDROCK : Material.AIR);
				}
		
		// that has a pressure plate on it
		y = plinthPeakHeight + 1;
		Location plinthLoc = new Location(world, x, y, z);
		for ( int ix = x - 1; ix < x + 2; ix++ )
				for ( int iz = z - 1; iz < z + 2; iz++ )
				{
					Block b = world.getBlockAt(ix, y, iz);
					b.setType(ix == x && iz == z ? Material.STONE_PLATE : Material.AIR);
				}
				
		// then a space
		for ( y = plinthPeakHeight + 2; y <= plinthPeakHeight + spaceBetweenPlinthAndGlowstone; y++ )
			for ( int ix = x - 1; ix < x + 2; ix++ )
				for ( int iz = z - 1; iz < z + 2; iz++ )
				{
					Block b = world.getBlockAt(ix, y, iz);
					b.setType(Material.AIR);
				}
		
		// and then a 1x1 pillar of glowstone, up to max height
		for ( y = plinthPeakHeight + spaceBetweenPlinthAndGlowstone + 1; y < world.getMaxHeight(); y++ )
			for ( int ix = x - 1; ix < x + 2; ix++ )
				for ( int iz = z - 1; iz < z + 2; iz++ )
				{
					Block b = world.getBlockAt(ix, y, iz);
					b.setType(ix == x && iz == z ? Material.GLOWSTONE : Material.AIR);
				}
		
		return plinthLoc;
	}
}
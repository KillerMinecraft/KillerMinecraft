package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.server.NBTTagCompound;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

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
		
		if ( isAlive(target) && Killer.instance.isGameWorld(target.getWorld()))
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

	public static ItemStack setArmorColor(ItemStack item, int color)
	{
        CraftItemStack craftStack = null;
        net.minecraft.server.ItemStack itemStack = null;
        if (item instanceof CraftItemStack) {
            craftStack = (CraftItemStack) item;
            itemStack = craftStack.getHandle();
        }
        else if (item instanceof ItemStack) {
            craftStack = new CraftItemStack(item);
            itemStack = craftStack.getHandle();
        }
        NBTTagCompound tag = itemStack.tag;
        if (tag == null) {
            tag = new NBTTagCompound();
            tag.setCompound("display", new NBTTagCompound());
            itemStack.tag = tag;
        }
 
        tag = itemStack.tag.getCompound("display");
        tag.setInt("color", color);
        itemStack.tag.setCompound("display", tag);
        return craftStack;
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
}

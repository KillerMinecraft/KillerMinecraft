package com.ftwinston.KillerMinecraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.ftwinston.KillerMinecraft.Game.PlayerInfo;

public class Helper
{
	@SuppressWarnings("deprecation")
	public static Player getPlayer(String name)
	{
		return KillerMinecraft.instance.getServer().getPlayerExact(name);		
	}
	
	@SuppressWarnings("deprecation")
	public static OfflinePlayer getOfflinePlayer(String name)
	{
		return KillerMinecraft.instance.getServer().getOfflinePlayer(name);		
	}
	
	public static void teleport(Player player, Location loc)
	{
		if ( player.isDead() )
			KillerMinecraft.instance.craftBukkit.forceRespawn(player); // stop players getting stuck at the "you are dead" screen, unable to do anything except disconnect
		player.setVelocity(new Vector(0,0,0));
		player.teleport(loc);
	}
	
	public static void makeSpectator(Game game, Player player)
	{
		game.getPlayerInfo(player).setSpectator(true);
		
		if ( player.isDead() )
			return;

		player.sendMessage("You are now a spectator. You can fly, but can't be seen or interact. Press " + ChatColor.GOLD + "1" + ChatColor.RESET + " to show players you can teleport to, and left click on them to view from their perspective.");
		
		player.setGameMode(org.bukkit.GameMode.SPECTATOR);
				
		// hide from everyone else
		for ( Player online : game.getOnlinePlayers() )
			if ( online != player && !online.canSee(player) )
				KillerMinecraft.instance.craftBukkit.sendForScoreboard(online, player, true);
	}
	
	public static void stopSpectating(Game game, Player player)
	{
		player.sendMessage("You are no longer a spectator.");

		PlayerInfo info = game.getPlayerInfo(player); 
		if (info != null)
			info.setSpectator(false);
		
		if ( player.isDead() )
			return;
		
		player.setGameMode(org.bukkit.GameMode.SURVIVAL);
		
		player.getInventory().clear();
	}
	
	public static boolean isSpectator(Game game, Player player)
	{
		if ( game == null )
			return false;
		PlayerInfo info = game.getPlayerInfo(player);
		return info != null && info.isSpectator();
	}
	
	public static void hidePlayer(Player fromMe, Player hideMe)
	{
		fromMe.hidePlayer(hideMe);
		KillerMinecraft.instance.craftBukkit.sendForScoreboard(fromMe, hideMe, true); // hiding will take them out of the scoreboard, so put them back in again
	}

	static Random random = new Random();
	
	public static String tidyItemName(Material m)
	{
		return m.name().toLowerCase().replace('_', ' ');
	}

	private static final double maxAcceptableOffsetDot = 0.65;
	public static boolean canSee(Player looker, Player target, double maxDistanceSq)
	{
		Location specLoc = looker.getEyeLocation();
		Location targetLoc = target.getEyeLocation();
		
		// check they're in the same world
		if ( specLoc.getWorld() != targetLoc.getWorld() )
			return false;
		
		// then check the distance is appropriate
		double targetDistSqr = specLoc.distanceSquared(targetLoc); 
		if ( targetDistSqr > maxDistanceSq )
			return false;
		
		// check if they're facing the right way
		Vector specDir = specLoc.getDirection().normalize();
		Vector dirToTarget = targetLoc.subtract(specLoc).toVector().normalize();
		if ( specDir.dot(dirToTarget) < maxAcceptableOffsetDot )
			return false;
		
		// then do a ray trace to see if there's anything in the way
        Iterator<Block> itr = new BlockIterator(specLoc.getWorld(), specLoc.toVector(), dirToTarget, 0, (int)Math.sqrt(targetDistSqr));
        while (itr.hasNext())
        {
            Block block = itr.next();
            if ( block != null && !block.isEmpty() )
            	return false;
        }
        
        return true;
	}
	
	
	public static Location getNearestPlayerTo(Player player, List<Player> candidates)
	{
		Location nearest = null;
		double nearestDistSq = Double.MAX_VALUE;
		World playerWorld = player.getWorld();
		Game game = KillerMinecraft.instance.getGameForWorld(playerWorld);
		
		for ( Player other : candidates )
		{
			if ( other == player || other.getWorld() != playerWorld || isSpectator(game, other))
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
	
	public Location getRandomLocation(Location rangeMin, Location rangeMax)
	{
		return new Location(rangeMin.getWorld(),
				rangeMin.getX() + (rangeMax.getX() - rangeMin.getX()) * random.nextDouble(),
				rangeMin.getY() + (rangeMax.getY() - rangeMin.getY()) * random.nextDouble(),
				rangeMin.getZ() + (rangeMax.getZ() - rangeMin.getZ()) * random.nextDouble(),
				random.nextFloat() * 360.0f, 0f);
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
	
	public static int getHighestYAt(Chunk c, int x, int z, int minY, Material ... desiredBlockTypes)
	{
		int y = getHighestBlockYAt(c, x, z);
		Block b = c.getBlock(x, y, z);
		
		while ( y > minY )
		{
			for ( int i=0; i<desiredBlockTypes.length; i++ )
				if ( b.getType() == desiredBlockTypes[i] )
					return y;

			y--;
			b = c.getBlock(x, y, z);
		}

		return y;
	}
	
	public static int getHighestYAt(World w, int x, int z, int minY, Material ... desiredBlockTypes)
	{
		int y = w.getHighestBlockYAt(x, z);
		Block b = w.getBlockAt(x, y, z);
		
		while ( y > minY )
		{
			for ( int i=0; i<desiredBlockTypes.length; i++ )
				if ( b.getType() == desiredBlockTypes[i] )
					return y;

			y--;
			b = w.getBlockAt(x, y, z);
		}

		return y;
	}

	public static int getHighestYIgnoring(Chunk c, int x, int z, int minY, Material ... ignoredBlockTypes)
	{
		int y = getHighestBlockYAt(c, x, z);
		Block b = c.getBlock(x, y, z);
		
		while ( y > minY )
		{
			boolean ignore = false;
			for ( int i=0; i<ignoredBlockTypes.length; i++ )
				if ( b.getType() == ignoredBlockTypes[i] )
				{
					ignore = true;
					break;
				}

			if ( !ignore )
				return y;
			
			y--;
			b = c.getBlock(x, y, z);
		}

		return y;
	}
	
	public static int getHighestYIgnoring(World w, int x, int z, int minY, Material ... ignoredBlockTypes)
	{
		int y = w.getHighestBlockYAt(x, z);
		Block b = w.getBlockAt(x, y, z);
		
		while ( y > minY )
		{
			boolean ignore = false;
			for ( int i=0; i<ignoredBlockTypes.length; i++ )
				if ( b.getType() == ignoredBlockTypes[i] )
				{
					ignore = true;
					break;
				}

			if ( !ignore )
				return y;
			
			y--;
			b = w.getBlockAt(x, y, z);
		}

		return y;
	}

	public static Location findSpaceForPlayer(Location loc) 
	{
		World w = loc.getWorld();
		int x = loc.getBlockX(), y = loc.getBlockY() + 1, z = loc.getBlockZ();
		
		boolean prevEmpty = w.getBlockAt(loc).isEmpty();
		while ( y < w.getMaxHeight() )
		{
			boolean empty = w.getBlockAt(x, y, z).isEmpty();
			if ( empty && prevEmpty )
				return new Location(w, x + 0.5, y-1, z + 0.5);
			prevEmpty = empty;
			y++;
		}
		
		Location highest = w.getHighestBlockAt(x, z).getLocation();
		return new Location(w, highest.getX() + 0.5, highest.getY() + 1, highest.getZ() + 0.5);
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
	
	public static Location findNearestNetherFortress(Location loc)
	{
		return KillerMinecraft.instance.craftBukkit.findNearestNetherFortress(loc);
	}
	
	public static boolean createFlyingEnderEye(Player player, Location target)
	{
		return KillerMinecraft.instance.craftBukkit.createFlyingEnderEye(player, target);
	}
	
	public static void pushButton(Block b)
	{
		KillerMinecraft.instance.craftBukkit.pushButton(b);
	}
	
	public static void setCommandBlockCommand(Block b, String command)
	{
		KillerMinecraft.instance.craftBukkit.setCommandBlockCommand(b, command);
	}

	public static ItemStack setEnchantmentGlow(ItemStack item)
	{
		return KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(item);
	}
}

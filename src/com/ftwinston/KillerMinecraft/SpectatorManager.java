package com.ftwinston.KillerMinecraft;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

class SpectatorManager
{
	SpectatorManager()
	{
		transparentBlocks.clear();
		transparentBlocks.add(new Byte((byte)Material.AIR.getId()));
		transparentBlocks.add(new Byte((byte)Material.WATER.getId()));
		transparentBlocks.add(new Byte((byte)Material.STATIONARY_WATER.getId()));
	}
	
	void makeSpectator(Game game, Player player)
	{
		for(Player p : game.getOnlinePlayers(new PlayerFilter().includeSpectators()))
			if (p != player && p.canSee(player))
				Helper.hidePlayer(p, player);

		game.getPlayerInfo(player).setSpectator(true);
		
		if ( player.isDead() )
			return;

		player.sendMessage("You are now a spectator. You can fly, but can't be seen or interact. Clicking has different effects depending on the selected item. Type " + ChatColor.YELLOW + "/spec" + ChatColor.RESET + " to list available commands.");
		
		player.setAllowFlight(true);
		player.setFlying(true);
		
		// hide from everyone else
		for ( Player online : game.getOnlinePlayers() )
			if ( online != player && !online.canSee(player) )
			{
				online.hidePlayer(player);
				KillerMinecraft.instance.craftBukkit.sendForScoreboard(online, player, true);
			}
		
		// give spectator items
		ItemStack stack = new ItemStack(Settings.teleportModeItem, 1);
		ItemMeta meta = stack.getItemMeta();
		meta.setDisplayName("Teleport mode");
		stack.setItemMeta(meta);
		player.getInventory().addItem(stack);
		
		stack = new ItemStack(Settings.followModeItem, 1);
		meta = stack.getItemMeta();
		meta.setDisplayName("Follow mode");
		stack.setItemMeta(meta);
		player.getInventory().addItem(stack);
	}
	
	void stopSpectating(Game game, Player player)
	{
		player.sendMessage("You are no longer a spectator.");
		
		for(Player p : game.getOnlinePlayers(new PlayerFilter().includeSpectators()))
			if (p != player && !p.canSee(player))
				p.showPlayer(player);

		game.getPlayerInfo(player).setSpectator(false);
		
		if ( player.isDead() )
			return;
		
		player.setAllowFlight(false);
		player.setFlying(false);
		
		player.getInventory().clear();
	}
	
	private final double maxFollowSpectateRangeSq = 40 * 40, farEnoughSpectateRangeSq = 35 * 35;
	private final int maxSpectatePositionAttempts = 5, idealFollowSpectateRange = 20;
	
	void checkFollowTarget(Game game, Player player, String targetName)
	{
		Player target = Bukkit.getServer().getPlayerExact(targetName);
		if ( target == null || Helper.isSpectator(game, target) || !target.isOnline() || KillerMinecraft.instance.getGameForPlayer(target) != game )
		{
			target = getNearestFollowTarget(game, player);
			if ( target == null )
			{
				game.getPlayerInfo(player).spectatorTarget = null;
				return; // if there isn't a valid follow target, don't let it try to move them to it
			}
			
			game.getPlayerInfo(player).spectatorTarget = target.getName();
		}
		
		if ( !Helper.canSee(player,  target, maxFollowSpectateRangeSq) )
			moveToSee(player, target);
	}
	
	void moveToSee(Player player, Player target)
	{
		if ( target == null || !target.isOnline() )
			return;

		Location targetLoc = target.getEyeLocation();
		
		Location bestLoc = targetLoc;
		double bestDistSq = 0;
		
		// try a few times to move away in a random direction, see if we can make it up to idealFollowSpectateRange
		for ( int i=0; i<maxSpectatePositionAttempts; i++ )
		{
			// get a mostly-horizontal direction
			Vector dir = new Vector(Helper.random.nextDouble()-0.5, Helper.random.nextDouble() * 0.35 - 0.1, Helper.random.nextDouble()-0.5).normalize();
			if ( dir.getY() > 0.25 )
			{
				dir.setY(0.25);
				dir = dir.normalize();
			}
			else if ( dir.getY() < -0.1 )
			{
				dir.setY(-0.1);
				dir = dir.normalize();
			}
			
			Location pos = findSpaceForPlayer(player, targetLoc, dir, idealFollowSpectateRange, false, true);
			if ( pos == null )
				pos = targetLoc;
			
			double distSq = pos.distanceSquared(targetLoc); 
			if ( distSq > bestDistSq )
			{
				bestLoc = pos;
				bestDistSq = distSq; 
				
				if ( distSq > farEnoughSpectateRangeSq )
					break; // close enough to the max distance, just use this
			}
		}
		
		// work out the yaw
		double xDif = targetLoc.getX() - bestLoc.getX();
		double zDif = targetLoc.getZ() - bestLoc.getZ();
		
		if ( xDif == 0 )
		{
			if ( zDif >= 0 )
				bestLoc.setYaw(270);
			else
				bestLoc.setYaw(90);
		}
		else if ( xDif > 0 )
		{
			if ( zDif >= 0)
				bestLoc.setYaw(270f + (float)Math.toDegrees(Math.atan(zDif / xDif)));
			else
				bestLoc.setYaw(180f + (float)Math.toDegrees(Math.atan(xDif / -zDif)));
		}
		else
		{
			if ( zDif >= 0)
				bestLoc.setYaw((float)(Math.toDegrees(Math.atan(-xDif / zDif))));
			else
				bestLoc.setYaw(90f + (float)Math.toDegrees(Math.atan(zDif / xDif)));
		}
		
		// work out the pitch
		double horizDist = Math.sqrt(xDif * xDif + zDif * zDif);
		double yDif = targetLoc.getY() - bestLoc.getY();
		if ( horizDist == 0 )
			bestLoc.setPitch(0);
		else if ( yDif >= 0 )
			bestLoc.setPitch(-(float)Math.toDegrees(Math.atan(yDif / horizDist)));
		else
			bestLoc.setPitch((float)Math.toDegrees(Math.atan(-yDif / horizDist)));
		
		// as we're dealing in eye position thus far, reduce the Y to get the "feet position"
		bestLoc.setY(bestLoc.getY() - player.getEyeHeight());
		
		// set them as flying so they don't fall from this position, then do the teleport
		player.setFlying(true);
		player.teleport(bestLoc);
	}
	
	Player getNearestFollowTarget(Game game, Player lookFor)
	{
		double nearestDistSq = Double.MAX_VALUE;
		Player nearest = null;
		
		for ( Player player : game.getOnlinePlayers(new PlayerFilter().exclude(lookFor).world(lookFor.getWorld())) )
		{
			double testDistSq = player.getLocation().distanceSquared(lookFor.getLocation());
			if ( testDistSq < nearestDistSq )
			{
				nearest = player;
				nearestDistSq = testDistSq;
			}
		}
		
		if ( nearest != null )
			return nearest;
		
		List<Player> playersInOtherWorlds = game.getOnlinePlayers(new PlayerFilter().exclude(lookFor));
		return playersInOtherWorlds.size() > 0 ? playersInOtherWorlds.get(0) : null;
	}
	
	String getNextFollowTarget(Game game, Player lookFor, String currentTargetName, boolean forwards)
	{
		List<Player> validTargets = game.getOnlinePlayers(new PlayerFilter().exclude(lookFor));
		if ( validTargets.size() == 0 )
			return null;
		
		int start, end, increment;
		if ( forwards )
		{
			start = 0;
			end = validTargets.size();
			increment = 1;
		}
		else
		{
			start = validTargets.size()-1;
			end = -1;
			increment = -1;
		}
		
		boolean useNextTarget = false;
		for ( int i = start; i != end; i += increment )
		{
			if ( useNextTarget )
				return validTargets.get(i).getName();
			
			if ( validTargets.get(i).getName().equals(currentTargetName) )
				useNextTarget = true;
		}
						
		// ran off the end of the list, so use the "first" one from the list
		return validTargets.get(start).getName();
	}
	
	private Location findSpaceForPlayer(Player player, Location targetLoc, Vector dir, int maxDist, boolean seekClosest, boolean abortOnAnySolid)
	{
		Location bestPos = null;

		Iterator<Block> itr = new BlockIterator(targetLoc.getWorld(), targetLoc.toVector(), dir, 0, maxDist);
		while (itr.hasNext())
		{
			Block block = itr.next();
			if ( block == null || block.getLocation().getBlockY() <= 0 || block.getLocation().getBlockY() >= block.getWorld().getMaxHeight() )
				break; // don't go out the world
			
			if ( !block.isEmpty() && !block.isLiquid() )
				if ( abortOnAnySolid )
					break;
				else
					continue;
			
			Block blockBelow = targetLoc.getWorld().getBlockAt(block.getLocation().getBlockX(), block.getLocation().getBlockY()-1, block.getLocation().getBlockZ());
			if ( blockBelow.isEmpty() || blockBelow.isLiquid() )
			{
				bestPos = new Location(blockBelow.getWorld(), blockBelow.getX() + 0.5, blockBelow.getY() + player.getEyeHeight()-1, blockBelow.getZ() + 0.5);
				if ( seekClosest )
					return bestPos;
			}
		}
		
		return bestPos;
	}
	
	private final int maxSpecTeleportDist = 64, maxSpecTeleportPenetrationDist = 32;
	private final HashSet<Byte> transparentBlocks = new HashSet<Byte>();
	
	// teleport forward, to get around doors, walls, etc. that spectators can't dig through
	void doSpectatorTeleport(Player player, boolean goThroughTarget)
	{
		Location lookAtPos = player.getTargetBlock(transparentBlocks, maxSpecTeleportDist).getLocation();
		
		Vector facingDir = player.getLocation().getDirection().normalize();
		Location traceStartPos = goThroughTarget ? lookAtPos.add(facingDir) : lookAtPos;
		Vector traceDir = goThroughTarget ? facingDir : facingDir.multiply(-1.0);
	
		Location targetPos = findSpaceForPlayer(player, traceStartPos, traceDir, goThroughTarget ? maxSpecTeleportPenetrationDist : maxSpecTeleportDist, true, false);

		player.setFlying(true);
		if ( targetPos != null )
		{
			targetPos.setPitch(player.getLocation().getPitch());
			targetPos.setYaw(player.getLocation().getYaw());
			player.teleport(targetPos);
		}
		else
			player.sendMessage("No space to teleport into!");
	}
}

package com.ftwinston.Killer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;


class PlayerManager
{
	public static PlayerManager instance;
	private Killer plugin;
	private Random random;
	
	public PlayerManager(Killer _plugin)
	{
		this.plugin = _plugin;
		instance = this;
		random = new Random();
		
		transparentBlocks.clear();
		transparentBlocks.add(new Byte((byte)Material.AIR.getId()));
		transparentBlocks.add(new Byte((byte)Material.WATER.getId()));
		transparentBlocks.add(new Byte((byte)Material.STATIONARY_WATER.getId()));
	}
	
	public Map<String, Location> previousLocations = new HashMap<String, Location>();
	public void movePlayerOutOfKillerGame(Player player)
	{
		Location exitPoint = previousLocations.get(player.getName());
		if ( exitPoint == null )
			exitPoint = plugin.getServer().getWorlds().get(0).getSpawnLocation();

		Game game = plugin.getGameForPlayer(player);
		if ( game != null )
		{
			game.getGameMode().broadcastMessage(player.getName() + " quit the game");
			game.updatePlayerCount();
		}
		
		playerKilled(game, player);
		resetPlayer(game, player);
		teleport(player, exitPoint);
	}
	
	public void movePlayerIntoKillerGame(Player player)
	{
		previousLocations.put(player.getName(), player.getLocation());
		
		if ( plugin.games.length == 1 )
		{
			Game game = plugin.games[0];
			if ( game.getGameState().usesGameWorlds )
			{
				resetPlayer(game, player);
				game.addPlayerToGame(player);
				teleport(player, game.getGameMode().getSpawnLocation(player));
				return;
			}
		}
		
		putPlayerInStagingWorld(player);
	}
	
	public void putPlayerInStagingWorld(Player player)
	{
		teleport(player, plugin.worldManager.getStagingAreaSpawnPoint());
		player.setFlying(false);
		player.setGameMode(GameMode.SURVIVAL);
		giveStagingWorldInstructionBook(player);
		
		if ( plugin.games.length == 1 )
			plugin.games[0].addPlayerToGame(player);
	}
	
	public void giveStagingWorldInstructionBook(Player player)
	{
		PlayerInventory inv = player.getInventory();
		inv.clear();
		
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
		
		BookMeta bm = (BookMeta)book.getItemMeta();
		bm.setDisplayName("Instructions");
		
		bm.setAuthor("Killer Minecraft");
		bm.setTitle("The Staging World");
		bm.setPages("       Welcome to\n  �4�lKiller Minecraft�r�0\n\nThis is the staging world, where you can configure your next game.\n\nEach of the buttons on the left wall will show a different menu of options on the right wall when pressed.",
					"These let you select a game mode, configure any options it may have, choose world options, and more.\n\nTypically, only one option from a menu can be enabled at a time, but many game mode configuration options can be enabled or disabled simultaneously.",
					"When you're ready to start a game, push the start button at the end of the corridor.\n\nThis will create the game world(s), and then move everyone into them.\n\nYou will be returned to the staging world when the game ends.",
					"If you're waiting for other players to join, check out the arena behind the spawn point.\n\nThis can be set to play either spleef or a survival mode where you try to kill as many waves of monsters as possible.\n\n      �4�oHappy killing!");
		book.setItemMeta(bm);
		
		inv.setItem(8, book);
	}
	
	public void removeInventoryItems(PlayerInventory inv, Material... typesToRemove)
	{
		for ( Material type : typesToRemove )
			inv.remove(type);
	}
	
	public Info CreateInfo(boolean alive) { return new Info(alive); }
	
	public class Info
	{
		public Info(boolean alive) { a = alive; t = -1; target = null; }
		
		private boolean a;
		private int t;
		public int getTeam() { return t; }
		
		public void setTeam(int i)
		{
			t = i;
		}
		
		// am I a survivor or a spectator?
		public boolean isAlive() { return a; }
		
		public void setAlive(boolean b)
		{
			a = b;
		}
		
		// spectator target, and also kill target in Contract Killer mode
		public String target;
		
		public int nextHelpMessage = 0;
	}

	public void reset(Game game)
	{
		game.getPlayerInfo().clear();
	}
	
	public void startGame(Game game)
	{
		for ( Player player : game.getOnlinePlayers() )
			resetPlayer(game, player);
	}
	
	// player either died, or disconnected and didn't rejoin in the required time
	public void playerKilled(final Game game, final OfflinePlayer player)
	{
		if ( game == null || !game.getGameState().usesGameWorlds )
			return;
		
		Info info = game.getPlayerInfo().get(player.getName());
		info.setAlive(false);
		
		if ( game.getOnlinePlayers().size() == 0 )
		{// no one still playing, so end the game
			game.forcedGameEnd = true;
			game.getGameMode().gameFinished();
			game.endGame(null);
			return;
		}
		
		plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				game.getGameMode().playerKilledOrQuit(player);
			}
		}, 15); // game mode doesn't respond for short period, so as to be able to account for other deaths happening simultaneously (e.g. caused by the same explosion)
		
		Player online = player.isOnline() ? (Player)player : null;
		if ( online != null )
		{
			if ( game.getGameMode().isAllowedToRespawn(online) )
			{
				setAlive(game, online, true);
				return;
			}
			
			if ( !game.getGameMode().teamAllocationIsSecret() )
			{// clear this player's name color (cos they're a spectator now), by removing name as it was when was alive, then sending it as it is when dead
				info.setAlive(true);
				String name = game.calculateColoredName(online);
				for ( Player other : game.getOnlinePlayers() )
					plugin.craftBukkit.sendForScoreboard(other, name, false);
				
				info.setAlive(false);
				name = game.calculateColoredName(online);
				for ( Player other : game.getOnlinePlayers() )
					plugin.craftBukkit.sendForScoreboard(other, name, true);
			}
		}
		
		if ( !Settings.allowSpectators )
		{
			if ( online != null )
				putPlayerInStagingWorld(online);
		}
	}
	
	public Location getCompassTarget(Game game, Player player)
	{
		Location target = game.getGameMode().getCompassTarget(player);
		if ( target == null )
			return player.getWorld().getSpawnLocation();
		else
			return target;
	}

	public void setAlive(Game game, Player player, boolean bAlive)
	{
		boolean wasAlive;
		Info info = game.getPlayerInfo().get(player.getName());
		wasAlive = info.isAlive();

		Inventory inv = player.getInventory();
		if ( !bAlive || !wasAlive )
			inv.clear();
		
		info.setAlive(bAlive);
		
		if ( bAlive )
		{
			// you shouldn't stop being able to fly in creative mode, cos you're (hopefully) only there for testing
			if ( player.getGameMode() != GameMode.CREATIVE )
			{
				player.setFlying(false);
				player.setAllowFlight(false);
			}
			makePlayerVisibleToAll(game, player);
			
			if ( !wasAlive && !player.isDead() )
				player.sendMessage("You are no longer a spectator.");
		}
		else if ( !player.isDead() )
		{
			player.setAllowFlight(true);
			player.setFlying(true);
			makePlayerInvisibleToAll(game, player);
			
			ItemStack stack = new ItemStack(Settings.teleportModeItem, 1);
			ItemMeta meta = stack.getItemMeta();
			meta.setDisplayName("Teleport mode");
			stack.setItemMeta(meta);
			inv.addItem(stack);
			
			stack = new ItemStack(Settings.followModeItem, 1);
			meta = stack.getItemMeta();
			meta.setDisplayName("Follow mode");
			stack.setItemMeta(meta);
			inv.addItem(stack);
			
			player.sendMessage("You are now a spectator. You can fly, but can't be seen or interact. Clicking has different effects depending on the selected item. Type " + ChatColor.YELLOW + "/spec" + ChatColor.RESET + " to list available commands.");
		}
	}
	
	public void hidePlayer(Player fromMe, Player hideMe)
	{
		fromMe.hidePlayer(hideMe);
		plugin.craftBukkit.sendForScoreboard(fromMe, hideMe, true); // hiding will take them out of the scoreboard, so put them back in again
	}
	
	public void makePlayerInvisibleToAll(Game game, Player player)
	{
		for(Player p : game.getOnlinePlayers())
			if (p != player && p.canSee(player))
				hidePlayer(p, player);
	}
	
	public void makePlayerVisibleToAll(Game game, Player player)
	{
		for(Player p : game.getOnlinePlayers())
			if (p != player && !p.canSee(player))
				p.showPlayer(player);
	}

	public void resetPlayer(Game game, Player player)
	{
		if ( !player.isDead() )
		{
			player.setTotalExperience(0);
			
			Damageable damag = player;
			player.setHealth(damag.getMaxHealth());
			player.setFoodLevel(20);
			player.setSaturation(20);
			player.setExhaustion(0);
			player.setFireTicks(0);
			
			PlayerInventory inv = player.getInventory();
			inv.clear();
			inv.setHelmet(null);
			inv.setChestplate(null);
			inv.setLeggings(null);
			inv.setBoots(null);
			
			for (PotionEffectType p : PotionEffectType.values())
			     if (p != null && player.hasPotionEffect(p))
			          player.removePotionEffect(p);
			
			player.closeInventory(); // this stops them from keeping items they had in (e.g.) a crafting table
			
			/*if ( game != null && Helper.isAlive(game, player) ) // if any starting items are configured, give them if the player is alive
				for ( Material material : Settings.startingItems )
					inv.addItem(new ItemStack(material));*/
		}
	}
	
	private final double maxFollowSpectateRangeSq = 40 * 40, maxAcceptableOffsetDot = 0.65, farEnoughSpectateRangeSq = 35 * 35;
	private final int maxSpectatePositionAttempts = 5, idealFollowSpectateRange = 20;
	
	public void checkFollowTarget(Game game, Player player, String targetName)
	{
		if ( game == null )
			return;
		
		Player target = targetName == null ? null : plugin.getServer().getPlayerExact(targetName);
		if ( target == null || !Helper.isAlive(game, target) || !target.isOnline() || plugin.getGameForWorld(target.getWorld()) != game )
		{
			target = getNearestFollowTarget(game, player);
			
			if ( target == null )
				return; // if there isn't a valid follow target, don't let it try to move them to it

			if ( target == null || !Helper.isAlive(game, target) || !target.isOnline() )
			{// something went wrong with the default follow target, so just clear it
				target = null;
				Helper.setTargetOf(game, player, target);
				return;
			}
			
			Helper.setTargetOf(game, player, target);
		}
		
		if ( !canSee(player,  target, maxFollowSpectateRangeSq) )
			moveToSee(player, target);
	}
	
	public boolean canSee(Player looker, Player target, double maxDistanceSq)
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
	
	public void moveToSee(Player player, Player target)
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
			Vector dir = new Vector(random.nextDouble()-0.5, random.nextDouble() * 0.35 - 0.1, random.nextDouble()-0.5).normalize();
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
	
	public Player getNearestFollowTarget(Game game, Player lookFor)
	{
		double nearestDistSq = Double.MAX_VALUE;
		Player nearest = null;
		
		for ( Player player : game.getOnlinePlayers(new PlayerFilter().alive().exclude(lookFor).world(lookFor.getWorld())) )
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
		
		List<Player> playersInOtherWorlds = game.getOnlinePlayers(new PlayerFilter().alive().exclude(lookFor));
		return playersInOtherWorlds.size() > 0 ? playersInOtherWorlds.get(0) : null;
	}
	
	public String getNextFollowTarget(Game game, Player lookFor, String currentTargetName, boolean forwards)
	{
		List<Player> validTargets = game.getOnlinePlayers(new PlayerFilter().alive().exclude(lookFor));
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
	public void doSpectatorTeleport(Player player, boolean goThroughTarget)
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

	public void teleport(Player player, Location loc)
	{
		if ( player.isDead() )
			plugin.craftBukkit.forceRespawn(player); // stop players getting stuck at the "you are dead" screen, unable to do anything except disconnect
		player.setVelocity(new Vector(0,0,0));
		player.teleport(loc);
	}

	public boolean isInventoryEmpty(PlayerInventory inv)
	{
		for (ItemStack is : inv.getArmorContents())
            if (is != null && is.getAmount() > 0)
                return false;
        for (ItemStack is : inv.getContents())
            if (is != null && is.getAmount() > 0)
                return false;
        return true;
	}
}

package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.server.NBTTagCompound;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.ftwinston.Killer.Killer.GameState;
import com.ftwinston.Killer.PlayerManager;
import com.ftwinston.Killer.PlayerManager.Info;

public abstract class GameMode implements Listener
{
	static List<GameModePlugin> gameModes = new ArrayList<GameModePlugin>();

	static GameModePlugin get(int num) { return gameModes.get(num); }
	
	Killer plugin;
	protected final Random random = new Random();
	
	final void initialize(Killer killer, GameModePlugin modePlugin)
	{
		plugin = killer;
		name = modePlugin.getName();

		for ( Option option : setupOptions() )
			options.add(option);
	}
	
	// methods to be overridden by each game mode
	public abstract int getMinPlayers();

	private String name; 
	public String getName() 
	{
		return name;
	}
				
	protected abstract Option[] setupOptions();
	
	public Environment[] getWorldsToGenerate() { return new Environment[] { Environment.NORMAL, Environment.NETHER }; }
	public ChunkGenerator getCustomChunkGenerator(int worldNumber) { return null; }
	public BlockPopulator[] getExtraBlockPopulators(int worldNumber) { return null; }

	public abstract String getHelpMessage(int messageNum, int teamNum);
	
	private String getExtraHelpMessage(int messageNum)
	{
		boolean usesNether = false;
		for ( Environment env : getWorldsToGenerate() )
			if ( env == Environment.NETHER )
			{
				usesNether = true;
				break;
			}
	
		boolean use1 = usesNether && plugin.isEnderEyeRecipeEnabled();
		boolean use2 = plugin.isMonsterEggRecipeEnabled();
		boolean use3 = plugin.isDispenserRecipeEnabled();
		
		if ( !use1 )
			messageNum --;
		
		if ( messageNum < -1 && !use2 )
			messageNum --;
		
		if ( messageNum < -2 && !use3 )
			messageNum --;
			
		switch ( messageNum )
		{
			case -1:
				return "Eyes of ender will help you find nether fortresses (to get blaze rods).\nThey can be crafted from an ender pearl and a spider eye.";
			case -2:
				return "Several monster eggs can be crafted by combining one of their dropped items with an iron ingot.";
			case -3:
				return "Dispensers can be crafted using a sapling instead of a bow. These work well with monster eggs.";
			default:
				return null;
		}
	}

	public abstract boolean teamAllocationIsSecret();
	

	public void worldGenerationComplete() { } // create plinth, etc.

	public abstract boolean isLocationProtected(Location l); // for protecting plinth, respawn points, etc.


	public abstract boolean isAllowedToRespawn(Player player); // return false and player will become a spectator
	
	public abstract Location getSpawnLocation(Player player); // where should this player spawn?


	protected abstract void gameStarted(); // assign player teams if we do that immediately, etc

	protected abstract void gameFinished(); // clean up scheduled tasks, etc

	public abstract boolean useDiscreetDeathMessages(); // should we tweak death messages to keep stuff secret?

	public abstract void playerJoinedLate(Player player, boolean isNewPlayer);

	public abstract void playerKilledOrQuit(OfflinePlayer player);


	protected abstract Location getCompassTarget(Player player); // if compasses should follow someone / something, control that here
	
	public void playerActivatedPlinth(Player player) { }
	
	
	// helper methods that exist to help out the game modes
	
	protected final String tidyItemName(Material m)
	{
		return m.name().toLowerCase().replace('_', ' ');
	}

	protected final int getTeam(OfflinePlayer player)
	{
		return plugin.playerManager.getTeam(player.getName());
	}
	
	protected final void setTeam(OfflinePlayer player, int teamNum)
	{
		plugin.playerManager.setTeam(player, teamNum);
	}
	
	protected final Player getTargetOf(OfflinePlayer player)
	{
		Info info = plugin.playerManager.getInfo(player.getName());
		if ( info.target == null )
			return null;
		
		Player target = plugin.getServer().getPlayerExact(info.target);
		
		if ( isAlive(target) && plugin.isGameWorld(target.getWorld()))
			return target;
		
		return null;
	}
	
	protected final void setTargetOf(OfflinePlayer player, OfflinePlayer target)
	{
		Info info = plugin.playerManager.getInfo(player.getName());
		if ( target == null )
			info.target = null;
		else
			info.target = target.getName();
	}
	
	protected final boolean isAlive(OfflinePlayer player)
	{
		return plugin.playerManager.isAlive(player.getName());
	}
	
	protected final boolean playerCanSeeOther(Player looker, Player target, double maxDistanceSq)
	{
		return plugin.playerManager.canSee(looker, target, maxDistanceSq);
	}
	
	protected final List<Player> getOnlinePlayers()
	{
		return plugin.getOnlinePlayers();
	}
	
	protected final List<Player> getOnlinePlayers(int team)
	{
		ArrayList<Player> players = new ArrayList<Player>();
		for ( Map.Entry<String, Info> info : plugin.playerManager.getPlayerInfo() )
			if ( info.getValue().getTeam() == team )
			{
				Player p = plugin.getServer().getPlayerExact(info.getKey());
				if ( p != null && p.isOnline() )
					players.add(p);
			}
		
		return players;
	}
	
	protected final List<Player> getOnlinePlayers(boolean alive)
	{
		ArrayList<Player> players = new ArrayList<Player>();
		for ( Map.Entry<String, Info> info : plugin.playerManager.getPlayerInfo() )
			if ( alive == info.getValue().isAlive() )
			{
				Player p = plugin.getServer().getPlayerExact(info.getKey());
				if ( p != null && p.isOnline() )
					players.add(p);
			}
		
		return players;
	}
	
	protected final List<Player> getOnlinePlayers(int team, boolean alive)
	{
		ArrayList<Player> players = new ArrayList<Player>();
		for ( Map.Entry<String, Info> info : plugin.playerManager.getPlayerInfo() )
			if ( info.getValue().getTeam() == team && alive == info.getValue().isAlive() )
			{
				Player p = plugin.getServer().getPlayerExact(info.getKey());
				if ( p != null && p.isOnline() )
					players.add(p);
			}
		
		return players;
	}
	
	protected final void broadcastMessage(String message)
	{
		for ( Player player : getOnlinePlayers() )
			player.sendMessage(message);
	}
	
	protected final void broadcastMessage(Player notToMe, String message)
	{
		for ( Player player : getOnlinePlayers() )
			if ( player != notToMe )
				player.sendMessage(message);
	}
	
	protected final Location getNearestPlayerTo(Player player, boolean ignoreTeammates)
	{
		Location nearest = null;
		double nearestDistSq = Double.MAX_VALUE;
		World playerWorld = player.getWorld();
		int playerTeam = getTeam(player);
		for ( Player other : getOnlinePlayers() )
		{
			if ( other == player || other.getWorld() != playerWorld || !isAlive(other))
				continue;
			
			if ( ignoreTeammates && getTeam(other) == playerTeam )
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
	
	protected final <T> List<T> selectRandom(int num, List<T> candidates)
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
	
	protected final <T> T selectRandom(List<T> candidates)
	{
		return candidates.get(random.nextInt(candidates.size()));
	}
	
	// returns the index with the highest value. If multiple indices share the highest value, picks one of these at random.
	protected final int getHighestValueIndex(int[] values)
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
	protected final int getLowestValueIndex(int[] values)
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
		
	public ChatColor getTeamChatColor(int team)
	{
		switch ( team )
		{
		case 0:
			return ChatColor.BLUE;
		case 1:
			return ChatColor.RED;
		case 2:
			return ChatColor.YELLOW;
		case 3:
			return ChatColor.GREEN;
		case 4:
			return ChatColor.DARK_PURPLE;
		case 5:
			return ChatColor.AQUA;
		case 8:
			return ChatColor.WHITE;
		case 6:
			return ChatColor.DARK_GRAY;
		case 7:
			return ChatColor.GRAY;
		case 9:
			return ChatColor.LIGHT_PURPLE;
		default:
			return ChatColor.RESET;
		}
	}
	
	public int getTeamItemColor(int team)
	{
		switch ( team )
		{
		case 0:
			return 0x0066FF; // blue
		case 1:
			return 0xFF0000; // red
		case 2:
			return 0xDDDD00; // yellow
		case 3:
			return 0x00CC00; // green
		case 4:
			return 0xBE00BE; // purple
		case 5:
			return 0x3FFEFE; // aqua
		case 6:
			return 0xEEEEEE; // white
		case 7:
			return 0x3F3F3F; // dark grey
		case 8:
			return 0xBEBEBE; // light grey
		case 9:
			return 0xFE3FFE; // pink
		default:
			return 0xFFFFFF;
		}
	}
	
	public byte getTeamWoolColor(int team)
	{
		switch ( team )
		{
		case 0:
			return 0xB; // blue
		case 1:
			return 0xE; // red
		case 2:
			return 0x4; // yellow
		case 3:
			return 0x5; // green
		case 4:
			return 0xA; // purple
		case 5:
			return 0x3; // aqua
		case 6:
			return 0x0; // white
		case 7:
			return 0x7; // dark grey
		case 8:
			return 0x8; // light grey
		case 9:
			return 0x6; // pink
		default:
			return 0x0;
		}
	}
	
	public String getTeamName(int team)
	{
		switch ( team )
		{
		case 0:
			return "blue team";
		case 1:
			return "red team";
		case 2:
			return "yellow team";
		case 3:
			return "green team";
		case 4:
			return "purple team";
		case 5:
			return "aqua team";
		case 6:
			return "white team";
		case 7:
			return "dark grey team";
		case 8:
			return "light grey team";
		case 9:
			return "pink team";
		default:
			return "unnamed team";
		}
	}
	
	public ItemStack setColor(ItemStack item, int color)
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
	
	// Adds a random offset to a location. Each component of the location will be offset by a value at least as different as the corresponding min, and at most as different as the max.
	protected final Location randomizeLocation(Location loc, double xMin, double yMin, double zMin, double xMax, double yMax, double zMax)
	{
		double xOff = xMin + (xMax - xMin) * random.nextDouble();
		double yOff = yMin + (yMax - yMin) * random.nextDouble();
		double zOff = zMin + (zMax - zMin) * random.nextDouble();
	
		return loc.clone().add(random.nextBoolean() ? xOff : -xOff, random.nextBoolean() ? yOff : -yOff, random.nextBoolean() ? zOff : -zOff);
	}
	
	protected final Location getSafeSpawnLocationNear(Location loc)
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
	
	protected final boolean isSafeSpawnLocation(Location loc)
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
	
	private boolean isBlockSafe(Block b)
	{
		//return !b.isLiquid(); // actually, we don't care about water, only lava
		return b.getType() != Material.LAVA && b.getType() != Material.STATIONARY_LAVA;
	}
	
	public final Player getAttacker(EntityDamageEvent event)
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
	
	private Location plinthLoc = null;
	protected final Location generatePlinth(World world)
	{
		return generatePlinth(new Location(world, world.getSpawnLocation().getX() + 20,
												  world.getSpawnLocation().getY(),
												  world.getSpawnLocation().getZ()));
	}
	
	protected final Location generatePlinth(Location loc)
	{
		World world = loc.getWorld();
		int x = loc.getBlockX(), z = loc.getBlockZ();
		
		final int plinthPeakHeight = 76, spaceBetweenPlinthAndGlowstone = 4;
		
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
		plinthLoc = new Location(world, x, y, z);
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
	
	protected final boolean isOnPlinth(Location loc)
	{
		return  plinthLoc != null && loc.getWorld() == plinthLoc.getWorld()
	            && loc.getX() >= plinthLoc.getBlockX() - 1
	            && loc.getX() <= plinthLoc.getBlockX() + 1
	            && loc.getZ() >= plinthLoc.getBlockZ() - 1
	            && loc.getZ() <= plinthLoc.getBlockZ() + 1;
	}
	
	protected final void setPlayerVisibility(Player player, boolean visible)
	{
		if ( visible )
			plugin.playerManager.makePlayerVisibleToAll(player);
		else
			plugin.playerManager.makePlayerInvisibleToAll(player);
	}
	
	protected final void hidePlayer(Player player, Player looker)
	{
		plugin.playerManager.hidePlayer(looker, player);
	}
	
	protected final JavaPlugin getPlugin() { return plugin; }
	
	protected final int getNumWorlds() { return plugin.worldManager.worlds.size(); }
	protected final World getWorld(int number) { return plugin.worldManager.worlds.get(number); }
	
	public Location getPortalDestination(TeleportCause cause, Location entrance)
	{
		if ( cause != TeleportCause.NETHER_PORTAL || getNumWorlds() < 2 )
			return null;
		
		World toWorld;
		double blockRatio;
		
		if ( entrance.getWorld() == getWorld(0) )
		{
			toWorld = getWorld(1);
			blockRatio = 0.125;
		}
		else if ( entrance.getWorld() == getWorld(1) )
		{
			toWorld = getWorld(0);
			blockRatio = 8;
		}
		else
			return null;
		
		return new Location(toWorld, (entrance.getX() * blockRatio), entrance.getY(), (entrance.getZ() * blockRatio), entrance.getYaw(), entrance.getPitch());
	}
	
	// methods to be used by external code for accessing the game modes, rather than going directly into the mode-specific functions
	
	public final void startGame()
	{	
		plugin.forcedGameEnd = false;
		plugin.playerManager.startGame();
		gameStarted();
		
		for ( Player player : getOnlinePlayers() )
			player.teleport(getSpawnLocation(player));
	}
	
	protected final boolean hasGameFinished()
	{
		return !plugin.getGameState().usesGameWorlds || plugin.getGameState() == GameState.finished;
	}
	
	public final void finishGame()
	{	
		if ( hasGameFinished() )
			return;
		
		gameFinished();
		plinthLoc = null;
		
		plugin.setGameState(GameState.finished);
		
		if ( !plugin.forcedGameEnd )
		{
			plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					if ( Settings.voteRestartAtEndOfGame )
						plugin.voteManager.startVote("Play another game in the same world?", null, new Runnable() {
							public void run()
							{
								plugin.restartGame(null);
							}
						}, new Runnable() {
							public void run()
							{
								plugin.endGame(null);
							}
						}, new Runnable() {
							public void run()
							{
								plugin.endGame(null);
							}
						});
					else if  ( Settings.autoRestartAtEndOfGame )
						plugin.restartGame(null);
					else
						plugin.endGame(null);
				}
			}, 220); // add a 12 second delay
		}
	}
	
	public final void sendGameModeHelpMessage()
	{
		for ( Player player : getOnlinePlayers() )
			sendGameModeHelpMessage(player);
	}
	
	public final boolean sendGameModeHelpMessage(Player player)
	{
		PlayerManager.Info info = plugin.playerManager.getInfo(player.getName());
		String message = null;
		
		if ( info.nextHelpMessage >= 0 )
		{
			message = getHelpMessage(info.nextHelpMessage, info.getTeam()); // 0 ... n
			if ( message == null )
				info.nextHelpMessage = -1;
			else
			{
				if ( info.nextHelpMessage == 0 )
					message = ChatColor.YELLOW + getName() + ChatColor.RESET + "\n" + message; // put the game mode name on the front of the first message
				
				player.sendMessage(message);
				info.nextHelpMessage ++;
				return true;
			}
		}
		
		message = getExtraHelpMessage(info.nextHelpMessage); // -1 ... -m
		if ( message == null )
			return false;
		
		player.sendMessage(message);
		info.nextHelpMessage --;
		return true;
	}
	
	protected final class Option
	{
		public Option(String name, boolean enabledByDefault)
		{
			this.name = name;
			this.enabled = enabledByDefault;
		}
		
		private String name;
		public String getName() { return name; }
		
		private boolean enabled;
		public boolean isEnabled() { return enabled; }
		public void setEnabled(boolean enabled) { this.enabled = enabled; }
	}
	
	private List<Option> options = new ArrayList<Option>();
	public final List<Option> getOptions() { return options; }
	public final Option getOption(int num) { return options.get(num); }
	
	public boolean toggleOption(int num)
	{
		Option option = getOptions().get(num);
		if ( option == null )
			return false;

		option.setEnabled(!option.isEnabled());
		return option.isEnabled();
	}
	
	// allows game modes to determine if an event is in their game world
	protected final boolean shouldIgnoreEvent(Entity e)
	{
		return !plugin.isGameWorld(e.getWorld());
	}
	
	// allows events to determine if an event is in their game world
	protected final boolean shouldIgnoreEvent(Block b)
	{
		return !plugin.isGameWorld(b.getWorld());
	}
	
	// allows events to determine if an event is in their game world
	protected final boolean shouldIgnoreEvent(World w)
	{
		return !plugin.isGameWorld(w);
	}
	
	// allows events to determine if an event is in their game world
	protected final boolean shouldIgnoreEvent(Location l)
	{
		return !plugin.isGameWorld(l.getWorld());
	}
}

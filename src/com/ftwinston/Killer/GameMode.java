package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.ftwinston.Killer.Killer.GameState;
import com.ftwinston.Killer.PlayerManager;

import com.ftwinston.Killer.GameModes.ContractKiller;
import com.ftwinston.Killer.GameModes.CrazyKiller;
import com.ftwinston.Killer.GameModes.InvisibleKiller;
import com.ftwinston.Killer.GameModes.MysteryKiller;
import com.ftwinston.Killer.GameModes.TeamKiller;
import com.ftwinston.Killer.PlayerManager.Info;

public abstract class GameMode implements Listener
{
	public static List<GameMode> gameModes = new ArrayList<GameMode>();

	public static void setup(Killer killer)
	{
		GameMode g;
	
		if ( Settings.allowModeMysteryKiller )
		{
			g = new MysteryKiller();
			g.initialize(killer);
			gameModes.add(g);
		}
		
		if ( Settings.allowModeInvisibleKiller )
		{
			g = new InvisibleKiller();
			g.initialize(killer);
			gameModes.add(g);
		}
		
		if ( Settings.allowModeCrazyKiller )
		{
			g = new CrazyKiller();
			g.plugin = killer;
			g.initialize(killer);
			gameModes.add(g);
		}
		
		if ( Settings.allowModeTeamKiller )
		{
			g = new TeamKiller();
			g.initialize(killer);
			gameModes.add(g);
		}
		
		if ( Settings.allowModeContractKiller )
		{
			g = new ContractKiller();
			g.initialize(killer);
			gameModes.add(g);
		}
		
		killer.setGameMode(gameModes.get(0));
	}
	
	public static GameMode get(int num) { return gameModes.get(num); }
	
	private Killer plugin;
	protected Random random = new Random();
	
	private final void initialize(Killer killer)
	{
		plugin = killer;

		for ( Option option : setupOptions() )
			options.add(option);
	}
	
	// methods to be overridden by each game mode
	
	public abstract String getName();

	public abstract int getMinPlayers();

	protected abstract Option[] setupOptions();

	public abstract String[] getSignDescription();

	protected abstract String describeTeam(int teamNum, boolean plural);

	public abstract String getHelpMessage(int messageNum, int teamNum);

	public abstract boolean teamAllocationIsSecret();
	

	public abstract boolean usesNether(); // return false to stop nether generation, and prevent portals from working

	public abstract void worldGenerationComplete(World main, World nether); // create plinth, etc.

	public abstract boolean isLocationProtected(Location l); // for protecting plinth, respawn points, etc.


	public abstract boolean isAllowedToRespawn(Player player); // return false and player will become a spectator

	public abstract boolean lateJoinersMustSpectate();
	
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
	
	protected final Location randomizeLocation(Location loc, double xMin, double xMax, double yMin, double yMax, double zMin, double zMax)
	{
		double xOff = xMin == xMax ? xMin : (xMin + random.nextDouble() * (xMax - xMin));
		double yOff = yMin == yMax ? yMin : (yMin + random.nextDouble() * (yMax - yMin));
		double zOff = zMin == zMax ? zMin : (zMin + random.nextDouble() * (zMax - zMin));
		
		return loc.add(xOff, yOff, zOff);
	}
	
	protected final Location getSafeSpawnLocationNear(Location loc)
	{
		int attempt = 1; boolean locationIsSafe;
		World world = loc.getWorld();
		do
		{
			double range = attempt * 2.5;
			loc = randomizeLocation(loc, -range, range, 0, 0, -range, range);
			
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
	
	public Player getAttacker(EntityDamageEvent event)
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
		return generatePlinth(world.getSpawnLocation().add(20, 0, 0));
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
	
	protected boolean isOnPlinth(Location loc)
	{
		return  plinthLoc != null && loc.getWorld() == plinthLoc.getWorld()
	            && loc.getX() >= plinthLoc.getBlockX() - 1
	            && loc.getX() <= plinthLoc.getBlockX() + 1
	            && loc.getZ() >= plinthLoc.getBlockZ() - 1
	            && loc.getZ() <= plinthLoc.getBlockZ() + 1;
	}
	
	protected void setPlayerVisibility(Player player, boolean visible)
	{
		if ( visible )
			plugin.playerManager.makePlayerVisibleToAll(player);
		else
			plugin.playerManager.makePlayerInvisibleToAll(player);
	}
	
	protected void hidePlayer(Player player, Player looker)
	{
		plugin.playerManager.hidePlayer(looker, player);
	}
	
	protected final JavaPlugin getPlugin() { return plugin; }
	
	
	// methods to be used by external code for accessing the game modes, rather than going directly into the mode-specific functions
	
	public final void startGame()
	{
		plugin.forcedGameEnd = false;
		plugin.playerManager.startGame();
		gameStarted();
		
		for ( Player player : getOnlinePlayers() )
			player.teleport(getSpawnLocation(player));
	}
	
	public final void finishGame()
	{	
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
			}, 100); // add a 5 second delay
		}
	}
	
	public final String describeTeam(int teamNum)
	{
		int playersOnTeam = 0;
		
		for ( Map.Entry<String, Info> info : plugin.playerManager.getPlayerInfo() )
			if ( info.getValue().getTeam() == teamNum )
			{
				playersOnTeam ++;
				if ( playersOnTeam > 1 )
					break; // no need to keep going, we only care if there's one or not
			}
		
		return describeTeam(teamNum, playersOnTeam != 1);
	}
	
	public final void sendGameModeHelpMessage()
	{
		for ( Player player : getOnlinePlayers() )
			sendGameModeHelpMessage(player);
	}
	
	public final void sendGameModeHelpMessage(Player player)
	{
		PlayerManager.Info info = plugin.playerManager.getInfo(player.getName());
		if ( info.nextHelpMessage == -1 )
			return;
		
		String message = getHelpMessage(info.nextHelpMessage, info.getTeam());
		if ( message == null )
		{
			info.nextHelpMessage = -1; 
			return;
		}
		
		if ( info.nextHelpMessage == 0 )
			message = getName() + "\n" + message; // put the game mode name on the front of the first message
		player.sendMessage(message);
		info.nextHelpMessage ++;
	}
	
	protected class Option
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
	public List<Option> getOptions() { return options; }
	public Option getOption(int num) { return options.get(num); }
	
	public boolean toggleOption(int num)
	{
		Option option = getOptions().get(num);
		if ( option == null )
			return false;

		option.setEnabled(!option.isEnabled());
		return option.isEnabled();
	}
	
	// allows game modes to determine if an event is in their game world
	protected boolean shouldIgnoreEvent(Entity e)
	{
		return !plugin.isGameWorld(e.getWorld());
	}
	
	// allows events to determine if an event is in their game world
	protected boolean shouldIgnoreEvent(Block b)
	{
		return !plugin.isGameWorld(b.getWorld());
	}
	
	// allows events to determine if an event is in their game world
	protected boolean shouldIgnoreEvent(World w)
	{
		return !plugin.isGameWorld(w);
	}
	
	// allows events to determine if an event is in their game world
	protected boolean shouldIgnoreEvent(Location l)
	{
		return !plugin.isGameWorld(l.getWorld());
	}
}

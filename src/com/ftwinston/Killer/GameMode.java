package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
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
			g.plugin = killer;
			gameModes.add(g);
		}
		
		if ( Settings.allowModeInvisibleKiller )
		{
			g = new InvisibleKiller();
			g.plugin = killer;
			gameModes.add(g);
		}
		
		if ( Settings.allowModeCrazyKiller )
		{
			g = new CrazyKiller();
			g.plugin = killer;
			gameModes.add(g);
		}
		
		if ( Settings.allowModeTeamKiller )
		{
			g = new TeamKiller();
			g.plugin = killer;
			gameModes.add(g);
		}
		
		if ( Settings.allowModeContractKiller )
		{
			g = new ContractKiller();
			g.plugin = killer;
			gameModes.add(g);
		}
		
		if ( gameModes.size() == 1 )
			killer.setGameMode(gameModes.get(0));
	}
	
	public static GameMode get(int num) { return gameModes.get(num); }
	
	private Killer plugin;
	protected Random random = new Random();
	
	
	// methods to be overridden by each game mode
	
	public abstract String getName();

	public abstract int getMinPlayers();

	public abstract Option[] setupOptions();

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

	public abstract void playerKilledOrQuit(Player player);


	protected abstract Location getCompassTarget(Player player); // if compasses should follow someone / something, control that here
	
	public void playerActivatedPlinth(Player player) { }
	
	
	// helper methods that exist to help out the game modes
	
	protected final int countPlayersOnTeam(int teamNum, boolean onlyLivingPlayers)
	{
		return plugin.playerManager.countPlayersOnTeam(teamNum, onlyLivingPlayers);
	}
	
	protected final String tidyItemName(Material m)
	{
		return m.name().toLowerCase().replace('_', ' ');
	}

	protected final int getTeam(Player player)
	{
		return plugin.playerManager.getTeam(player.getName());
	}
	
	protected final void setTeam(Player player, int teamNum)
	{
		plugin.playerManager.setTeam(player, teamNum);
	}
	
	protected final Player getTargetOf(Player player)
	{
		Info info = plugin.playerManager.getInfo(player.getName());
		if ( info.target == null )
			return null;
		
		Player target = plugin.getServer().getPlayerExact(info.target);
		
		if ( isAlive(target) && plugin.isGameWorld(target.getWorld()))
			return target;
		
		return null;
	}
	
	protected final void setTargetOf(Player player, Player target)
	{
		Info info = plugin.playerManager.getInfo(player.getName());
		if ( target == null )
			info.target = null;
		else
			info.target = target.getName();
	}
	
	protected final boolean isAlive(Player player)
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
			if ( info.getValue().getTeam() == team && (alive == info.getValue().isAlive()) )
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
		
		Block below = b.getRelative(BlockFace.DOWN);
		if ( !isBlockSafe(below) )
			return false;
		
		Block above = b.getRelative(BlockFace.UP);
		if ( !isBlockSafe(above) )
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
		
		if ( !plugin.forcedGameEnd )
		{
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
	}
	
	public final String describeTeam(int teamNum)
	{
		return describeTeam(teamNum, countPlayersOnTeam(teamNum, false) != 1);
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
	
	/*
	public abstract int absMinPlayers();
	public abstract boolean killersCompassPointsAtFriendlies();
	public abstract boolean friendliesCompassPointsAtKiller();
	public boolean compassPointsAtTarget() { return false; }
	public abstract boolean discreteDeathMessages();
	public abstract boolean usesPlinth();
	
	public abstract void assignTeams();
	
	public abstract int getNumTeams();
	public abstract boolean immediateTeamAssignment();
	public abstract boolean informOfTeamAssignment(PlayerManager pm);
	
	public abstract boolean revealTeamIdentityAtEnd(int team);
	
	public boolean assignTeams(int numKillers, CommandSender sender, PlayerManager pm)
	{
		int availablePlayers = 0;
		for ( Map.Entry<String, Info> entry : pm.getPlayerInfo() )
		{
			if ( !entry.getValue().isAlive() || entry.getValue().getTeam() == 1 )
				continue; // spectators and already-assigned killers don't count towards the minimum
			
			Player player = plugin.getServer().getPlayerExact(entry.getKey());
			if ( player != null && player.isOnline() )
				availablePlayers ++; // "just disconnected" players don't count towards the minimum
		}
		
		if ( availablePlayers < absMinPlayers() )
		{
			String message = "Insufficient players to assign a killer. A minimum of " + absMinPlayers() + " players are required.";
			if ( sender != null )
				sender.sendMessage(message);
			if ( informOfTeamAssignment(pm) )
				plugin.broadcastMessage(message);
			return false;
		}
		
		if ( informOfTeamAssignment(pm) && teamAllocationIsSecret() )
		{
			String message;
			if ( numKillers > 1 )
				message = numKillers + " killers have";
			else
				message = "A killer has";
			
			message += " been randomly assigned";
			
			
			if ( sender != null )
				message += " by " + sender.getName();
			message += " - nobody but the";
			
			if ( numKillers > 1 || pm.numPlayersOnTeam(1) > 0 )
				message += " killers";
			else
				message += " killer";
			message += " knows who they are.";

			plugin.broadcastMessage(message);
		}
	
		if ( !plugin.statsManager.isTracking )
			plugin.statsManager.gameStarted(pm.numSurvivors());
		
		
		if ( numKillers >= availablePlayers )
			numKillers = availablePlayers;
		
		int[] killerIndices = new int[numKillers];
		for ( int i=0; i<numKillers; i++ )
		{
			int rand;
			boolean ok;
			do
			{
				rand = r.nextInt(availablePlayers);
				ok = true;
				for ( int j=0; j<i; j++ )
					if ( rand == killerIndices[j] ) // already used this one, it's not good to use again
					{
						ok = false;
						break;
					}

			} while ( !ok );
			killerIndices[i] = rand;
		}
		
		Arrays.sort(killerIndices);
		
		int num = 0, nextIndex = 0;
		for ( Map.Entry<String, Info> entry : pm.getPlayerInfo() )
		{
			if ( !entry.getValue().isAlive() || entry.getValue().getTeam() == 1 )
				continue;
			
			Player player = plugin.getServer().getPlayerExact(entry.getKey());
			if ( player == null || !player.isOnline() )
				continue;

			if ( nextIndex < numKillers && num == killerIndices[nextIndex] )
			{
				entry.getValue().setTeam(1);
				
				// don't show this in team killer mode, but do show it in mystery killer, even when assigning a new killer midgame and not telling everyone else
				if ( informOfTeamAssignment(pm) || teamAllocationIsSecret() )
				{
					String message = ChatColor.RED + "You are ";
					message += numKillers > 1 || pm.numPlayersOnTeam(1) > 1 ? "now a" : "the";
					message += " killer!";
					
					if ( !informOfTeamAssignment(pm) && teamAllocationIsSecret() )
						message += ChatColor.WHITE + " No one else has been told a new killer was assigned.";
						
					player.sendMessage(message);
				}
				if ( !teamAllocationIsSecret() )
				{
					if ( informOfTeamAssignment(pm) )
						plugin.broadcastMessage(player.getName() + " is the killer!");
					pm.colorPlayerName(player, ChatColor.RED);
				}
				
				preparePlayer(player, pm, 1, true);
				
				nextIndex ++;
				
				plugin.statsManager.killerAdded();
				if ( sender != null )
					plugin.statsManager.killerAddedByAdmin();
			}
			else if ( pm.getTeam(player.getName()) != 1 )
			{
				preparePlayer(player, pm, 0, true);
				
				if ( !teamAllocationIsSecret() )
					pm.colorPlayerName(player, ChatColor.BLUE);
				else if ( informOfTeamAssignment(pm) )
					player.sendMessage(ChatColor.YELLOW + "You are not " + ( numKillers > 1 || pm.numPlayersOnTeam(1) > 1 ? "a" : "the") + " killer.");
			}
			
			num++;
		}
		return true;
	}
	
	public abstract void playerJoined(Player player, PlayerManager pm, boolean isNewPlayer, PlayerManager.Info info);
	public void playerKilledOrQuit(Player player, PlayerManager pm, PlayerManager.Info info) { }
	
	public abstract void preparePlayer(Player player, PlayerManager pm, int teamNum, boolean isNewPlayer);
	
	public abstract void checkForEndOfGame(PlayerManager pm, Player playerOnPlinth, Material itemOnPlinth);
	
	public boolean playerDamaged(Player victim, Entity attacker, DamageCause cause, int amount) { return true; }
	public void playerEmptiedBucket(PlayerBucketEmptyEvent event) { }
	public void playerPickedUpItem(PlayerPickupItemEvent event) { }
	public void playerDroppedItem(Player player, Item item) { }
	public void playerInventoryClick(Player player, InventoryClickEvent event) { }
	public void playerItemSwitch(Player player, int prevSlot, int newSlot) { }
	*/
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
	
	protected List<Option> options = new ArrayList<Option>();
	public List<Option> getOptions() { return options; }
	
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

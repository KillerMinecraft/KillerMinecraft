package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.ftwinston.Killer.Killer.GameState;
import com.ftwinston.Killer.PlayerManager;

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
		options = setupOptions();
	}
	
	// methods to be overridden by each game mode
	public abstract int getMinPlayers();

	private String name; 
	public String getName() 
	{
		return name;
	}
	
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

	public abstract boolean isLocationProtected(Location l); // for protecting plinth, respawn points, etc.


	public abstract boolean isAllowedToRespawn(Player player); // return false and player will become a spectator
	
	public abstract Location getSpawnLocation(Player player); // where should this player spawn?


	protected void initializeGame(boolean isNewWorlds) { };
	
	protected abstract void gameStarted(boolean isNewWorlds); // assign player teams if we do that immediately, etc

	protected abstract void gameFinished(); // clean up scheduled tasks, etc

	public abstract boolean useDiscreetDeathMessages(); // should we tweak death messages to keep stuff secret?

	public abstract void playerJoinedLate(Player player, boolean isNewPlayer);

	public abstract void playerKilledOrQuit(OfflinePlayer player);


	protected abstract Location getCompassTarget(Player player); // if compasses should follow someone / something, control that here
	
	public void playerActivatedPlinth(Player player) { }
	
	
	// helper methods that exist to help out the game modes	
	protected final List<Player> getOnlinePlayers()
	{		
		return getOnlinePlayers(new PlayerFilter());
	}
	
	protected final List<Player> getOnlinePlayers(PlayerFilter filter)
	{		
		return filter.getOnlinePlayers();
	}
	
	protected final List<OfflinePlayer> getOfflinePlayers(PlayerFilter filter)
	{		
		return filter.offline().getPlayers();
	}
	
	protected final List<OfflinePlayer> getPlayers(PlayerFilter filter)
	{		
		return filter.getPlayers();
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
	protected final BukkitScheduler getScheduler() { return plugin.getServer().getScheduler(); }
	
	protected final int getNumWorlds() { return plugin.worldManager.worlds.size(); }
	protected final World getWorld(int number) { return plugin.worldManager.worlds.get(number); }
	
	public void handlePortal(TeleportCause cause, Location entrance, PortalHelper helper)
	{
		if ( cause != TeleportCause.NETHER_PORTAL || getNumWorlds() < 2 )
			return;
		
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
			return;
		
		helper.setupScaledDestination(toWorld, entrance, blockRatio);
	}
	
	// methods to be used by external code for accessing the game modes, rather than going directly into the mode-specific functions
	
	public final void startGame(boolean isNewWorlds)
	{	
		plugin.forcedGameEnd = false;
		plugin.playerManager.startGame();
		gameStarted(isNewWorlds);
		
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
	
	private Option[] options;
	protected abstract Option[] setupOptions();
	public final Option[] getOptions() { return options; }
	public final Option getOption(int num) { return options[num]; }
	public final int getNumOptions() { return options.length; }
	
	public void toggleOption(int num)
	{
		Option option = options[num];
		option.setEnabled(!option.isEnabled());
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

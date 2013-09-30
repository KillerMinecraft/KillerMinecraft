package com.ftwinston.KillerMinecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scoreboard.Scoreboard;

import com.ftwinston.KillerMinecraft.Game.GameState;
import com.ftwinston.KillerMinecraft.Configuration.Team;


public abstract class GameMode extends KillerModule
{
	static List<GameModePlugin> gameModes = new ArrayList<GameModePlugin>();

	static GameModePlugin get(int num) { return gameModes.get(num); }
	static GameModePlugin getByName(String name)
	{
		for ( GameModePlugin plugin : gameModes )
			if ( name.equalsIgnoreCase(plugin.getName()) )
				return plugin;
		
		return null;
	}
	static int indexOf(GameModePlugin plugin)
	{
		for ( int i=0; i<gameModes.size(); i++ )
			if ( gameModes.get(i) == plugin ) 
				return i;
		return -1;
	}

	protected final Random random = new Random();
	
	public Scoreboard createScoreboard() { return Bukkit.getScoreboardManager().getMainScoreboard(); }
	public boolean shouldShowScoreboardBeforeStarting() { return true; }
	

	public Team[] initializeTeams() { return new Team[0]; }
	private Team[] teams;
	public final Team[] getTeams() { return teams; }
	public abstract int getMinPlayers();

	public boolean allowWorldGeneratorSelection() { return true; }
	public Environment[] getWorldsToGenerate() { return new Environment[] { Environment.NORMAL, Environment.NETHER }; }
	public void beforeWorldGeneration(int worldNumber, WorldConfig world) { }

	public abstract String getHelpMessage(int messageNum, Team team);
	
	public abstract boolean teamAllocationIsSecret();

	public abstract boolean isLocationProtected(Location l, Player p); // for protecting plinth, respawn points, etc.

	public abstract boolean isAllowedToRespawn(Player player); // return false and player will become a spectator
	
	public abstract Location getSpawnLocation(Player player); // where should this player spawn?
	
	protected abstract void gameStarted(); // assign player teams if we do that immediately, etc

	protected abstract void gameFinished(); // clean up scheduled tasks, etc

	public boolean useDiscreetDeathMessages() { return false; } // should we tweak death messages to keep stuff secret?

	public void playerJoined(Player player, boolean isNewPlayer) { };

	public void playerQuit(OfflinePlayer player) { };
	
	protected Location getCompassTarget(Player player) { return null; } // if compasses should follow someone / something, control that here
	
	// helper methods that exist to help out the game modes	
	protected final List<Player> getOnlinePlayers()
	{		
		return game.getOnlinePlayers(new PlayerFilter());
	}
	
	protected final List<Player> getOnlinePlayers(PlayerFilter filter)
	{
		return game.getOnlinePlayers(filter);
	}
	
	protected final List<OfflinePlayer> getOfflinePlayers(PlayerFilter filter)
	{		
		return game.getOfflinePlayers(filter);
	}
	
	protected final List<OfflinePlayer> getPlayers(PlayerFilter filter)
	{		
		return game.getPlayers(filter);
	}
	
	protected final void broadcastMessage(String message)
	{
		game.broadcastMessage(message);
	}
	
	protected final void broadcastMessage(PlayerFilter recipients, String message)
	{
		game.broadcastMessage(recipients, message);
	}
	
	public int getMonsterSpawnLimit(int quantity)
	{
		switch ( game.monsterNumbers )
		{
		case 0:
			return 0;
		case 1:
			return 35;
		case 2: // MC defaults
		default:
			return 70;
		case 3:
			return 110;
		case 4:
			return 180;
		}
	}
	
	public int getAnimalSpawnLimit(int quantity)
	{
		switch ( game.monsterNumbers )
		{
		case 0:
			return 0;
		case 1:
			return 8;
		case 2: // MC defaults
		default:
			return 15;
		case 3:
			return 25;
		case 4:
			return 40;
		}
	}
	
	protected final void setPlayerVisibility(Player player, boolean visible)
	{
		if ( visible )
			plugin.playerManager.makePlayerVisibleToAll(game, player);
		else
			plugin.playerManager.makePlayerInvisibleToAll(game, player);
	}
	
	protected final void hidePlayer(Player player, Player looker)
	{
		plugin.playerManager.hidePlayer(looker, player);
	}
	
	protected final int getNumWorlds() { return game.getWorlds().size(); }
	protected final World getWorld(int number) { return game.getWorlds().get(number); }
	
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

	final void startGame()
	{	
		game.forcedGameEnd = false;
		plugin.playerManager.startGame(game);
		gameStarted();
		
		for ( Player player : getOnlinePlayers() )
			player.teleport(getSpawnLocation(player));
	}
	
	protected final boolean hasGameFinished()
	{
		return !game.getGameState().usesGameWorlds || game.getGameState() == GameState.finished;
	}
	
	public final void finishGame()
	{	
		if ( hasGameFinished() )
			return;
		
		gameFinished();
		
		game.setGameState(GameState.finished);
		
		if ( !game.forcedGameEnd )
		{
			plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
				@Override
				public void run() {
					game.endGame(null);
				}
			}, 220); // add a 12 second delay
		}
	}
	
	final void sendGameModeHelpMessage()
	{
		for ( Player player : getOnlinePlayers() )
			sendGameModeHelpMessage(player);
	}
	
	final boolean sendGameModeHelpMessage(Player player)
	{
		PlayerManager.Info info = game.getPlayerInfo().get(player.getName());
		String message = null;
		
		if ( info.nextHelpMessage >= 0 )
		{
			message = getHelpMessage(info.nextHelpMessage, info.getTeam()); // 0 ... n
			if ( message != null )
			{
				if ( info.nextHelpMessage == 0 )
					message = ChatColor.YELLOW + getName() + ChatColor.RESET + "\n" + message; // put the game mode name on the front of the first message
				
				player.sendMessage(message);
				info.nextHelpMessage ++;
				return true;
			}
		}
		return false;
	}
}

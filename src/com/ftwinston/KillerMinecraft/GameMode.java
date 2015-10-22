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
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.ftwinston.KillerMinecraft.Game.GameState;
import com.ftwinston.KillerMinecraft.Game.PlayerInfo;
import com.ftwinston.KillerMinecraft.Configuration.TeamInfo;


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

	public Scoreboard createScoreboard()
	{
		TeamInfo[] teams = getTeams();
		if ( teams == null || teams.length == 0 )
			return Bukkit.getScoreboardManager().getMainScoreboard();
		
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		for ( TeamInfo teamInfo : teams )
		{
			teamInfo.setScoreboardScore(null);
			
			Team team = scoreboard.registerNewTeam(teamInfo.getName());
			team.setAllowFriendlyFire(true);
			team.setCanSeeFriendlyInvisibles(true);
			team.setNameTagVisibility(NameTagVisibility.HIDE_FOR_OTHER_TEAMS);
			team.setPrefix(teamInfo.getChatColor().toString());
			teamInfo.setScoreboardTeam(team);
			
			for ( Player player : getOnlinePlayers(new PlayerFilter().team(teamInfo)) )
				team.addEntry(player.getName());
		}
		return scoreboard;
	}

	public abstract int getMinPlayers();

	private TeamInfo[] teams = null;
	public final TeamInfo[] getTeams() { return teams; }
	protected void setTeams(TeamInfo... teams)
	{
		this.teams = teams;
		if ( game != null )
			game.menuManager.repopulateMenu(MenuManager.GameMenu.TEAM_SELECTION);
	}

	public TeamInfo getTeam(OfflinePlayer player)
	{
		PlayerInfo info = game.getPlayerInfo(player);
		return info == null ? null : info.getTeam();
	}

	public void setTeam(OfflinePlayer player, TeamInfo team) 
	{
		PlayerInfo info = game.getPlayerInfo().get(player.getName());
		if ( info != null )
		{
			TeamInfo oldTeam = info.getTeam();
			if ( oldTeam == team )
				return;
			
			info.setTeam(team);
			
			if ( game.scoreboard != null )
			{	
				Team sbTeam = game.scoreboard.getEntryTeam(player.getName());
				if ( sbTeam != null )
					sbTeam.removeEntry(player.getName());
				
				if ( team != null)
				{
					sbTeam = game.scoreboard.getTeam(team.getName());
					if ( sbTeam != null )
						sbTeam.addEntry(player.getName());
				}
			}
			
			if ( allowTeamSelection() && player.isOnline() )
			{
				Player online = (Player)player;
				if ( team != null )
					online.sendMessage("You are on the " + team.getChatColor() + team.getName());
				else if ( game.getGameState() == GameState.LOBBY )
					online.sendMessage("Your team will be decided automatically");
			}
		}
	}

	public Random random = new Random();
	public void allocateTeams(List<Player> players)
	{
		TeamInfo[] teams = getTeams();
		int[] counts = new int[teams.length];
		for ( int i=0; i<teams.length; i++ )
			counts[i] = game.getPlayers(new PlayerFilter().team(teams[i])).size();
		
		List<Integer> lowest = new ArrayList<Integer>();
		
		// move through the list of players in a random order, assigning them to (one of) the teams with the fewest players
		while ( players.size() > 0 )
		{
			// recalculate which of the teams have the lowest numbers
			if ( lowest.size() == 0 )
			{
				int bestNum = 0;
				for ( int i=0; i<teams.length; i++ )
					if ( counts[i] < bestNum )
					{
						lowest.clear();
						bestNum = counts[i];
						lowest.add(i);
					}
					else if ( counts[i] == bestNum )
						lowest.add(i);
			}
			
			// assign this player
			int iTeam = lowest.remove(random.nextInt(lowest.size()));
			Player player = players.remove(random.nextInt(players.size()));
			setTeam(player, teams[iTeam]);
			counts[iTeam]++;
		} 
	}
	
	public boolean allowWorldGeneratorSelection() { return true; }

	public Environment[] getWorldsToGenerate() { return new Environment[] { Environment.NORMAL, Environment.NETHER }; }
	public void beforeWorldGeneration(int worldNumber, WorldConfig world) { }

	public abstract String getHelpMessage(int messageNum, TeamInfo team);

	public boolean allowTeamSelection() { return teams != null; }

	public abstract boolean isLocationProtected(Location l, Player p); // for protecting plinth, respawn points, etc.

	public abstract Location getSpawnLocation(Player player); // where should this player spawn?

	protected abstract void gameStarted(); // assign player teams if we do that immediately, etc

	protected void gameFinished() { } // clean up scheduled tasks, etc

	public void playerJoinedLate(Player player) { }
	public void playerReconnected(Player player) { }
	public void playerQuit(OfflinePlayer player) { }

	protected Location getCompassTarget(Player player) { return null; } // if compasses should follow someone / something, control that here

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

	protected final int getNumWorlds() { return game.getWorlds().size(); }

	public final World getWorld(int number) { return game.getWorlds().get(number); }

	public void handlePortal(TeleportCause cause, Location entityLoc, PortalHelper helper)
	{
		if ( cause != TeleportCause.NETHER_PORTAL || getNumWorlds() < 2 )
			return;
		
		World toWorld;
		double blockRatio;
		
		if ( entityLoc.getWorld() == getWorld(0) )
		{
			toWorld = getWorld(1);
			blockRatio = 0.125;
		}
		else if ( entityLoc.getWorld() == getWorld(1) )
		{
			toWorld = getWorld(0);
			blockRatio = 8;
		}
		else
			return;
		
		helper.setupScaledDestination(toWorld, entityLoc, blockRatio);
	}

	final void startGame()
	{	
		/*
		plugin.playerManager.startGame(game);
		*/
		gameStarted();
	}
	
	protected final boolean hasGameFinished()
	{
		return !game.getGameState().playersInWorld || game.getGameState() == GameState.FINISHED;
	}

	protected final void finishGame()
	{
		if ( hasGameFinished() )
			return;	
		
		game.setGameState(GameState.FINISHED);
	}

	final void sendGameModeHelpMessage()
	{
		for ( Player player : getOnlinePlayers() )
			sendGameModeHelpMessage(player);
	}
	
	final boolean sendGameModeHelpMessage(Player player)
	{
		PlayerInfo info = game.getPlayerInfo().get(player.getName());
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

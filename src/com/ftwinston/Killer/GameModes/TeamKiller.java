package com.ftwinston.Killer.GameModes;

import java.util.List;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.Settings;
import com.ftwinston.Killer.WorldManager;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Material;

public class TeamKiller extends GameMode
{
	public static final int friendlyFire = 0, compassPointsAtEnemies = 1, optionTwoTeams = 2, optionThreeTeams = 3, optionFourTeams = 4;

	@Override
	public String getName() { return "Team Killer"; }
	
	private int numTeams = 2;
	
	@Override
	public int getMinPlayers() { return numTeams; } // one player on each team is our minimum
	
	@Override
	public Option[] setupOptions()
	{
		Option[] options = {
			new Option("Players can hurt teammates", true),
			new Option("Compasses point at enemies", false),
			new Option("Two teams", true),
			new Option("Three teams", false),
			new Option("Four teams", false)
		};
		
		return options;
	}
	
	@Override
	public String[] getSignDescription()
	{
		return new String[] {
			"Players are put",
			"into teams,",
			"which must then",
			"either kill the",
			
			"other team, or",
			"be the first to",
			"get a blaze rod",
			"and bring it to",
			
			"the spawn point",
			"",
			"",
			""
		};
	}
	
	@Override
	public String describeTeam(int team, boolean plural)
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
		default:
			return plural ? "players" : "player";
		}
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
		default:
			return ChatColor.RESET;
		}
	}
	
	@Override
	public String getHelpMessage(int num, int team)
	{
		switch ( num )
		{
			case 0:
			{
				switch ( numTeams )
				{
					case 2:
						return "Players have been split into two teams.\nThe scoreboard shows what team each player is on.";
					case 3:
						return "Players have been split into three teams.\nThe scoreboard shows what team each player is on.";
					case 4:
						return "Players have been split into four teams.\nThe scoreboard shows what team each player is on.";
					default:
						return "Players have been split into teams.\nThe scoreboard shows what team each player is on.";
				}
			}
			case 1:
				String message = "The teams must race to bring a ";			
				message += tidyItemName(Settings.winningItems[0]);
				
				if ( Settings.winningItems.length > 1 )
				{
					for ( int i=1; i<Settings.winningItems.length-1; i++)
						message += ", a " + tidyItemName(Settings.winningItems[i]);
					
					message += " or a " + tidyItemName(Settings.winningItems[Settings.winningItems.length-1]);
				}
				
				message += " to the plinth near the spawn, or to kill the other team.";
				return message;
			
			case 2:
				return "If everyone else is killed, the last team standing wins.";
			
			default:
				return null;
		}
	}
	
	@Override
	public boolean teamAllocationIsSecret() { return false; }
	
	@Override
	public boolean usesNether() { return true; }
	
	Location plinthLoc;
	
	@Override
	public void worldGenerationComplete(World main, World nether)
	{
		plinthLoc = generatePlinth(main);
	}
	
	@Override
	public boolean isLocationProtected(Location l)
	{
		return isOnPlinth(l); // no protection, except for the plinth
	}
	
	@Override
	public boolean isAllowedToRespawn(Player player) { return false; }
	
	@Override
	public boolean lateJoinersMustSpectate() { return false; }
	
	@Override
	public boolean useDiscreetDeathMessages() { return false; }
	
	@Override
	public Location getSpawnLocation(Player player)
	{
		Location loc;
		switch ( getTeam(player) )
		{
			case 0:
				if ( numTeams == 3 )
				{
					loc = plinthLoc.add(-17, 0, -10); // for 3 teams, ensure they're equidistant from each other, as well as from the plinth
					break;
				}
				else
					loc = plinthLoc.add(-20, 0, 0); break;
			case 1:
				if ( numTeams == 3 )
					loc = plinthLoc.add(17, 0, -10); // for 3 teams, ensure they're equidistant from each other, as well as from the plinth
				else
					loc = plinthLoc.add(20, 0, 0);
				break;
			case 2:
				loc = plinthLoc.add(0, 0, 20); break;
			case 3:
				loc = plinthLoc.add(0, 0, -20); break;
			default:
				loc = WorldManager.instance.mainWorld.getSpawnLocation(); // todo: improve this
				break;
		}
		
		return getSafeSpawnLocationNear(loc);
	}
	
	private int pickSmallestTeam(int[] teamCounts)
	{
		// determining which team(s) have the fewest players in this way means the same one won't always be undersized.
		boolean[] candidateTeams = new boolean[numTeams];
		int fewest = 0, numCandidates = 0;
		for ( int i=0; i<numTeams; i++ )
		{
			int num = teamCounts[i];
			if ( num == fewest )
			{
				candidateTeams[i] = true;
				numCandidates ++;
			}
			else if ( num < fewest )
			{
				candidateTeams[i] = true;
				fewest = num;
				numCandidates = 1;
				
				// clear previous candidates
				for ( int j=0; j<i; j++ )
					candidateTeams[j] = false;
			}
			else
				candidateTeams[i] = false;
		}
		
		// add them to one of the candidates
		int candidatesToSkip = random.nextInt(numCandidates);
		for ( int i=0; i<numTeams; i++ )
			if ( candidateTeams[i] )
			{
				if ( candidatesToSkip == 0 )
					return i;
				candidatesToSkip --;
			}
		
		// should never get here, but ... who knows
		return random.nextInt(teamCounts.length);
	}
	
	@Override
	public void gameStarted()
	{
		int[] teamCounts = new int[numTeams];
		List<Player> players = getOnlinePlayers();
		
		while ( players.size() > 0 )
		{// pick random player, add them to one of the teams with the fewest players (picked randomly)
			Player player = players.remove(random.nextInt(players.size()));
		
			int team = pickSmallestTeam(teamCounts);
			
			setTeam(player, team);
			teamCounts[team] ++;
			player.sendMessage("You are on the " + getTeamChatColor(team) + describeTeam(team, false) + "\n" + ChatColor.RESET + "Use the /team command to send messages to your team only");
		}
	}
	
	@Override
	public void gameFinished()
	{
		// do we need to do anything here?
	}
	
	@Override
	public void playerJoinedLate(Player player, boolean isNewPlayer)
	{
		if ( !isNewPlayer )
			return;
		
		// put this player onto one of the teams with the fewest survivors
		int[] teamCounts = new int[numTeams];
		for ( int i=0; i<numTeams; i++ )
			teamCounts[i] = getOnlinePlayers(i, true).size();
		
		int team = pickSmallestTeam(teamCounts);
		setTeam(player, team);

		player.sendMessage("You are on the " + getTeamChatColor(team) + describeTeam(team, false) + "\n" + ChatColor.RESET + "Use the /team command to send messages to your team only");
		
		broadcastMessage(player, player.getName() + " has joined the " + getTeamChatColor(team) + describeTeam(team, false));
	}
	
	@Override
	public void playerKilledOrQuit(OfflinePlayer player)
	{
		if ( hasGameFinished() )
			return;
		
		int[] teamSizes = new int[numTeams];
		int numTeamsWithSurvivors = 0, someTeamWithSurvivors = -1;
		for ( int i=0; i<numTeams; i++ )
		{
			int num = getOnlinePlayers(i, true).size();
			teamSizes[i] = num;
			if ( num > 0 )
			{
				numTeamsWithSurvivors ++;
				someTeamWithSurvivors = i;
			}
		}
		
		if ( numTeamsWithSurvivors == 1 )
		{
			broadcastMessage("The " + describeTeam(someTeamWithSurvivors, true) + " are the last team standing - they win!");
			finishGame(); // only one team left standing, they win
		}
		else if ( numTeamsWithSurvivors == 0 )
		{
			broadcastMessage("Everybody died - nobody wins!");
			finishGame(); // nobody left standing, nobody wins
		}
	}
	
	@Override
	public Location getCompassTarget(Player player)
	{
		if ( getOption(compassPointsAtEnemies).isEnabled() )
			return getNearestPlayerTo(player, true); // points in a random direction if no players are found
		
		return null;
	}

	@Override
	public void playerActivatedPlinth(Player player)
	{
		// see if the player's inventory contains a winning item
		PlayerInventory inv = player.getInventory();
		
		for ( Material material : Settings.winningItems )
			if ( inv.contains(material) )
			{
				broadcastMessage(player.getName() + " brought a " + tidyItemName(material) + " to the plinth! - The " + describeTeam(getTeam(player), true) + " win!");				
				finishGame(); // winning item brought to the plinth, that player's team win
				break;
			}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void entityDamaged(EntityDamageEvent event)
	{
		if ( shouldIgnoreEvent(event.getEntity()) )
			return;
		
		if ( getOption(friendlyFire).isEnabled() )
			return;
		
		Player victim = (Player)event.getEntity();
		if ( victim == null )
			return;
		
		Player attacker = getAttacker(event);
		if ( attacker == null )
			return;
		
		if ( getTeam(victim) == getTeam(attacker) )
			event.setCancelled(true);
	}

	@Override
	public boolean toggleOption(int num)
	{
		boolean retVal = super.toggleOption(num);
		
		int firstTeamOption = optionTwoTeams, lastTeamOption = optionFourTeams;
		if ( num < optionTwoTeams || num > optionFourTeams )
			return retVal;
		
		if ( retVal )
		{// turned on; turn the others off
			for ( int i=firstTeamOption; i<=lastTeamOption; i++ )
				if ( i != num )
					getOption(i).setEnabled(false);
			
			// change the numTeams value ... it's a happy coincidence that optionTwoTeams = 2, optionThreeTeams = 3, optionFourTeams = 4
			numTeams = num;
		}
		else
		{// turned off; if all are off, turn this one back on
			boolean allOff = true;
			for ( int i=optionTwoTeams; i<=lastTeamOption; i++ )
				if ( getOption(i).isEnabled() )
				{
					allOff = false;
					break;
				}
			if ( allOff )
			{
				getOption(num).setEnabled(true);
				retVal = true;
			}
		}
		return retVal;
	}
}

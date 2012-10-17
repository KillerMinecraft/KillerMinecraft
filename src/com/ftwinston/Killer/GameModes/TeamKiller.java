package com.ftwinston.Killer.GameModes;

import java.util.Map;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.PlayerManager;
import com.ftwinston.Killer.PlayerManager.Info;
import com.ftwinston.Killer.Settings;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.Material;

public class TeamKiller extends GameMode
{
	public static int friendlyFire;
	
	@Override
	public String getName() { return "Team Killer"; }
	
	@Override
	public int getModeNumber() { return 4; }

	@Override
	public int absMinPlayers() { return 2; }

	@Override
	public int getNumTeams() { return 2; } // this should be changeable 

	@Override
	public boolean killersCompassPointsAtFriendlies() { return false; }

	@Override
	public boolean friendliesCompassPointsAtKiller() { return false; }

	@Override
	public boolean discreteDeathMessages() { return false; }

	@Override
	public boolean usesPlinth() { return true; }

	@Override
	public int determineNumberOfKillersToAdd(int numAlive, int numKillers, int numAliveKillers)
	{
		// if we're not set to auto-reassign the killer once one has been assigned at all, even if they're no longer alive / connected, don't do so
		if ( numKillers > 0 )
			return 0;
		
		// half the players should be killers. don't care if its the "bigger" half or not
		if ( numAlive % 2 == 1 )
			return numAlive / 2 + r.nextInt(1); // it's random which team should have the extra player
		else
			return numAlive / 2;
	}
	
	@Override
	public String describePlayer(int team, boolean plural)
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
	
	@Override
	public boolean informOfTeamAssignment(PlayerManager pm) { return false; }
	
	@Override
	public boolean teamAllocationIsSecret() { return false; }
	
	@Override
	public boolean revealTeamIdentityAtEnd(int team) { return false; }
	
	@Override
	public boolean immediateTeamAssignment() { return true; }
	
	@Override
	public String getHelpMessage(int num, int team, boolean isAllocationComplete)
	{
		switch ( num )
		{
			case 0:
				if ( isAllocationComplete )
					return "Players have been split into two teams.\nThe scoreboard shows what team each player is on.";
				else
					return "Players will soon be split into two teams.\nThe scoreboard will show what team each player is on.";
			case 1:
				String message = "The teams must race to bring a ";			
				message += plugin.tidyItemName(Settings.winningItems[0]);
				
				if ( Settings.winningItems.length > 1 )
				{
					for ( int i=1; i<Settings.winningItems.length-1; i++)
						message += ", a " + plugin.tidyItemName(Settings.winningItems[i]);
					
					message += " or a " + plugin.tidyItemName(Settings.winningItems[Settings.winningItems.length-1]);
				}
				
				message += " to the plinth near the spawn, or to kill the other team.";
				return message;
			
			case 2:
				return "If a team are all killed, the other team wins.";
				
			case 3:
				return "Eyes of ender will help you find fortresses in the nether (to get blaze rods).\nThey can be crafted from an ender pearl and a spider eye.";
			
			case 4:
				return "Several monster eggs can be crafted by combining one of their dropped items with an iron ingot.";

			case 5:
				return "Dispensers can be crafted using a sapling instead of a bow. These work well with monster eggs.";
				
			default:
				return null;
		}
	}
	
	@Override
	public String[] getSignDescription()
	{
		return new String[] {
			"Players are put",
			"into two teams,",
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
	public void playerJoined(Player player, PlayerManager pm, boolean isNewPlayer, PlayerManager.Info info)
	{
		if ( isNewPlayer ) // this is a new player, tell them the rules & state of the game
		{
			player.sendMessage("Welcome to Killer Minecraft!");
			
			if ( pm.numPlayersOnTeam(1) > 0 && info.isAlive() )
			{
				// add them to whichever team has fewest players
				int numFriendlies = 0, numKillers = 0;
				for ( Map.Entry<String, Info> entry : pm.getPlayerInfo() )
					if ( entry.getValue().getTeam() == 1 )
						numKillers++;
					else
						numFriendlies++;
				
				boolean killer;
				
				if ( numKillers < numFriendlies )
					killer = true;
				else if ( numKillers == numFriendlies )
					killer = r.nextInt() == 1;
				else
					killer = false;
				
				if ( killer )
				{
					info.setTeam(1);
					plugin.broadcastMessage(player.getName() + " is on the " + ChatColor.RED + "red" + ChatColor.RESET + " team.");
				}
				else
				{
					info.setTeam(0);
					plugin.broadcastMessage(player.getName() + " is on the " + ChatColor.BLUE + "blue" + ChatColor.RESET + " team.");
				}
			}
		}
		else
			player.sendMessage("Welcome back.");
	}
	
	private final int teamSeparationOffset = 25;
	
	@Override
	public void preparePlayer(Player player, PlayerManager pm, int team, boolean isNewPlayer)
	{
		if ( team == 0 )
		{
			player.sendMessage("You are on the " + ChatColor.BLUE + "blue" + ChatColor.RESET + " team. Use the /team command to chat without the other team seeing your messages.");
			
			if ( !isNewPlayer )
				return; // don't do the teleport thing when reconnecting
				
			Location moveTo = player.getWorld().getSpawnLocation();
			moveTo.setZ(moveTo.getZ() - teamSeparationOffset - r.nextDouble() * 6);
			moveTo.setX(moveTo.getX() + r.nextDouble() * 6);
			moveTo.setY(moveTo.getWorld().getHighestBlockYAt(moveTo) + 1);
			
			player.teleport(moveTo);		
		}
		else if ( team == 1 )
		{
			player.sendMessage("You are on the " + ChatColor.RED + "red" + ChatColor.RESET + " team. Use the /team command to chat without the other team seeing your messages.");
			
			if ( !isNewPlayer )
				return; // don't do the teleport thing when reconnecting
				
			Location moveTo = player.getWorld().getSpawnLocation();
			moveTo.setZ(moveTo.getZ() + teamSeparationOffset + r.nextDouble() * 6);
			moveTo.setX(moveTo.getX() + r.nextDouble() * 6);
			moveTo.setY(moveTo.getWorld().getHighestBlockYAt(moveTo) + 1);
			
			player.teleport(moveTo);
		}
	}
	
	@Override
	public void checkForEndOfGame(PlayerManager pm, Player playerOnPlinth, Material itemOnPlinth)
	{
		// if there's no one alive at all, game was drawn
		if (pm.numSurvivors() == 0 )
		{
			pm.gameFinished(false, false, null, null);
			return;
		}
		
		// if someone stands on the plinth with a winning item, their team wins
		if ( playerOnPlinth != null && itemOnPlinth != null )
		{
			boolean killersWin = pm.getTeam(playerOnPlinth.getName()) == 1;
			pm.gameFinished(killersWin, !killersWin, playerOnPlinth.getName(), itemOnPlinth);
			return;
		}
		
		boolean killersAlive = false, friendliesAlive = false;
		for ( Map.Entry<String, Info> entry : pm.getPlayerInfo() )
			if ( entry.getValue().isAlive() )
				if ( entry.getValue().getTeam() == 1 )
					killersAlive = true;
				else
					friendliesAlive = true;
		
		// if only killers are left alive, the killers won
		if ( killersAlive && !friendliesAlive )
			pm.gameFinished(true, false, null, null);
		// if only friendlies are left alive, the friendlies won
		else if ( !killersAlive && friendliesAlive )
			pm.gameFinished(false, true, null, null);
	}
	
	@Override
	public boolean playerDamaged(Player victim, Entity attacker, DamageCause cause, int amount)
	{	
		if ( options.get(friendlyFire).isEnabled() )
			return true;
		
		if ( !(attacker instanceof Player) )
			return true; // we only care about damage by players

		PlayerManager pm = plugin.playerManager;
		Player attackerPlayer = (Player)attacker;
		return pm.getTeam(victim.getName()) != pm.getTeam(attackerPlayer.getName());
	}
	
	@Override
	public boolean toggleOption(int num)
	{
		boolean retVal = super.toggleOption(num);
		/*
		// if we have "number of teams" options
		int numTeamOptions = 4;
		if ( retVal )
		{// turned on, turn the others off
			for ( int i=0; i<numTeamOptions; i++ )
				if ( i != num )
					options.get(i).setEnabled(false);
		}
		else
		{// turned off; if all are off, turn this one back on
			boolean allOff = true;
			for ( int i=0; i<numTeamOptions; i++ )
				if ( options.get(i).isEnabled() )
				{
					allOff = false;
					break;
				}
			if ( allOff )
				options.get(num).setEnabled(true);
		}
		*/
		return retVal;
	}
}

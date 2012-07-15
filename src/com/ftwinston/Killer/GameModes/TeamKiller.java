package com.ftwinston.Killer.GameModes;

import java.util.Random;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.PlayerManager;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.Material;

public class TeamKiller extends GameMode
{
	Random r = new Random();
	
	@Override
	public String getName() { return "Team Killer"; }

	@Override
	public int absMinPlayers() { return 2; }

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
	public String describePlayer(boolean killer)
	{
		if ( killer )
			return "red player";
		else
			return "blue player";
	}
	
	@Override
	public boolean informOfKillerAssignment(PlayerManager pm) { return true; }
	
	@Override
	public boolean informOfKillerIdentity() { return true; }
	
	@Override
	public boolean immediateKillerAssignment() { return true; }
	
	@Override
	public boolean playerJoined(Player player, PlayerManager pm, boolean isNewPlayer, boolean isKiller, int numKillersAssigned)
	{
		if ( isKiller ) // inform them that they're still a killer
			player.sendMessage("Welcome back. You are on the " + ChatColor.RED + "red" + ChatColor.RESET + " team."); 
		else if ( isNewPlayer ) // this is a new player, tell them the rules & state of the game
		{
			String message = "Welcome to Killer Minecraft! Players ";
			message += numKillersAssigned > 0 ? "have been" : "will soon be";
			message += " split into two teams, and the teams must race to bring a ";
			
			message += plugin.tidyItemName(plugin.winningItems[0]);
			
			if ( plugin.winningItems.length > 1 )
			{
				for ( int i=1; i<plugin.winningItems.length-1; i++)
					message += ", a " + plugin.tidyItemName(plugin.winningItems[i]);
				
				message += " or a " + plugin.tidyItemName(plugin.winningItems[plugin.winningItems.length-1]);
			}
			
			message += " to the plinth near the spawn, or to eliminate each other.";
			player.sendMessage(message);
			
			if ( numKillersAssigned > 0 )
			{
				if ( plugin.lateJoinersStartAsSpectator )
					return false; // not alive, should be a spectator

				// add them to whichever team has fewest left alive
				int numFriendliesAlive = 0, numKillersAlive = 0;
				for ( String count : pm.getSurvivors() )
					if ( pm.isKiller(count) )
						numKillersAlive++;
					else
						numFriendliesAlive++;
				
				if ( numKillersAlive > numFriendliesAlive )
				{
					pm.setKiller(player.getName());
					plugin.getServer().broadcastMessage(player.getName() + " is on the " + ChatColor.RED + "red" + ChatColor.RESET + " team.");
					pm.colorPlayerName(player, ChatColor.RED);
				}
				else if ( numKillersAlive == numFriendliesAlive )
				{
					if ( r.nextInt() == 1 )
					{
						pm.setKiller(player.getName());
						plugin.getServer().broadcastMessage(player.getName() + " is on the " + ChatColor.RED + "red" + ChatColor.RESET + " team.");
						pm.colorPlayerName(player, ChatColor.RED);						
					}
					else
					{
						plugin.getServer().broadcastMessage(player.getName() + " is on the " + ChatColor.BLUE + "blue" + ChatColor.RESET + " team.");
						pm.colorPlayerName(player, ChatColor.BLUE);
					}
				}
				else
				{
					plugin.getServer().broadcastMessage(player.getName() + " is on the " + ChatColor.BLUE + "blue" + ChatColor.RESET + " team.");
					pm.colorPlayerName(player, ChatColor.BLUE);
				}
			}
		}
		else
			player.sendMessage("Welcome back. You are on the " + ChatColor.BLUE + "blue" + ChatColor.RESET + " team.");
			
		return true; // this player should now be alive
	}
	
	private final int teamSeparationOffset = 25;
	
	@Override
	public void prepareKiller(Player player, PlayerManager pm)
	{
		player.sendMessage("You are on the " + ChatColor.RED + "red" + ChatColor.RESET + " team.");
		
		Location moveTo = player.getWorld().getSpawnLocation();
		moveTo.setZ(moveTo.getZ() + teamSeparationOffset + r.nextDouble() * 6);
		moveTo.setX(moveTo.getX() + r.nextDouble() * 6);
		moveTo.setY(moveTo.getWorld().getHighestBlockYAt(moveTo) + 1);
		
		player.teleport(moveTo);
	}
	
	@Override
	public void prepareFriendly(Player player, PlayerManager pm)
	{
		player.sendMessage("You are on the " + ChatColor.BLUE + "blue" + ChatColor.RESET + " team.");
		
		Location moveTo = player.getWorld().getSpawnLocation();
		moveTo.setZ(moveTo.getZ() - teamSeparationOffset - r.nextDouble() * 6);
		moveTo.setX(moveTo.getX() + r.nextDouble() * 6);
		moveTo.setY(moveTo.getWorld().getHighestBlockYAt(moveTo) + 1);
		
		player.teleport(moveTo);
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
			boolean killersWin = pm.isKiller(playerOnPlinth.getName());
			pm.gameFinished(killersWin, !killersWin, playerOnPlinth.getName(), itemOnPlinth);
			return;
		}
		
		boolean killersAlive = false, friendliesAlive = false;
		for ( String survivor : pm.getSurvivors() )
			if ( pm.isKiller(survivor) )
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
}

package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import com.ftwinston.Killer.PlayerManager;

import com.ftwinston.Killer.GameModes.ContractKiller;
import com.ftwinston.Killer.GameModes.CrazyKiller;
import com.ftwinston.Killer.GameModes.InvisibleKiller;
import com.ftwinston.Killer.GameModes.MysteryKiller;
import com.ftwinston.Killer.GameModes.TeamKiller;
import com.ftwinston.Killer.PlayerManager.Info;

public abstract class GameMode
{
	public static List<GameMode> gameModes = new ArrayList<GameMode>();

	public static void setup(Killer killer)
	{
		GameMode g;
	
		if ( Settings.allowModeMysteryKiller )
		{
			g = new MysteryKiller();
			g.plugin = killer;
			//g.options.put("Assign sooner", false);
			//g.options.put("Multiple killers", false);
			gameModes.add(g);
		}
		
		if ( Settings.allowModeInvisibleKiller )
		{
			g = new InvisibleKiller();
			g.plugin = killer;
			//g.options.put("Decloak when sword drawn", false);
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
			//g.options.put("Friendly fire", true);
			gameModes.add(g);
		}
		
		if ( Settings.allowModeContractKiller )
		{
			g = new ContractKiller();
			g.plugin = killer;
			//g.options.put("Players spawn far apart", false);
			gameModes.add(g);
		}
		
		if ( gameModes.size() == 1 )
			killer.setGameMode(gameModes.get(0));
	}
	
	public static GameMode get(int num) { return gameModes.get(num); }
	
	protected Killer plugin;
	protected Random r = new Random();
	
	public abstract String getName();
	public abstract int getModeNumber();
	public abstract int absMinPlayers();
	public abstract boolean killersCompassPointsAtFriendlies();
	public abstract boolean friendliesCompassPointsAtKiller();
	public boolean compassPointsAtTarget() { return false; }
	public abstract boolean discreteDeathMessages();
	public abstract boolean usesPlinth();
	public abstract int determineNumberOfKillersToAdd(int numAlive, int numKillers, int numAliveKillers);
	
	public abstract String describePlayer(boolean killer, boolean plural);
	public abstract boolean immediateKillerAssignment();
	public abstract boolean informOfKillerAssignment(PlayerManager pm);
	public abstract boolean informOfKillerIdentity();
	public abstract boolean revealKillersIdentityAtEnd();
	
	public void sendGameModeHelpMessage(PlayerManager pm)
	{
		for ( Player player : plugin.getOnlinePlayers() )
			sendGameModeHelpMessage(pm, player);
	}
	
	public void sendGameModeHelpMessage(PlayerManager pm, Player player)
	{
		PlayerManager.Info info = pm.getInfo(player.getName());
		if ( info.nextHelpMessage == -1 )
			return;
		
		boolean isAllocationComplete = pm.numKillersAssigned() > 0; // todo: replace with proper value, once we have a game state variable
		
		String message = getHelpMessage(info.nextHelpMessage, info.isKiller(), isAllocationComplete);
		if ( info.nextHelpMessage == 0 )
			message = getName() + "\n" + message; // put the game mode name on the front of the first message
		player.sendMessage(message);
		
		if ( info.nextHelpMessage == getNumHelpMessages(info.isKiller()) - 1 )
			info.nextHelpMessage = -1;
		else
			info.nextHelpMessage ++;
	}
	
	public abstract String[] getSignDescription();
	
	public abstract int getNumHelpMessages(boolean forKiller);
	public abstract String getHelpMessage(int num, boolean forKiller, boolean isAllocationComplete);
	
	public void gameStarted() { }
	public void gameFinished() { }
	
	public boolean assignKillers(int numKillers, CommandSender sender, PlayerManager pm)
	{
		int availablePlayers = 0;
		for ( Map.Entry<String, Info> entry : pm.getPlayerInfo() )
		{
			if ( !entry.getValue().isAlive() || entry.getValue().isKiller() )
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
			if ( informOfKillerAssignment(pm) )
				plugin.broadcastMessage(message);
			return false;
		}
		
		if ( informOfKillerAssignment(pm) && !informOfKillerIdentity() )
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
			
			if ( numKillers > 1 || pm.numKillersAssigned() > 0 )
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
			if ( !entry.getValue().isAlive() || entry.getValue().isKiller() )
				continue;
			
			Player player = plugin.getServer().getPlayerExact(entry.getKey());
			if ( player == null || !player.isOnline() )
				continue;

			if ( nextIndex < numKillers && num == killerIndices[nextIndex] )
			{
				entry.getValue().setKiller(true);
				
				// don't show this in team killer mode, but do show it in mystery killer, even when assigning a new killer midgame and not telling everyone else
				if ( informOfKillerAssignment(pm) || !informOfKillerIdentity() )
				{
					String message = ChatColor.RED + "You are ";
					message += numKillers > 1 || pm.numKillersAssigned() > 1 ? "now a" : "the";
					message += " killer!";
					
					if ( !informOfKillerAssignment(pm) && !informOfKillerIdentity() )
						message += ChatColor.WHITE + " No one else has been told a new killer was assigned.";
						
					player.sendMessage(message);
				}
				if ( informOfKillerIdentity() )
				{
					if ( informOfKillerAssignment(pm) )
						plugin.broadcastMessage(player.getName() + " is the killer!");
					pm.colorPlayerName(player, ChatColor.RED);
				}
				
				prepareKiller(player, pm, true);
				
				nextIndex ++;
				
				plugin.statsManager.killerAdded();
				if ( sender != null )
					plugin.statsManager.killerAddedByAdmin();
			}
			else if ( !pm.isKiller(player.getName()) )
			{
				prepareFriendly(player, pm, true);
				
				if ( informOfKillerIdentity() )
					pm.colorPlayerName(player, ChatColor.BLUE);
				else if ( informOfKillerAssignment(pm) )
					player.sendMessage(ChatColor.YELLOW + "You are not " + ( numKillers > 1 || pm.numKillersAssigned() > 1 ? "a" : "the") + " killer.");
			}
			
			num++;
		}
		return true;
	}
	
	public abstract void playerJoined(Player player, PlayerManager pm, boolean isNewPlayer, PlayerManager.Info info);
	public void playerKilled(Player player, PlayerManager pm, PlayerManager.Info info) { }
	
	public abstract void prepareKiller(Player player, PlayerManager pm, boolean isNewPlayer);
	public abstract void prepareFriendly(Player player, PlayerManager pm, boolean isNewPlayer);
	
	public abstract void checkForEndOfGame(PlayerManager pm, Player playerOnPlinth, Material itemOnPlinth);
	
	public boolean playerDamaged(Player victim, Entity attacker, DamageCause cause, int amount) { return true; }
	public void playerEmptiedBucket(PlayerBucketEmptyEvent event) { }
	public void playerPickedUpItem(PlayerPickupItemEvent event) { }
	
	private Map<String, Boolean> options = new LinkedHashMap<String, Boolean>();
	public Map<String, Boolean> getOptions() { return options; }

	public Entry<String, Boolean> getOptionEntry(int num)
	{
		Set<Map.Entry<String, Boolean>> entries = options.entrySet();
		int i = 0;
		for ( Entry<String, Boolean> entry : entries )
		{
			if ( i == num )
				return entry;
			i++;
		}
		return null;
	}
}

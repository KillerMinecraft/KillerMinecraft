package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
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
			gameModes.add(g);

			MysteryKiller.dontAssignKillerUntilSecondDay = g.options.size();
			g.options.add(g.new Option("Don't assign killer until the second day", true));

			MysteryKiller.autoReallocateKillers = g.options.size();
			g.options.add(g.new Option("Allocate new killer if old ones die", true));

			MysteryKiller.allowMultipleKillers = g.options.size();
			g.options.add(g.new Option("Assign multiple killers if lots of people play", false));
		}
		
		if ( Settings.allowModeInvisibleKiller )
		{
			g = new InvisibleKiller();
			g.plugin = killer;
			gameModes.add(g);

			InvisibleKiller.killerDistanceMessages = g.options.size();
			g.options.add(g.new Option("Tell players how far away the killer is", true));

			InvisibleKiller.decloakWhenWeaponDrawn = g.options.size();
			g.options.add(g.new Option("Killer decloaks when sword or bow is drawn", false));
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

			TeamKiller.friendlyFire = g.options.size();
			g.options.add(g.new Option("Players can hurt teammates", true));
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
	
	public abstract void assignTeams();
	public abstract boolean teamAllocationIsSecret();
	
	public abstract int getNumTeams();
	public abstract String describePlayer(int team, boolean plural);
	public abstract boolean immediateTeamAssignment();
	public abstract boolean informOfTeamAssignment(PlayerManager pm);
	
	public abstract boolean revealTeamIdentityAtEnd(int team);
	
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
		
		boolean isAllocationComplete = pm.numPlayersOnTeam(1) > 0; // todo: replace with proper value, once we have a game state variable
		
		String message = getHelpMessage(info.nextHelpMessage, info.getTeam(), isAllocationComplete);
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
	
	public abstract String[] getSignDescription();
	
	public abstract String getHelpMessage(int num, int team, boolean isAllocationComplete);
	
	public void gameStarted() { }
	public void gameFinished() { }
	
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
	public void playerKilled(Player player, PlayerManager pm, PlayerManager.Info info) { }
	
	public abstract void preparePlayer(Player player, PlayerManager pm, int teamNum, boolean isNewPlayer);
	
	public abstract void checkForEndOfGame(PlayerManager pm, Player playerOnPlinth, Material itemOnPlinth);
	
	public boolean playerDamaged(Player victim, Entity attacker, DamageCause cause, int amount) { return true; }
	public void playerEmptiedBucket(PlayerBucketEmptyEvent event) { }
	public void playerPickedUpItem(PlayerPickupItemEvent event) { }
	public void playerDroppedItem(Player player, Item item) { }
	public void playerInventoryClick(Player player, InventoryClickEvent event) { }
	public void playerItemSwitch(Player player, int prevSlot, int newSlot) { }
	
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
	protected boolean shouldIgnoreEvent(LivingEntity e)
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

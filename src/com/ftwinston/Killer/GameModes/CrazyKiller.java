package com.ftwinston.Killer.GameModes;

import java.util.Map;
import java.util.Random;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.PlayerManager;
import com.ftwinston.Killer.PlayerManager.Info;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class CrazyKiller extends GameMode
{
	@Override
	public String getName() { return "Crazy Killer"; }
	
	@Override
	public int getModeNumber() { return 3; }

	@Override
	public int absMinPlayers() { return 2; }

	@Override
	public boolean killersCompassPointsAtFriendlies() { return true; }

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
		if ( !plugin.autoReassignKiller && numKillers > 0 )
			return 0;
		
		// for now, one living killer at a time is plenty
		return numAliveKillers > 0 ? 0 : 1;
	}
	
	@Override
	public String describePlayer(boolean killer, boolean plural)
	{
		if ( killer )
			return plural ? "killers" : "killer";
		else
			return plural ? "friendly players" : "friendly player";
	}
	
	@Override
	public boolean informOfKillerAssignment(PlayerManager pm) { return true; }
	
	@Override
	public boolean informOfKillerIdentity() { return true; }
	
	@Override
	public boolean revealKillersIdentityAtEnd() { return false; }
	
	@Override
	public boolean immediateKillerAssignment() { return true; }
	
	@Override
	public int getNumHelpMessages(boolean forKiller) { return 6; }
	
	@Override
	public String getHelpMessage(int num, boolean forKiller, boolean isAllocationComplete)
	{
		switch ( num )
		{
			case 0:
				if ( forKiller )
					return "You have been chosen to be the killer, and must kill everyone else. They know who you are.";
				else if ( isAllocationComplete )
					return "A player has been chosen to be the killer, and must kill everyone else.";
				else
					return "A player will soon be chosen to be the killer. You'll be told who it is, and they will be teleported away from the other players.";
			case 1:
				if ( forKiller )
					return "Every dirt block you pick up will turn into TNT, so have fun with that.";
				else
					return "Every dirt block the killer picks up will turn into TNT, so beware.";
			case 2:
				if ( forKiller )
					return "The other players each start with a sword, so avoid a direct fight.";
				else
					return "The killer doesn't start iwth a sword, but all the other players do.";
			case 3:
				if ( forKiller )
					return "Your compass will point at the nearest player.";
				else
					return "The killer starts with a compass, which points at the nearest player.";
			case 4:
				String message = "The other players win if the killer dies, or if they bring a ";			
				message += plugin.tidyItemName(plugin.winningItems[0]);
				
				if ( plugin.winningItems.length > 1 )
				{
					for ( int i=1; i<plugin.winningItems.length-1; i++)
						message += ", a " + plugin.tidyItemName(plugin.winningItems[i]);
					
					message += " or a " + plugin.tidyItemName(plugin.winningItems[plugin.winningItems.length-1]);
				}
				
				message += " to the plinth near the spawn.";
				return message;
			case 5:
				if ( forKiller )
					return "You can make buttons and pressure plates with the stone you started with.\nTry to avoid blowing yourself up!";
				else
					return "The killer starts with enough stone and redstone to make plenty buttons, wires and pressure plates.";
			default:
				return "";
		}
	}
	
	@Override
	public String[] getSignDescription()
	{
		return new String[] {
			"A player is",
			"chosen to kill",
			"the rest. They",
			"pick up TNT",
			"instead of dirt",
			"and start with",
			"some redstone.",
			"The others must",
			"kill them, or",
			"get a blaze rod",
			"and bring it to",
			"the spawn point"
		};
	}

	@Override
	public void playerJoined(Player player, PlayerManager pm, boolean isNewPlayer, PlayerManager.Info info)
	{
		if ( info.isKiller() ) // inform them that they're still a killer
			player.sendMessage("Welcome back. " + ChatColor.RED + "You are still " + (pm.numKillersAssigned() > 1 ? "a" : "the" ) + " killer."); 
		else if ( isNewPlayer || !info.isAlive() ) // this is a new player
			player.sendMessage("Welcome to Killer Minecraft!");
		else
			player.sendMessage("Welcome back. You are not the killer, and you're still alive.");
	}
	
	@Override
	public void prepareKiller(Player player, PlayerManager pm, boolean isNewPlayer)
	{
		player.sendMessage("Every dirt block you pick up will turn into TNT...");
		
		if ( !isNewPlayer )
			return; // don't teleport or give new items on rejoining
		
		PlayerInventory inv = player.getInventory();
		inv.addItem(new ItemStack(Material.COMPASS, 1));
		inv.addItem(new ItemStack(Material.COOKED_BEEF, 10));
		
		inv.addItem(new ItemStack(Material.REDSTONE, 64));
		inv.addItem(new ItemStack(Material.STONE, 64));
		
		// you don't START with piles of TNT, however
		inv.addItem(new ItemStack(Material.TNT, 4));
		
		// teleport the killer a little bit away from the other players, to stop them being immediately stabbed
		Random r = new Random();
		Location loc = player.getLocation();
		
		if ( r.nextBoolean() )
			loc.setX(loc.getX() + 32 + r.nextDouble() * 20);
		else
			loc.setX(loc.getX() - 32 - r.nextDouble() * 20);
			
		if ( r.nextBoolean() )
			loc.setZ(loc.getZ() + 32 + r.nextDouble() * 20);
		else
			loc.setZ(loc.getZ() - 32 - r.nextDouble() * 20);
		
		loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1);
		player.teleport(loc);
	}
	
	@Override
	public void prepareFriendly(Player player, PlayerManager pm, boolean isNewPlayer)
	{
		player.sendMessage("Use the /team command to chat without the killer seeing your messages");
	
		if ( !isNewPlayer )
			return; // don't give items on rejoining
			
		PlayerInventory inv = player.getInventory();
		inv.addItem(new ItemStack(Material.IRON_SWORD, 1));
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
		
		// if someone stands on the plinth with a winning item, the friendlies win
		if ( playerOnPlinth != null && itemOnPlinth != null )
		{
			pm.gameFinished(false, true, playerOnPlinth.getName(), itemOnPlinth);
			return;
		}
		
		boolean killersAlive = false, friendliesAlive = false;
		for ( Map.Entry<String, Info> entry : pm.getPlayerInfo() )
			if ( entry.getValue().isAlive() )
				if ( entry.getValue().isKiller() )
					killersAlive = true;
				else
					friendliesAlive = true;
		
		// if only killers are left alive, the killer won
		if ( killersAlive && !friendliesAlive )
			pm.gameFinished(true, false, null, null);
		// if only friendlies are left alive, the friendlies won
		else if ( !killersAlive && friendliesAlive )
			pm.gameFinished(false, true, null, null);
	}
	
	@Override
	public void playerPickedUpItem(PlayerPickupItemEvent event)
	{
		if ( event.getItem().getItemStack().getType() == Material.DIRT && plugin.playerManager.isKiller(event.getPlayer().getName()) )
			event.getItem().getItemStack().setType(Material.TNT);
	}
}

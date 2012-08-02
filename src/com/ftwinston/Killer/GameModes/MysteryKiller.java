package com.ftwinston.Killer.GameModes;

import java.util.Map;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.PlayerManager;
import com.ftwinston.Killer.PlayerManager.Info;

import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class MysteryKiller extends GameMode
{
	@Override
	public String getName() { return "Mystery Killer"; }

	@Override
	public int getModeNumber() { return 1; }
	
	@Override
	public int absMinPlayers() { return 3; }

	@Override
	public boolean killersCompassPointsAtFriendlies() { return true; }

	@Override
	public boolean friendliesCompassPointsAtKiller() { return false; }

	@Override
	public boolean discreteDeathMessages() { return true; }

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
	public boolean informOfKillerAssignment(PlayerManager pm) { return pm.numKillersAssigned() == 0; }
	
	@Override
	public boolean informOfKillerIdentity() { return false; }
	
	@Override
	public boolean immediateKillerAssignment() { return false; }
	
	@Override
	public void explainGameMode(Player player, PlayerManager pm)
	{
		boolean isKiller = pm.isKiller(player.getName());
		String message = getName() + "\n";
		if ( isKiller )
			message += "You have been";
		else
			message += "One player " + (pm.numKillersAssigned() > 0 ? "has been" : "will soon be");
		
		message += " randomly chosen to be the killer, and must kill everyone else. No one else knows who the killer is, and to win, the other players must bring a ";
			
			message += plugin.tidyItemName(plugin.winningItems[0]);
			
			if ( plugin.winningItems.length > 1 )
			{
				for ( int i=1; i<plugin.winningItems.length-1; i++)
					message += ", a " + plugin.tidyItemName(plugin.winningItems[i]);
				
				message += " or a " + plugin.tidyItemName(plugin.winningItems[plugin.winningItems.length-1]);
			}
			
			message += " to the plinth near the spawn.\nThe other players will not automatically win when the killer is killed, and another killer may be assigned once the first one is dead.";
			
			if ( isKiller )
				message += "\nIf you make a compass, it will point at the nearest player.";
			player.sendMessage(message);
	}
	
	@Override
	public void playerJoined(Player player, PlayerManager pm, boolean isNewPlayer, PlayerManager.Info info)
	{
		if ( info.isKiller() ) // inform them that they're still a killer
			player.sendMessage("Welcome back. " + ChatColor.RED + "You are still " + (pm.numKillersAssigned() > 1 ? "a" : "the" ) + " killer!"); 
		else if ( isNewPlayer ) // this is a new player, tell them the rules & state of the game
			player.sendMessage("Welcome to Killer Minecraft!");
		else
			player.sendMessage("Welcome back. You are not the killer, and you're still alive.");
	}
	
	@Override
	public void prepareKiller(Player player, PlayerManager pm, boolean isNewPlayer)
	{
		if ( !isNewPlayer )
			return; // don't let the killer rejoin to get more items
	
		player.sendMessage("If you make a compass, it will point at the nearest player.");
				
		PlayerInventory inv = player.getInventory();
		int numFriendlies = pm.numSurvivors() - pm.numKillersAssigned();
		
		if ( numFriendlies >= 2 )
			inv.addItem(new ItemStack(Material.STONE, 6));
		else
			return;
		
		if ( numFriendlies >= 3 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.IRON_INGOT, 1), new ItemStack(Material.REDSTONE, 2));
		else
			return;
		
		if ( numFriendlies >= 4 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.SULPHUR, 1));
		else
			return;
		
		if ( numFriendlies >= 5 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.IRON_INGOT, 1), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.ARROW, 3));
		else
			return;
		
		if ( numFriendlies >= 6 )
			inv.addItem(new ItemStack(Material.MONSTER_EGG, 1, (short)50), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.SULPHUR, 1), new ItemStack(Material.ARROW, 2));
		else
			return;
		
		if ( numFriendlies >= 7 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.SULPHUR, 1), new ItemStack(Material.ARROW, 2));
			
			if ( numFriendlies < 11 )
				inv.addItem(new ItemStack(Material.IRON_PICKAXE, 1)); // at 11 friendlies, they'll get a diamond pick instead
		}
		else
			return;
		
		if ( numFriendlies >= 8 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.BOW, 1), new ItemStack(Material.ARROW, 3));
		else
			return;
		
		if ( numFriendlies >= 9 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.MONSTER_EGGS, 4, (short)0), new ItemStack(Material.STONE, 2));
		else
			return;
		
		if ( numFriendlies >= 10 )
			inv.addItem(new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.MONSTER_EGG, 1, (short)50), new ItemStack(Material.ARROW, 2));
		else
			return;
		
		if ( numFriendlies >= 11 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.SULPHUR, 1));
			
			if ( numFriendlies < 18 )
				inv.addItem(new ItemStack(Material.DIAMOND_PICKAXE, 1)); // at 18 friendlies, they get an enchanted version
		}
		else
			return;
		
		if ( numFriendlies >= 12 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.STONE, 2), new ItemStack(Material.SULPHUR, 1));
		else
			return;
		
		if ( numFriendlies >= 13 )
			inv.addItem(new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.MONSTER_EGGS, 2, (short)0), new ItemStack(Material.ARROW, 2));
		else
			return;
		
		if ( numFriendlies >= 14 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2), new ItemStack(Material.MONSTER_EGGS, 1, (short)0), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.STONE, 2));
		else
			return;
		
		if ( numFriendlies >= 15 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2), new ItemStack(Material.MONSTER_EGGS, 1, (short)0), new ItemStack(Material.PISTON_STICKY_BASE, 3));
		else
			return;
		
		if ( numFriendlies >= 16 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 1), new ItemStack(Material.SULPHUR, 5));
		else
			return;
		
		if ( numFriendlies >= 17 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 1), new ItemStack(Material.MONSTER_EGG, 1, (short)50), new ItemStack(Material.ARROW, 2));
		else
			return;
		
		if ( numFriendlies >= 18 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2));
			if ( numFriendlies == 18 )
			{
				ItemStack stack = new ItemStack(Material.DIAMOND_PICKAXE, 1);
				stack.addEnchantment(Enchantment.DIG_SPEED, 1);
				inv.addItem(stack);
			}
		}
		else
			return;
		
		if ( numFriendlies >= 19 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2));
			if ( numFriendlies == 19 )
			{
				ItemStack stack = new ItemStack(Material.DIAMOND_PICKAXE, 1);
				stack.addEnchantment(Enchantment.DIG_SPEED, 2);
				inv.addItem(stack);
			}
		}
		else
			return;
		
		if ( numFriendlies >= 20 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2));
		
			ItemStack stack = new ItemStack(Material.DIAMOND_PICKAXE, 1);
			stack.addEnchantment(Enchantment.DIG_SPEED, 3);
			inv.addItem(stack);
		}
		else
			return;	
	}
	
	@Override
	public void prepareFriendly(Player player, PlayerManager pm, boolean isNewPlayer)
	{
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
		
		// if there's no one alive that isn't a killer, the killer(s) won
		boolean onlyKillersLeft = true;
		for ( Map.Entry<String, Info> entry : pm.getPlayerInfo() )
			if ( entry.getValue().isAlive() && !entry.getValue().isKiller() )
			{// this person is alive and not a killer
				onlyKillersLeft = false;
				break;
			}
		
		if ( onlyKillersLeft )
		{
			pm.gameFinished(true, false, null, null);
			return;
		}

		// if only one person left (and they're not the killer), tell people they can /vote if they want to start a new game
		if ( pm.numSurvivors() == 1 )
			for ( Map.Entry<String, Info> entry : pm.getPlayerInfo() )
				if ( entry.getValue().isAlive() )
				{
					Player last = plugin.getServer().getPlayerExact(entry.getKey());
					if ( last != null && last.isOnline() )
					{
						plugin.getServer().broadcastMessage("There's only one player left, and they're not the killer. If you want to draw this game and start another, start a vote by typing " + ChatColor.YELLOW + "/vote");	
						return;
					}
				}
	}
}

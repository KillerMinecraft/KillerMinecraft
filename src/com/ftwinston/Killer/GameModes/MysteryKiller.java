package com.ftwinston.Killer.GameModes;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.Killer;
import com.ftwinston.Killer.PlayerManager;

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
		if ( !Killer.instance.autoReassignKiller && numKillers > 0 )
			return 0;
		
		// for now, one living killer at a time is plenty
		return numAliveKillers > 0 ? 0 : 1;
	}
	
	@Override
	public String describePlayer(boolean killer)
	{
		if ( killer )
			return "killer";
		else
			return "friendly player";
	}
	
	@Override
	public boolean playerJoined(Player player, PlayerManager playerManager, boolean isNewPlayer, boolean isKiller, int numKillersAssigned)
	{
		Killer plugin = Killer.instance;
	
		if ( isKiller ) // inform them that they're still a killer
			player.sendMessage("Welcome back. " + ChatColor.RED + "You are still " + (numKillersAssigned > 1 ? "a" : "the" ) + " killer!"); 
		else if ( isNewPlayer ) // this is a new player, tell them the rules & state of the game
		{
			String message = "Welcome to Killer Minecraft! One player ";
			message += numKillersAssigned > 0 ? "has been" : "will soon be";
			message += " assigned as the killer, and must kill the rest. To win, the other players must bring a ";
			
			message += plugin.tidyItemName(plugin.winningItems[0]);
			
			if ( plugin.winningItems.length > 1 )
			{
				for ( int i=1; i<plugin.winningItems.length-1; i++)
					message += ", a " + plugin.tidyItemName(plugin.winningItems[i]);
				
				message += " or a " + plugin.tidyItemName(plugin.winningItems[plugin.winningItems.length-1]);
			}
			
			message += " to the plinth near the spawn.";
			player.sendMessage(message);
			
			if ( numKillersAssigned > 0 && plugin.lateJoinersStartAsSpectator )
				return false; // not alive, should be a spectator
		}
		else
			player.sendMessage("Welcome back. You are not the killer, and you're still alive.");
			
		return true; // this player should now be alive
	}
	
	@Override
	public void giveItemsToKiller(Player player, int numKillers, int numFriendlies)
	{
		PlayerInventory inv = player.getInventory();
		
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
	public void giveItemsToFriendly(Player player, int numKillers, int numFriendlies)
	{
		
	}
	
	@Override
	public void checkForEndOfGame(PlayerManager playerManager, Player playerOnPlinth, Material itemOnPlinth)
	{
		// if there's no one alive at all, game was drawn
		if (playerManager.numSurvivors() == 0 )
		{
			playerManager.gameFinished(false, false, null, null);
			return;
		}
		
		// if someone stands on the plinth with a winning item, the friendlies win
		if ( playerOnPlinth != null && itemOnPlinth != null )
		{
			playerManager.gameFinished(false, true, playerOnPlinth.getName(), itemOnPlinth);
			return;
		}
		
		// if only one person left, and they're not the killer, tell people they can /vote if they want to start a new game
		if ( playerManager.numSurvivors() == 1 && !playerManager.isKiller(playerManager.getSurvivors().get(0)) )
		{
			Player last = Killer.instance.getServer().getPlayerExact(playerManager.getSurvivors().get(0));
			if ( last != null && last.isOnline() )
			{
				Killer.instance.getServer().broadcastMessage("There's only one player left, and they can't be the killer. If you want to draw this game and start another, start a vote by typing " + ChatColor.YELLOW + "/vote");	
				return;
			}
		}
		
		// if there's no one alive that isn't a killer, the killer(s) won
		else
		{
			for ( String survivor : playerManager.getSurvivors() )
				if ( !playerManager.isKiller(survivor) )
					return;
			
			playerManager.gameFinished(true, false, null, null);
		}
	}
}

package com.ftwinston.Killer.GameModes;

import java.util.Map;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.PlayerManager;
import com.ftwinston.Killer.PlayerManager.Info;
import com.ftwinston.Killer.Settings;

import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class MysteryKiller extends GameMode
{
	public static int dontAssignKillerUntilSecondDay, autoReallocateKillers, allowMultipleKillers; 
	
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
		// in these calculations, should we consider only living killers, or ALL killers?
		int numKillersToConsider = options.get(autoReallocateKillers).isEnabled() ? numAliveKillers : numKillers;
		
		if ( !options.get(allowMultipleKillers).isEnabled() )
			return numKillersToConsider > 0 ? 0 : 1; // one killer at a time

		// 1-5 players should have 1 killer. 6-11 should have 2. 12-17 should have 3. 18-23 should have 4. 
		int targetNumKillers = numAlive / 6 + 1;
		return targetNumKillers - numKillersToConsider;
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
	public boolean immediateKillerAssignment() { return !options.get(dontAssignKillerUntilSecondDay).isEnabled(); }
	
	@Override
	public boolean revealKillersIdentityAtEnd() { return true; }
	
	@Override
	public int getNumHelpMessages(boolean forKiller) { return 9; }
	
	@Override
	public String getHelpMessage(int num, boolean forKiller, boolean isAllocationComplete)
	{
		switch ( num )
		{
			case 0:
				if ( forKiller )
					return "You have been chosen to try and kill everyone else. No one else has been told who was chosen.";
				else if ( isAllocationComplete )
					return "One player has been chosen to try and kill everyone else. No one else has been told who it is.";
				else
					return "One player will be chosen to try and kill everyone else. No one else will be told who it is.";
			case 1:
				if ( forKiller )
					return "As the killer, you win if everyone else dies.";
				else
					return "The killer wins if everyone else dies... so watch your back!";
			case 2:
				String message = "To win, the other players must bring a ";
				
				message += plugin.tidyItemName(Settings.winningItems[0]);
				
				if ( Settings.winningItems.length > 1 )
				{
					for ( int i=1; i<Settings.winningItems.length-1; i++)
						message += ", a " + plugin.tidyItemName(Settings.winningItems[i]);
					
					message += " or a " + plugin.tidyItemName(Settings.winningItems[Settings.winningItems.length-1]);
				}
				
				message += " to the plinth near the spawn.";
				return message;
			case 3:
				return "The other players will not automatically win when the killer dies, and another killer may be assigned once the first one is dead.";
			
			case 4:
				return "Death messages won't say how someone died, or who killed them.";
			
			case 5:
				if ( forKiller )
					return "If you make a compass, it will point at the nearest player. This won't work for other players.";
				else
					return "If the killer makes a compass, it will point at the nearest player. This won't work for other players.";

			case 6:
				return "Eyes of ender will help you find fortresses in the nether (to get blaze rods).\nThey can be crafted from an ender pearl and a spider eye.";
			
			case 7:
				return "Several monster eggs can be crafted by combining one of their dropped items with an iron ingot.";

			case 8:
				return "Dispensers can be crafted using a sapling instead of a bow. These work well with monster eggs.";
				
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
			"the rest. Other",
			"players aren't",
			"told who is the",
			"killer! Death",
			"messages are a",
			"bit more vague.",
			"The others must",
			"get a blaze rod",
			"and bring it to",
			"the spawn point"
		};
	}

	@Override
	public void playerJoined(Player player, PlayerManager pm, boolean isNewPlayer, PlayerManager.Info info)
	{
		if ( info.isKiller() ) // inform them that they're still a killer
			player.sendMessage("Welcome back. " + ChatColor.RED + "You are still " + (pm.numKillersAssigned() > 1 ? "a" : "the" ) + " killer!"); 
		else if ( isNewPlayer || !info.isAlive() ) // this is a new player, tell them the rules & state of the game
			player.sendMessage("Welcome to Killer Minecraft!");
		else
			player.sendMessage("Welcome back. You are not the killer, and you're still alive.");
	}
	
	@Override
	public void prepareKiller(Player player, PlayerManager pm, boolean isNewPlayer)
	{
		if ( !isNewPlayer )
			return; // don't let the killer rejoin to get more items
					
		PlayerInventory inv = player.getInventory();
		float numFriendliesPerKiller = pm.numSurvivors() / pm.numKillersAssigned();
		
		if ( numFriendliesPerKiller >= 2 )
			inv.addItem(new ItemStack(Material.STONE, 6));
		else
			return;
		
		if ( numFriendliesPerKiller >= 3 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.IRON_INGOT, 1), new ItemStack(Material.REDSTONE, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 4 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.SULPHUR, 1));
		else
			return;
		
		if ( numFriendliesPerKiller >= 5 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.IRON_INGOT, 1), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.ARROW, 3));
		else
			return;
		
		if ( numFriendliesPerKiller >= 6 )
			inv.addItem(new ItemStack(Material.MONSTER_EGG, 1, (short)50), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.SULPHUR, 1), new ItemStack(Material.ARROW, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 7 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.SULPHUR, 1), new ItemStack(Material.ARROW, 2));
			
			if ( numFriendliesPerKiller < 11 )
				inv.addItem(new ItemStack(Material.IRON_PICKAXE, 1)); // at 11 friendlies, they'll get a diamond pick instead
		}
		else
			return;
		
		if ( numFriendliesPerKiller >= 8 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.BOW, 1), new ItemStack(Material.ARROW, 3));
		else
			return;
		
		if ( numFriendliesPerKiller >= 9 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.MONSTER_EGGS, 4, (short)0), new ItemStack(Material.STONE, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 10 )
			inv.addItem(new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.MONSTER_EGG, 1, (short)50), new ItemStack(Material.ARROW, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 11 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.SULPHUR, 1));
			
			if ( numFriendliesPerKiller < 18 )
				inv.addItem(new ItemStack(Material.DIAMOND_PICKAXE, 1)); // at 18 friendlies, they get an enchanted version
		}
		else
			return;
		
		if ( numFriendliesPerKiller >= 12 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.STONE, 2), new ItemStack(Material.SULPHUR, 1));
		else
			return;
		
		if ( numFriendliesPerKiller >= 13 )
			inv.addItem(new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.MONSTER_EGGS, 2, (short)0), new ItemStack(Material.ARROW, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 14 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2), new ItemStack(Material.MONSTER_EGGS, 1, (short)0), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.STONE, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 15 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2), new ItemStack(Material.MONSTER_EGGS, 1, (short)0), new ItemStack(Material.PISTON_STICKY_BASE, 3));
		else
			return;
		
		if ( numFriendliesPerKiller >= 16 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 1), new ItemStack(Material.SULPHUR, 5));
		else
			return;
		
		if ( numFriendliesPerKiller >= 17 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 1), new ItemStack(Material.MONSTER_EGG, 1, (short)50), new ItemStack(Material.ARROW, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 18 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2));
			if ( numFriendliesPerKiller == 18 )
			{
				ItemStack stack = new ItemStack(Material.DIAMOND_PICKAXE, 1);
				stack.addEnchantment(Enchantment.DIG_SPEED, 1);
				inv.addItem(stack);
			}
		}
		else
			return;
		
		if ( numFriendliesPerKiller >= 19 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2));
			if ( numFriendliesPerKiller == 19 )
			{
				ItemStack stack = new ItemStack(Material.DIAMOND_PICKAXE, 1);
				stack.addEnchantment(Enchantment.DIG_SPEED, 2);
				inv.addItem(stack);
			}
		}
		else
			return;
		
		if ( numFriendliesPerKiller >= 20 )
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
						plugin.broadcastMessage("There's only one player left, and they're not the killer. If you want to draw this game and start another, start a vote by typing " + ChatColor.YELLOW + "/vote");	
						return;
					}
				}
	}
}

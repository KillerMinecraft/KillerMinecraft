package com.ftwinston.Killer.GameModes;

import java.util.List;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.Settings;
import com.ftwinston.Killer.WorldManager;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Material;

public class MysteryKiller extends GameMode
{
	public static final int dontAssignKillerUntilSecondDay = 0, autoReallocateKillers = 1, allowMultipleKillers = 2;
	
	@Override
	public String getName() { return "Mystery Killer"; }

	@Override
	public int getMinPlayers() { return 3; }
	
	@Override
	public Option[] setupOptions()
	{
		Option[] options =
		{
			new Option("Don't assign killer until the second day", true),
			new Option("Allocate new killer if old ones die", true),
			new Option("Assign multiple killers if lots of people play", false)
		};
		
		return options;
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
	public String describeTeam(int team, boolean plural)
	{
		if ( team == 1 )
			return plural ? "killers" : "killer";
		else
			return plural ? "friendly players" : "friendly player";
	}
	
	@Override
	public String getHelpMessage(int num, int team)
	{
		switch ( num )
		{
			case 0:
				if ( team == 1 )
				{
					if ( options.get(allowMultipleKillers).isEnabled() )
						return "You have been chosen to try and kill everyone else.\nIf there are more than 5 players in the game, multiple players will have been chosen.\nNo one else has been told who was chosen.";
					else
						return "You have been chosen to try and kill everyone else.\nNo one else has been told who was chosen.";
				}
				else if ( countPlayersOnTeam(1, false) > 0 )
				{
					if ( options.get(allowMultipleKillers).isEnabled() )
						return "(At least) one player has been chosen to try and kill everyone else.\nIf there are more than 5 players in the game, multiple players will be chosen.\nNo one else has been told who they are.";
					else
						return "One player has been chosen to try and kill everyone else. No one else has been told who it is.";
				}
				else
				{
					if ( options.get(dontAssignKillerUntilSecondDay).isEnabled() )
					{
						if ( options.get(allowMultipleKillers).isEnabled() )
							return "At the start of the next game day, (at least) one player will be chosen to try and kill everyone else.\nIf there are more than 5 players in the game, multiple players will be chosen.\nNo one else will be told who they are.";
						else
							return "At the start of the next game day, one player will be chosen to try and kill everyone else.\nNo one else will be told who it is.";
					}
					else
					{
						if ( options.get(allowMultipleKillers).isEnabled() )
							return "(At least) one player will shortly be chosen to try and kill everyone else.\nIf there are more than 5 players in the game, multiple players will be chosen.\nNo one else will be told who they are.";
						else
							return "One player will shortly be chosen to try and kill everyone else.\nNo one else will be told who it is.";
					}
				}
			case 1:
				if ( team == 1 )
				{
					if ( options.get(allowMultipleKillers).isEnabled() )
						return "As a killer, you win if all the friendly players die. You won't be told who the other killers are.";
					else
						return "As the killer, you win if everyone else dies.";
				}
				else
				{
					if ( options.get(allowMultipleKillers).isEnabled() )
						return "The killers win if everyone else dies... so watch your back!";
					else
						return "The killer wins if everyone else dies... so watch your back!";
				}
			case 2:
				String message = "To win, the other players must bring a ";
				
				message += tidyItemName(Settings.winningItems[0]);
				
				if ( Settings.winningItems.length > 1 )
				{
					for ( int i=1; i<Settings.winningItems.length-1; i++)
						message += ", a " + tidyItemName(Settings.winningItems[i]);
					
					message += " or a " + tidyItemName(Settings.winningItems[Settings.winningItems.length-1]);
				}
				
				message += " to the plinth near the spawn.";
				return message;
			case 3:
				if ( options.get(allowMultipleKillers).isEnabled() )
				{
					if ( options.get(autoReallocateKillers).isEnabled() )
						return "The other players will not automatically win when all the killers are dead, and additional killers may be assigned once to replace dead ones.";
					else
						return "The other players will not automatically win when all the killers are dead.";
				}
				else
				{
					if ( options.get(autoReallocateKillers).isEnabled() )
						return "The other players will not automatically win when the killer dies, and another killer may be assigned once the first one is dead.";
					else
						return "The other players will not automatically win when the killer dies.";
				}
			
			case 4:
				return "Death messages won't say how someone died, or who killed them.";
			
			case 5:
				if ( team == 1 )
					return "If you make a compass, it will point at the nearest player. This won't work for other players.";
				else if ( options.get(allowMultipleKillers).isEnabled() )
					return "If one of the killers make a compass, it will point at the nearest player. This won't work for other players.";
				else
					return "If the killer makes a compass, it will point at the nearest player. This won't work for other players.";

			case 6:
				return "Eyes of ender will help you find fortresses in the nether (to get blaze rods).\nThey can be crafted from an ender pearl and a spider eye.";
			
			case 7:
				return "Several monster eggs can be crafted by combining one of their dropped items with an iron ingot.";

			case 8:
				return "Dispensers can be crafted using a sapling instead of a bow. These work well with monster eggs.";
				
			default:
				return null;
		}
	}
	
	@Override
	public boolean teamAllocationIsSecret() { return true; }
	
	@Override
	public boolean usesNether() { return true; }
	
	@Override
	public void worldGenerationComplete(World main, World nether)
	{
		generatePlinth(main);
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
	public boolean useDiscreetDeathMessages() { return true; }
	
	@Override
	public Location getSpawnLocation(Player player)
	{
		return getSafeSpawnLocationNear(WorldManager.instance.mainWorld.getSpawnLocation());
	}
	
	int allocationProcessID = -1;
	
	@Override
	public void gameStarted()
	{
		List<Player> players = getOnlinePlayers(true);
		for ( Player player : players )
			setTeam(player, 0);
		
		if ( options.get(dontAssignKillerUntilSecondDay).isEnabled() )
		{
			// check based on the time of day
			// ...
		}
		else // allocate in 30 seconds
			allocationProcessID = getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
				public void run()
				{
					allocateKiller();
					allocationProcessID = -1;
				}
			}, 600L);
	}
	
	private void allocateKiller()
	{
		// pick one player, put them on team 1
		List<Player> players = getOnlinePlayers(true);		
		Player killer = selectRandom(players);
		if ( killer == null )
		{
			broadcastMessage("Unable to find a player to allocate as the killer");
			return;
		}
		

		setTeam(killer, 1);
		
		
		// give the killer their items, teleport them a distance away
		killer.sendMessage(ChatColor.RED + "You are the killer!\n" + ChatColor.RESET + "Try to kill your allies without them working out who's after them.");
		
		
	}
	
	@Override
	public void gameFinished(int winningTeam)
	{
		if ( allocationProcessID != -1 )
		{
			getPlugin().getServer().getScheduler().cancelTask(allocationProcessID);
			allocationProcessID = -1;
		}
	}
	
	@Override
	public void playerJoinedLate(Player player, boolean isNewPlayer)
	{
		if ( isNewPlayer )
			setTeam(player, 0);
	}
	
	@Override
	public void playerKilledOrQuit(Player player)
	{
		int numFriendlies = getOnlinePlayers(0, true).size();
		if ( numFriendlies != 0 )
			return;
		
		if ( getOnlinePlayers(1, true).size() > 0 )
		{
			finishGame(1); // killers win
		}
		else
		{
			finishGame(-1); // nobody wins
		}
	}
	
	@Override
	public Location getCompassTarget(Player player)
	{
		if ( getTeam(player) == 1 )
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
				broadcastMessage(player.getName() + " brought a " + tidyItemName(material) + " to the plinth!");
				finishGame(0); // winning item brought to the plinth, friendlies win
				break;
			}
	}
/*
	@Override
	public int determineNumberOfKillersToAdd(int numAlive, int numKillers, int numAliveKillers)
	{
		// if any killers have already been assigned, and we're not meant to reallocate, don't add any more
		if ( !options.get(autoReallocateKillers).isEnabled() && numKillers > 0 )
			return 0;
		
		// if we don't allow multiple killers, only ever add 0 or 1
		if ( !options.get(allowMultipleKillers).isEnabled() )
			return numAliveKillers > 0 ? 0 : 1;

		// 1-5 players should have 1 killer. 6-11 should have 2. 12-17 should have 3. 18-23 should have 4. 
		int targetNumKillers = numAlive / 6 + 1;
		return targetNumKillers - numAliveKillers;
	}
	
	@Override
	public boolean informOfTeamAssignment(PlayerManager pm) { return pm.numPlayersOnTeam(1) == 0; }
	
	@Override
	public boolean teamAllocationIsSecret() { return true; }
	
	@Override
	public boolean immediateTeamAssignment() { return !options.get(dontAssignKillerUntilSecondDay).isEnabled(); }
	
	@Override
	public boolean revealTeamIdentityAtEnd(int team) { return team == 1; }
	
	@Override
	public void playerJoined(Player player, PlayerManager pm, boolean isNewPlayer, PlayerManager.Info info)
	{
		if ( info.getTeam() == 1 ) // inform them that they're still a killer
			player.sendMessage("Welcome back. " + ChatColor.RED + "You are still " + (pm.numPlayersOnTeam(1) > 1 ? "a" : "the" ) + " killer!"); 
		else if ( isNewPlayer || !info.isAlive() ) // this is a new player, tell them the rules & state of the game
			player.sendMessage("Welcome to Killer Minecraft!");
		else
			player.sendMessage("Welcome back. You are not the killer, and you're still alive.");
	}
	
	@Override
	public void preparePlayer(Player player, PlayerManager pm, int team, boolean isNewPlayer)
	{
		if ( team == 1 )
		{
			if ( !isNewPlayer )
				return; // don't let the killer rejoin to get more items
						
			PlayerInventory inv = player.getInventory();
			float numFriendliesPerKiller = pm.numSurvivors() / pm.numPlayersOnTeam(1);
			
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
			if ( entry.getValue().isAlive() && entry.getValue().getTeam() != 1 )
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
*/
}

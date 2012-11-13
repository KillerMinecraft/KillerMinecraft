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
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class CrazyKiller extends GameMode
{
	static final long allocationDelayTicks = 600L; // 30 seconds

	@Override
	public String getName() { return "Crazy Killer"; }
	
	@Override
	public int getMinPlayers() { return 2; }
	
	@Override
	public Option[] setupOptions()
	{
		return new Option[0]; // at present, this mode has no options
	}
	
	@Override
	public String[] getSignDescription()
	{
		return new String[] {
			"One player is",
			"chosen to kill",
			"the rest. They",
			"are the Killer.",
			
			"Dirt turns into",
			"TNT when they",
			"pick it up.",
			"",
			
			"The others must",
			"kill them, or",
			"get a blaze rod",
			"to the spawn."
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
					return "You have been chosen to be the killer, and must kill everyone else. They know who you are.";
				else if ( getOnlinePlayers(1, false).size() > 0 )
					return "A player has been chosen to be the killer, and must kill everyone else.";
			case 1:
				if ( team == 1 )
					return "Every dirt block you pick up will turn into TNT, so have fun with that.";
				else
					return "Every dirt block the killer picks up will turn into TNT, so beware.";
			case 2:
				if ( team == 1 )
					return "The other players each start with a sword, so avoid a direct fight.";
				else
					return "The killer doesn't start with a sword, but all the other players do.";
			case 3:
				if ( team == 1 )
					return "Your compass will point at the nearest player.";
				else
					return "The killer starts with a compass, which points at the nearest player.";
			case 4:
				String message = "The other players win if the killer dies, or if they bring a ";			
				message += tidyItemName(Settings.winningItems[0]);
				
				if ( Settings.winningItems.length > 1 )
				{
					for ( int i=1; i<Settings.winningItems.length-1; i++)
						message += ", a " + tidyItemName(Settings.winningItems[i]);
					
					message += " or a " + tidyItemName(Settings.winningItems[Settings.winningItems.length-1]);
				}
				
				message += " to the plinth near the spawn.";
				return message;
			case 5:
				if ( team == 1 )
					return "You can make buttons and pressure plates with the stone you started with.\nTry to avoid blowing yourself up!";
				else
					return "The killer starts with enough stone and redstone to make plenty buttons, wires and pressure plates.";
			
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
	public boolean teamAllocationIsSecret() { return false; }
	
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
	public boolean useDiscreetDeathMessages() { return false; }
	
	@Override
	public Location getSpawnLocation(Player player)
	{
		Location spawnPoint;
		if ( getTeam(player) == 1 )
		{
			// the killer starts a good distance away from the other players
			spawnPoint = randomizeLocation(WorldManager.instance.mainWorld.getSpawnLocation(), 64, 0, 64, 96, 0, 96);
		}
		else
			spawnPoint = randomizeLocation(WorldManager.instance.mainWorld.getSpawnLocation(), 0, 0, 0, 8, 0, 8);
		
		return getSafeSpawnLocationNear(spawnPoint);
	}
	
	
	@Override
	public void gameStarted()
	{
		// pick one player, put them on team 1
		final List<Player> players = getOnlinePlayers(true);
		Player killer = selectRandom(players);
		if ( killer == null )
		{
			broadcastMessage("Unable to find a player to allocate as the killer");
			return;
		}
		
		setTeam(killer, 1);
		
		// give the killer their items
		killer.sendMessage(ChatColor.RED + "You are the killer!\n" + ChatColor.RESET + "Every dirt block you pick up will turn into TNT...");
		
		PlayerInventory inv = killer.getInventory();
		inv.addItem(new ItemStack(Material.COMPASS, 1));
		inv.addItem(new ItemStack(Material.COOKED_BEEF, 10));
		
		inv.addItem(new ItemStack(Material.REDSTONE, 64));
		inv.addItem(new ItemStack(Material.STONE, 64));
		
		inv.addItem(new ItemStack(Material.TNT, 4));
		inv.addItem(new ItemStack(Material.DIRT, 16));
		
		// then setup everyone else on team 0
		for ( Player player : players )
			if ( player != killer )
			{
				setTeam(player, 0);
				player.sendMessage(ChatColor.RED + killer.getName() + " is the killer!\n" + ChatColor.RESET + "Use the /team command to chat without them seeing your messages");
		
				inv = player.getInventory();
				inv.addItem(new ItemStack(Material.IRON_SWORD, 1));
			}
		
		
		// send the message to dead players also
		for ( Player player : getOnlinePlayers(false) )
		{
			setTeam(player, 0);
			player.sendMessage(ChatColor.RED + killer.getName() + " is the killer!");
		}
	}
	
	@Override
	public void gameFinished()
	{
		
	}
	
	@Override
	public void playerJoinedLate(Player player, boolean isNewPlayer)
	{
		// give this player a sword, ensure they're on team 0, and set them on their way
		if ( isNewPlayer )
		{
			PlayerInventory inv = player.getInventory();
			inv.addItem(new ItemStack(Material.IRON_SWORD, 1));
			
			setTeam(player, 0);
		}
	}
	
	@Override
	public void playerKilledOrQuit(OfflinePlayer player)
	{
		int team = getTeam(player);
		int numSurvivorsOnTeam = getOnlinePlayers(team, true).size();
		
		if ( numSurvivorsOnTeam > 0 )
			return; // this players still has living allies, so this doesn't end the game
		
		int numSurvivorsTotal = getOnlinePlayers(true).size();
		if ( numSurvivorsTotal == 0 )
		{
			broadcastMessage("Everybody died - nobody wins!");
			finishGame(); // draw, nobody wins
		}
		else if ( team == 1 )
		{
			broadcastMessage("The killer died - the friendly players win!");
			finishGame(); // killer died, friendlies win
		}
		else
		{
			broadcastMessage("All the friendly players died - the killer wins!");
			finishGame(); // friendlies died, killer wins
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
				broadcastMessage(player.getName() + " brought a " + tidyItemName(material) + " to the plinth - the friendly players win!");
				finishGame(); // winning item brought to the plinth, friendlies win
				break;
			}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void playerPickedUpItem(PlayerPickupItemEvent event)
	{
		if ( shouldIgnoreEvent(event.getPlayer()) )
			return;
		
		if ( event.getItem().getItemStack().getType() == Material.DIRT && getTeam(event.getPlayer()) == 1 )
			event.getItem().getItemStack().setType(Material.TNT);
	}
}
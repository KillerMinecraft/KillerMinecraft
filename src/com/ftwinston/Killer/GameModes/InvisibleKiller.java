package com.ftwinston.Killer.GameModes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.PlayerManager;
import com.ftwinston.Killer.PlayerManager.Info;
import com.ftwinston.Killer.Settings;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;

public class InvisibleKiller extends GameMode
{
	public static int decloakWhenWeaponDrawn, killerDistanceMessages;
	
	@Override
	public String getName() { return "Invisible Killer"; }
	
	@Override
	public int getModeNumber() { return 2; }

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
	
	private static final double maxKillerDetectionRangeSq = 50 * 50;
	private Map<String, Boolean> inRangeLastTime = new LinkedHashMap<String, Boolean>();
	
	@Override
	public void gameStarted()
	{
		inRangeLastTime.clear();
		
		if ( !options.get(killerDistanceMessages).isEnabled() )
			return; // don't start the range message process
			
		updateRangeMessageProcessID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run()
			{
				for ( Map.Entry<String, Info> entry : plugin.playerManager.getPlayerInfo() )
					if ( entry.getValue().isAlive() )
					{
						Player looker = plugin.getServer().getPlayerExact(entry.getKey());
						if ( looker == null || !looker.isOnline() )
							continue;
						
						double bestRangeSq = maxKillerDetectionRangeSq + 1;
						if ( entry.getValue().isKiller() ) 
						{
							for ( Map.Entry<String, Info> entry2 : plugin.playerManager.getPlayerInfo() )
								if ( entry2.getValue().isAlive() && !entry2.getValue().isKiller() )
								{
									Player target = plugin.getServer().getPlayerExact(entry2.getKey());
									if ( target == null || !target.isOnline() || target.getWorld() != looker.getWorld() )
										continue;
									
									double rangeSq = target.getLocation().distanceSquared(looker.getLocation());
									if ( rangeSq < bestRangeSq )
										bestRangeSq = rangeSq;
								}
						}
						else
						{
							for ( Map.Entry<String, Info> entry2 : plugin.playerManager.getPlayerInfo() )
								if ( entry2.getValue().isAlive() && entry2.getValue().isKiller() )
								{
									Player target = plugin.getServer().getPlayerExact(entry2.getKey());
									if ( target == null || !target.isOnline() || target.getWorld() != looker.getWorld() )
										continue;
									
									double rangeSq = target.getLocation().distanceSquared(looker.getLocation());
									if ( rangeSq < bestRangeSq )
										bestRangeSq = rangeSq;
								}
						}
						
						if ( bestRangeSq < maxKillerDetectionRangeSq )
						{
							int bestRange = (int)(Math.sqrt(bestRangeSq) + 0.5); // round to nearest integer
							if ( entry.getValue().isKiller() )
								looker.sendMessage("Range to nearest player: " + bestRange + " metres");
							else
								looker.sendMessage((entry.getValue().isKiller() ? "Player " : ChatColor.RED + "Killer detected!") + " Range: " + bestRange + " metres");
							inRangeLastTime.put(looker.getName(), true);
						}
						else if ( inRangeLastTime.containsKey(looker.getName()) && inRangeLastTime.get(looker.getName()) )
						{
							if ( entry.getValue().isKiller() )
								looker.sendMessage("All players are out of range");
							else
								looker.sendMessage("No killer detected");
							inRangeLastTime.put(looker.getName(), false);
						}
					}
			}
		}, 50, 200);
	}
	
	@Override
	public void gameFinished()
	{
		if ( updateRangeMessageProcessID != -1 )
		{
			plugin.getServer().getScheduler().cancelTask(updateRangeMessageProcessID);
			updateRangeMessageProcessID = -1;
		}
		
		inRangeLastTime.clear();
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
	public int getNumHelpMessages(boolean forKiller) { return 8; }
	
	@Override
	public String getHelpMessage(int num, boolean forKiller, boolean isAllocationComplete)
	{
		switch ( num )
		{
			case 0:
				if ( forKiller )
					return "You have been chosen to be the killer, and must kill everyone else.\nYou are invisible, but they know who you are.";
				else if ( isAllocationComplete )
					return "A player has been chosen to be the killer, and must kill everyone else.\nThey are invisible!";
				else
					return "A player will soon be chosen to be the killer.\nThey will be invisible, and you'll be told who it is.";
			case 1:
				if ( forKiller )
					return "You will briefly become visible when damaged.\nYou cannot be hit while invisible, except by ranged weapons.";
				else
					return "The killer will briefly become visible when damaged.\nThey cannot be hit while invisible, except by ranged weapons.";
			case 2:
				String msg;
				if ( forKiller )
				{
					if ( options.get(decloakWhenWeaponDrawn).isEnabled() )
						msg = "You will be decloaked when wielding a sword or bow.\n";
					else
						msg = "";
					msg += "Your compass points at the nearest player.";
					
					if ( options.get(killerDistanceMessages).isEnabled() )
						msg += "\nThe other players are told how far away you are.";
				}
				else	
				{
					if ( options.get(decloakWhenWeaponDrawn).isEnabled() )
						msg = "The killer will be decloaked when wielding a sword or bow.\n";
					else
						msg = "";
					msg += "The killer's compass points at the nearest player.";
					
					if ( options.get(killerDistanceMessages).isEnabled() )
						msg += "\nThe other players are told how far away the killer is.";
				}
				return msg;
			case 3:
				return "The other players get infinity bows and splash damage potions.";
			case 4:
				String message = "To win, the other players must kill the killer, or bring a ";
			
				message += plugin.tidyItemName(Settings.winningItems[0]);
				
				if ( Settings.winningItems.length > 1 )
				{
					for ( int i=1; i<Settings.winningItems.length-1; i++)
						message += ", a " + plugin.tidyItemName(Settings.winningItems[i]);
					
					message += " or a " + plugin.tidyItemName(Settings.winningItems[Settings.winningItems.length-1]);
				}
				
				message += " to the plinth near the spawn.";
				return message;
				
			case 5:
				return "Eyes of ender will help you find fortresses in the nether (to get blaze rods).\nThey can be crafted from an ender pearl and a spider eye.";
			
			case 6:
				return "Several monster eggs can be crafted by combining one of their dropped items with an iron ingot.";

			case 7:
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
			"the rest. They",
			"are invisible!",
			"Players receive",
			"alerts when the",
			"killer is near.",
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
		// hide all killers from this player!
		for ( Map.Entry<String, Info> entry : pm.getPlayerInfo() )
			if ( entry.getValue().isKiller() && entry.getValue().isAlive() && !entry.getKey().equals(player.getName()) )
			{
				Player killer = plugin.getServer().getPlayerExact(entry.getKey());
				if ( killer != null && killer.isOnline() )
					pm.hidePlayer(player, killer);
			}
		
		if ( info.isKiller() ) // inform them that they're still a killer
			player.sendMessage("Welcome back. " + ChatColor.RED + "You are still " + (pm.numKillersAssigned() > 1 ? "a" : "the" ) + " killer, and you are invisible!");
		else if ( isNewPlayer || !info.isAlive() ) // this is a new player, tell them the rules & state of the game
			player.sendMessage("Welcome to Killer Minecraft!");
		else
			player.sendMessage("Welcome back. You are not the killer, and you're still alive.");
	}
	
	@Override
	public void prepareKiller(Player player, PlayerManager pm, boolean isNewPlayer)
	{
		pm.makePlayerInvisibleToAll(player);
		player.sendMessage("You are invisible. Other players will see a message telling them how far away you are every 10 seconds. You will see a message with the distance to the nearest player at the same time.");
		
		if ( !isNewPlayer )
			return; // don't teleport or give new items on rejoining
			
		PlayerInventory inv = player.getInventory();
		inv.addItem(new ItemStack(Material.COMPASS, 1));
		inv.addItem(new ItemStack(Material.COOKED_BEEF, 10));
		
		// teleport the killer a little bit away from the other players, to stop them being potion-stomped
		Random r = new Random();
		Location loc = player.getLocation();
		
		if ( r.nextBoolean() )
			loc.setX(loc.getX() + 16 + r.nextDouble() * 10);
		else
			loc.setX(loc.getX() - 16 - r.nextDouble() * 10);
			
		if ( r.nextBoolean() )
			loc.setZ(loc.getZ() + 16 + r.nextDouble() * 10);
		else
			loc.setZ(loc.getZ() - 16 - r.nextDouble() * 10);
		
		loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1);
		player.teleport(loc);
	}
	
	@Override
	public void prepareFriendly(Player player, PlayerManager pm, boolean isNewPlayer)
	{
		player.sendMessage("Use the /team command to chat without the killer seeing your messages");
	
		PlayerInventory inv = player.getInventory();
		
		if ( !isNewPlayer )
			return; // don't give items on rejoining
			
		ItemStack stack = new ItemStack(Material.BOW, 1);
		stack.addEnchantment(Enchantment.ARROW_INFINITE, 1);
		inv.addItem(stack);
		inv.addItem(new ItemStack(Material.ARROW, 1)); // you need 1 arrow for the infinity bow, iirc
		
		// give some splash potions of damage
		Potion pot = new Potion(PotionType.INSTANT_DAMAGE);
		pot.setLevel(1);
		pot.splash();
		
		// if decloakWhenWeaponDrawn is enabled, give fewer splash potions
		stack = pot.toItemStack(Math.max(2, (int)((options.get(decloakWhenWeaponDrawn).isEnabled() ? 32f : 64f) / (pm.numSurvivors() - 1))));
		inv.addItem(stack);
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
	public void playerEmptiedBucket(PlayerBucketEmptyEvent event)
	{
		// disable buckets for killer (within 5 blocks of other players)
		String killerName = event.getPlayer().getName();
		if ( !plugin.playerManager.isKiller(killerName) )
			return;
		
		Location target = event.getBlockClicked().getLocation();
		
		for ( Map.Entry<String, Info> entry : plugin.playerManager.getPlayerInfo() )
		{
			if ( entry.getKey().equals(killerName) )
				continue; // doesn't matter if it's near yourself, it obviously will be
			
			Player player = plugin.getServer().getPlayerExact(entry.getKey());
			if ( player == null || !player.isOnline() )
				continue;
			
			if ( target.getWorld() != player.getWorld() )
				continue;
			
			double distSq = player.getLocation().distanceSquared(target);			
			if ( distSq < 25 )
			{
				event.setCancelled(true);
				player.sendMessage("You can't empty a bucket within 5 blocks of another player. That would be mean!");
				return;
			}
		}
	}
	
	@Override
	public boolean playerDamaged(Player victim, Entity attacker, DamageCause cause, int amount)
	{
		if ( !plugin.playerManager.isKiller(victim.getName()) )
			return true;
		
		if ( restoreMessageProcessID != -1 )
		{// the "cooldown" must be reset
			plugin.getServer().getScheduler().cancelTask(restoreMessageProcessID);
		}
		else
		{// make them visible for a period of time
			plugin.playerManager.makePlayerVisibleToAll(victim);
			victim.sendMessage(ChatColor.RED + "You can be seen!");
		}
		
		restoreMessageProcessID = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new RestoreInvisibility(victim.getName()), 100L); // 5 seconds
		return true;
	}
	
	private int restoreMessageProcessID = -1, updateRangeMessageProcessID = -1;
	
    class RestoreInvisibility implements Runnable
    {
    	String name;
    	public RestoreInvisibility(String playerName)
		{
			name = playerName;
		}
    	
    	public void run()
    	{
			Player player = plugin.getServer().getPlayerExact(name);
			if ( player == null || !player.isOnline() )
				return; // player has reconnected, so don't kill them
			
			ItemStack heldItem = player.getItemInHand();
			if ( heldItem != null && isWeapon(heldItem.getType()) )
			{
				player.sendMessage("You will be invisible when you put your weapon away");
			}
			else
			{
				plugin.playerManager.makePlayerInvisibleToAll(player);
				player.sendMessage("You are now invisible again");
			}
			restoreMessageProcessID = -1;
    	}
    }
    
    @Override
	public void playerItemSwitch(Player player, int prevSlot, int newSlot)
    {
		if ( !options.get(decloakWhenWeaponDrawn).isEnabled() )
			return;

		if ( !plugin.playerManager.isKiller(player.getName()) )
			return;
		
		ItemStack prevItem = player.getInventory().getItem(prevSlot);
		ItemStack newItem = player.getInventory().getItem(newSlot);
		
		boolean prevIsWeapon = prevItem != null && isWeapon(prevItem.getType());
		boolean newIsWeapon = newItem != null && isWeapon(newItem.getType());
		
		if ( prevIsWeapon == newIsWeapon || restoreMessageProcessID != -1 ) // if they're already visible because of damage, change nothing
			return;
		
		if ( newIsWeapon )
		{
			plugin.playerManager.makePlayerVisibleToAll(player);
			player.sendMessage(ChatColor.RED + "You can be seen!");
		}
		else if ( !newIsWeapon )
		{
			plugin.playerManager.makePlayerInvisibleToAll(player);
			player.sendMessage("You are now invisible again");	
		}
    }

    @Override
	public void playerDroppedItem(final Player player, Item item)
    {
		if ( !options.get(decloakWhenWeaponDrawn).isEnabled() )
			return;
		
		if ( !isWeapon(item.getItemStack().getType()) )
			return;
		
		if ( !plugin.playerManager.isKiller(player.getName()) )
			return;
		
		// if they currently have nothing in their hand, assume they just dropped this weapon
		if ( player.getItemInHand() != null )
			return;
		
		if ( restoreMessageProcessID == -1&& isWeapon(player.getItemInHand().getType()) )
		{
			plugin.playerManager.makePlayerInvisibleToAll(player);
			player.sendMessage("You are now invisible again");	
		}
    }

	@Override
	public void playerPickedUpItem(PlayerPickupItemEvent event)
	{
		if ( !options.get(decloakWhenWeaponDrawn).isEnabled() )
			return;
		
		if ( !isWeapon(event.getItem().getItemStack().getType()) )
			return;
		
		if ( !plugin.playerManager.isKiller(event.getPlayer().getName()) )
			return;
		
		final Player player = event.getPlayer();
		
		// wait a bit, for the item to actually get INTO their inventory. Then make them visible, if its in their hand
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run()
			{
				ItemStack item = player.getItemInHand(); 
				if ( restoreMessageProcessID == -1 && item != null && isWeapon(item.getType()) )
				{
					plugin.playerManager.makePlayerVisibleToAll(player);
					player.sendMessage(ChatColor.RED + "You can be seen!");
				}
			}
		}, 10); // hopefully long enough for pickup
	}
	
	@Override
	public void playerInventoryClick(final Player player, InventoryClickEvent event)
	{
		if ( !options.get(decloakWhenWeaponDrawn).isEnabled() )
			return;
		
		if ( !plugin.playerManager.isKiller(player.getName()) )
			return;
		
		// rather than work out all the click crap, let's just see if it changes
		ItemStack item = player.getItemInHand();
		final boolean weaponBefore = item != null && isWeapon(item.getType());
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run()
			{
				ItemStack item = player.getItemInHand(); 
				if ( item == null )
					return;
				
				boolean weaponAfter = isWeapon(item.getType());
				if ( weaponBefore == weaponAfter || restoreMessageProcessID != -1)
					return;
				
				if ( weaponAfter )
				{
					plugin.playerManager.makePlayerVisibleToAll(player);
					player.sendMessage(ChatColor.RED + "You can be seen!");
				}
				else if ( !weaponAfter )
				{
					plugin.playerManager.makePlayerInvisibleToAll(player);
					player.sendMessage("You are now invisible again");	
				}
			}
		}, 1);
	}
	
    private boolean isWeapon(Material mat)
    {
    	return mat == Material.BOW || mat == Material.IRON_SWORD || mat == Material.STONE_SWORD || mat == Material.DIAMOND_SWORD || mat == Material.GOLD_SWORD;
    }
}

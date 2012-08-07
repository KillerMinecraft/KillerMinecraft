package com.ftwinston.Killer.GameModes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.PlayerManager;
import com.ftwinston.Killer.PlayerManager.Info;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;

public class InvisibleKiller extends GameMode
{
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
	
	private static final double maxKillerDetectionRangeSq = 50 * 50;
	private Map<String, Boolean> inRangeLastTime = new LinkedHashMap<String, Boolean>();
	
	@Override
	public void gameStarted()
	{
		inRangeLastTime.clear();
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
	public void explainGameMode(Player player, PlayerManager pm)
	{
		boolean isKiller = pm.isKiller(player.getName());
		String message = getName() + "\n";
		if ( isKiller )
			message += "You have been";
		else
			message += "One player " + (pm.numKillersAssigned() > 0 ? "has been" : "will soon be");
		
		message += " randomly chosen to be the killer, and must kill everyone else. " + (isKiller ? "You" : "They") + (pm.numKillersAssigned() > 0 ? "are" : "will be") + " invisible, but will become briefly visible when damaged. " + (isKiller ? "You" : "They") + " cannot be hit while invisible, except by ranged weapons, and also have a compass that points at the other players.\nThe other players are each given an infinity bow and splash damage potions, and their compasses will point at the killer. To win, they must kill the killer, or bring a ";
			
		message += plugin.tidyItemName(plugin.winningItems[0]);
		
		if ( plugin.winningItems.length > 1 )
		{
			for ( int i=1; i<plugin.winningItems.length-1; i++)
				message += ", a " + plugin.tidyItemName(plugin.winningItems[i]);
			
			message += " or a " + plugin.tidyItemName(plugin.winningItems[plugin.winningItems.length-1]);
		}
		
		message += " to the plinth near the spawn.";
		player.sendMessage(message);
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
		stack = pot.toItemStack(Math.max(2, 64 / (pm.numSurvivors() - 1)));
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
			
    		plugin.playerManager.makePlayerInvisibleToAll(player);
			player.sendMessage("You are now invisible again");
			restoreMessageProcessID = -1;
    	}
    }
}

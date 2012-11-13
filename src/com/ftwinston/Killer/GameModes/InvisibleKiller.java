package com.ftwinston.Killer.GameModes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.Settings;
import com.ftwinston.Killer.WorldManager;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;

public class InvisibleKiller extends GameMode
{
	public static final int decloakWhenWeaponDrawn = 0, killerDistanceMessages = 1;
	
	@Override
	public String getName() { return "Invisible Killer"; }
	
	@Override
	public int getMinPlayers() { return 2; }
	
	@Override
	public Option[] setupOptions()
	{
		Option[] options =
		{
			new Option("Tell players how far away the killer is", true),
			new Option("Killer decloaks when sword or bow is drawn", false)
		};
		
		return options;
	}
	
	@Override
	public String[] getSignDescription()
	{
		return new String[] {
			"A player is",
			"chosen to kill",
			"the rest. They",
			"are invisible!",
			
			"They become",
			"visible when",
			"damaged.",
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
					return "You have been chosen to be the killer, and must kill everyone else.\nYou are invisible, but they know who you are.";
				else if ( getOnlinePlayers(1, false).size() > 0 )
					return "A player has been chosen to be the killer, and must kill everyone else.\nThey are invisible!";
			case 1:
				if ( team == 1 )
					return "You will briefly become visible when damaged.\nYou cannot be hit while invisible, except by ranged weapons.";
				else
					return "The killer will briefly become visible when damaged.\nThey cannot be hit while invisible, except by ranged weapons.";
			case 2:
				String msg;
				if ( team == 1 )
				{
					if ( getOption(decloakWhenWeaponDrawn).isEnabled() )
						msg = "You will be decloaked when wielding a sword or bow.\n";
					else
						msg = "";
					msg += "Your compass points at the nearest player.";
					
					if ( getOption(killerDistanceMessages).isEnabled() )
						msg += "\nThe other players are told how far away you are.";
				}
				else	
				{
					if ( getOption(decloakWhenWeaponDrawn).isEnabled() )
						msg = "The killer will be decloaked when wielding a sword or bow.\n";
					else
						msg = "";
					msg += "The killer's compass points at the nearest player.";
					
					if ( getOption(killerDistanceMessages).isEnabled() )
						msg += "\nThe other players are told how far away the killer is.";
				}
				return msg;
			case 3:
				return "The other players get infinity bows and splash damage potions.";
			case 4:
				String message = "To win, the other players must kill the killer, or bring a ";
			
				message += tidyItemName(Settings.winningItems[0]);
				
				if ( Settings.winningItems.length > 1 )
				{
					for ( int i=1; i<Settings.winningItems.length-1; i++)
						message += ", a " + tidyItemName(Settings.winningItems[i]);
					
					message += " or a " + tidyItemName(Settings.winningItems[Settings.winningItems.length-1]);
				}
				
				message += " to the plinth near the spawn.";
				return message;
			
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
			// the killer starts a little bit away from the other players
			spawnPoint = randomizeLocation(WorldManager.instance.mainWorld.getSpawnLocation(), 32, 0, 32, 48, 0, 48);
		}
		else
			spawnPoint = randomizeLocation(WorldManager.instance.mainWorld.getSpawnLocation(), 0, 0, 0, 8, 0, 8);
		
		return getSafeSpawnLocationNear(spawnPoint);
	}
	
	private static final double maxKillerDetectionRangeSq = 50 * 50;
	private Map<String, Boolean> inRangeLastTime = new LinkedHashMap<String, Boolean>();
	
	@Override
	public void gameStarted()
	{	
		restoreMessageProcessID = updateRangeMessageProcessID = -1;
	
		// pick one player, put them on team 1, and teleport them away
		final List<Player> players = getOnlinePlayers(true);
		Player killer = selectRandom(players);
		if ( killer == null )
		{
			broadcastMessage("Unable to find a player to allocate as the killer");
			return;
		}
		
		setTeam(killer, 1);
		
		
		// give the killer their items
		killer.sendMessage(ChatColor.RED + "You are the killer!\n" + ChatColor.RESET + "You are invisible.");
		setPlayerVisibility(killer, false);
		
		if ( getOption(killerDistanceMessages).isEnabled() )
		{
			killer.sendMessage("Other players will see a message telling them how far away you are every 10 seconds. You will see a message with the distance to the nearest player at the same time.");
		}
			
		PlayerInventory inv = killer.getInventory();
		inv.addItem(new ItemStack(Material.COMPASS, 1));
		inv.addItem(new ItemStack(Material.COOKED_BEEF, 10));
		
		
		int numFriendlyPlayers = players.size() - 1;
		
		// then setup everyone else
		for ( Player player : players )
			if ( player != killer )
			{
				setTeam(player, 0);
				
				player.sendMessage(ChatColor.RED + killer.getName() + " is the killer!\n" + ChatColor.RESET + "Use the /team command to chat without them seeing your messages");
				
				giveFriendlyPlayerItems(player.getInventory(), numFriendlyPlayers);
			}
		
		// send the message to dead players also
		for ( Player player : getOnlinePlayers(false) )
		{
			setTeam(player, 0);
			player.sendMessage(ChatColor.RED + killer.getName() + " is the killer!");
		}
		
		// start our scheduled process
		inRangeLastTime.clear();
		
		if ( !getOption(killerDistanceMessages).isEnabled() )
			return; // don't start the range message process
			
		updateRangeMessageProcessID = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
			public void run()
			{
				for ( Player looker : getOnlinePlayers(true) )
				{
					if ( looker == null || !looker.isOnline() )
						continue;
					
					double bestRangeSq = maxKillerDetectionRangeSq + 1;
					int lookerTeam = getTeam(looker);
					int targetTeam = lookerTeam == 1 ? 0 : 1;
					
					for ( Player target : getOnlinePlayers(targetTeam, true) )
					{
						if ( target.getWorld() != looker.getWorld() )
							continue;
						
						double rangeSq = target.getLocation().distanceSquared(looker.getLocation());
						if ( rangeSq < bestRangeSq )
							bestRangeSq = rangeSq;
					}
					
					if ( bestRangeSq < maxKillerDetectionRangeSq )
					{
						int bestRange = (int)(Math.sqrt(bestRangeSq) + 0.5); // round to nearest integer
						if ( lookerTeam == 1 )
							looker.sendMessage("Range to nearest player: " + bestRange + " metres");
						else
							looker.sendMessage(ChatColor.RED + "Killer detected! Range: " + bestRange + " metres");
						inRangeLastTime.put(looker.getName(), true);
					}
					else if ( inRangeLastTime.containsKey(looker.getName()) && inRangeLastTime.get(looker.getName()) )
					{
						if ( lookerTeam == 1 )
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
		// stop our scheduled processes
		if ( updateRangeMessageProcessID != -1 )
		{
			getPlugin().getServer().getScheduler().cancelTask(updateRangeMessageProcessID);
			updateRangeMessageProcessID = -1;
		}
		
		if ( restoreMessageProcessID != -1 )
		{
			getPlugin().getServer().getScheduler().cancelTask(restoreMessageProcessID);
			restoreMessageProcessID = -1;
		}
		
		inRangeLastTime.clear();
	}
	
	private void giveFriendlyPlayerItems(PlayerInventory inv, int numFriendlyPlayers)
	{	
		ItemStack stack = new ItemStack(Material.BOW, 1);
		stack.addEnchantment(Enchantment.ARROW_INFINITE, 1);
		inv.addItem(stack);
		inv.addItem(new ItemStack(Material.ARROW, 1)); // you need 1 arrow for the infinity bow
		
		// give some splash potions of damage
		Potion pot = new Potion(PotionType.INSTANT_DAMAGE);
		pot.setLevel(1);
		pot.splash();
		
		// if decloakWhenWeaponDrawn is enabled, give fewer splash potions
		stack = pot.toItemStack(Math.max(2, (int)((getOption(decloakWhenWeaponDrawn).isEnabled() ? 32f : 64f) / (numFriendlyPlayers - 1))));
		inv.addItem(stack);
	}
	
	@Override
	public void playerJoinedLate(Player player, boolean isNewPlayer)
	{
		// hide all killers from this player!
		for ( Player killer : getOnlinePlayers(1, true) )
			if ( killer != player )
				hidePlayer(killer, player);
	
		if ( isNewPlayer )
		{
			setTeam(player, 0);
			giveFriendlyPlayerItems(player.getInventory(), getOnlinePlayers(0, false).size());
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
	public void playerEmptiedBucket(PlayerBucketEmptyEvent event)
	{
		if ( shouldIgnoreEvent(event.getPlayer()) || getTeam(event.getPlayer()) != 1 )
			return;
		
		// disable buckets for killer (within 5 blocks of other players)
		Location target = event.getBlockClicked().getLocation();
		
		for ( Player player : getOnlinePlayers(0, true) )
		{
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

	private int restoreMessageProcessID = -1, updateRangeMessageProcessID = -1;

	@EventHandler(ignoreCancelled = true)
	public void entityDamaged(EntityDamageEvent event)
	{
		if ( shouldIgnoreEvent(event.getEntity()) )
			return;
		
		Player victim = (Player)event.getEntity();
		if ( victim == null || getTeam(victim) != 1 )
			return;
		
		if ( restoreMessageProcessID != -1 )
		{// the "cooldown" must be reset
			getPlugin().getServer().getScheduler().cancelTask(restoreMessageProcessID);
		}
		else
		{// make them visible for a period of time
			setPlayerVisibility(victim, true);
			victim.sendMessage(ChatColor.RED + "You can be seen!");
		}
		
		restoreMessageProcessID = getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new RestoreInvisibility(victim.getName()), 100L); // 5 seconds
	}
	
    class RestoreInvisibility implements Runnable
    {
    	String name;
    	public RestoreInvisibility(String playerName)
		{
			name = playerName;
		}
    	
    	public void run()
    	{
			Player player = getPlugin().getServer().getPlayerExact(name);
			if ( player == null || !player.isOnline() && !isAlive(player) )
				return; // only if the player is still in the game
			
			ItemStack heldItem = player.getItemInHand();
			if ( heldItem != null && isWeapon(heldItem.getType()) )
			{
				player.sendMessage("You will be invisible when you put your weapon away");
			}
			else
			{
				setPlayerVisibility(player, false);
				player.sendMessage("You are now invisible again");
			}
			restoreMessageProcessID = -1;
    	}
    }
    
	@EventHandler(ignoreCancelled = true)
	public void playerItemSwitch(PlayerItemHeldEvent event)
    {
		if ( !getOption(decloakWhenWeaponDrawn).isEnabled() )
			return;

		if ( shouldIgnoreEvent(event.getPlayer()) || getTeam(event.getPlayer()) != 1 )
			return;
		
		Player player = event.getPlayer();
		ItemStack prevItem = player.getInventory().getItem(event.getPreviousSlot());
		ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
		
		boolean prevIsWeapon = prevItem != null && isWeapon(prevItem.getType());
		boolean newIsWeapon = newItem != null && isWeapon(newItem.getType());
		
		if ( prevIsWeapon == newIsWeapon || restoreMessageProcessID != -1 ) // if they're already visible because of damage, change nothing
			return;
		
		if ( newIsWeapon )
		{
			setPlayerVisibility(player, true);
			player.sendMessage(ChatColor.RED + "You can be seen!");
		}
		else if ( !newIsWeapon )
		{
			setPlayerVisibility(player, false);
			player.sendMessage("You are now invisible again");	
		}
    }

	@EventHandler(ignoreCancelled = true)
	public void playerDroppedItem(PlayerDropItemEvent event)
	{
		if ( shouldIgnoreEvent(event.getPlayer()) )
			return;
		
		if ( !getOption(decloakWhenWeaponDrawn).isEnabled() )
			return;
		
		Player player = event.getPlayer();
		
		if ( getTeam(player) != 1 )
			return;
		
		// if they currently have nothing in their hand, assume they just dropped this weapon
		if ( player.getItemInHand() != null )
			return;
		
		if ( restoreMessageProcessID == -1 && isWeapon(player.getItemInHand().getType()) )
		{
			setPlayerVisibility(player, false);
			player.sendMessage("You are now invisible again");	
		}
    }

	@EventHandler(ignoreCancelled = true)
	public void playerPickedUpItem(PlayerPickupItemEvent event)
	{
		if ( shouldIgnoreEvent(event.getPlayer()) || !getOption(decloakWhenWeaponDrawn).isEnabled() )
			return;
		
		if ( !isWeapon(event.getItem().getItemStack().getType()) )
			return;
		
		if ( getTeam(event.getPlayer()) != 1 )
			return;
		
		final Player player = event.getPlayer();
		
		// wait a bit, for the item to actually get INTO their inventory. Then make them visible, if its in their hand
		getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
			@Override
			public void run()
			{
				ItemStack item = player.getItemInHand(); 
				if ( restoreMessageProcessID == -1 && item != null && isWeapon(item.getType()) )
				{
					setPlayerVisibility(player, true);
					player.sendMessage(ChatColor.RED + "You can be seen!");
				}
			}
		}, 10); // hopefully long enough for pickup
	}

	@EventHandler(ignoreCancelled = true)
	public void playerInventoryClick(InventoryClickEvent event)
	{
		if ( shouldIgnoreEvent(event.getWhoClicked()) )
			return;
	
		if ( !getOption(decloakWhenWeaponDrawn).isEnabled() )
			return;
			
		final Player player = (Player)event.getWhoClicked();
    	if ( player == null )
    		return;
	
		if ( getTeam(player) != 1 )
			return;
		
		// rather than work out all the click crap, let's just see if it changes
		ItemStack item = player.getItemInHand();
		final boolean weaponBefore = item != null && isWeapon(item.getType());
		
		getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
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
					setPlayerVisibility(player, true);
					player.sendMessage(ChatColor.RED + "You can be seen!");
				}
				else if ( !weaponAfter )
				{
					setPlayerVisibility(player, false);
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

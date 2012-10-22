package com.ftwinston.Killer.GameModes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.WorldManager;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class ContractKiller extends GameMode
{
	static final long allocationDelayTicks = 600L; // 30 seconds
	
	public static final int playersStartFarApart = 0;

	@Override
	public String getName() { return "Contract Killer"; }

	@Override
	public int getMinPlayers() { return 4; }
	
	@Override
	public Option[] setupOptions()
	{
		Option[] options = {
			new Option("Players start spread out, quite far apart", false)
		};
		return options;
	}
	
	@Override
	public String[] getSignDescription()
	{
		return new String[] {
			"Each player is",
			"given a target,",
			"to kill without",
			"anyone seeing.",
			
			"You can only",
			"kill your own",
			"target, or your",
			"hunter.",
			
			"You take your",
			"victim's target",
			"when they die.",
			""
		};
	}
	
	@Override
	public String describeTeam(int team, boolean plural)
	{
		return plural ? "players" : "player";
	}
	
	@Override
	public String getHelpMessage(int num, int team)
	{
		switch ( num )
		{
			case 0:
				if ( allocationComplete )
					return "Every player has been assigned a target to kill, which they must do without being seen by anyone else.";
				else
					return "Every player will soon be assigned a target to kill, which they must do without being seen by anyone else.";
			
			case 1:
				return "Your compass points towards your victim, and if anyone sees you kill them, you will die instead of them.";
			
			case 2:
				return "Remember that someone else is hunting you! If you kill anyone other than your target or your hunter, you will die instead of them.";
			
			case 3:
				return "When you kill your target, you are assigned their target, and the game continues until only one player remains alive.";
			
			case 4:
				return "When someone attacks you, you will be told if they are allowed to kill you. If they're not, don't kill them back!";
			
			case 5:
				return "Several monster eggs can be crafted by combining one of their dropped items with an iron ingot.";

			case 6:
				return "Dispensers can be crafted using a sapling instead of a bow. These work well with monster eggs.";
			
			default:
				return null;
		}
	}
	
	@Override
	public boolean teamAllocationIsSecret() { return true; }
	
	@Override
	public boolean usesNether() { return false; }
	
	@Override
	public void worldGenerationComplete(World main, World nether)
	{
		
	}
	
	@Override
	public boolean isLocationProtected(Location l) { return false; }
	
	@Override
	public boolean isAllowedToRespawn(Player player) { return false; }
	
	@Override
	public boolean lateJoinersMustSpectate() { return false; }
	
	@Override
	public boolean useDiscreetDeathMessages() { return false; }
	
	@Override
	public Location getSpawnLocation(Player player)
	{
		Location worldSpawn = WorldManager.instance.mainWorld.getSpawnLocation();
		
		if ( !options.get(playersStartFarApart).isEnabled() )
		{
			Location spawnPoint = randomizeLocation(worldSpawn, -10, 10, 0, 0, -10, 10);
			return getSafeSpawnLocationNear(spawnPoint);
		}
		
		// ok, we're going to spawn one player in each chunk, moving in a spiral outward from the center.
		// This shape is called an Ulam spiral, and it might be a bit crazy to use this, but there you go.
		
		int x = 0, z = 0;
		int side = 0, number = 0, sideLength = 0;
		int dir = 0; // 0 = +x, 1 = +z, 2 = -x, 3 = -z
		
		int playerNumber = nextPlayerNumber ++;
		
		while ( true )
		{
			side++;
			for (int k = 0; k < sideLength; k++)
			{
				// move forward
				switch ( dir )
				{
					case 0:
						x++; break;
					case 1:
						z++; break;
					case 2:
						x--; break;
					case 3:
						z--; break;
				}
				number++;
				
				if ( number == playerNumber )
					return getSafeSpawnLocationNear(worldSpawn.add(x * 16, 0, z * 16));
			}
			
			// turn left
			if ( dir >= 3 )
				dir = 0;
			else
				dir++;
				
			if (side % 2 == 0)
				sideLength++;
		}
	}
	
	boolean allocationComplete = false;
	int nextPlayerNumber = 1;
	int allocationProcessID = -1;
	
	@Override
	public void gameStarted()
	{
		allocationComplete = false;
		nextPlayerNumber = 1; // ensure that the player placement logic starts over again
		
		// allocation doesn't happen right away, there's 30 seconds of "scrabbling" first
		allocationProcessID = getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
			public void run()
			{
				allocateTargets();
				allocationProcessID = -1;
			}
		}, allocationDelayTicks);
	}
	
	private void allocateTargets()
	{
		// give everyone a target, make them be someone else's target
		List<Player> players = getOnlinePlayers(true);
		
		if ( players.size() < getMinPlayers() )
		{
			broadcastMessage("Cannot start game: insufficient players to assign targets. A minimum of " + getMinPlayers() + " players are required.");
			return;
		}
		
		Player firstOne = players.remove(random.nextInt(players.size()));
		Player prevOne = firstOne;
		
		while ( players.size() > 0 )
		{
			
			Player current = players.remove(random.nextInt(players.size()));
			setTargetOf(prevOne, current);
			
			prevOne.sendMessage("Your target is: " +  ChatColor.YELLOW + current.getName() + ChatColor.RESET + "!");
			prevOne.getInventory().addItem(new ItemStack(Material.COMPASS, 1));
			prevOne = current;
		}
		
		setTargetOf(prevOne, firstOne);
		prevOne.sendMessage("Your target is: " +  ChatColor.YELLOW + firstOne.getName() + ChatColor.RESET + "!");
		prevOne.getInventory().addItem(new ItemStack(Material.COMPASS, 1));
		
		broadcastMessage("All players have been allocated a target to kill");
		allocationComplete = true;
	}
	
	@Override
	public void gameFinished()
	{
		allocationComplete = false;
		victimWarningTimes.clear();
		nextPlayerNumber = 1;
		
		if ( allocationProcessID != -1 )
		{
			getPlugin().getServer().getScheduler().cancelTask(allocationProcessID);
			allocationProcessID = -1;
		}
	}
	
	@Override
	public void playerJoinedLate(Player player, boolean isNewPlayer)
	{
		if ( !isNewPlayer )
		{
			Player target = getTargetOf(player);
			if ( target != null )
				player.sendMessage("Your target is: " +  ChatColor.YELLOW + target.getName() + ChatColor.RESET + "!");
			else
				player.sendMessage("You don't seem to have a target... sorry!");
			return;
		}
		
		List<Player> players = getOnlinePlayers(true);
		if ( players.size() < 2 )
			return;
		
		// pick a player to be this player's hunter. This player's victim will be the hunter's victim.
		int hunterIndex = random.nextInt(players.size()-1), i = 0;
		for ( Player hunter : players )
			if ( hunter == player )
				continue; // ignore self
			else if ( i == hunterIndex )
			{
				Player target = getTargetOf(hunter);
				setTargetOf(player, target);
				setTargetOf(hunter, player);
				
				hunter.sendMessage("Your target has changed, and is now: " +  ChatColor.YELLOW + player.getName() + ChatColor.RESET + "!");
				
				player.sendMessage("Your target is: " +  ChatColor.YELLOW + target.getName() + ChatColor.RESET + "!");
				player.getInventory().addItem(new ItemStack(Material.COMPASS, 1));
				break;
			}
			else
				i++;
	}
	
	@Override
	public void playerKilledOrQuit(OfflinePlayer player)
	{
		List<Player> survivors = getOnlinePlayers(true);
		
		if ( survivors.size() > 1 ) 
		{// find this player's hunter ... change their target to this player's target
			for ( Player survivor : survivors )
				if ( player == getTargetOf(survivor) )
				{
					Player target = getTargetOf(player);
					setTargetOf(survivor, target);
					
					survivor.sendMessage("Your target has changed, and is now: " +  ChatColor.YELLOW + target.getName() + ChatColor.RESET + "!");
					break;
				}
		}
		setTargetOf(player, null);
		
		if ( survivors.size() == 1 )
		{
			Player survivor = survivors.get(0);
			broadcastMessage(survivor, survivor.getName() + " is the last man standing, and wins the game!");
			survivor.sendMessage("You are the last man standing: you win the game!");
		}
		else if ( survivors.size() == 0 )
			broadcastMessage("All players died, nobody wins!");
		else if ( survivors.size() == 3 )
		{
			broadcastMessage("Three players remain: everyone is now a legitimate target!");
			return;
		}
		else
			return; // multiple people left in the game
		
		finishGame();
	}
	
	@Override
	public Location getCompassTarget(Player player)
	{
		Player target = getTargetOf(player);
		if ( target != null )
			return target.getLocation();
		
		return null;
	}
	
	private static final double maxObservationRangeSq = 60 * 60;
	
	@EventHandler(ignoreCancelled = true)
	public void entityDamaged(EntityDamageEvent event)
	{
		if ( shouldIgnoreEvent(event.getEntity()) )
			return;
		
		Player victim = (Player)event.getEntity();
		if ( victim == null )
			return;
		
		Player attacker = getAttacker(event);
		if ( attacker == null )
			return;
		
		Player victimTarget = getTargetOf(victim);
		Player attackerTarget = getTargetOf(attacker);

		// armour is a problem. looks like its handled in EntityHuman.b(DamageSource damagesource, int i) - can replicate the code ... technically also account for enchantments
		if ( event.getDamage() >= victim.getHealth() )
			if ( attackerTarget == victim || victimTarget == attacker )
			{// this interaction was allowed ... should still check if they were observed!
				List<Player> survivors = getOnlinePlayers(true);
				for ( Player observer : survivors )
				{
					 if ( observer == victim || observer == attacker )
						 continue;
					 
					 if ( playerCanSeeOther(observer, attacker, maxObservationRangeSq) )
					 {
						 attacker.damage(50);
						 
						 attacker.sendMessage("You were observed trying to kill " + victim.getName() + " by " + observer.getName() + ", so you've been killed instead.");
						 victim.sendMessage(attacker.getName() + " tried to kill you, but was observed doing so by " + observer.getName() + " - so " + attacker.getName() + " has been killed instead.");
						 observer.sendMessage("You observed " + attacker.getName() + " trying to kill " + victim.getName() + ", so " + attacker.getName() + " was killed instead.");
						 
						 event.setCancelled(true);
						 return;
					 }
				}
				
				if ( victimTarget == attacker && survivors.size() > 1)
					victim.sendMessage("You killed your hunter - but someone else is hunting you now!");
			}
			else
			{
				// this wasn't a valid kill target, and was a killing blow
				attacker.damage(50);
				
				attacker.sendMessage(victim.getName() + " was neither your target nor your hunter, so you've been killed for trying to kill them!");
				victim.sendMessage(attacker.getName() + " tried to kill you - they've been killed instead.");
				
				event.setCancelled(true);
				return;
			}
		else if ( attackerTarget == victim )
		{
			if ( shouldSendVictimMessage(victim.getName(), attacker.getName(), "H") )
				victim.sendMessage(attacker.getName() + " is your hunter - " + ChatColor.RED + "they can kill you!");
		}
		else if ( victimTarget == attacker )
		{
			if ( shouldSendVictimMessage(victim.getName(), attacker.getName(), "V") )
				victim.sendMessage(attacker.getName() + " is your victim - " + ChatColor.RED + "they can kill you!");
		}
		else
		{
			if ( shouldSendVictimMessage(victim.getName(), attacker.getName(), "-") )
				victim.sendMessage(attacker.getName() + " is neither your hunter nor your victim - they cannot kill you, and will die if they try!");
		}
	}
	
	private static final long victimWarningRepeatInterval = 200;
	private Map<String, Long> victimWarningTimes = new LinkedHashMap<String, Long>();
	
	private boolean shouldSendVictimMessage(String victim, String attacker, String relationship)
	{
		// if there's a value saved for this player pair/relationship, see if it was saved within the last 10 secs - if so, don't send.
		String key = victim + "|" + attacker + "|" + relationship;
		long currentTime = WorldManager.instance.mainWorld.getTime();
		
		if ( victimWarningTimes.containsKey(key) )
		{
			long warnedTime = victimWarningTimes.get(key);
			if ( currentTime - warnedTime <= victimWarningRepeatInterval )
				return false; // they were warned about this attacker IN THIS RELATIONSHIP within the last 10 secs. Don't warn again.
		}
		
		// save this off, so they don't get this same message again in the next 10 secs
		victimWarningTimes.put(key, currentTime);
		return true;
	}
}

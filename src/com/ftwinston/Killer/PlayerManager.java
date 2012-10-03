package com.ftwinston.Killer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import jline.internal.Log;

import net.minecraft.server.Packet201PlayerInfo;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class PlayerManager
{
	public static PlayerManager instance;
	private Killer plugin;
	private Random random;
	private int killerAssignProcess, helpMessageProcess, compassProcess, spectatorFollowProcess;
	
	public PlayerManager(Killer _plugin)
	{
		this.plugin = _plugin;
		instance = this;
		random = new Random();
		
		transparentBlocks.clear();
		transparentBlocks.add(new Byte((byte)Material.AIR.getId()));
		transparentBlocks.add(new Byte((byte)Material.WATER.getId()));
		transparentBlocks.add(new Byte((byte)Material.STATIONARY_WATER.getId()));
	}
	
	private void startCheckAutoAssignKiller()
	{
		if ( !plugin.autoAssignKiller )
			return;
		
		killerAssignProcess = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			long lastRun = 0;
			public void run()
			{
				long time = plugin.worldManager.mainWorld.getTime();
			
				if ( time < lastRun ) // time of day has gone backwards! Must be a new day! See if we need to add a killer
					assignKillers(null);
				
				lastRun = time;
			}
		}, 600L, 100L); // initial wait: 30s, then check every 5s (still won't try to assign unless it detects a new day starting)
	}
	
	public class Info
	{
		public Info(boolean alive) { a = alive; k = false; target = null; if ( alive ) numAlive ++; }
		
		private boolean k, a;
		public boolean isKiller() { return k; }
		
		public void setKiller(boolean b)
		{
			if ( b )
			{
				if ( !k ) // not currently a killer, being assigned
					numKillers ++;
			}
			else if ( k ) // currently a killer, being cleared
				numKillers --;
		
			k = b;
		}
		
		// am I a survivor or a spectator?
		public boolean isAlive() { return a; }
		
		public void setAlive(boolean b)
		{
			if ( b )
			{
				nextHelpMessage = 0;
				if ( !a ) // not currently alive, being assigned
					numAlive ++;
			}
			else if ( a ) // currently alive, being cleared
			{
				numAlive --;
				nextHelpMessage = -1;
			}
		
			a = b;
		}
		
		// spectator target, and also kill target in Contract Killer mode
		public String target;
		
		public int nextHelpMessage = 0;
	}
	
	private TreeMap<String, Info> playerInfo = new TreeMap<String, Info>();
	public Set<Map.Entry<String, Info>> getPlayerInfo() { return playerInfo.entrySet(); }
	
	// any changes are automatically tracked, so these values should always be right. Includes dead killers!
	private int numKillers = 0, numAlive = 0;
	
	public int numKillersAssigned() { return numKillers; }
	
	public int numSurvivors() { return numAlive;}
	
	public int numSpectators() { return playerInfo.size() - numAlive; }
	
	public int determineNumberOfKillersToAdd()
	{
		// if we don't have enough players for a game, we don't want to assign a killer
		if ( numSurvivors() < plugin.getGameMode().absMinPlayers() )
			return 0;
		
		int numAliveKillers = 0;
		for ( Map.Entry<String, Info> entry : getPlayerInfo() )
			if ( entry.getValue().isAlive() && entry.getValue().isKiller() )
				numAliveKillers ++;
		
		return plugin.getGameMode().determineNumberOfKillersToAdd(numSurvivors(), numKillersAssigned(), numAliveKillers);
	}
	
	public void reset(boolean resetInventories)
	{
		stopAssignmentCountdown();

		playerInfo.clear();
		numKillers = numAlive = 0;
		
		if ( helpMessageProcess != -1 )
		{
			plugin.getServer().getScheduler().cancelTask(helpMessageProcess);
			helpMessageProcess = -1;
		}
		
		for ( Player player : plugin.getOnlinePlayers() )
		{
			resetPlayer(player, resetInventories);
			setAlive(player, true);
		}
		
		if ( plugin.banOnDeath )
			for ( OfflinePlayer player : plugin.getServer().getBannedPlayers() )
				player.setBanned(false);
		
		if ( killerAssignProcess != -1 )
		{
			plugin.getServer().getScheduler().cancelTask(killerAssignProcess);
			killerAssignProcess = -1;
		}

		plugin.getServer().getScheduler().cancelTask(compassProcess);
		plugin.getServer().getScheduler().cancelTask(spectatorFollowProcess);
	}
	
	public void putPlayersInStagingWorld()
	{
		for ( Player player : plugin.getOnlinePlayers() )
			putPlayerInWorld(player, plugin.worldManager.stagingWorld);
		
		reset(true);
	}
	
	public void putPlayersInGameWorld()
	{
		for ( Player player : plugin.getOnlinePlayers() )
			putPlayerInWorld(player, plugin.worldManager.mainWorld);	
	}
	
	public void startGame()
	{
		reset(true);
		checkImmediateKillerAssignment();
		
		// start sending out help messages explaining the game rules
		helpMessageProcess = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run()
			{
				plugin.getGameMode().sendGameModeHelpMessage(plugin.playerManager);
			}
		}, 0, 550L); // send every 25 seconds
		
		startCheckAutoAssignKiller();
		
		if ( plugin.getGameMode().killersCompassPointsAtFriendlies() || plugin.getGameMode().friendliesCompassPointsAtKiller() || plugin.getGameMode().compassPointsAtTarget() )
			compassProcess = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
	        	public void run()
	        	{
		        	for ( Player player : plugin.getOnlinePlayers() )
		        		if ( isAlive(player.getName()) && player.getInventory().contains(Material.COMPASS) )
		        			if ( plugin.getGameMode().compassPointsAtTarget() )
		        			{
	    						String targetName = getInfo(player.getName()).target;
	    						if ( targetName != null )
	    						{
	    							Player targetPlayer = plugin.getServer().getPlayerExact(targetName);
	    							if ( targetPlayer != null && targetPlayer.isOnline() && targetPlayer.getWorld() == player.getWorld() )
	    								player.setCompassTarget(targetPlayer.getLocation());
	    						}
		        			}
		        			else if ( isKiller(player.getName()) )
		        			{
		        				if ( plugin.getGameMode().killersCompassPointsAtFriendlies() )
				        			player.setCompassTarget(getNearestPlayerTo(player, true));	
		        			}
		        			else if ( plugin.getGameMode().friendliesCompassPointsAtKiller() )
			        			player.setCompassTarget(getNearestPlayerTo(player, false));
	        	}
	        }, 20, 10);
	        			
		spectatorFollowProcess = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
        	public void run()
        	{
	        	for ( Player player : plugin.getOnlinePlayers() )
	        	{
	        		PlayerManager.Info info = getInfo(player.getName());
	        		if ( !info.isAlive() && info.target != null )
	        			checkFollowTarget(player);
	        	}
        	}
        }, 40, 40);
	}
	
	public boolean assignKillers(CommandSender sender)
	{
		int numToAdd = determineNumberOfKillersToAdd();
		if ( numToAdd > 0 )  // don't inform people of any killer being added apart from the first one, unless the config is set
			return assignKillers(numToAdd, sender);
		return false;
	}
	
	private boolean assignKillers(int numKillers, CommandSender sender)
	{
		stopAssignmentCountdown();
		boolean startOfGame = numKillersAssigned() == 0;
		boolean retval = plugin.getGameMode().assignKillers(numKillers, sender, this);
		
		if ( startOfGame )
			plugin.getGameMode().gameStarted();
		
		return retval;
	}
	
	public void colorPlayerName(Player player, ChatColor color)
	{
		String oldListName = player.getPlayerListName();
	
		player.setDisplayName(color + ChatColor.stripColor(player.getDisplayName()));
		
		// mustn't be > 16 chars, or it throws an exception
		String name = ChatColor.stripColor(player.getPlayerListName());
		if ( name.length() > 15 )
			name = name.substring(0, 15);
		player.setPlayerListName(color + name);
		
		// ensure this change occurs on the scoreboard of anyone I'm currently invisible to
		for ( Player online : plugin.getOnlinePlayers() )
			if ( !online.canSee(player) )
			{
				((CraftPlayer)online).getHandle().netServerHandler.sendPacket(new Packet201PlayerInfo(oldListName, false, 9999));
				sendForScoreboard(online, player, true);
			}
	}
	
	public void clearPlayerNameColor(Player player)
	{
		String oldListName = player.getPlayerListName();
		
		player.setDisplayName(ChatColor.stripColor(player.getDisplayName()));
		player.setPlayerListName(ChatColor.stripColor(player.getPlayerListName()));
		
		// ensure this change occurs on the scoreboard of anyone I'm currently invisible to
		for ( Player online : plugin.getOnlinePlayers() )
			if ( !online.canSee(player) )
			{
				((CraftPlayer)online).getHandle().netServerHandler.sendPacket(new Packet201PlayerInfo(oldListName, false, 9999));
				sendForScoreboard(online, player, true);
			}
	}

	public void playerJoined(Player player)
	{
		Info info = playerInfo.get(player.getName());
		boolean isNewPlayer;
		if ( info == null )
		{
			isNewPlayer = true;
			
			if ( !plugin.getGameState().usesSpectators )
				info = new Info(true);
			else if ( plugin.lateJoinersStartAsSpectator )
				info = new Info(false);
			else
			{
				info = new Info(true);
				plugin.statsManager.playerJoinedLate();
			}
			playerInfo.put(player.getName(), info);
			
			// this player is new for this game, but they might still have stuff from previous game on same world. clear them down.
			resetPlayer(player, true);
		}
		else
			isNewPlayer = false;
		
		if ( !plugin.getGameState().usesGameWorlds )
			return;

		// hide all spectators from this player
		for ( Map.Entry<String, Info> entry : getPlayerInfo() )
			if ( !entry.getValue().isAlive() && !entry.getKey().equals(player.getName()) )
			{
				Player other = plugin.getServer().getPlayerExact(entry.getKey());
				if ( other != null && other.isOnline() && plugin.isGameWorld(other.getWorld()) )
					hidePlayer(player, other);
			}

		plugin.getGameMode().playerJoined(player, this, isNewPlayer, info);
		
		if ( !info.isAlive() )
		{
			String message = isNewPlayer ? "" : "Welcome Back. ";
			message += "You are now a spectator. You can fly, but can't be seen or interact. Type " + ChatColor.YELLOW + "/spec" + ChatColor.RESET + " to list available commands.";
			
			player.sendMessage(message);
			setAlive(player, false);
			
			// send this player to everyone else's scoreboards, because they're now invisible, and won't show otherwise
			for ( Player online : plugin.getOnlinePlayers() )
				if ( online != player && !online.canSee(player) )
					sendForScoreboard(online, player, true);
		}
		else
		{
			if ( info.isAlive() )
			{
				setAlive(player, true);
				
				if ( info.isKiller() )
					plugin.getGameMode().prepareKiller(player, this, isNewPlayer);
				else
					plugin.getGameMode().prepareFriendly(player, this, isNewPlayer);
				
				if ( plugin.getGameMode().informOfKillerIdentity() )
					colorPlayerName(player, info.isKiller() ? ChatColor.RED : ChatColor.BLUE);
			}
			else
			{
				setAlive(player, false); // game mode made them a spectator for some reason
				player.sendMessage("You are now a spectator. You can fly, but can't be seen or interact. Type " + ChatColor.YELLOW + "/spec" + ChatColor.RESET + " to list available commands.");
			}
		}
		if ( isNewPlayer );
			plugin.getGameMode().sendGameModeHelpMessage(this, player);
			
		if ( numKillersAssigned() == 0 )
		{
			checkImmediateKillerAssignment();
			
			if ( plugin.restartDayWhenFirstPlayerJoins && plugin.getOnlinePlayers().size() == 1 )
				plugin.worldManager.mainWorld.setTime(0);
		}
		else
			checkPlayerCompassTarget(player);
	}
	
	int countdownProcessID = -1;
	public void checkImmediateKillerAssignment()
	{
		if ( countdownProcessID == -1 && plugin.getGameMode().immediateKillerAssignment() )
			if ( numSurvivors() >= plugin.getGameMode().absMinPlayers() )
			{
				plugin.broadcastMessage("Allocation in 30 seconds...");
				countdownProcessID = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					@Override
					public void run()
					{
						assignKillers(null);
					}
				}, 600L);
			}
			else
				plugin.broadcastMessage(ChatColor.RED + "Insufficient players to start game - " + plugin.getGameMode().getName() + " requires at least " + plugin.getGameMode().absMinPlayers() + " players");
	}
	
	public void stopAssignmentCountdown()
	{
		if ( countdownProcessID == -1 )
			return;
		
		plugin.getServer().getScheduler().cancelTask(countdownProcessID);
		countdownProcessID = -1;
	}
	
	// player either died, or disconnected and didn't rejoin in the required time
	public void playerKilled(String playerName)
	{
		Player player = plugin.getServer().getPlayerExact(playerName);
		Info info = playerInfo.get(playerName);
		
		if ( player == null || !player.isOnline() )
		{
			if ( info != null )
			{
				info.setAlive(false);
				if ( numKillersAssigned() > 0 )
				{
					if ( plugin.banOnDeath )
						player.setBanned(true);

					plugin.getGameMode().playerKilled(player, this, info);
					plugin.getGameMode().checkForEndOfGame(this, null, null);
				}
			}
			return;
		}
		
		if ( numKillersAssigned() == 0 )
		{// game hasn't started yet, just respawn them normally
			setAlive(player, true);
			return;	
		}
		
		if ( plugin.getGameMode().informOfKillerIdentity() )
			plugin.playerManager.clearPlayerNameColor(player);
		
		setAlive(player, false);

		if ( plugin.banOnDeath )
		{
			player.setBanned(true);
			player.kickPlayer("You died, and are now banned until the end of the game");
		}
		
		plugin.getGameMode().playerKilled(player, this, info);
		plugin.getGameMode().checkForEndOfGame(this, null, null);
	}
	
	public void gameFinished(boolean killerWon, boolean friendliesWon, String winningPlayerName, Material winningItem)
	{
		Log.warn("gameFinished called...");
		String message = null;
		int numFriendlies = playerInfo.size() - numKillersAssigned();
		
		if ( winningItem != null )
		{
			if ( friendliesWon )
				message = (winningPlayerName == null ? "The " + plugin.getGameMode().describePlayer(false, numFriendlies > 1) : winningPlayerName) + " brought " + (winningItem == null ? "an item" : "a " + plugin.tidyItemName(winningItem)) + " to the plinth - the " + plugin.getGameMode().describePlayer(false, numFriendlies > 1) + (numFriendlies > 1 ? "s win! " : " wins");
			else
				message = (winningPlayerName == null ? "The " + plugin.getGameMode().describePlayer(true, numKillersAssigned() > 1) : winningPlayerName) + " brought " + (winningItem == null ? "an item" : "a " + plugin.tidyItemName(winningItem)) + " to the plinth - the " + plugin.getGameMode().describePlayer(true, numKillersAssigned() > 1) + (numKillersAssigned() > 1 ? "s win! " : " wins");
		}
		else if ( numKillersAssigned() == 0 || numFriendlies == 0 ) // some mode might not assign specific killers, or have everyone as a killer. In this case, we only care about the winning player
		{
			if ( numSurvivors() == 1 )
			{
				for ( Map.Entry<String, Info> entry : getPlayerInfo() )
					if ( entry.getValue().isAlive() )
					{
						message = "Only one player left standing, " + entry.getKey() + " wins!";
						break;
					}
			}
			else if ( numSurvivors() == 0 )
				message = "No players survived, game drawn!";
			else
				return; // multiple people still alive... ? don't end the game.
		}
		else if ( killerWon )
		{
			if ( numFriendlies > 1 )
				message = "All of the " + plugin.getGameMode().describePlayer(false, true) + " have";
			else
				message = "The " + plugin.getGameMode().describePlayer(false, false) + " has";
			message += " died, the " + plugin.getGameMode().describePlayer(true, numKillersAssigned() > 1);
			
			if ( numKillersAssigned() > 1 )
			{
				message += " win!";

				if ( winningPlayerName != null )
					message += "\nWinning kill by " + winningPlayerName + ".";
			}
			else
				message += " wins!";
		}
		else if ( friendliesWon )
		{
			if ( numKillersAssigned() > 1 )
				message =  "All of the " + plugin.getGameMode().describePlayer(true, true) + " have";
			else
				message = "The " + plugin.getGameMode().describePlayer(true, false) + " has";
		
			message += " been killed, the " + plugin.getGameMode().describePlayer(false, numFriendlies > 1);

			if ( numFriendlies > 1 )
			{
				message += " win!";

				if ( winningPlayerName != null )
					message += "\nWinning kill by " + winningPlayerName + ".";
			}
			else
				message += " wins!";
		}
		else
			message = "No players survived, game drawn!";
		
		plugin.statsManager.gameFinished(plugin.getGameMode(), numSurvivors(), killerWon ? 1 : (friendliesWon ? 2 : 0), winningItem == null ? 0 : winningItem.getId());
		
		plugin.broadcastMessage(ChatColor.YELLOW + message);
		clearKillers(null);

		if ( winningItem != null || plugin.autoRecreateWorld || plugin.voteManager.isInVote() || plugin.getOnlinePlayers().size() == 0 )
		{	// plinth victory or other scenario where we don't want a vote, end the game in 10 secs
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
	
				@Override
				public void run()
				{
	    			plugin.endGame(null);					
				}
	    	}, 200L);
		}
		else
			plugin.roundFinished();
	}
	
	public void clearKillers(CommandSender sender)
	{
		String message;
		
		if ( numKillersAssigned() > 0 )
		{
			if ( plugin.getGameMode().revealKillersIdentityAtEnd() )
			{
				message = ChatColor.RED + (sender == null ? "Revealed: " : "Revealed by " + sender.getName() + ": ");
				if ( numKillersAssigned() == 1 )
				{
					for ( Map.Entry<String, Info> entry : getPlayerInfo() )
						if ( entry.getValue().isKiller() )
						{
							entry.getValue().setKiller(false);
							message += entry.getKey() + " was the killer!";
							break;
						}
				}
				else
				{
					message += "The killers were ";
					
					int i = 0;
					for ( Map.Entry<String, Info> entry : getPlayerInfo() )
						if ( entry.getValue().isKiller() )
						{
							if ( i > 0 )
								message += i == numKillersAssigned()-1 ? " and " : ", ";
							message += entry.getKey();
							i++;
						}
					
					message += "!";
				}
				plugin.broadcastMessage(message);
			}
			
			for ( Map.Entry<String, Info> entry : getPlayerInfo() )
				entry.getValue().setKiller(false);
			
			// this game was "aborted"
			if ( plugin.statsManager.isTracking )
				plugin.statsManager.gameFinished(plugin.getGameMode(), numSurvivors(), 3, 0);
		}
		else
		{
			message = "No killers are currently assigned!";
			if ( sender == null )
				plugin.broadcastMessage(message);
			else
				sender.sendMessage(message);
		}	
	}
	
	public Info getInfo(String player)
	{
		return playerInfo.get(player);
	}
	
	public boolean isSpectator(String player)
	{
		Info info = playerInfo.get(player);
		return info == null || !info.isAlive();
	}

	public boolean isAlive(String player)
	{
		Info info = playerInfo.get(player);
		return info != null && info.isAlive();
	}

	public boolean isKiller(String player)
	{
		Info info = playerInfo.get(player);
		return info != null && info.isKiller();
	}

	public void setAlive(Player player, boolean bAlive)
	{
		boolean wasAlive;
		Info info = playerInfo.get(player.getName());
		if ( info == null )
		{
			info = new Info(bAlive);
			playerInfo.put(player.getName(), info);
			wasAlive = true;
		}
		else
			wasAlive = info.isAlive();

		Inventory inv = player.getInventory(); 
		inv.clear();
		
		info.setAlive(bAlive);
		if ( bAlive )
		{
			// you shouldn't stop being able to fly in creative mode, cos you're (hopefully) only there for testing
			if ( player.getGameMode() != GameMode.CREATIVE )
			{
				player.setFlying(false);
				player.setAllowFlight(false);
			}
			makePlayerVisibleToAll(player);
			
			if ( !wasAlive )
				player.sendMessage("You are no longer a spectator.");
		}
		else
		{
			player.setAllowFlight(true);
			player.setFlying(true);
			inv.addItem(new ItemStack(plugin.teleportModeItem, 1));
			inv.addItem(new ItemStack(plugin.followModeItem, 1));
			makePlayerInvisibleToAll(player);
			
			if ( wasAlive )
				player.sendMessage("You are now a spectator. You can fly, but can't be seen or interact. Clicking has different effects depending on the selected item. Type " + ChatColor.YELLOW + "/spec" + ChatColor.RESET + " to list available commands.");
		}
	}
	
	public void hidePlayer(Player fromMe, Player hideMe)
	{
		fromMe.hidePlayer(hideMe);
		sendForScoreboard(fromMe, hideMe, true); // hiding will take them out of the scoreboard, so put them back in again
	}
	
	public void sendForScoreboard(Player viewer, Player other, boolean show)
	{
		((CraftPlayer)viewer).getHandle().netServerHandler.sendPacket(new Packet201PlayerInfo(other.getPlayerListName(), show, show ? ((CraftPlayer)other).getHandle().ping : 9999));
	}
	
	public void makePlayerInvisibleToAll(Player player)
	{
		for(Player p : plugin.getOnlinePlayers())
			if (p != player && p.canSee(player))
				hidePlayer(p, player);
	}
	
	public void makePlayerVisibleToAll(Player player)
	{
		for(Player p : plugin.getOnlinePlayers())
			if (p != player && !p.canSee(player))
				p.showPlayer(player);
	}

	public void resetPlayer(Player player, boolean resetInventory)
	{
		if ( !player.isDead() )
		{
			player.setTotalExperience(0);
			player.setHealth(player.getMaxHealth());
			player.setFoodLevel(20);
			player.setSaturation(20);
			player.setExhaustion(0);
			player.setFireTicks(0);
			
			if ( resetInventory )
			{
				PlayerInventory inv = player.getInventory();
				inv.clear();
				inv.setHelmet(null);
				inv.setChestplate(null);
				inv.setLeggings(null);
				inv.setBoots(null);
				
				player.closeInventory(); // this stops them from keeping items they had in (e.g.) a crafting table
				
				if ( isAlive(player.getName()) ) // if any starting items are configured, give them if the player is alive
					for ( Material material : plugin.startingItems )
						inv.addItem(new ItemStack(material));
			}
		}
		
		clearPlayerNameColor(player);
	}
	
	public void putPlayerInWorld(Player player, World world)
	{	
		if ( player.isDead() && player.getWorld() == world )
			return;
		
		Location spawn = world.getSpawnLocation();
		
		// can't use getHighestBlockYAt in the nether, because of the bedrock ceiling
		if ( world.getEnvironment() == Environment.NETHER )
		{
			if ( spawn.getBlock().isEmpty() )
			{	// while the block below spawn is empty, move down one
				Block b = world.getBlockAt(spawn.getBlockX(), spawn.getBlockY() - 1, spawn.getBlockZ()); 
				while ( b.isEmpty() && !b.isLiquid() )
				{
					spawn.setY(spawn.getY()-1);
					b = world.getBlockAt(spawn.getBlockX(), spawn.getBlockY() - 1, spawn.getBlockZ());
				}
			}
			else
			{	// keep moving up til we have two empty blocks
				do
				{
					do
					{
						spawn.setY(spawn.getY()+1);
					}
					while ( !spawn.getBlock().isEmpty() );
				
					// that's one empty, see if there's another (or if we're at the tom of the world)
					if ( spawn.getBlockY() >= world.getMaxHeight() || world.getBlockAt(spawn.getBlockX(), spawn.getBlockY() + 1, spawn.getBlockZ()).isEmpty() )
						break;
				} while (true);
				
				world.setSpawnLocation(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
			}
		}
		else
		{// it's ok to use getHighestBlockYAt, so we can randomize them around a bit also
			spawn.setX(spawn.getX() + random.nextDouble() * 6 - 3);
			spawn.setZ(spawn.getZ() + random.nextDouble() * 6 - 3);
			spawn.setY(world.getHighestBlockYAt(spawn) + 1);
		}
		
		player.teleport(spawn);
	}
	
    public void checkPlayerCompassTarget(Player player)
    {
    	if ( plugin.getGameMode().usesPlinth() && player.getWorld().getEnvironment() == Environment.NORMAL && !plugin.getGameMode().compassPointsAtTarget() )
		{
			if ( isKiller(player.getName()) )
			{
				if ( !plugin.getGameMode().killersCompassPointsAtFriendlies() )
					player.setCompassTarget(plugin.getPlinthLocation());
			}
			else if ( !plugin.getGameMode().friendliesCompassPointsAtKiller() )
				player.setCompassTarget(plugin.getPlinthLocation());
		}
    }

	public Location getNearestPlayerTo(Player player, boolean findFriendlies)
	{
		Location nearest = null;
		double nearestDistSq = Double.MAX_VALUE;
		World playerWorld = player.getWorld();
		for ( Player other : plugin.getOnlinePlayers() )
		{
			if ( other == player || other.getWorld() != playerWorld || !isAlive(other.getName()))
				continue;
			
			if ( findFriendlies == isKiller(other.getName()) )
				continue;
				
			double distSq = other.getLocation().distanceSquared(player.getLocation());
			if ( distSq < nearestDistSq )
			{
				nearestDistSq = distSq;
				nearest = other.getLocation();
			}
		}
		
		// if there's no player to point at, point in a random direction
		if ( nearest == null )
		{
			nearest = player.getLocation();
			nearest.setX(nearest.getX() + random.nextDouble() * 32 - 16);
			nearest.setZ(nearest.getZ() + random.nextDouble() * 32 - 16);
		}
		return nearest;
	}
	
	public String getFollowTarget(Player player)
	{
		Info info = playerInfo.get(player.getName());
		return info == null ? null : info.target;
	}
	
	public void setFollowTarget(Player player, String target)
	{
		Info info = playerInfo.get(player.getName());
		
		if ( info != null )
			info.target = target;
	}
	
	private final double maxFollowSpectateRangeSq = 40 * 40, maxAcceptableOffsetDot = 0.65, farEnoughSpectateRangeSq = 35 * 35;
	private final int maxSpectatePositionAttempts = 5, idealFollowSpectateRange = 20;
	
	public void checkFollowTarget(Player player)
	{
		Info info = playerInfo.get(player.getName());
		
		if ( info == null || info.target == null )
			return; // don't have a target, don't want to get moved to it
		
		String targetName = info.target;
		
		Player target = plugin.getServer().getPlayerExact(targetName);
		if ( !isAlive(targetName) || target == null || !target.isOnline() || !plugin.isGameWorld(target.getWorld()) )
		{
			targetName = getNearestFollowTarget(player);
			setFollowTarget(player, targetName);
			if ( targetName == null )
				return; // if there isn't a valid follow target, don't let it try to move them to it

			target = plugin.getServer().getPlayerExact(targetName);
			if ( !isAlive(targetName) || target == null || !target.isOnline() )
			{// something went wrong with the default follow target, so just clear it
				setFollowTarget(player, null);
				return;
			}
		}
		
		if ( !canSee(player,  target, maxFollowSpectateRangeSq) )
			moveToSee(player, target);
	}
	
	public boolean canSee(Player player, Player target, double maxDistanceSq)
	{
		Location specLoc = player.getEyeLocation();
		Location targetLoc = target.getEyeLocation();
		
		// check they're in the same world
		if ( specLoc.getWorld() != targetLoc.getWorld() )
			return false;
		
		// then check the distance is appropriate
		double targetDistSqr = specLoc.distanceSquared(targetLoc); 
		if ( targetDistSqr > maxDistanceSq )
			return false;
		
		// check if they're facing the right way
		Vector specDir = specLoc.getDirection().normalize();
		Vector dirToTarget = targetLoc.subtract(specLoc).toVector().normalize();
		if ( specDir.dot(dirToTarget) < maxAcceptableOffsetDot )
			return false;
		
		// then do a ray trace to see if there's anything in the way
        Iterator<Block> itr = new BlockIterator(specLoc.getWorld(), specLoc.toVector(), dirToTarget, 0, (int)Math.sqrt(targetDistSqr));
        while (itr.hasNext())
        {
            Block block = itr.next();
            if ( block != null && !block.isEmpty() )
            	return false;
        }
        
        return true;
	}
	
	public void moveToSee(Player player, Player target)
	{
		if ( target == null || !target.isOnline() )
			return;

		Location targetLoc = target.getEyeLocation();
		
		Location bestLoc = targetLoc;
		double bestDistSq = 0;
		
		// try a few times to move away in a random direction, see if we can make it up to idealFollowSpectateRange
		for ( int i=0; i<maxSpectatePositionAttempts; i++ )
		{
			// get a mostly-horizontal direction
			Vector dir = new Vector(random.nextDouble()-0.5, random.nextDouble() * 0.35 - 0.1, random.nextDouble()-0.5).normalize();
			if ( dir.getY() > 0.25 )
			{
				dir.setY(0.25);
				dir = dir.normalize();
			}
			else if ( dir.getY() < -0.1 )
			{
				dir.setY(-0.1);
				dir = dir.normalize();
			}
			
			Location pos = findSpaceForPlayer(player, targetLoc, dir, idealFollowSpectateRange, false, true);
			if ( pos == null )
				pos = targetLoc;
			
			double distSq = pos.distanceSquared(targetLoc); 
			if ( distSq > bestDistSq )
			{
				bestLoc = pos;
				bestDistSq = distSq; 
				
				if ( distSq > farEnoughSpectateRangeSq )
					break; // close enough to the max distance, just use this
			}
		}
		
		// work out the yaw
		double xDif = targetLoc.getX() - bestLoc.getX();
		double zDif = targetLoc.getZ() - bestLoc.getZ();
		
		if ( xDif == 0 )
		{
			if ( zDif >= 0 )
				bestLoc.setYaw(270);
			else
				bestLoc.setYaw(90);
		}
		else if ( xDif > 0 )
		{
			if ( zDif >= 0)
				bestLoc.setYaw(270f + (float)Math.toDegrees(Math.atan(zDif / xDif)));
			else
				bestLoc.setYaw(180f + (float)Math.toDegrees(Math.atan(xDif / -zDif)));
		}
		else
		{
			if ( zDif >= 0)
				bestLoc.setYaw((float)(Math.toDegrees(Math.atan(-xDif / zDif))));
			else
				bestLoc.setYaw(90f + (float)Math.toDegrees(Math.atan(zDif / xDif)));
		}
		
		// work out the pitch
		double horizDist = Math.sqrt(xDif * xDif + zDif * zDif);
		double yDif = targetLoc.getY() - bestLoc.getY();
		if ( horizDist == 0 )
			bestLoc.setPitch(0);
		else if ( yDif >= 0 )
			bestLoc.setPitch(-(float)Math.toDegrees(Math.atan(yDif / horizDist)));
		else
			bestLoc.setPitch((float)Math.toDegrees(Math.atan(-yDif / horizDist)));
		
		// as we're dealing in eye position thus far, reduce the Y to get the "feet position"
		bestLoc.setY(bestLoc.getY() - player.getEyeHeight());
		
		// set them as flying so they don't fall from this position, then do the teleport
		player.setFlying(true);
		player.teleport(bestLoc);
	}
	
	public String getNearestFollowTarget(Player lookFor)
	{
		double nearestDistSq = Double.MAX_VALUE;
		String nearestName = null, firstName = null;
		
		for ( Map.Entry<String, Info> entry : getPlayerInfo() )
		{
			if ( !entry.getValue().isAlive() )
				continue;
			
			Player player = plugin.getServer().getPlayerExact(entry.getKey());
			if ( player != null && player.isOnline() )
			{
				if ( firstName == null && plugin.isGameWorld(player.getWorld()) )
					firstName = player.getName();
				
				if ( lookFor.getWorld() == player.getWorld() )
				{
					double testDistSq = player.getLocation().distanceSquared(lookFor.getLocation());
					if ( testDistSq < nearestDistSq )
					{
						nearestName = player.getName();
						nearestDistSq = testDistSq;
					}
				}
			}
		}
		
		if ( nearestName != null )
			return nearestName;
		
		return firstName;
	}
	
	public String getNextFollowTarget(Player lookFor, String currentTargetName, boolean forwards)
	{
		Set<Map.Entry<String, Info>> players = forwards ? playerInfo.entrySet() : playerInfo.descendingMap().entrySet();
		boolean useNextTarget = false;
		
		for ( Map.Entry<String, Info> entry : players )
		{
			if ( !useNextTarget && entry.getKey().equals(currentTargetName) )
			{
				useNextTarget = true;
				continue;
			}
			if ( !entry.getValue().isAlive() || !useNextTarget )
				continue;
			
			Player player = plugin.getServer().getPlayerExact(entry.getKey());
			if ( player != null && player.isOnline() && plugin.isGameWorld(player.getWorld()) )
				return entry.getKey();
		}
				
		// couldn't find one, or ran off the end of the list, so start again at the beginning, and use the first valid one
		for ( Map.Entry<String, Info> entry : players )
		{
			if ( !entry.getValue().isAlive() )
				continue;
			
			Player player = plugin.getServer().getPlayerExact(entry.getKey());
			if ( player != null && player.isOnline() && plugin.isGameWorld(player.getWorld()) )
				return entry.getKey();
		}
		
		// there really just wasn't one
		return null;
	}

	private Location findSpaceForPlayer(Player player, Location targetLoc, Vector dir, int maxDist, boolean seekClosest, boolean abortOnAnySolid)
	{
		Location bestPos = null;

		Iterator<Block> itr = new BlockIterator(targetLoc.getWorld(), targetLoc.toVector(), dir, 0, maxDist);
		while (itr.hasNext())
		{
			Block block = itr.next();
			if ( block == null || block.getLocation().getBlockY() <= 0 || block.getLocation().getBlockY() >= block.getWorld().getMaxHeight() )
				break; // don't go out the world
			
			if ( !block.isEmpty() && !block.isLiquid() )
				if ( abortOnAnySolid )
					break;
				else
					continue;
			
			Block blockBelow = targetLoc.getWorld().getBlockAt(block.getLocation().getBlockX(), block.getLocation().getBlockY()-1, block.getLocation().getBlockZ());
			if ( blockBelow.isEmpty() || blockBelow.isLiquid() )
			{
				bestPos = blockBelow.getLocation().add(new Vector(0.5, player.getEyeHeight()-1, 0.5));
				if ( seekClosest )
					return bestPos;
			}
		}
		
		return bestPos;
	}
	
	
	private final int maxSpecTeleportDist = 64, maxSpecTeleportPenetrationDist = 32;
	private final HashSet<Byte> transparentBlocks = new HashSet<Byte>();
	
	// teleport forward, to get around doors, walls, etc. that spectators can't dig through
	public void doSpectatorTeleport(Player player, boolean goThroughTarget)
	{
		Location lookAtPos = player.getTargetBlock(transparentBlocks, maxSpecTeleportDist).getLocation();
		
		Vector facingDir = player.getLocation().getDirection().normalize();
		Location traceStartPos = goThroughTarget ? lookAtPos.add(facingDir) : lookAtPos;
		Vector traceDir = goThroughTarget ? facingDir : facingDir.multiply(-1.0);
	
		Location targetPos = findSpaceForPlayer(player, traceStartPos, traceDir, goThroughTarget ? maxSpecTeleportPenetrationDist : maxSpecTeleportDist, true, false);

		player.setFlying(true);
		if ( targetPos != null )
		{
			targetPos.setPitch(player.getLocation().getPitch());
			targetPos.setYaw(player.getLocation().getYaw());
			player.teleport(targetPos);
		}
		else
			player.sendMessage("No space to teleport into!");
	}
}

package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerManager
{
	public static PlayerManager instance;
	private Killer plugin;
	private Random random;
	public PlayerManager(Killer plugin)
	{
		this.plugin = plugin;
		instance = this;
		random = new Random();
		
    	if ( plugin.autoAssignKiller )
    	{
			plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
    			long lastRun = 0;
    			public void run()
    			{
    				long time = plugin.getServer().getWorlds().get(0).getTime();
    				
    				if ( time < lastRun && !hasEnoughKillers() ) // time of day has gone backwards! Must be a new day! See if we need to add a killer
						assignKiller(null, plugin.informEveryoneOfReassignedKillers || killers.size() == 0); // don't inform people of any killer being added apart from the first one, unless the config is set

					lastRun = time;
    			}
    		}, 600L, 100L); // initial wait: 30s, then check every 5s (still won't try to assign unless it detects a new day starting)
    	}
	}
	
	private List<String> alive = new ArrayList<String>();
	private List<String> killers = new ArrayList<String>();
	private List<String> spectators = new ArrayList<String>();
	
	public boolean hasKillerAssigned() { return killers.size() > 0; }
	public boolean hasEnoughKillers()
	{
		// if we don't have enough players for a game, we don't want to assign a killer
		if ( alive.size() < plugin.absMinPlayers )
			return true;
		
		// if we're not set to auto-reassign the killer once one has been assigned at all, even if they're no longer alive / connected, don't do so
		if ( !plugin.autoReassignKiller && killers.size() > 0 )
			return true;
		
		int numAliveKillers = 0;
		for ( String name : alive )
			if ( isKiller(name) )
				numAliveKillers ++;
		
		// for now, one living killer at a time is plenty. But this should be easy to extend later.
		return numAliveKillers > 0;
	}
	
	public void reset()
	{
		alive.clear();
		for ( Player player : plugin.getServer().getOnlinePlayers() )
			addAlive(player);
		
		// don't do this til after, so addAlive can remove spectator effects if needed
		spectators.clear();
		
		// inform all killers that they're not any more, just to be clear
		for ( String killerName : killers )
		{
			Player killerPlayer = Bukkit.getServer().getPlayerExact(killerName);
			if ( killerPlayer != null && killerPlayer.isOnline() )
				killerPlayer.sendMessage(ChatColor.YELLOW + "You are no longer " + (killers.size() == 1 ? "the" : "a") + " killer.");
		}
		killers.clear();
		
		if ( plugin.banOnDeath )
			for ( OfflinePlayer player : plugin.getServer().getBannedPlayers() )
				player.setBanned(false);
	}
	
	public boolean assignKiller(CommandSender sender, boolean informNonKillers)
	{
		Player[] players = getServer().getOnlinePlayers();
		if ( players.length < plugin.absMinPlayers )
		{
			if ( sender != null )
				sender.sendMessage("This game mode really doesn't work with fewer than " + plugin.absMinPlayers + " players. Seriously.");
			return false;
		}
		
		if ( informNonKillers )
		{
			String senderName = sender == null ? "" : " by " + sender.getName();
			getServer().broadcastMessage("A killer has been randomly assigned" + senderName + " - nobody but the killer knows who it is.");
		}
		
		int availablePlayers = 0;
		for ( String name : alive )
		{
			if ( isKiller(name) )
				continue;
			
			Player player = plugin.getServer().getPlayerExact(name);
			if ( player != null && player.isOnline() )
				availablePlayers ++;
		}
		
		if ( availablePlayers == 0 )
			return false;
		
		int randomIndex = random.nextInt(availablePlayers);

		int num = 0;
		for ( Player player : players )
		{
			if ( isKiller(player.getName()) || isSpectator(player.getName()) )
				continue;
		
			if ( num == randomIndex )
			{
				addKiller(player);
				String message = ChatColor.RED + "You are ";
				message += killers.size() > 1 ? "now a" : "the";
				message += " killer!";
				
				if ( !informNonKillers )
					message += ChatColor.WHITE + " No one else has been told a new killer was assigned.";
					
				player.sendMessage(message);
			}
			else if ( informNonKillers )
				player.sendMessage(ChatColor.YELLOW + "You are not the killer.");
				
			num++;
		}
		
		return true;
	}

	public void playerJoined(Player player)
	{
		for(String spec:spectators)
			if(spec != player.getName())
			{
				Player other = plugin.getServer().getPlayerExact(spec);
				if ( other != null )
					player.hidePlayer(other);
			}
		
		if ( isSpectator(player.getName()) )
			addSpectator(player);
		
		else if ( isKiller(player.getName()) ) // inform them that they're still a killer
			player.sendMessage(ChatColor.RED + "You are still " + (killers.size() > 1 ? "a" : "the" ) + " killer!"); 
		
		else if ( !isFriendly(player) && !isKiller(player) )
			if ( hasKillerAssigned() && plugin.lateJoinersStartAsSpectator )
				addSpectator(player);
			else
				addFriendly(player);
		
    	if ( plugin.restartDayWhenFirstPlayerJoins && plugin.getServer().getOnlinePlayers().length == 1 )
			plugin.getServer().getWorlds().get(0).setTime(0);
	}
	
	// player either died, or disconnected and didn't rejoin in the required time
	public void playerKilled(String playerName)
	{
		Player player = plugin.getServer().getPlayerExact(playerName);
		if ( player != null && player.isOnline() )
		{
			setAlive(player, false);
		}
		else // player disconnected ... move them to spectator in our records, in case they reconnect
		{
			if(alive.contains(playerName) )
				alive.remove(playerName);
			if(!spectators.contains(playerName))
				spectators.add(playerName);
		}
		
		if ( plugin.banOnDeath )
		{
			player.setBanned(true);
			player.kickPlayer("You died, and are now banned until the end of the game");
		}
		
		// if there's no one alive at all, game was drawn
		if (alive.size() == 0 )
			gameFinished(false, false, null);
		else
		{// check for victory ... if there's no one alive that isn't a killer, the killer(s) won
			for ( String survivor : alive )
				if ( !isKiller(survivor) )
					return;
					
			gameFinished(true, false, null);
		}
	}
	
	public boolean gameFinished(boolean killerWon, boolean friendliesWon, String winningPlayerName)
	{
		String message;
		if ( killerWon )
		{
			message = "All friendly players have been killed, the killers win!";
			if ( winningPlayerName != null )
				message += " Winning kill by " + winningPlayerName + ".";
		}
		else if ( friendliesWon )
			message = (winningPlayerName == null ? "The players" : winningPlayerName) + " brought a blaze rod to the plinth - the friendlies win!";
		else
			message = "No players survived, game drawn!";
		
		plugin.getServer().broadcastMessage(ChatColor.YELLOW + message);
		if ( plugin.autoReveal )
			revealKillers(null);

		// schedule a game restart in 10 secs
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
    			plugin.restartGame();
    		}, 200L);
	}
	
	public void revealKillers(String revealedByPlayer)
	{
		if ( hasKillerAssigned() )
		{
			String message = ChatColor.RED + "Revealed: ";
			if ( killers.size() == 1 )
				message += killers.get(0) + " was the killer!";
			else
			{
				message += "The killers were ";
				message += killers.get(0);
				
				for ( int i=1; i<killers.size(); i++ )
				{
					message += i == killers.size()-1 ? " and " : ", ";
					message += killers.get(i);
				}
				
				message += "!";
			}
			
			if ( sender != null )
				message += ChatColor.WHITE + " (revealed by " + senderName + ")"
			
			plugin.getServer().broadcastMessage(message);
		}
		else if ( sender != null )
			sender.sendMessage("No killers have been assigned, nothing to reveal!");
	}
	
	public boolean isSpectator(String player)
	{
		return spectators.contains(player);
	}
		
	public boolean isAlive(String player)
	{
		return alive.contains(player);
	}
	
	public boolean isKiller(String player)
	{
		return killers.contains(player);
	}
	
	public void setAlive(Player player, boolean alive)
	{
		if ( alive )
		{
			player.setFlying(false);
			player.setAllowFlight(false);
			player.getInventory().clear();
			makePlayerVisibleToAll(player);
			
			if(!alive.contains(player.getName()))
				alive.add(player.getName());
			if(spectators.contains(player.getName()))
				spectators.remove(player.getName());
		}
		else
		{
			player.setAllowFlight(true);
			player.getInventory().clear();
			makePlayerInvisibleToAll(player);
			
			if(alive.contains(player.getName()))
				alive.remove(player.getName());
			if(!spectators.contains(player.getName()))
				spectators.add(player.getName());
				
			player.sendMessage("You are now a spectator. You can fly, but can't be seen or interact.");
		}
	}
	
	public void addKiller(Player player)
	{
		if(!killers.contains(player.getName()))
			killers.add(player.getName());
	}
	
	public void removeKiller(Player player)
	{
		if(killers.contains(player.getName()))
			killers.remove(player.getName());
	}
	
	private void makePlayerInvisibleToAll(Player player)
	{
		for(Player p : plugin.getServer().getOnlinePlayers())
			p.hidePlayer(player);
	}
	
	private void makePlayerVisibleToAll(Player player)
	{
		for(Player p :  plugin.getServer().getOnlinePlayers())
			p.showPlayer(player);
	}
	
	public String handleSpectatorCommand(String command, String param)
	{
		if ( command.equals("add") )
		{
				Player player = plugin.getServer().getPlayer(param);
				if(player == null)
					return String.format("Player not found: " + param);
				
				setAlive(player, false);
				return "Player Added to spectators";
		}
		else if ( command.equals("remove") )
		{
				Player player = plugin.getServer().getPlayer(param);
				if(player == null)
					return String.format("Player not found: " + param);
				
				setAlive(player, true);
				return "Player removed from spectators";
		}
		else if ( command.equals("list") )
		{
				StringBuilder list = new StringBuilder();
				list.append(spectators.size() +" spectator(s)");
				if(spectators.size() > 0)
				{
					list.append(": ");
					for(String spec:spectators)
						list.append(spec + ", ");
				}
				return list.toString().substring(0,list.length()-2);
		}
		return "No command " + command + " found. Valid commands are add {player} and remove {player}";
	}
}

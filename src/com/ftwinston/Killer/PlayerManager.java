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
	}
	
	private List<String> alive = new ArrayList<String>();
	private List<String> killers = new ArrayList<String>();
	private List<String> spectators = new ArrayList<String>();
	
	public boolean hasKillerAssigned() { return killers.size() > 0; }
	public boolean hasEnoughKillers() { return killers.size() > 0; } // at the moment, one is always enough
	
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
	}
	
	public void assignKiller(CommandSender sender)
	{
		Player[] players = getServer().getOnlinePlayers();
		if ( players.length < absMinPlayers )
		{
			if ( sender != null )
				sender.sendMessage("This game mode really doesn't work with fewer than " + absMinPlayers + " players. Seriously.");
			return;
		}
		
		String senderName = sender == null ? "" : " by " + sender.getName();
		getServer().broadcastMessage("A killer has been randomly assigned" + senderName + " - nobody but the killer knows who it is.");
		
		int randomIndex = random.nextInt(players.length);
		for ( int i=0; i<players.length; i++ )
		{
			Player player = players[i];
			if ( i == randomIndex )
			{
				addKiller(player);
				player.sendMessage(ChatColor.RED + "You are the killer!");
			}
			else
				player.sendMessage(ChatColor.YELLOW + "You are not the killer.");
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
		else if ( !isFriendly(player) && !isKiller(player) )
			if ( hasKillerAssigned() && plugin.lateJoinersStartAsSpectator )
				addSpectator(player);
			else
				addFriendly(player);
		
		Player[] players = null;
    	if ( plugin.restartDayWhenFirstPlayerJoins )
    	{
    		players = plugin.getServer().getOnlinePlayers();
    		if ( players.length == 1 )
    			plugin.getServer().getWorlds().get(0).setTime(0);
    	}
    	
    	if ( plugin.autoAssignKiller )
    	{
    		if ( players == null )
    			players = plugin.getServer().getOnlinePlayers();
    		
    		if ( players.length != 1 )
    			return; // only do this when the first player joins
    		
    		plugin.autoStartProcessID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
    			long lastRun = 0;
    			public void run()
    			{
    				long time = plugin.getServer().getWorlds().get(0).getTime();    				
    				
    				if ( time < lastRun ) // time of day has gone backwards! Must be a new day!
    				{	
    					// if we already have enough killers, cancel this task
						// if we don't have enough, assign one. If we then have enough, cancel this task.
    					if ( hasEnoughKillers() || (plugin.assignKiller(null) && hasEnoughKillers()) )
    					{
    						plugin.getServer().getScheduler().cancelTask(autoStartProcessID);
        					plugin.autoStartProcessID = -1;
    					}
    					else
    						lastRun = time;
    				}
    				else
    					lastRun = time;
    			}
    		}, 600L, 100L); // initial wait: 30s, then check every 5s
    	}
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
		
		// if there's no one alive at all, game was drawn
		if (alive.size() == 0 )
			gameFinished(false, false, null);
		else
		{
			// check for victory ... if there's no one alive that isn't a killer, the killer(s) won
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
					message += (i == killers.size()-1 ? " and " : ", ");
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

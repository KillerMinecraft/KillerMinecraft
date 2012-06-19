package com.ftwinston.Killer;

/*
 * Killer Minecraft
 * a Minecraft Bukkit Mod by 
 * David "Canazza" McQuillan and
 * Andrew Winston "FTWinston" Winston Watkins AKA Winston
 * Created 18/06/2012
 */
import java.util.Random;
import java.util.Vector;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Killer extends JavaPlugin
{
	public void onEnable()
	{
		getConfig().addDefault("autoAssign", false);
		getConfig().addDefault("autoReveal", true);
		getConfig().addDefault("restartDay", true);
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		autoAssignKiller = getConfig().getBoolean("autoAssign");
		autoReveal = getConfig().getBoolean("autoReveal");
		restartDayWhenFirstPlayerJoins = getConfig().getBoolean("restartDay");
		
		deadPlayers = new Vector<String>();
		
        getServer().getPluginManager().registerEvents(eventListener, this);
	}

	public void onDisable()
	{
		//saveConfig();
		//reloadConfig();
	}
	
	private final int absMinPlayers = 2;
	private EventListener eventListener = new EventListener(this);
	public boolean autoAssignKiller, autoReveal, restartDayWhenFirstPlayerJoins;
	public Vector<String> deadPlayers;
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("killer"))
		{
			if ( sender instanceof Player )
			{
				Player player = (Player)sender;
				if ( !player.isOp() )
				{
					sender.sendMessage("Sorry, you must be an op to use this command.");
					return true;
				}
			}
			
			if ( args.length > 0 )
			{
				if ( args[0].equalsIgnoreCase("assign") )
				{
					if  ( !assignKiller(sender) )
						return true;
				}
				else if ( args[0].equalsIgnoreCase("reveal") )
				{
					revealKiller(sender);
					return true;
				}
				else if ( args[0].equalsIgnoreCase("clear") )
				{
					clearKiller(sender);					
					return true;
				}
				else if ( args[0].equalsIgnoreCase("restart") )
					restartServer();
					return true;
			}

			sender.sendMessage("Invalid command, available parameters are: assign, reveal, clear");
			return true;
		}
		return false;
	}
	
	public boolean assignKiller(CommandSender sender)
	{
		Player[] players = getServer().getOnlinePlayers();
		if ( players.length < absMinPlayers )
		{
			if ( sender != null )
				sender.sendMessage("This game mode really doesn't work with fewer than " + absMinPlayers + " players. Seriously.");
			return false;
		}
		
		Random random = new Random();
		int randomIndex = random.nextInt(players.length);
		
		for ( int i=0; i<players.length; i++ )
		{
			Player player = players[i];
			if ( i == randomIndex )
			{
				killerName = player.getName();
				player.sendMessage(ChatColor.RED + "You are the killer!");
			}
			else
				player.sendMessage(ChatColor.YELLOW + "You are not the killer.");
		}
		
		String senderName = sender == null ? "" : " by " + sender.getName();
		getServer().broadcastMessage("A killer has been randomly assigned" + senderName + " - nobody but the killer knows who it is.");
		return true;
	}
	
	public void revealKiller(CommandSender sender)
	{
		if ( hasKillerAssigned() )
		{
			String senderName = sender == null ? "automatically" : "by " + sender.getName();
			getServer().broadcastMessage(ChatColor.RED + "Revealed: " + killerName + " was the killer! " + ChatColor.WHITE + "(revealed " + senderName + ")");
			
			killerName = null;
		}
		else if ( sender != null )
			sender.sendMessage("No killer has been assigned, nothing to reveal!");
	}
	
	private void clearKiller(CommandSender sender)
	{
		if ( hasKillerAssigned() )
		{
			String senderName = sender == null ? "automatically" : "by " + sender.getName();
			getServer().broadcastMessage(ChatColor.RED + "The killer has been cleared: there is no longer a killer! " + ChatColor.WHITE + "(cleared " + senderName + ")");
			
			Player killerPlayer = Bukkit.getServer().getPlayerExact(killerName);
			if ( killerPlayer != null )
				killerPlayer.sendMessage(ChatColor.YELLOW + "You are no longer the killer.");
				
			killerName = null;
		}
		else if ( sender != null )
			sender.sendMessage("No killer has been assigned, nothing to clear!");
	}

	private String killerName = null;
	protected int autoStartProcessID;

	public boolean hasKillerAssigned()
	{
		return killerName != null;
	}

	public void cancelAutoStart()
	{
		if ( autoAssignKiller && autoStartProcessID != -1 )
    	{
    		Player[] players = getServer().getOnlinePlayers();
    		if ( players.length > 1 )
    			return; // only do this when the server is empty
    		
    		getServer().getScheduler().cancelTask(autoStartProcessID);
			autoStartProcessID = -1;
    	}
		
	}

	public void playerKilled(String name)
	{
		boolean alreadyDead = false;
		for ( int i=0; i<deadPlayers.size(); i++ )
			if ( name.equals(deadPlayers.get(i)))
			{
				alreadyDead = true;
				break;
			}
		
		if ( !alreadyDead )
		{
			deadPlayers.add(name);
			
			// currently, we're banning players instead of setting them into some "observer" mode
			Player player = Bukkit.getServer().getPlayerExact(name);
			if (player != null)
			{
				player.setBanned(true);
				player.kickPlayer("You were killed, and are banned until the end of the game");
			}
		}
		
		if ( !autoReveal )
			return;
		
		int numSurvivors = 0;
		Player[] players = getServer().getOnlinePlayers();
		if ( players.length == 0 )
		{
			restartServer();
			return;
		}
		
		for ( int i=0; i<players.length; i++ )
		{
			boolean isDead = false;
			for ( int j=0; j<deadPlayers.size(); j++ )
				if ( players[i].getName().equals(deadPlayers.get(j)) )
				{
					isDead = true;
					break;
				}
		
			if ( !isDead )
				numSurvivors ++;
		}		
		
		if ( numSurvivors < 2 )
			revealKiller(null);
	}
	
	public void restartServer()
	{
		// unban everyone
		for ( OfflinePlayer player : getServer().getBannedPlayers() )
			player.setBanned(false);
		
		// kick everyone, just in case
		Player[] players = getServer().getOnlinePlayers();
		for ( int i=0; i<players.length; i++ )
			players[i].kickPlayer("Game is restarting");
		
		// unload all worlds and delete all worlds
		List<World> worlds = getServer().getWorlds();
		String[] worldNames = new String[worlds.size()];
		World.Environment[] worldTypes = new World.Environment[worlds.size()];
		File serverFolder = getDataFolder().getAbsoluteFile().getParentFile().getParentFile();
		
		for ( int i=0; i<worldNames.length; i++ )
		{
			worldNames[i] = worlds[i].getName();
			worldTypes[i] = worlds[i].getEnvironment();
			
			try
			{
				getServer().unloadWorld(worldNames[i], false);
			}
			catch ( Exception e )
			{
				log.info("An error occurred when unloading the " + worldNames[i] + " world: " + e.getMessage());
			}
			
			try
			{
				delete(new File(serverFolder + File.separator + worldNames[i]);
			}
			catch ( Exception e )
			{
				log.info("An error occurred when deleting the " + worldNames[i] + " world: " + e.getMessage());
			}
		}
		
		// now want to create new worlds, with the same names and types as we had before
		// ... hopefully this will keep the default settings (generate structures, etc)
		for ( int i=0; i<worldNames.length; i++ )
		{
			WorldCreator wc = new WorldCreator(worldNames[i]);
			wc.environment(worldTypes[i]);
			getServer().createWorld(wc);
		}
		
		getServer().reload(); // stops and starts *PLUGINS* ... can't hurt
	}
	
	private static boolean delete(File folder)
	{
		if (folder.isDirectory())
			for (File f : folder.listFiles())
				if (!delete(f)) return false;
		return folder.delete();
	}
}
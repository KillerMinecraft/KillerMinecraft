package com.ftwinston.Killer;

/*
 * Killer Minecraft
 * a Minecraft Bukkit Mod by 
 * David "Canazza" McQuillan and
 * Andrew Winston "FTWinston" Winston Watkins AKA Winston
 * Created 18/06/2012
 */

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Killer extends JavaPlugin
{
	public void onEnable()
	{	
        instance = this;
        
		getConfig().addDefault("autoAssign", false);
		getConfig().addDefault("autoReassign", false);
		getConfig().addDefault("autoReveal", true);
		getConfig().addDefault("restartDay", true);
		getConfig().addDefault("lateJoinersStartAsSpectator", false);
		getConfig().addDefault("tweakDeathMessages", true);
		getConfig().addDefault("banOnDeath", false);
		getConfig().addDefault("recreateWorld", false);
		getConfig().addDefault("stopServer", false);
		getConfig().addDefault("informEveryoneOfReassignedKillers", false);
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		autoAssignKiller = getConfig().getBoolean("autoAssign");
		autoReassignKiller = getConfig().getBoolean("autoReassign");
		autoReveal = getConfig().getBoolean("autoReveal");
		restartDayWhenFirstPlayerJoins = getConfig().getBoolean("restartDay");
		lateJoinersStartAsSpectator = getConfig().getBoolean("lateJoinersStartAsSpectator");
		tweakDeathMessages = getConfig().getBoolean("tweakDeathMessages");
		banOnDeath = getConfig().getBoolean("banOnDeath");
		recreateWorld = getConfig().getBoolean("recreateWorld");
		stopServer = getConfig().getBoolean("stopServer");
		informEveryoneOfReassignedKillers = getConfig().getBoolean("informEveryoneOfReassignedKillers");
		
        getServer().getPluginManager().registerEvents(eventListener, this);
        playerManager = new PlayerManager(this);
        worldManager = new WorldManager(this);
        
        // create a plinth in the default world. Always done with the same offset, so if the world already has a plinth, it should just get overwritten.
        plinthPressurePlateLocation = worldManager.createPlinth(getServer().getWorlds().get(0));
	}
	
	public void onDisable()
	{
		worldManager.onDisable();
	}
	
	public static Killer instance;
	Logger log = Logger.getLogger("Minecraft");
	Location plinthPressurePlateLocation;

	private EventListener eventListener = new EventListener(this);
	private WorldManager worldManager;
	public PlayerManager playerManager;
	
	public final int absMinPlayers = 2;
	public boolean autoAssignKiller, autoReassignKiller, autoReveal, restartDayWhenFirstPlayerJoins, lateJoinersStartAsSpectator, tweakDeathMessages, banOnDeath, recreateWorld, stopServer, informEveryoneOfReassignedKillers;
	
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
					playerManager.assignKiller(sender, true);
					return true;
				}
				else if ( args[0].equalsIgnoreCase("reveal") )
				{
					playerManager.revealKillers(sender);
					return true;
				}
				else if ( args[0].equalsIgnoreCase("clear") )
				{
					if ( !playerManager.hasKillerAssigned() )
					{
						sender.sendMessage("No killer has been assigned, nothing to clear!");
						return true;
					}

					getServer().broadcastMessage(ChatColor.RED + sender.getName() +" cleared the killer - there is no longer a killer!");
					playerManager.reset();
					return true;
				}
				else if( args[0].equalsIgnoreCase("spectator"))
				{
					if(args.length > 2)
						sender.sendMessage(playerManager.handleSpectatorCommand(args[1], args[2]));
					else
						sender.sendMessage(playerManager.handleSpectatorCommand(args[1],""));
					return true;
				}
				else if ( args[0].equalsIgnoreCase("restart") )
				{
					restartGame();
					return true;
				}
			}
			
			sender.sendMessage("Invalid command, available parameters are: assign, reveal, clear, spectator, restart");
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("spec"))
		{
			if ( !(sender instanceof Player) )
				return false;
			
			if ( args.length == 0 )
			{
				sender.sendMessage("Usage: /spec main, /spec nether, or /spec <player name>");
				return true;
			}
			
			if ( !playerManager.isSpectator(sender.getName()) )
			{
				sender.sendMessage("Only spectators can use this command");
				return true;
			}
			
			Player player = (Player)sender;
			if ( args[0].equalsIgnoreCase("main") )
			{
				playerManager.putPlayerInWorld(player, getServer().getWorlds().get(0), true);
			}
			else if ( args[0].equalsIgnoreCase("nether") )
			{
				if ( getServer().getWorlds().size() > 1 )
					playerManager.putPlayerInWorld(player, getServer().getWorlds().get(1), true);
				else
					sender.sendMessage("Nether world not found, please try again");
			}
			else
			{
				Player other = getServer().getPlayer(args[0]);
				if ( other == null || !other.isOnline() )
				{
					sender.sendMessage("Player not found: " + args[0]);
					return true;
				}
				
				player.teleport(other.getLocation());
			}
			
			return true;
		}
		return false;
	}

	public void restartGame()
	{
		if ( recreateWorld )
		{
			getServer().broadcastMessage("Game is restarting, please wait while the world is deleted and a new one is prepared...");
			playerManager.reset();
			worldManager.deleteWorlds(new Runnable() {
				public void run()
				{
					World defaultWorld = getServer().getWorlds().get(0);
					plinthPressurePlateLocation = worldManager.createPlinth(defaultWorld);
					playerManager.reset();
				}
			});
		}
		else if ( stopServer )
		{
			getServer().shutdown();
		}
		else
		{
			// what should we do to the world on restart if we're not deleting it?
			// remove all user-placed portal, obsidian, chests, dispensers and furnaces? We'd have to track them being placed.
			getServer().broadcastMessage("Game is restarting, but Killer has been set not to automatically delete the world when restarting.");

			boolean first = true; // only check the spawn point is valid the first time 
			World defaultWorld = getServer().getWorlds().get(0);
			for ( Player player : getServer().getOnlinePlayers() )
			{
				playerManager.resetPlayer(player);
				playerManager.putPlayerInWorld(player, defaultWorld, first);
				first = false;
			}
			
			playerManager.reset();
		}
	}
}
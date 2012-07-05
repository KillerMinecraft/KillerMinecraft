package com.ftwinston.Killer;

/*
 * Killer Minecraft
 * a Minecraft Bukkit Mod by 
 * David "Canazza" McQuillan and
 * Andrew Winston "FTWinston" Winston Watkins AKA Winston
 * Created 18/06/2012
 */

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Killer extends JavaPlugin
{
	public static Killer instance;
	Logger log = Logger.getLogger("Minecraft");
	Location plinthPressurePlateLocation;

	private EventListener eventListener = new EventListener(this);
	private WorldManager worldManager;
	public PlayerManager playerManager;
	public VoteManager voteManager;
	
	public final int absMinPlayers = 2;
	public boolean autoAssignKiller, autoReassignKiller, autoReveal, restartDayWhenFirstPlayerJoins, lateJoinersStartAsSpectator, tweakDeathMessages, banOnDeath, informEveryoneOfReassignedKillers, autoRecreateWorld, recreateWorldWithoutStoppingServer;
	public Material[] winningItems;
	
	private int compassProcessID, spectatorFollowProcessID;
	
	public void onEnable()
	{	
        instance = this;
        
        setupConfiguration();
		
        getServer().getPluginManager().registerEvents(eventListener, this);
        playerManager = new PlayerManager(this);
        worldManager = new WorldManager(this);
        voteManager = new VoteManager(this);
        
        // create a plinth in the default world. Always done with the same offset, so if the world already has a plinth, it should just get overwritten.
        plinthPressurePlateLocation = worldManager.createPlinth(getServer().getWorlds().get(0));
        
        // set up a task to mess with killers' compasses
        compassProcessID = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
        	public void run()
        	{
	        	for ( Player player : instance.getServer().getOnlinePlayers() )
	        		if ( playerManager.isKiller(player.getName()) && playerManager.isAlive(player.getName()) && player.getInventory().contains(Material.COMPASS) )
	        			player.setCompassTarget(playerManager.getNearestPlayerTo(player));
        	}
        }, 20, 10);
	        			
		spectatorFollowProcessID = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
        	public void run()
        	{
	        	for ( Player player : instance.getServer().getOnlinePlayers() )
	        	{
	        		String target = playerManager.getFollowTarget(player);
	        		if ( target == null )	        			
	        			continue;
	        		
        			if ( !playerManager.canSeeFollowTarget(player) )
        				playerManager.moveToSeeFollowTarget(player);
	        	}
        	}
        }, 40, 40);
	}
	
	public void onDisable()
	{
		worldManager.onDisable();
		getServer().getScheduler().cancelTask(compassProcessID);
		getServer().getScheduler().cancelTask(spectatorFollowProcessID);
	}
	
	private void setupConfiguration()
	{
		getConfig().addDefault("autoAssign", false);
		getConfig().addDefault("autoReassign", false);
		getConfig().addDefault("autoReveal", true);
		getConfig().addDefault("restartDay", true);
		getConfig().addDefault("lateJoinersStartAsSpectator", false);
		getConfig().addDefault("tweakDeathMessages", true);
		getConfig().addDefault("banOnDeath", false);
		getConfig().addDefault("informEveryoneOfReassignedKillers", true);
		getConfig().addDefault("winningItems", Arrays.asList(Material.BLAZE_ROD.getId(), Material.GHAST_TEAR.getId()));		
		
		getConfig().addDefault("autoRecreateWorld", false);
		getConfig().addDefault("recreateWorldWithoutStoppingServer", true);
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		autoAssignKiller = getConfig().getBoolean("autoAssign");
		autoReassignKiller = getConfig().getBoolean("autoReassign");
		autoReveal = getConfig().getBoolean("autoReveal");
		restartDayWhenFirstPlayerJoins = getConfig().getBoolean("restartDay");
		lateJoinersStartAsSpectator = getConfig().getBoolean("lateJoinersStartAsSpectator");
		tweakDeathMessages = getConfig().getBoolean("tweakDeathMessages");
		banOnDeath = getConfig().getBoolean("banOnDeath");
		informEveryoneOfReassignedKillers = getConfig().getBoolean("informEveryoneOfReassignedKillers");
		autoRecreateWorld = getConfig().getBoolean("autoRecreateWorld");
		recreateWorldWithoutStoppingServer = getConfig().getBoolean("recreateWorldWithoutStoppingServer");

		List<Integer> winningItemIDs = getConfig().getIntegerList("winningItems"); 
		winningItems = new Material[winningItemIDs.size()];
		for ( int i=0; i<winningItems.length; i++ )
		{
			Material mat = Material.getMaterial(winningItemIDs.get(i));
			if ( mat == null )
			{
				mat = Material.BLAZE_ROD;
				log.warning("Material ID " + winningItemIDs.get(i) + " not recognized.");
			} 
			winningItems[i] = mat;
		}
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("spec"))
		{
			if ( !(sender instanceof Player) )
				return false;
			
			if ( args.length == 0 )
			{
				sender.sendMessage("Usage: /spec main, /spec nether, /spec <player name>, or /spec follow");
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
			else if ( args[0].equalsIgnoreCase("follow") )
			{
				if ( playerManager.getFollowTarget(player) == null )
				{
					playerManager.setFollowTarget(player, playerManager.getDefaultFollowTarget());
					if ( !playerManager.canSeeFollowTarget(player) )
						playerManager.moveToSeeFollowTarget(player);
				}
				else
					playerManager.setFollowTarget(player, null);
			}
			else
			{
				Player other = getServer().getPlayer(args[0]);
				if ( other == null || !other.isOnline() )
					sender.sendMessage("Player not found: " + args[0]);
				else if ( playerManager.getFollowTarget(player) != null )
					playerManager.setFollowTarget(player, other.getName());
				else
					player.teleport(other.getLocation());
			}
			
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("vote"))
		{
			if ( sender instanceof Player )
				voteManager.showVoteMenu((Player)sender);
			return true;
		}
		
		return false;
	}

	public void restartGame(boolean useSameWorld, boolean resetItems)
	{
		if ( useSameWorld )
		{
			// what should we do to the world on restart if we're not deleting it?
			// remove all user-placed portal, obsidian, chests, dispensers and furnaces? We'd have to track them being placed.
			
			getServer().broadcastMessage("Game is restarting, using the same world...");

			boolean first = true; // only check the spawn point is valid the first time 
			World defaultWorld = getServer().getWorlds().get(0);
			for ( Player player : getServer().getOnlinePlayers() )
			{
				playerManager.putPlayerInWorld(player, defaultWorld, first);
				first = false;
			}
			
			playerManager.reset(resetItems);
		}
		else if ( recreateWorldWithoutStoppingServer )
		{
			getServer().broadcastMessage("Game is restarting, please wait while the world is deleted and a new one is prepared...");
			playerManager.reset(resetItems);
			worldManager.deleteWorlds(new Runnable() {
				public void run()
				{
					World defaultWorld = getServer().getWorlds().get(0);
					plinthPressurePlateLocation = worldManager.createPlinth(defaultWorld);
					playerManager.reset(false);
				}
			});
		}
		else
		{
			getServer().shutdown();
		}
	}

	public String tidyItemName(Material m)
	{
		return m.name().toLowerCase().replace('_', ' ');
	}
}
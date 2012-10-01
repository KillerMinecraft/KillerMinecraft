package com.ftwinston.Killer;

/*
 * Killer Minecraft
 * a Minecraft Bukkit Mod by 
 * David "Canazza" McQuillan and
 * Andrew Winston "FTWinston" Winston Watkins AKA Winston
 * Created 18/06/2012
 */

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.minecraft.server.MinecraftServer;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.LazyPlayerSet;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class Killer extends JavaPlugin
{
	public static Killer instance;
	public Logger log = Logger.getLogger("Minecraft");
	Location plinthPressurePlateLocation;

	public boolean stagingWorldIsServerDefault;
	public String stagingWorldName;
	
	private EventListener eventListener = new EventListener(this);
	public WorldManager worldManager;
	public PlayerManager playerManager;
	public VoteManager voteManager;
	public StatsManager statsManager;
	
	public boolean canChangeGameMode, autoAssignKiller, autoReassignKiller, restartDayWhenFirstPlayerJoins, lateJoinersStartAsSpectator, banOnDeath, informEveryoneOfReassignedKillers, autoRecreateWorld, reportStats;
	public boolean autoRestartAtEndOfGame, voteRestartAtEndOfGame;
	
	public Material[] winningItems, startingItems;
	
	private int compassProcessID, spectatorFollowProcessID;
	private boolean restarting;
	
	public Material teleportModeItem = Material.WATCH, followModeItem = Material.ARROW;
	
	private GameMode gameMode, nextGameMode;
	public GameMode getGameMode() { return gameMode; }
	public GameMode getNextGameMode() { return nextGameMode; }
	public void setNextGameMode(GameMode g, CommandSender changedBy)
	{
		nextGameMode = g;
		if ( changedBy == null )
			broadcastMessage("The next game mode will be " + g.getName());
		else
			broadcastMessage(changedBy.getName() + " set the next game mode to " + g.getName());
	}
	
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
	{
		return new StagingWorldGenerator();
	}
	
	boolean firstStart = true;
	public void onEnable()
	{
        instance = this;
        restarting = false;
        GameMode.setupGameModes(this);
        setupConfiguration();
		
		if ( firstStart )
		{
			firstStart = false;
			if ( getConfig().getBoolean("startDisabled") )
			{
				getServer().getPluginManager().disablePlugin(this);
				log.info("Killer's startDisabled config setting is set to true, so the plugin will now disabling itself.");
				return;
			}
		}
		
		createRecipes();
		
		//playerManager = new PlayerManager(this);
        worldManager = new WorldManager(this, getConfig().getString("killerWorldName"));
        voteManager = new VoteManager(this);
        statsManager = new StatsManager(this);
        getServer().getPluginManager().registerEvents(eventListener, this);

		stagingWorldName = getConfig().getString("stagingWorldName");
		MinecraftServer ms = getMinecraftServer();
		if ( ms != null && ms.getPropertyManager().getString("level-name", "world").equalsIgnoreCase(stagingWorldName) )
		{
			worldManager.hijackDefaultWorldAsStagingWorld(stagingWorldName); // Killer's staging world is the server's default, so hijack how it's going to be configured
			stagingWorldIsServerDefault = true;
		}
		else
		{
			worldManager.createStagingWorld(stagingWorldName); // staging world isn't server default, so create it as a new world
			stagingWorldIsServerDefault = false;
		}

        // disable spawn protection
        getServer().setSpawnRadius(0);
        
        // set up a task to mess with compasses, to point at other players as appropriate
        compassProcessID = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
        	public void run()
        	{
	        	for ( Player player : instance.getOnlinePlayers() )
	        		if ( playerManager.isAlive(player.getName()) && player.getInventory().contains(Material.COMPASS) )
	        			if ( getGameMode().compassPointsAtTarget() )
	        			{
    						String targetName = playerManager.getInfo(player.getName()).target;
    						if ( targetName != null )
    						{
    							Player targetPlayer = getServer().getPlayerExact(targetName);
    							if ( targetPlayer != null && targetPlayer.isOnline() && targetPlayer.getWorld() == player.getWorld() )
    								player.setCompassTarget(targetPlayer.getLocation());
    						}
	        			}
	        			else if ( playerManager.isKiller(player.getName()) )
	        			{
	        				if ( getGameMode().killersCompassPointsAtFriendlies() )
			        			player.setCompassTarget(playerManager.getNearestPlayerTo(player, true));	
	        			}
	        			else if ( getGameMode().friendliesCompassPointsAtKiller() )
		        			player.setCompassTarget(playerManager.getNearestPlayerTo(player, false));
        	}
        }, 20, 10);
	        			
		spectatorFollowProcessID = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
        	public void run()
        	{
	        	for ( Player player : instance.getOnlinePlayers() )
	        	{
	        		PlayerManager.Info info = playerManager.getInfo(player.getName());
	        		if ( !info.isAlive() && info.target != null )
	        			playerManager.checkFollowTarget(player);
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
	
	private void createRecipes()
	{
		// add "simplified" dispenser recipe: replace bow with sapling (of any sort) 
		ShapedRecipe dispenser = new ShapedRecipe(new ItemStack(Material.DISPENSER, 1));
		dispenser.shape(new String[] { "AAA", "ABA", "ACA" });
		dispenser.setIngredient('A', Material.COBBLESTONE);
		dispenser.setIngredient('B', Material.SAPLING);
		dispenser.setIngredient('C', Material.REDSTONE);
		getServer().addRecipe(dispenser);
		
		short zero = 0;
		
		ShapelessRecipe recipe = new ShapelessRecipe(new ItemStack(Material.MONSTER_EGGS, 1, zero, (byte)EntityType.SPIDER.getTypeId()));
		recipe.addIngredient(Material.FEATHER);
		recipe.addIngredient(Material.IRON_INGOT);
		getServer().addRecipe(recipe);
		
		recipe = new ShapelessRecipe(new ItemStack(Material.MONSTER_EGG, 1, zero, (byte)EntityType.ZOMBIE.getTypeId()));
		recipe.addIngredient(Material.ROTTEN_FLESH);
		recipe.addIngredient(Material.IRON_INGOT);
		getServer().addRecipe(recipe);
		
		recipe = new ShapelessRecipe(new ItemStack(Material.MONSTER_EGG, 1, zero, (byte)EntityType.CREEPER.getTypeId()));
		recipe.addIngredient(Material.SULPHUR);
		recipe.addIngredient(Material.IRON_INGOT);
		getServer().addRecipe(recipe);
		
		recipe = new ShapelessRecipe(new ItemStack(Material.MONSTER_EGG, 1, zero, (byte)EntityType.SKELETON.getTypeId()));
		//recipe.addIngredient(3, Material.INK_SACK, 15);
		recipe.addIngredient(Material.BONE);
		recipe.addIngredient(Material.IRON_INGOT);
		getServer().addRecipe(recipe);
		
		recipe = new ShapelessRecipe(new ItemStack(Material.MONSTER_EGG, 1, zero, (byte)EntityType.ENDERMAN.getTypeId()));
		recipe.addIngredient(Material.ENDER_PEARL);
		recipe.addIngredient(Material.IRON_INGOT);
		getServer().addRecipe(recipe);
		
		recipe = new ShapelessRecipe(new ItemStack(Material.MONSTER_EGG, 1, zero, (byte)EntityType.SLIME.getTypeId()));
		recipe.addIngredient(Material.SLIME_BALL);
		recipe.addIngredient(Material.IRON_INGOT);
		getServer().addRecipe(recipe);
	}
	
	private void setupConfiguration()
	{
		getConfig().addDefault("startDisabled", false);
		getConfig().addDefault("stagingWorldName", "world");
		getConfig().addDefault("killerWorldName", "killer");
		
		getConfig().addDefault("defaultGameMode", "Mystery Killer");
		getConfig().addDefault("canChangeGameMode", true);
		getConfig().addDefault("restartAtEndOfGame", "vote");
		
		getConfig().addDefault("autoAssign", false);
		getConfig().addDefault("autoReassign", false);
		getConfig().addDefault("restartDay", true);
		getConfig().addDefault("lateJoinersStartAsSpectator", false);
		getConfig().addDefault("banOnDeath", false);
		getConfig().addDefault("informEveryoneOfReassignedKillers", true);
		getConfig().addDefault("reportStats", true);
		getConfig().addDefault("winningItems", Arrays.asList(Material.BLAZE_ROD.getId(), Material.GHAST_TEAR.getId()));
		getConfig().addDefault("startingItems", new ArrayList<Integer>());
		
		getConfig().addDefault("autoRecreateWorld", false);
		
		
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		gameMode = nextGameMode = GameMode.getByName(getConfig().getString("defaultGameMode"));
		
		if ( gameMode == null )
		{
			log.warning("Invalid value for defaultGameMode: " + getConfig().getString("defaultGameMode"));
			gameMode = nextGameMode = GameMode.getDefault();
		}
		
		canChangeGameMode = getConfig().getBoolean("canChangeGameMode");
		
		String restartAtEnd = getConfig().getString("restartAtEndOfGame");
		if ( restartAtEnd.equalsIgnoreCase("vote") )
		{
			voteRestartAtEndOfGame = true;
			autoRestartAtEndOfGame = false;
		}
		else if ( restartAtEnd.equalsIgnoreCase("true") )
		{
			voteRestartAtEndOfGame = false;
			autoRestartAtEndOfGame = true;
		}
		else
		{
			voteRestartAtEndOfGame = false;
			autoRestartAtEndOfGame = false;
		}
		
		autoAssignKiller = getConfig().getBoolean("autoAssign");
		autoReassignKiller = getConfig().getBoolean("autoReassign");
		restartDayWhenFirstPlayerJoins = getConfig().getBoolean("restartDay");
		lateJoinersStartAsSpectator = getConfig().getBoolean("lateJoinersStartAsSpectator");
		banOnDeath = getConfig().getBoolean("banOnDeath");
		informEveryoneOfReassignedKillers = getConfig().getBoolean("informEveryoneOfReassignedKillers");
		autoRecreateWorld = getConfig().getBoolean("autoRecreateWorld");
		reportStats = getConfig().getBoolean("reportStats");

		List<Integer> itemIDs = getConfig().getIntegerList("winningItems"); 
		winningItems = new Material[itemIDs.size()];
		for ( int i=0; i<winningItems.length; i++ )
		{
			Material mat = Material.getMaterial(itemIDs.get(i));
			if ( mat == null )
			{
				mat = Material.BLAZE_ROD;
				log.warning("Winning item ID " + itemIDs.get(i) + " not recognized.");
			} 
			winningItems[i] = mat;
		}
		
		itemIDs = getConfig().getIntegerList("startingItems"); 
		startingItems = new Material[itemIDs.size()];
		for ( int i=0; i<startingItems.length; i++ )
		{
			Material mat = Material.getMaterial(itemIDs.get(i));
			if ( mat == null )
			{
				mat = Material.STONE_PICKAXE;
				log.warning("Starting item ID " + itemIDs.get(i) + " not recognized.");
			} 
			startingItems[i] = mat;
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
				playerManager.putPlayerInWorld(player, worldManager.mainWorld);
			}
			else if ( args[0].equalsIgnoreCase("nether") )
			{
				if ( worldManager.netherWorld != null )
					playerManager.putPlayerInWorld(player, worldManager.netherWorld);
				else
					sender.sendMessage("Nether world not found, please try again");
			}
			else if ( args[0].equalsIgnoreCase("follow") )
			{
				if ( playerManager.getFollowTarget(player) == null )
				{
					playerManager.setFollowTarget(player, playerManager.getNearestFollowTarget(player));
					playerManager.checkFollowTarget(player);
					sender.sendMessage("Follow mode enabled. Type " + ChatColor.YELLOW + "/spec follow" + ChatColor.RESET + " again to exist follow mode. Type /spec <player name> to follow another player.");
				}
				else
				{
					playerManager.setFollowTarget(player, null);
					sender.sendMessage("Follow mode disabled.");
				}
			}
			else
			{
				Player other = getServer().getPlayer(args[0]);
				if ( other == null || !other.isOnline() )
					sender.sendMessage("Player not found: " + args[0]);
				else if ( playerManager.getFollowTarget(player) != null )
					playerManager.setFollowTarget(player, other.getName());
				
				playerManager.moveToSee(player, other);
			}
			
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("vote"))
		{
			if ( sender instanceof Player )
				voteManager.showVoteMenu((Player)sender);
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("team"))
		{
			if ( !getGameMode().informOfKillerIdentity() )
			{
				sender.sendMessage("Team chat is not available in " + getGameMode().getName() + " mode");
				return true;
			}
			
			if ( playerManager.numKillersAssigned() == 0 || !(sender instanceof Player) )
				return true;
			
			if ( args.length == 0 )
			{
				sender.sendMessage("Usage: /team <message>");
				return true;
			}
			
			String message = "[Team] " + ChatColor.RESET + args[0];
			for ( int i=1; i<args.length; i++ )
				message += " " + args[i];
			
			Player player = (Player)sender;
			PlayerManager.Info info = playerManager.getInfo(player.getName());
		
			// most of this code is a clone of the actual chat code in NetServerHandler.chat
			AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(false, player, "ignored", new LazyPlayerSet());
			getServer().getPluginManager().callEvent(event);

			if (event.isCancelled())
				return true;
		
			message = String.format(event.getFormat(), player.getDisplayName(), message);
			getServer().getConsoleSender().sendMessage(message);
			
			for (Player recipient : event.getRecipients())
                if ( playerManager.isKiller(recipient.getName()) == info.isKiller() )
					recipient.sendMessage(message);
			
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("help"))
		{
			if ( sender instanceof Player )
				getGameMode().explainGameMode((Player)sender, playerManager);
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("killer"))
		{
			if ( sender instanceof Player && !((Player)sender).isOp() )
			{
				sender.sendMessage("You must be a server op to run this command");
				return true;
			}
			
			if ( args.length == 0 )
			{
				sender.sendMessage("Usage: /killer mode, /killer restart, /killer end, /killer add, /killer clear, /killer reallocate");
				return true;
			}
			
			if ( args[0].equalsIgnoreCase("add") )
			{
				playerManager.assignKillers(sender);
			}
			else if ( args[0].equalsIgnoreCase("clear") )
			{
				playerManager.clearKillers(sender);
			}
			else if ( args[0].equalsIgnoreCase("reallocate") )
			{
				playerManager.clearKillers(sender);
				playerManager.assignKillers(sender);
			}
			else if ( args[0].equalsIgnoreCase("restart") )
			{
				if ( restarting )
					return true;
				
				restartGame(sender);
			}
			else if ( args[0].equalsIgnoreCase("end") )
			{
				if ( restarting )
					return true;
				
				endGame(sender);
			}
			else if ( args[0].equalsIgnoreCase("mode") )
			{
				if ( args.length < 2 )
				{
					sender.sendMessage("Current game mode is " + getGameMode().getName());
					return true;
				}
				String mode = args[1];
				for ( int i=2; i<args.length; i++ )
					mode += " " + args[i];
				
				GameMode check = GameMode.getByName(mode);
				if ( check == null )
				{
					String message = "Invalid game mode: " + mode + "! Valid modes are:";
					for (GameMode possibility : GameMode.gameModes.values())
						message += "\n " + possibility.getName();
					sender.sendMessage(message);	
				}
				else
					setNextGameMode(check, sender);
			}
			else
				sender.sendMessage("Invalid parameter: " + args[0] + " - type /killer to list allowed parameters");
			
			return true;
		}
		
		return false;
	}

	public MinecraftServer getMinecraftServer()
	{
		try
		{
			CraftServer server = (CraftServer)getServer();
			Field f = server.getClass().getDeclaredField("console");
			f.setAccessible(true);
			MinecraftServer console = (MinecraftServer)f.get(server);
			f.setAccessible(false);
			return console;
		}
		catch ( IllegalAccessException ex )
		{
		}
		catch  ( NoSuchFieldException ex )
		{
		}
		
		return null;
	}
	
	public YamlConfiguration getBukkitConfiguration()
	{
		YamlConfiguration config = null;
		try
		{
        	Field configField = CraftServer.class.getDeclaredField("configuration");
        	configField.setAccessible(true);
        	config = (YamlConfiguration)configField.get((CraftServer)getServer());
			configField.setAccessible(false);
		}
		catch ( IllegalAccessException ex )
		{
			log.warning("Error removing world from bukkit master list: " + ex.getMessage());
		}
		catch  ( NoSuchFieldException ex )
		{
			log.warning("Error removing world from bukkit master list: " + ex.getMessage());
		}
		return config;
	}
	
	public boolean isGameWorld(World world)
	{
		return world == worldManager.mainWorld || world == worldManager.netherWorld || world == worldManager.endWorld || world == worldManager.stagingWorld;
	}
	
	public List<Player> getOnlinePlayers()
	{
		ArrayList<Player> players = new ArrayList<Player>();
		for ( Player player : getServer().getOnlinePlayers() )
			if ( isGameWorld(player.getWorld()) )
				players.add(player);
		return players;
	}
	
	public void broadcastMessage(String message)
	{
		for ( Player player : getOnlinePlayers() )
			player.sendMessage(message);
	}
	
	public Location getPlinthLocation()
	{
		return plinthPressurePlateLocation;
	}
	
	public void roundFinished()
	{
		if ( voteRestartAtEndOfGame )
			voteManager.startVote("Play another game in the same world?", null, new Runnable() {
				public void run()
				{
					restartGame(null);
				}
			}, new Runnable() {
				public void run()
				{
					endGame(null);
				}
			}, new Runnable() {
				public void run()
				{
					endGame(null);
				}
			});
		else if  ( autoRestartAtEndOfGame )
			restartGame(null);
		else
			endGame(null);
	}
	
	public void endGame(CommandSender actionedBy)
	{
		if ( restarting )
			return;
		
		playerManager.stopAssignmentCountdown();
		
		// if the stats manager is tracking, then the game didn't finish "properly" ... this counts as an "aborted" game
		if ( statsManager.isTracking )
			statsManager.gameFinished(getGameMode(), playerManager.numSurvivors(), 3, 0);
		
		getGameMode().gameFinished();
		
		restarting = true;
		if ( actionedBy != null )
			broadcastMessage(actionedBy.getName() + " ended the gameis restarting the game, please wait while the world is deleted and a new one is prepared...");
		else
			broadcastMessage("The game has ended. You've been moved to the staging world to allow you to set up a new one...");
		
		playerManager.reset(true);
		worldManager.deleteWorlds();
	}
	
	public void restartGame(CommandSender actionedBy)
	{
		if ( restarting )
			return;
		
		playerManager.stopAssignmentCountdown();
		
		// if the stats manager is tracking, then the game didn't finish "properly" ... this counts as an "aborted" game
		if ( statsManager.isTracking )
			statsManager.gameFinished(getGameMode(), playerManager.numSurvivors(), 3, 0);
		
		getGameMode().gameFinished();
		
		if ( gameMode != nextGameMode )
		{
			gameMode = nextGameMode;
			broadcastMessage("Changed to " + gameMode.getName() + " mode");
		}
		
		if ( actionedBy != null )
			broadcastMessage(actionedBy.getName() + " is restarting the game...");
		else
			broadcastMessage("Game is restarting...");
 
		for ( Player player : getOnlinePlayers() )
			playerManager.putPlayerInWorld(player, worldManager.mainWorld);
			
		playerManager.reset(true);
		gameMode.explainGameModeForAll(playerManager);
		playerManager.checkImmediateKillerAssignment();
		
		worldManager.removeAllItems(worldManager.mainWorld);
		worldManager.mainWorld.setTime(0);
	}

	public String tidyItemName(Material m)
	{
		return m.name().toLowerCase().replace('_', ' ');
	}
}
package com.ftwinston.Killer;

/*
 * Killer Minecraft
 * a Minecraft Bukkit Mod by 
 * David "Canazza" McQuillan and
 * Andrew Winston "FTWinston" Winston Watkins AKA Winston
 * Created 18/06/2012
 */

import java.lang.reflect.Field;
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
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class Killer extends JavaPlugin
{
	public static Killer instance;
	public Logger log = Logger.getLogger("Minecraft");
	

	public enum GameState
	{
		stagingWorldSetup(false, false, true), // in staging world, players need to choose mode/world
		stagingWorldReady(false, false, true), // in staging world, players need to push start
		stagingWorldConfirm(false, false, true), // in staging world, players have chosen a game mode that requires confirmation (e.g. they don't have the recommended player number)
		worldGeneration(false, false, false), // in staging world, game worlds are being generated
		beforeAssignment(true, false, false), // game is active, killer(s) not yet assigned
		active(true, true, false), // game is active, killer(s) assigned
		finished(true, true, false); // game is finished, but not yet restarted
		
		public final boolean usesGameWorlds, usesSpectators, canChangeGameSetup;
		GameState(boolean useGameWorlds, boolean useSpectators, boolean canChangeGameSetup)
		{
			this.usesGameWorlds = useGameWorlds;
			this.usesSpectators = useSpectators;
			this.canChangeGameSetup = canChangeGameSetup;
		}
	}
	
	private GameState gameState = GameState.stagingWorldSetup;
	public GameState getGameState() { return gameState; }
	public void setGameState(GameState newState)
	{
		GameState prevState = gameState;
		gameState = newState;
		
		if ( !newState.usesGameWorlds && prevState.usesGameWorlds )
		{
			// if the stats manager is tracking, then the game didn't finish "properly" ... this counts as an "aborted" game
			if ( statsManager.isTracking )
				statsManager.gameFinished(getGameMode(), playerManager.numSurvivors(), 3, 0);
			
			getGameMode().gameFinished();
			
			playerManager.putPlayersInWorld(worldManager.stagingWorld);
			playerManager.reset(true);
			worldManager.deleteWorlds();
		}
		
		if ( newState == GameState.stagingWorldSetup )
		{
			worldManager.showStartButton(false);
			worldManager.showConfirmButtons(false);
		}
		else if ( newState == GameState.stagingWorldReady )
		{
			worldManager.showConfirmButtons(false);
			worldManager.showStartButton(true);
		}
		else if ( newState == GameState.stagingWorldConfirm )
		{
			worldManager.showStartButton(false);
			worldManager.showConfirmButtons(true);
		}
		else if( newState == GameState.worldGeneration )
		{
			worldManager.generateWorlds(worldOption, new Runnable() {
				@Override
				public void run() {
					setGameState(GameState.beforeAssignment);
				}
			});
		}
		else if ( newState == GameState.beforeAssignment )
		{
			// if the stats manager is tracking, then the game didn't finish "properly" ... this counts as an "aborted" game
			if ( statsManager.isTracking )
				statsManager.gameFinished(getGameMode(), playerManager.numSurvivors(), 3, 0);
			
			if ( prevState.usesGameWorlds )
			{
				getGameMode().gameFinished();
				worldManager.removeAllItems(worldManager.mainWorld);
				worldManager.mainWorld.setTime(0);				
			}

			playerManager.putPlayersInWorld(worldManager.mainWorld);			
			playerManager.startGame();
		}
	}
	
	
	List<Recipe> allRecipes = new ArrayList<Recipe>(); 
	Location plinthPressurePlateLocation;

	public boolean stagingWorldIsServerDefault;
	
	private EventListener eventListener = new EventListener(this);
	public WorldManager worldManager;
	public PlayerManager playerManager;
	public VoteManager voteManager;
	public StatsManager statsManager;
	
	private GameMode gameMode = null;
	public GameMode getGameMode() { return gameMode; }
	public boolean setGameMode(GameMode g) { gameMode = g; return gameMode != null && worldOption != null; }
	
	private WorldOption worldOption = null;
	public WorldOption getWorldOption() { return worldOption; }
	public boolean setWorldOption(WorldOption w) { worldOption = w; return gameMode != null && worldOption != null; }
		
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
	{
		return new StagingWorldGenerator();
	}
	
	public void onEnable()
	{
        instance = this;
        
        Settings.setup(this);
        GameMode.setup(this);
        WorldOption.setup(this);
		
		createRecipes();
		
        playerManager = new PlayerManager(this);
        worldManager = new WorldManager(this);
        voteManager = new VoteManager(this);
        statsManager = new StatsManager(this);
        getServer().getPluginManager().registerEvents(eventListener, this);

		MinecraftServer ms = getMinecraftServer();
		if ( ms != null && ms.getPropertyManager().getString("level-name", "world").equalsIgnoreCase(Settings.stagingWorldName) )
		{
			worldManager.hijackDefaultWorldAsStagingWorld(Settings.stagingWorldName); // Killer's staging world is the server's default, so hijack how it's going to be configured
			stagingWorldIsServerDefault = true;
		}
		else
		{
			worldManager.createStagingWorld(Settings.stagingWorldName); // staging world isn't server default, so create it as a new world
			stagingWorldIsServerDefault = false;
		}

		// remove existing Killer worlds
		worldManager.deleteWorlds();
		
        // disable spawn protection
        getServer().setSpawnRadius(0);
	}
	
	public void onDisable()
	{
		playerManager.reset(true);
		worldManager.onDisable();
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
		allRecipes.add(dispenser);
		
		// eye of ender recipe for use in finding nether fortresses. Replacing blaze powder with spider eye!
		ShapelessRecipe recipe = new ShapelessRecipe(new ItemStack(Material.EYE_OF_ENDER, 1));
		recipe.addIngredient(Material.ENDER_PEARL);
		recipe.addIngredient(Material.SPIDER_EYE);
		getServer().addRecipe(recipe);
		allRecipes.add(recipe);
		
		short zero = 0;
		
		recipe = new ShapelessRecipe(new ItemStack(Material.MONSTER_EGGS, 1, zero, (byte)EntityType.SPIDER.getTypeId()));
		recipe.addIngredient(Material.STRING);
		recipe.addIngredient(Material.IRON_INGOT);
		getServer().addRecipe(recipe);
		allRecipes.add(recipe);
		
		recipe = new ShapelessRecipe(new ItemStack(Material.MONSTER_EGG, 1, zero, (byte)EntityType.ZOMBIE.getTypeId()));
		recipe.addIngredient(Material.ROTTEN_FLESH);
		recipe.addIngredient(Material.IRON_INGOT);
		getServer().addRecipe(recipe);
		allRecipes.add(recipe);
		
		recipe = new ShapelessRecipe(new ItemStack(Material.MONSTER_EGG, 1, zero, (byte)EntityType.CREEPER.getTypeId()));
		recipe.addIngredient(Material.SULPHUR);
		recipe.addIngredient(Material.IRON_INGOT);
		getServer().addRecipe(recipe);
		allRecipes.add(recipe);
		
		recipe = new ShapelessRecipe(new ItemStack(Material.MONSTER_EGG, 1, zero, (byte)EntityType.SKELETON.getTypeId()));
		recipe.addIngredient(Material.BONE);
		recipe.addIngredient(Material.IRON_INGOT);
		getServer().addRecipe(recipe);
		allRecipes.add(recipe);
		
		recipe = new ShapelessRecipe(new ItemStack(Material.MONSTER_EGG, 1, zero, (byte)EntityType.SLIME.getTypeId()));
		recipe.addIngredient(Material.SLIME_BALL);
		recipe.addIngredient(Material.IRON_INGOT);
		getServer().addRecipe(recipe);
		allRecipes.add(recipe);
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
			if ( !(sender instanceof Player) )
				return true;
			
			// if they've already reached the end of the messages, start again from the beginning
			Player player = (Player)sender;
			PlayerManager.Info info = playerManager.getInfo(player.getName());
			if ( info.nextHelpMessage == -1 )
				info.nextHelpMessage = 0;
			
			getGameMode().sendGameModeHelpMessage(playerManager, player);
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("killer"))
		{	
			Player player;
			if ( sender instanceof Player )
				player = (Player)sender;
			else
				player = null;
			
			// players, op or otherwise, can use /killer join and /killer quit,
			// ONLY IF the staging world isn't the server default.
			if ( !stagingWorldIsServerDefault )
			{
				if ( args[0].equalsIgnoreCase("join") )
				{
					if ( player == null )
					{
						sender.sendMessage("Only players can run this command");
						return true;
					}
					if ( isGameWorld(player.getWorld()) )
					{
						sender.sendMessage("You are already part of the Killer game, you can't join again!");
						return true;
					}
					
					playerManager.movePlayerIntoKillerGame(player);
					return true;
				}
				else if ( args[0].equalsIgnoreCase("quit") )
				{
					if ( player == null )
					{
						sender.sendMessage("Only players can run this command");
						return true;
					}
					if ( !isGameWorld(player.getWorld()) )
					{
						sender.sendMessage("You are not part of the Killer game, so you can't quit!");
						return true;
					}
					
					playerManager.movePlayerOutOfKillerGame(player);
					return true;
				}
				else if ( player != null && !player.isOp() )
				{
					sender.sendMessage("Invalid command: Use /killer join to enter the game, and /killer quit to leave it");
					return true;
				}
			}
		
			// op players and non-players can use the others
		
			if ( player != null && !player.isOp() )
			{
				sender.sendMessage("You must be a server op to run this command");
				return true;
			}
			
			if ( args.length == 0 )
			{
				if ( !stagingWorldIsServerDefault && player != null )
					sender.sendMessage("Usage: /killer join, /killer quit, /killer restart, /killer end, /killer add, /killer clear");
				else
					sender.sendMessage("Usage: /killer restart, /killer end, /killer add, /killer clear");
				return true;
			}
			
			if ( args[0].equalsIgnoreCase("add") )
			{
				if ( getGameState().usesGameWorlds )
					playerManager.assignKillers(sender);
			}
			else if ( args[0].equalsIgnoreCase("clear") )
			{
				if ( getGameState().usesGameWorlds )
					playerManager.clearKillers(sender);
			}
			else if ( args[0].equalsIgnoreCase("restart") )
			{
				if ( getGameState().usesGameWorlds )
					restartGame(sender);
			}
			else if ( args[0].equalsIgnoreCase("end") )
			{
				if ( getGameState().usesGameWorlds )
					endGame(sender);
			}
			else if ( args[0].equalsIgnoreCase("seed") )
			{
				if ( args.length < 2 )
				{
					WorldOption.setCustomSeed(null);
					broadcastMessage(sender.getName() + " cleared the seed");
					return true;
				}

				String seed = args[1];
				for ( int i=2; i<args.length; i++ )
					seed += " " + args[i];
				
				WorldOption.setCustomSeed(seed);
				broadcastMessage(sender.getName() + " set the seed to: " + seed);
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
		return world == worldManager.mainWorld || world == worldManager.netherWorld || world == worldManager.stagingWorld;
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
		if ( Settings.voteRestartAtEndOfGame )
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
		else if  ( Settings.autoRestartAtEndOfGame )
			restartGame(null);
		else
			endGame(null);
	}
	
	public void endGame(CommandSender actionedBy)
	{
		if ( actionedBy != null )
			broadcastMessage(actionedBy.getName() + " ended the game. You've been moved to the staging world to allow you to set up a new one...");
		else
			broadcastMessage("The game has ended. You've been moved to the staging world to allow you to set up a new one...");
		
		setGameState(GameState.stagingWorldReady); // stagingWorldReady cos the options from last time will still be selected
	}
	
	public void restartGame(CommandSender actionedBy)
	{
		if ( actionedBy != null )
			broadcastMessage(actionedBy.getName() + " is restarting the game...");
		else
			broadcastMessage("Game is restarting...");
		
		setGameState(GameState.beforeAssignment);
	}

	public String tidyItemName(Material m)
	{
		return m.name().toLowerCase().replace('_', ' ');
	}
}
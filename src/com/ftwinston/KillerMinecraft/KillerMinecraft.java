package com.ftwinston.KillerMinecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/*
 * Killer Minecraft
 * a Minecraft Bukkit Mod by 
 * David "Canazza" McQuillan and
 * Andrew Winston "FTWinston" Winston Watkins AKA Winston
 * Created 18/06/2012
 */

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import com.ftwinston.KillerMinecraft.CraftBukkit.CraftBukkitAccess;

public class KillerMinecraft extends JavaPlugin
{
	public static KillerMinecraft instance;
	CraftBukkitAccess craftBukkit;
	public Logger log = Logger.getLogger("Minecraft");
	
	EventManager eventListener = new EventManager(this);
	WorldManager worldManager;
	PlayerManager playerManager;
	RecipeManager recipeManager;
//	VoteManager voteManager;
	Game[] games;
	int portalUpdateProcess = -1;
	
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
	{
		return new StagingWorldGenerator();
	}
	
	public void onEnable()
	{
        instance = this;
        craftBukkit = CraftBukkitAccess.createCorrectVersion(this);
        if ( craftBukkit == null )
        {
        	setEnabled(false);
        	return;
        }
        
        Settings.setup(this);
        
        worldManager = new WorldManager(this);
        
		playerManager = new PlayerManager(this);
        recipeManager = new RecipeManager();
        Vote.setupConversation();

        if ( Settings.nothingButKiller )
        	worldManager.hijackDefaultWorld();
	
        final YamlConfiguration persistentData = loadPersistentData();
        
        // delay this by 1 tick so that the plugins are all loaded and the worlds are deleted 
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			
			@Override
			public void run() {
				afterPluginsEnabled(persistentData);
			}
		}, 1);
        
        deleteNonPersistentWorldFolders(persistentData);
	}

	GameModePlugin defaultGameMode;
	
	public void afterPluginsEnabled(YamlConfiguration persistentData)
	{
		if ( GameMode.gameModes.size() == 0 )
		{
			log.warning("Killer cannot start: No game modes have been loaded!");
			log.warning("Add some game mode plugins to your server!");
			setEnabled(false);
			return;
		}
		if ( WorldGenerator.getGenerators(Environment.NORMAL).size() == 0 )
		{
			log.warning("Killer cannot start: No world generators have been loaded!");
			log.warning("Add some world generator plugins to your server!");
			setEnabled(false);
			return;
		}

		MenuManager.createRootMenu();
		games = new Game[Settings.numGames];
		
		defaultGameMode = GameMode.getByName(Settings.defaultGameMode);
		if ( defaultGameMode == null )
		{
			defaultGameMode = GameMode.get(0);
			log.info("Default game mode not found: " + Settings.defaultGameMode);
		}
				
		for ( int i = 0; i < games.length; i++ )
			games[i] = new Game(this, i + 1);

        getServer().getPluginManager().registerEvents(eventListener, KillerMinecraft.instance);
        
		// if using the floating island staging world, ensure it is set up correctly
		if ( Settings.nothingButKiller )
		{
			World world = getServer().getWorlds().get(0);
			
			ChunkGenerator gen = world.getGenerator(); 
			if ( gen != null && gen.getClass() == StagingWorldGenerator.class )
			{
				world.setSpawnFlags(false, false);
				world.setDifficulty(Difficulty.PEACEFUL);
				world.setPVP(false);
				world.setSpawnLocation(0, 65, 0);

				world.setFullTime(6000);
				world.setGameRuleValue("doDaylightCycle", "false");
			}
		}

        portalUpdateProcess = getServer().getScheduler().scheduleSyncRepeatingTask(instance, new Runnable() {
			public void run()
			{	
		    	PortalHelper.checkDelays();
			}
		}, 20L, 60L); // check every 3 seconds
        
        if (persistentData != null)
        	restorePersistentGames(persistentData);
	}

	public void onDisable()
	{
		if ( portalUpdateProcess != -1 )
		{
			getServer().getScheduler().cancelTask(portalUpdateProcess);
			portalUpdateProcess = -1;
		}
		
		if ( games != null )
		{
			for ( Game game : games )
				if (!game.getGameMode().isPersistent())
					game.finishGame();

			savePersistentGameData();
		}
		
		if ( worldManager != null )
			worldManager.onDisable();
		
		craftBukkit = null;
		playerManager = null;
        worldManager = null;
	}
	
	private YamlConfiguration loadPersistentData()
	{

		File persistentDataFile = new File(getDataFolder(), "persistentGames.yml");
			
		if (!persistentDataFile.exists() )
			return null;
		
		return YamlConfiguration.loadConfiguration(persistentDataFile);	
	}
	
	private void restorePersistentGames(YamlConfiguration persistentData)
	{	
		for (int i=1; i<=games.length; i++)
		{
			ConfigurationSection section = persistentData.getConfigurationSection(Integer.toString(i));
			if (section == null)
				continue;
			
			games[i-1].loadPersistentData(section);
		}
	}
	
	private void savePersistentGameData()
	{
		YamlConfiguration persistentData = new YamlConfiguration();
		
		boolean any = false;
		for ( Game game : games )
			if (game.getGameMode().isPersistent())
			{
				any = true;
				
				ConfigurationSection section = persistentData.createSection(Integer.toString(game.getNumber()));
				game.savePersistentData(section);
			}
		
		File persistentDataFile = new File(getDataFolder(), "persistentGames.yml");
		
		if (any)
		{
			try
			{
				persistentData.save(persistentDataFile);
			}
			catch ( IOException ex )
			{
				log.warning("Unable to save persistentGames.yml file: " + ex.getMessage());
			}
		}
		else if (persistentDataFile.exists())
		{
			try
			{
				Files.delete(persistentDataFile.toPath());
			}
			catch ( IOException ex )
			{
				log.warning("Unable to delete old persistentGames.yml file: " + ex.getMessage());
			}
		}
	}
	
	private void deleteNonPersistentWorldFolders(YamlConfiguration persistentData)
	{
		for (int i=1; i<=Settings.numGames; i++)
		{
			String gameNum = Integer.toString(i);
			
			ConfigurationSection section = persistentData == null ? null : persistentData.getConfigurationSection(gameNum);
			if (section != null)
				continue; // this is a persistent game, it's worlds must be left intact 
			
			worldManager.deleteWorldFolders(Settings.killerWorldNamePrefix + "_" + gameNum + "_");
		}
	}

	static void registerPlugin(KillerModulePlugin plugin)
	{
		plugin.initialize(instance);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		return CommandHandler.onCommand(this, sender, cmd, label, args);
	}
	
	Map<String, Game> gamesByWorld = new HashMap<String, Game>(), gamesByPlayer = new HashMap<String, Game>();
	
	public Game getGameForWorld(World w) { return gamesByWorld.get(w.getName()); }
	
	public Game getGameForPlayer(Player player) { return gamesByPlayer.get(player.getName()); }
}
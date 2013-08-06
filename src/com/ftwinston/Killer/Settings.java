package com.ftwinston.Killer;

import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public class Settings
{
	public static final int absMaxGames = 8;
	public static int numGames;
	
	public static String
	stagingWorldName,
	killerWorldNamePrefix,
	defaultGameMode = "Mystery Killer", // remove this
	defaultWorldOption = "Default World"; // remove this
	
	public static boolean
	nothingButKiller,
	allowTeleportToStagingArea,
	filterChat,
	filterScoreboard,
	allowPlayerLimits,
	allowSpectators,
	allowLateJoiners,
	reportStats;
	
	public static Location spawnCoordMin, spawnCoordMax, protectionMin, protectionMax;
	
	public static final int minPlayerLimit = 2, maxPlayerLimit = 200;
		
	public static Material teleportModeItem = Material.WATCH, followModeItem = Material.ARROW;
	
	public static void setup(Killer plugin)
	{
		plugin.saveDefaultConfig();
		FileConfiguration config = plugin.getConfig();
		
		stagingWorldName = config.getString("stagingWorldName", "staging");
		killerWorldNamePrefix = config.getString("killerWorldNamePrefix", "killer");

		spawnCoordMin = readLocation(config, "stagingSpawn.from", null, -8, 64, -8);
		spawnCoordMax = readLocation(config, "stagingSpawn.to", null, 8, 64, 8);
		
		protectionMin = readLocation(config, "protected.from", null, -8, 64, -8);
		protectionMax = readLocation(config, "protected.to", null, 8, 64, 8);
		
		nothingButKiller = config.getBoolean("nothingButKiller", false);
		allowTeleportToStagingArea = config.getBoolean("allowTeleportToStagingArea", true);
		filterChat = config.getBoolean("filterChat", true);
		filterScoreboard = config.getBoolean("filterScoreboard", true);
		allowSpectators = config.getBoolean("allowSpectators", true);
		reportStats = config.getBoolean("reportStats", true);
		allowLateJoiners = config.getBoolean("allowLateJoiners", true);
		allowPlayerLimits = config.getBoolean("allowPlayerLimits", true);
		
		//startingItems = readMaterialList(config, "startingItems", new ArrayList<Integer>(), Material.STONE_PICKAXE);
	}

	private static void setStagingWorld(World world)
	{
		spawnCoordMin.setWorld(world);
		spawnCoordMax.setWorld(world);
		protectionMin.setWorld(world);
		protectionMax.setWorld(world);
	}
	
	public static boolean setupGames(Killer plugin)
	{
		if ( plugin.games != null )
			return false;
		
		setStagingWorld(plugin.stagingWorld);
		
		List<?> config = plugin.getConfig().getList("games");
		
		if ( config == null )
		{
			plugin.log.warning("Killer cannot start: Configuration contains no game information");
			return false;
		}
		
		numGames = config.size();
		if ( numGames == 0 )
		{
			plugin.log.warning("Killer cannot start: Configuration game section contains no information");
			return false;
		}
		
		if ( numGames > absMaxGames )
		{
			plugin.log.warning("Killer only supports up to " + absMaxGames + " games, but " + numGames + " were specified. Ignoring the excess...");
			numGames = absMaxGames;
		}
		
		plugin.games = new Game[numGames];
		
		for ( int iGame = 0; iGame < numGames; iGame++ )
		{
			Object o = config.get(iGame);
			
			LinkedHashMap<String, Object> gameConfig;
			
			try
			{
				@SuppressWarnings("unchecked")
				LinkedHashMap<String, Object> tmp = (LinkedHashMap<String, Object>)o;
				
				gameConfig = tmp;
			}
			catch ( ClassCastException ex )
			{
				gameConfig = null;
			}
			
			if ( gameConfig == null )
			{
				plugin.log.warning("Killer cannot start: Invalid configuration for game #" + (iGame+1));
				return false;
			}

			Game g;
			plugin.games[iGame] = g = new Game(plugin, iGame);
			setupGame(plugin, g, gameConfig);
		}
		
		return true;
	}

	private static void setupGame(Killer plugin, Game game, LinkedHashMap<String, Object> config)
	{
		String mode = getString(config, "mode", defaultGameMode);
		String world = getString(config, "world", defaultWorldOption);
		Location infoMap = readLocation(config, "infomap", plugin.stagingWorld);
		Location joinButton = readLocation(config, "buttons.join", plugin.stagingWorld);
		Location configButton = readLocation(config, "buttons.config", plugin.stagingWorld);
		Location startButton = readLocation(config, "buttons.start", plugin.stagingWorld);
		
		GameModePlugin modePlugin = GameMode.getByName(mode);
		if ( modePlugin == null )
			modePlugin = GameMode.get(0);
		game.setGameMode(modePlugin);
		
		WorldOptionPlugin worldPlugin = WorldOption.getByName(world);
		if ( worldPlugin == null )
			worldPlugin = WorldOption.get(0);
		game.setWorldOption(worldPlugin);
		
		game.initStagingArea(infoMap, joinButton, configButton, startButton);
	}
	
	private static String getString(LinkedHashMap<String, Object> config, String key, String defaultVal)
	{
		Object o = config.get(key);
		if ( o == null )
			return defaultVal;

		try
		{
			return (String)o;
		}
		catch ( ClassCastException ex )
		{
			Killer.instance.log.warning("'" + key + "' is not a string, but it's supposed to be!");
			return defaultVal;
		}
	}

	private static Location readLocation(FileConfiguration config, String name, World world, int defX, int defY, int defZ)
	{
		Location loc = readLocation(config, name, world);
		return loc == null ? new Location(world, defX, defY, defZ) : loc;
	}
	
	private static Location readLocation(FileConfiguration config, String name, World world)
	{
		String str = config.getString(name);
		if ( str == null )
			return null;
		
		String[] parts = str.split(",", 3);
		if ( parts.length != 3 )
		{
			Killer.instance.log.warning("Invalid location in config: " + config.getString(name));
			return null;
		}
		
		try
		{
			int x, y, z;
			x = Integer.parseInt(parts[0].trim());
			y = Integer.parseInt(parts[1].trim());
			z = Integer.parseInt(parts[2].trim());
			return new Location(world, x, y, z);
		}
		catch ( NumberFormatException ex )
		{
			Killer.instance.log.warning("Invalid location in config: " + config.getString(name));
			return null;
		}
	}

	private static Location readLocation(LinkedHashMap<String, Object> config, String name, World world)
	{
		String str = getString(config, name, null);
		if ( str == null )
			return null;
		
		String[] parts = str.split(",", 3);
		if ( parts.length != 3 )
		{
			Killer.instance.log.warning("Invalid location in config: " + str);
			return null;
		}
		
		try
		{
			int x, y, z;
			x = Integer.parseInt(parts[0].trim());
			y = Integer.parseInt(parts[1].trim());
			z = Integer.parseInt(parts[2].trim());
			return new Location(world, x, y, z);
		}
		catch ( NumberFormatException ex )
		{
			Killer.instance.log.warning("Invalid location in config: " + str);
			return null;
		}
	}
	
	/*
	private static Material[] readMaterialList(FileConfiguration config, String keyName, List<Integer> defaultValues, Material defaultOnError)
	{
		config.addDefault(keyName, defaultValues);
	
		List<Integer> itemIDs = config.getIntegerList(keyName); 
		Material[] retVal = new Material[itemIDs.size()];
		for ( int i=0; i<retVal.length; i++ )
		{
			Material mat = Material.getMaterial(itemIDs.get(i));
			if ( mat == null )
			{
				mat = defaultOnError;
				plugin.log.warning("Item ID " + itemIDs.get(i) + " not recognized in " + keyName + ".");
			} 
			retVal[i] = mat;
		}
		
		return retVal;
	}*/
}

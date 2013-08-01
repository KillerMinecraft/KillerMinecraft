package com.ftwinston.Killer;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
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
	
	public static boolean setup(Killer plugin)
	{
		plugin.saveDefaultConfig();
		FileConfiguration config = plugin.getConfig();
		
		stagingWorldName = config.getString("stagingWorldName", "staging");
		killerWorldNamePrefix = config.getString("killerWorldNamePrefix", "killer");

		World stagingWorld = null;//plugin.setupStagingWorld(); // get (creating if needed) the world in question.
		
		spawnCoordMin = readLocation(config, "stagingSpawn.from", stagingWorld, -8, 64, -8);
		spawnCoordMax = readLocation(config, "stagingSpawn.to", stagingWorld, 8, 64, 8);
		
		protectionMin = readLocation(config, "protected.from", stagingWorld, -8, 64, -8);
		protectionMax = readLocation(config, "protected.to", stagingWorld, 8, 64, 8);
		
		allowTeleportToStagingArea = config.getBoolean("allowTeleportToStagingArea", true);
		filterChat = config.getBoolean("filterChat", true);
		filterScoreboard = config.getBoolean("filterScoreboard", true);
		allowSpectators = config.getBoolean("allowSpectators", true);
		reportStats = config.getBoolean("reportStats", true);
		allowLateJoiners = config.getBoolean("allowLateJoiners", true);
		allowPlayerLimits = config.getBoolean("allowPlayerLimits", true);
		
		return setupGames(plugin, config.getConfigurationSection("games"));
		//startingItems = readMaterialList(config, "startingItems", new ArrayList<Integer>(), Material.STONE_PICKAXE);
	}

	private static boolean setupGames(Killer plugin, ConfigurationSection config)
	{
		if ( config == null )
		{
			plugin.log.warning("Configuration contains no game information - nothing for Killer to do!");
			return false;
		}
		
		Map<String, Object> allGames = config.getValues(true);
		
		numGames = allGames.size();
		if ( numGames == 0 )
		{
			plugin.log.warning("Configuration contains no game information - nothing for Killer to do!");
			return false;
		}
		
		if ( numGames > absMaxGames )
		{
			plugin.log.warning("Killer only supports up to " + absMaxGames + " games, but " + numGames + " were specified. Ignoring the excess...");
			numGames = absMaxGames;
		}
		
		plugin.games = new Game[numGames];
		
		int iGame = 0;
		for ( Object o : allGames.values() )
		{
			ConfigurationSection gameConfig = (ConfigurationSection)o;
			if ( gameConfig == null )
			{
				plugin.log.warning("Invalid configuration for game #" + (iGame+1));
				return false;
			}

			Game g;
			plugin.games[iGame] = g = new Game(plugin, iGame);
			setupGame(plugin, g, gameConfig);
			iGame++;
		}
		return true;
	}

	private static void setupGame(Killer plugin, Game game, ConfigurationSection config) {
		String mode = config.getString("mode", defaultGameMode);
		String world = config.getString("world", defaultWorldOption);
		Location infoMap = readLocation(config, "infomap", plugin.stagingWorld);
		Location joinButton = readLocation(config, "buttons.join", plugin.stagingWorld);
		Location configButton = readLocation(config, "buttons.config", plugin.stagingWorld);
		Location startButton = readLocation(config, "buttons.start", plugin.stagingWorld);

		//g.setGameMode(...);
		//g.setWorldOption(...);
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
	
	private static Location readLocation(ConfigurationSection config, String name, World world, int defX, int defY, int defZ)
	{
		Location loc = readLocation(config, name, world);
		return loc == null ? new Location(world, defX, defY, defZ) : loc;
	}
	
	private static Location readLocation(ConfigurationSection config, String name, World world)
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

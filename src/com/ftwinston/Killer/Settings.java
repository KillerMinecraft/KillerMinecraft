package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

public class Settings
{
	public static int maxSimultaneousGames;
	
	public static boolean
	allowPlayerLimits,
	lateJoinersStartAsSpectator,
	banOnDeath,
	reportStats,
	autoRestartAtEndOfGame,
	voteRestartAtEndOfGame;
	
	public static String
	stagingWorldName,
	killerWorldName,
	defaultGameMode,
	defaultWorldOption;
	
	public static final int minPlayerLimit = 2, maxPlayerLimit = 200;
	
	public static Material[] startingItems;
	
	public static Material teleportModeItem = Material.WATCH, followModeItem = Material.ARROW;
	
	public static void setup(Killer plugin)
	{
		plugin.getConfig().options().copyDefaults(true);
		
		maxSimultaneousGames = readInt(plugin, "maxSimultaneousGames", 1);
		if ( maxSimultaneousGames < 1 )
			maxSimultaneousGames = 1;
		else if ( maxSimultaneousGames > 8 )
			maxSimultaneousGames = 8;
		
		lateJoinersStartAsSpectator = readBoolean(plugin, "lateJoinersStartAsSpectator", false);
		allowPlayerLimits = readBoolean(plugin, "allowPlayerLimits", true); 
		banOnDeath = readBoolean(plugin, "banOnDeath", false);
		reportStats = readBoolean(plugin, "reportStats", true);
		
		stagingWorldName = readString(plugin, "stagingWorldName", "staging");
		killerWorldName = readString(plugin, "killerWorldName", "killer");
		defaultGameMode = readString(plugin, "defaultGameMode", "Mystery Killer");
		defaultWorldOption = readString(plugin, "defaultWorldOption", "Default World");
		
		String restartAtEnd = readString(plugin, "restartAtEndOfGame", "vote");
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
		
		startingItems = readMaterialList(plugin, "startingItems", new ArrayList<Integer>(), Material.STONE_PICKAXE);
		
		plugin.saveConfig();
	}
	
	private static int readInt(Killer plugin, String keyName, int defaultVal)
	{
		plugin.getConfig().addDefault(keyName, defaultVal);
		return plugin.getConfig().getInt(keyName);
	}
	
	private static boolean readBoolean(Killer plugin, String keyName, boolean defaultVal)
	{
		plugin.getConfig().addDefault(keyName, defaultVal);
		return plugin.getConfig().getBoolean(keyName);
	}
	
	private static String readString(Killer plugin, String keyName, String defaultVal)
	{
		plugin.getConfig().addDefault(keyName, defaultVal);
		return plugin.getConfig().getString(keyName);
	}
	
	private static Material[] readMaterialList(Killer plugin, String keyName, List<Integer> defaultValues, Material defaultOnError)
	{
		plugin.getConfig().addDefault(keyName, defaultValues);
	
		List<Integer> itemIDs = plugin.getConfig().getIntegerList(keyName); 
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
	}
}

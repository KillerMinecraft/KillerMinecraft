package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;

public class Settings
{
	public static boolean
	allowModeMysteryKiller,
	allowModeInvisibleKiller,
	allowModeCrazyKiller,
	allowModeTeamKiller,
	allowModeContractKiller,
	autoAssignKiller, // probably want to keep this
	restartDayWhenFirstPlayerJoins, // don't want to keep this. Worlds are being created at start time anyway
	lateJoinersStartAsSpectator,
	banOnDeath,
	informEveryoneOfReassignedKillers, // umm ... not sure
	reportStats,
	allowRandomWorlds,
	autoRestartAtEndOfGame,
	voteRestartAtEndOfGame;
	
	public static String
	stagingWorldName,
	killerWorldName;
	
	public static List<String> customWorldNames;
	
	public static Material[] winningItems, startingItems;
	
	public static Material teleportModeItem = Material.WATCH, followModeItem = Material.ARROW;
	
	public static void setup(Killer plugin)
	{
		plugin.getConfig().options().copyDefaults(true);
		
		allowModeMysteryKiller = readBoolean(plugin, "allowModeMysteryKiller", true);
		allowModeInvisibleKiller = readBoolean(plugin, "allowModeInvisibleKiller", true);
		allowModeCrazyKiller = readBoolean(plugin, "allowModeCrazyKiller", true);
		allowModeTeamKiller = readBoolean(plugin, "allowModeTeamKiller", true);
		allowModeContractKiller = readBoolean(plugin, "allowModeContractKiller", true);
		
		autoAssignKiller = readBoolean(plugin, "autoAssign", false);
		restartDayWhenFirstPlayerJoins = readBoolean(plugin, "restartDay", true);
		lateJoinersStartAsSpectator = readBoolean(plugin, "lateJoinersStartAsSpectator", false);
		banOnDeath = readBoolean(plugin, "banOnDeath", false);
		informEveryoneOfReassignedKillers = readBoolean(plugin, "informEveryoneOfReassignedKillers", true);
		reportStats = readBoolean(plugin, "reportStats", true);
		allowRandomWorlds = readBoolean(plugin, "allowRandomWorldGeneration", true);
		
		stagingWorldName = readString(plugin, "stagingWorldName", "world");
		killerWorldName = readString(plugin, "killerWorldName", "killer");
		
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

		plugin.getConfig().addDefault("customWorlds", new ArrayList<String>());
		customWorldNames = plugin.getConfig().getStringList("customWorlds");
		
		winningItems = readMaterialList(plugin, "winningItems", Arrays.asList(Material.BLAZE_ROD.getId(), Material.GHAST_TEAR.getId()), Material.BLAZE_ROD);		
		startingItems = readMaterialList(plugin, "startingItems", new ArrayList<Integer>(), Material.STONE_PICKAXE);
		
		plugin.saveConfig();
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
	
	public static void addCustomWorld(String name)
	{
		//customWorldNames.add(name); // only do this if we update the staging world without restarting the server
		Killer.instance.getConfig().getStringList("customWorlds").add(name);
		Killer.instance.saveConfig();
	}
}

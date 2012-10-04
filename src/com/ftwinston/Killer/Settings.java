package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;

public class Settings
{

	public static boolean canChangeGameMode, autoAssignKiller, autoReassignKiller, restartDayWhenFirstPlayerJoins, lateJoinersStartAsSpectator, banOnDeath, informEveryoneOfReassignedKillers, autoRecreateWorld, reportStats;
	public static boolean autoRestartAtEndOfGame, voteRestartAtEndOfGame;
	
	public static Material[] winningItems, startingItems;
	

	public static Material teleportModeItem = Material.WATCH, followModeItem = Material.ARROW;
	

	public static void setup(Killer plugin)
	{
		plugin.getConfig().addDefault("startDisabled", false);
		plugin.getConfig().addDefault("stagingWorldName", "world");
		plugin.getConfig().addDefault("killerWorldName", "killer");
		
		plugin.getConfig().addDefault("allowRandomWorldGeneration", true);
		
		plugin.getConfig().addDefault("defaultGameMode", "Mystery Killer");
		plugin.getConfig().addDefault("canChangeGameMode", true);
		plugin.getConfig().addDefault("restartAtEndOfGame", "vote");
		
		plugin.getConfig().addDefault("autoAssign", false);
		plugin.getConfig().addDefault("autoReassign", false);
		plugin.getConfig().addDefault("restartDay", true);
		plugin.getConfig().addDefault("lateJoinersStartAsSpectator", false);
		plugin.getConfig().addDefault("banOnDeath", false);
		plugin.getConfig().addDefault("informEveryoneOfReassignedKillers", true);
		plugin.getConfig().addDefault("reportStats", true);
		plugin.getConfig().addDefault("winningItems", Arrays.asList(Material.BLAZE_ROD.getId(), Material.GHAST_TEAR.getId()));
		plugin.getConfig().addDefault("startingItems", new ArrayList<Integer>());
		
		plugin.getConfig().addDefault("autoRecreateWorld", false);
	
		plugin.getConfig().options().copyDefaults(true);
		plugin.saveConfig();
		
		canChangeGameMode = plugin.getConfig().getBoolean("canChangeGameMode");
		
		String restartAtEnd = plugin.getConfig().getString("restartAtEndOfGame");
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
		
		autoAssignKiller = plugin.getConfig().getBoolean("autoAssign");
		autoReassignKiller = plugin.getConfig().getBoolean("autoReassign");
		restartDayWhenFirstPlayerJoins = plugin.getConfig().getBoolean("restartDay");
		lateJoinersStartAsSpectator = plugin.getConfig().getBoolean("lateJoinersStartAsSpectator");
		banOnDeath = plugin.getConfig().getBoolean("banOnDeath");
		informEveryoneOfReassignedKillers = plugin.getConfig().getBoolean("informEveryoneOfReassignedKillers");
		autoRecreateWorld = plugin.getConfig().getBoolean("autoRecreateWorld");
		reportStats = plugin.getConfig().getBoolean("reportStats");

		List<Integer> itemIDs = plugin.getConfig().getIntegerList("winningItems"); 
		winningItems = new Material[itemIDs.size()];
		for ( int i=0; i<winningItems.length; i++ )
		{
			Material mat = Material.getMaterial(itemIDs.get(i));
			if ( mat == null )
			{
				mat = Material.BLAZE_ROD;
				plugin.log.warning("Winning item ID " + itemIDs.get(i) + " not recognized.");
			} 
			winningItems[i] = mat;
		}
		
		itemIDs = plugin.getConfig().getIntegerList("startingItems"); 
		startingItems = new Material[itemIDs.size()];
		for ( int i=0; i<startingItems.length; i++ )
		{
			Material mat = Material.getMaterial(itemIDs.get(i));
			if ( mat == null )
			{
				mat = Material.STONE_PICKAXE;
				plugin.log.warning("Starting item ID " + itemIDs.get(i) + " not recognized.");
			} 
			startingItems[i] = mat;
		}
	}
}

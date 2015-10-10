package com.ftwinston.KillerMinecraft;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class Settings
{
	public static final int absMaxGames = 8;
	public static int numGames, generateChunksPerTick;
	
	public static String
	defaultGameMode,
	defaultWorldGen,
	killerWorldNamePrefix;

	public static boolean
	nothingButKiller,
	filterChat,
	filterScoreboard,
	allowPlayerLimits,
	allowSpectators,
	allowLateJoiners;
	
	public static final int minPlayerLimit = 2, maxPlayerLimit = 200;
		
	public static Material teleportModeItem = Material.WATCH, followModeItem = Material.ARROW;
	
	public static void setup(KillerMinecraft plugin)
	{
		plugin.saveDefaultConfig();
		FileConfiguration config = plugin.getConfig();
		
		defaultGameMode = config.getString("defaultGameMode", "Killer_on_the_Loose");
		defaultWorldGen = config.getString("defaultWorldGen", "Default_World");
		killerWorldNamePrefix = config.getString("killerWorldNamePrefix", "killer");

		nothingButKiller = config.getBoolean("nothingButKiller", true);
		filterChat = config.getBoolean("filterChat", true);
		filterScoreboard = config.getBoolean("filterScoreboard", true);
		allowSpectators = config.getBoolean("allowSpectators", true);
		allowLateJoiners = config.getBoolean("allowLateJoiners", true);
		allowPlayerLimits = config.getBoolean("allowPlayerLimits", true);
		
		numGames = config.getInt("numGames", 4);
		if ( numGames < 1 )
			numGames = 1;
		else if ( numGames > absMaxGames )
		{
			plugin.log.warning("Killer only supports up to " + absMaxGames + " games, but numGames = " + numGames + ". Ignoring the excess...");
			numGames = absMaxGames;
		}
		
		generateChunksPerTick = config.getInt("generateChunksPerTick", 3);
		if ( generateChunksPerTick < 1 )
			generateChunksPerTick = 1;
	}
}

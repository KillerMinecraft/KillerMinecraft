package com.ftwinston.KillerMinecraft;

import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

public class Settings
{
	public static final int absMaxGames = 8;
	public static int numGames, generateChunksPerTick;
	
	public static String
	defaultGameMode,
	defaultWorldGen,
	defaultNetherGen,
	defaultEndGen,
	killerWorldNamePrefix;

	public static boolean
	nothingButKiller,
	filterChat,
	filterScoreboard,
	allowPlayerLimits,
	allowSpectators,
	allowLateJoiners;
	
	public static double defaultWorldBorderSize;
	public static double[] worldBorderSizes;
	
	public static final int minPlayerLimit = 2, maxPlayerLimit = 200;
	
	public static void setup(KillerMinecraft plugin)
	{
		plugin.saveDefaultConfig();
		FileConfiguration config = plugin.getConfig();
		
		defaultGameMode = config.getString("defaultGameMode", "Killer_on_the_Loose");
		defaultWorldGen = config.getString("defaultWorldGen", "Default_World");
		defaultNetherGen = config.getString("defaultNetherGen", "Default_Nether");
		defaultEndGen = config.getString("defaultEndGen", "Default_End");
		killerWorldNamePrefix = config.getString("killerWorldNamePrefix", "killer");

		nothingButKiller = config.getBoolean("nothingButKiller", false);
		filterChat = config.getBoolean("filterChat", true);
		filterScoreboard = config.getBoolean("filterScoreboard", true);
		allowSpectators = config.getBoolean("allowSpectators", true);
		allowLateJoiners = config.getBoolean("allowLateJoiners", true);
		allowPlayerLimits = config.getBoolean("allowPlayerLimits", true);
		
		defaultWorldBorderSize = config.getDouble("defaultWorldBorderSize", 0);
		
		List<Double> sizes = config.getDoubleList("worldBorderSizes");
		worldBorderSizes = new double[sizes == null ? 0 : sizes.size()];
		if (sizes != null)
			for (int i=0; i<worldBorderSizes.length; i++)
				worldBorderSizes[i] = sizes.get(i).doubleValue();
		
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

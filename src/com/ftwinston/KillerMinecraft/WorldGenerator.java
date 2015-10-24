package com.ftwinston.KillerMinecraft;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.bukkit.World;
import org.bukkit.World.Environment;

public abstract class WorldGenerator extends KillerModule
{
	private static List<WorldGeneratorPlugin> overworldGenerators = new ArrayList<WorldGeneratorPlugin>();
	private static List<WorldGeneratorPlugin> netherGenerators = new ArrayList<WorldGeneratorPlugin>();
	private static List<WorldGeneratorPlugin> endGenerators = new ArrayList<WorldGeneratorPlugin>();
	
	static List<WorldGeneratorPlugin> getGenerators(Environment worldType)
	{
		switch(worldType)
		{
		case NORMAL:
			return overworldGenerators;
		case NETHER:
			return netherGenerators;
		case THE_END:
			return endGenerators;
		default:
			return null;
		}
	}
	
	private static EnumMap<Environment, WorldGeneratorPlugin> defaultWorldGenerators = new EnumMap<Environment, WorldGeneratorPlugin>(Environment.class);
	static WorldGeneratorPlugin getDefault(Environment worldType)
	{
		if (defaultWorldGenerators.containsKey(worldType))
			return defaultWorldGenerators.get(worldType);
		
		List<WorldGeneratorPlugin> generators;
		String defaultValue;
		
		switch(worldType)
		{
		case NORMAL:
			generators = overworldGenerators;
			defaultValue = Settings.defaultWorldGen;
			break;
		case NETHER:
			generators = netherGenerators;
			defaultValue = Settings.defaultNetherGen;
			break;
		case THE_END:
			generators = endGenerators;
			defaultValue = Settings.defaultEndGen;
			break;
		default:
			return null;
		}
		
		for (WorldGeneratorPlugin generator : generators)
			if (defaultValue.equalsIgnoreCase(generator.getName()))
			{
				defaultWorldGenerators.put(worldType, generator);
				return generator;
			}
		
		KillerMinecraft.instance.log.info("Default " + worldType.name() + " world generator not found: " + defaultValue);
		WorldGeneratorPlugin generator = generators.size() > 0 ? generators.get(0) : null;
		defaultWorldGenerators.put(worldType, generator);
		return generator;
	}

	protected final World createWorld(WorldConfig worldConfig, Runnable runWhenDone)
	{
		Game game = worldConfig.getGame();
		game.getGameMode().beforeWorldGeneration(game.getWorlds().size(), worldConfig);
		return plugin.worldManager.createWorld(worldConfig, runWhenDone);
	}
	
	protected abstract void setupWorld(WorldConfig world, Runnable runWhenDone);
}
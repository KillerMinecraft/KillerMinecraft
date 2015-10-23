package com.ftwinston.KillerMinecraft;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.World.Environment;

public abstract class WorldGenerator extends KillerModule
{
	static List<WorldGeneratorPlugin> overworldGenerators = new ArrayList<WorldGeneratorPlugin>();
	static List<WorldGeneratorPlugin> netherGenerators = new ArrayList<WorldGeneratorPlugin>();
	static List<WorldGeneratorPlugin> endGenerators = new ArrayList<WorldGeneratorPlugin>();
		
	static WorldGeneratorPlugin getDefault(Environment worldType)
	{
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
		
		for (WorldGeneratorPlugin generator : overworldGenerators)
			if (defaultValue.equalsIgnoreCase(generator.getName()))
				return generator;
		
		KillerMinecraft.instance.log.info("Default " + worldType.name() + " world generator not found: " + defaultValue);
		return generators.get(0);
	}

	protected final World createWorld(WorldConfig worldConfig, Runnable runWhenDone)
	{
		Game game = worldConfig.getGame();
		game.getGameMode().beforeWorldGeneration(game.getWorlds().size(), worldConfig);
		return plugin.worldManager.createWorld(worldConfig, runWhenDone);
	}
	
	protected abstract void setupWorld(WorldConfig world, Runnable runWhenDone);
}
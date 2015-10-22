package com.ftwinston.KillerMinecraft;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;

public abstract class WorldGenerator extends KillerModule
{
	static List<WorldGeneratorPlugin> worldGenerators = new ArrayList<WorldGeneratorPlugin>();
	static WorldGeneratorPlugin get(int num) { return worldGenerators.get(num); }
	static WorldGeneratorPlugin getByName(String name)
	{
		for ( WorldGeneratorPlugin plugin : worldGenerators )
			if ( name.equalsIgnoreCase(plugin.getName()) )
				return plugin;
		
		return null;
	}

	protected final World createWorld(WorldConfig worldConfig, Runnable runWhenDone)
	{
		Game game = worldConfig.getGame();
		game.getGameMode().beforeWorldGeneration(game.getWorlds().size(), worldConfig);
		return plugin.worldManager.createWorld(worldConfig, runWhenDone);
	}
	
	protected abstract void setupWorld(WorldConfig world, Runnable runWhenDone);
}
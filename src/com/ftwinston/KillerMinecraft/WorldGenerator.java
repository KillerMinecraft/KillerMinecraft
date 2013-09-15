package com.ftwinston.KillerMinecraft;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.World.Environment;

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
		
		World world = plugin.worldManager.createWorld(worldConfig, runWhenDone);
		game.getWorlds().add(world);
		return world;
	}
	
	final void createWorlds(Game game, Runnable runWhenDone)
	{
		final Environment[] environments = game.getGameMode().getWorldsToGenerate();
				
		for ( int i=environments.length-1; i>=0; i-- )
			runWhenDone = new WorldSetupRunner(game, environments[i], i, runWhenDone);
		
		runWhenDone.run();
	}
	
	protected abstract void setupWorld(WorldConfig world, Runnable runWhenDone);
	
	private class WorldSetupRunner implements Runnable
	{
		public WorldSetupRunner(Game game, Environment environment, int num, Runnable runNext)
		{
			this.game = game;
			this.environment = environment;
			this.num = num;
			this.runNext = runNext;
		}
	
		private Game game;
		private Environment environment;
		private int num;
		private Runnable runNext;
		
		public void run()
		{
			String worldName = Settings.killerWorldNamePrefix + "_" + (game.getNumber()+1) + "_" + num;
			final WorldConfig helper = new WorldConfig(game, worldName, environment);
			setupWorld(helper, runNext);
		}
	}
}
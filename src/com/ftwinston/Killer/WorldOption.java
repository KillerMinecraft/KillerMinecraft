package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class WorldOption
{
	static List<WorldOptionPlugin> worldOptions = new ArrayList<WorldOptionPlugin>();
	static WorldOptionPlugin get(int num) { return worldOptions.get(num); }
	
	final void initialize(Killer killer, WorldOptionPlugin optionPlugin)
	{
		plugin = killer;
		name = optionPlugin.getName();
		options = setupOptions();
	}
	
	private String name;
	public final String getName()
	{
		return name;
	}

	private Killer plugin;
	protected final JavaPlugin getPlugin() { return plugin; }
	
	protected final World createWorld(WorldConfig WorldConfig, Runnable runWhenDone)
	{
		World world = plugin.worldManager.createWorld(WorldConfig, runWhenDone);
		plugin.worldManager.worlds.add(world);
		return world;
	}
	
	final void createWorlds(Runnable runWhenDone)
	{
		final GameMode gameMode = plugin.getGameMode(); 
		final Environment[] environments = gameMode.getWorldsToGenerate();
		
		for ( int i=environments.length-1; i>=0; i-- )
			runWhenDone = new WorldSetupRunner(gameMode, environments[i], i, runWhenDone);
		
		runWhenDone.run();
	}
	
	protected abstract void setupWorld(WorldConfig world, Runnable runWhenDone);
	
	private class WorldSetupRunner implements Runnable
	{
		public WorldSetupRunner(GameMode gameMode, Environment environment, int num, Runnable runNext)
		{
			this.gameMode = gameMode;
			this.environment = environment;
			this.num = num;
			this.runNext = runNext;
		}
	
		private GameMode gameMode;
		private Environment environment;
		private int num;
		private Runnable runNext;
		
		public void run()
		{
			String worldName = Settings.killerWorldName + "_" + (num+1);
			
			WorldConfig helper = new WorldConfig(worldName, environment);
			
			ChunkGenerator generator = gameMode.getCustomChunkGenerator(num);
			if ( generator != null )
			{
				helper.setGenerator(generator);
				helper.lockChunkGenerator(); // don't let it be changed again, the game mode insists we use this
			}
			
			BlockPopulator[] populators = gameMode.getExtraBlockPopulators(num);
			if ( populators != null )
				for ( BlockPopulator populator : populators )
					helper.getExtraPopulators().add(populator);
			
			setupWorld(helper, runNext);
		}
	}
	
	private Option[] options;
	protected abstract Option[] setupOptions();
	public final Option[] getOptions() { return options; }
	public final Option getOption(int num) { return options[num]; }
	public final int getNumOptions() { return options.length; }
	
	public void toggleOption(int num)
	{
		Option option = options[num];
		option.setEnabled(!option.isEnabled());
	}
}
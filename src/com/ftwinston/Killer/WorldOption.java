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
	Killer plugin; Game game;
	
	final void initialize(Game game, WorldOptionPlugin optionPlugin)
	{
		this.game = game;
		plugin = game.plugin;
		name = optionPlugin.getName();
		options = setupOptions();
	}
	
	private String name;
	public final String getName()
	{
		return name;
	}

	protected final JavaPlugin getPlugin() { return plugin; }
	
	protected final World createWorld(WorldConfig worldConfig, Runnable runWhenDone)
	{
		World world = plugin.worldManager.createWorld(worldConfig, runWhenDone);
		worldConfig.getGame().getWorlds().add(world);
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
			String worldName = Settings.killerWorldName + "_" + (num+1);
			
			WorldConfig helper = new WorldConfig(game, worldName, environment);
			
			ChunkGenerator generator = game.getGameMode().getCustomChunkGenerator(num);
			if ( generator != null )
			{
				helper.setGenerator(generator);
				helper.lockChunkGenerator(); // don't let it be changed again, the game mode insists we use this
			}
			
			BlockPopulator[] populators = game.getGameMode().getExtraBlockPopulators(num);
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
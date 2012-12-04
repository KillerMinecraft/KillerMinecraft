package com.ftwinston.Killer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import com.ftwinston.Killer.WorldOptions.CopyExistingWorld;
import com.ftwinston.Killer.WorldOptions.DefaultWorld;
import com.ftwinston.Killer.WorldOptions.LavaLand;
import com.ftwinston.Killer.WorldOptions.LotsaTraps;
import com.ftwinston.Killer.WorldOptions.Superflat;

public abstract class WorldOption
{
	static List<WorldOption> options = new ArrayList<WorldOption>();
	static WorldOption get(int num) { return options.get(num); }
	
	static void setup(Killer plugin)
	{
		for ( int i=0; i<Settings.customWorldNames.size(); i++ )
		{
			String name = Settings.customWorldNames.get(i); 
			// check the corresponding folder exists for each of these. Otherwise, delete
			File folder = new File(plugin.getServer().getWorldContainer() + File.separator + name);
			if ( name.length() > 0 && folder.exists() && folder.isDirectory() )
				continue;
			
			Settings.customWorldNames.remove(i);
			i--;
		}
		
		if ( Settings.customWorldNames.size() == 0 )
			Settings.allowRandomWorlds = true;
		
		if ( Settings.allowRandomWorlds )
		{
			options.add(new DefaultWorld());
			options.add(new Superflat());
			
			options.add(new LavaLand());
			options.add(new LotsaTraps());
		}
		
		for ( String name : Settings.customWorldNames )
		{
			options.add(new CopyExistingWorld(name));
		}
		
		for ( WorldOption option : options )
			option.plugin = plugin;
		
		for ( Game game : plugin.games )
			game.setWorldOption(options.get(0));
	}
	
	protected WorldOption(String name)
	{
		this.name = name;
	}
	
	private String name;
	public final String getName()
	{
		return name;
	}

	private Killer plugin;
	protected final JavaPlugin getPlugin() { return plugin; }
	
	protected final World createWorld(WorldHelper worldHelper, Runnable runWhenDone)
	{
		World world = plugin.worldManager.createWorld(worldHelper, runWhenDone);
		plugin.worldManager.worlds.add(world);
		return world;
	}
	
	final void createWorlds(final Game game, final Runnable runWhenDone)
	{
		final Environment[] environments = game.getGameMode().getWorldsToGenerate();
		
		for ( int i=environments.length-1; i>=0; i-- )
			runWhenDone = new WorldSetupRunner(game, environments[i], i, runWhenDone);
		
		runWhenDone.run();
	}
	
	protected abstract void setupWorld(WorldHelper world, Runnable runWhenDone);
	
	public abstract boolean isFixedWorld();
	
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
			
			WorldHelper helper = new WorldHelper(game, worldName, environment);
			
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
}
package com.ftwinston.Killer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.generator.BlockPopulator;
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
	
	static void setup(Killer killer)
	{
		for ( int i=0; i<Settings.customWorldNames.size(); i++ )
		{
			String name = Settings.customWorldNames.get(i); 
			// check the corresponding folder exists for each of these. Otherwise, delete
			File folder = new File(killer.getServer().getWorldContainer() + File.separator + name);
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
			option.plugin = killer;
		
		killer.setWorldOption(options.get(0));
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
	
	final void createWorlds(Runnable runWhenDone)
	{
		final GameMode gameMode = plugin.getGameMode(); 
		final Environment[] environments = gameMode.getWorldsToGenerate();
		
		for ( int i=environments.length-1; i>=0; i-- )
			runWhenDone = new WorldSetupRunner(gameMode, environments[i], i, runWhenDone);
		
		runWhenDone.run();
	}
	
	protected abstract void setupWorld(WorldHelper world, Runnable runWhenDone);
	
	public abstract boolean isFixedWorld();
	
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
			
			WorldHelper helper = new WorldHelper(worldName, environment);
			helper.setGenerator(gameMode.getCustomChunkGenerator(num));
			
			BlockPopulator[] populators = gameMode.getExtraBlockPopulators(num);
			if ( populators != null )
				for ( BlockPopulator populator : populators )
					helper.getExtraPopulators().add(populator);
			
			setupWorld(helper, runNext);
		}
	}
}
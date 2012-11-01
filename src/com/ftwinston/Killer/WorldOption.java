package com.ftwinston.Killer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;

import com.ftwinston.Killer.WorldOptions.CopyExistingWorld;
import com.ftwinston.Killer.WorldOptions.DefaultWorld;
import com.ftwinston.Killer.WorldOptions.LavaLand;
import com.ftwinston.Killer.WorldOptions.Superflat;

public abstract class WorldOption
{
	public static List<WorldOption> options = new ArrayList<WorldOption>();
	public static WorldOption get(int num) { return options.get(num); }
	
	public static void setup(Killer killer)
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
	
	protected String name;
	public String getName()
	{
		return name;
	}

	protected Killer plugin;
	
	public final void createWorlds(final Runnable runWhenDone)
	{
		Runnable doneMainWorld = plugin.getGameMode().usesNether() ? new Runnable()
		{
			public void run()
			{
				createNetherWorld(Settings.killerWorldName + "_nether", runWhenDone);
			}
		} : runWhenDone;
		
		createMainWorld(Settings.killerWorldName, doneMainWorld);
	}
	
	protected abstract void createMainWorld(String name, Runnable runWhenDone);
	
	protected void createNetherWorld(String name, Runnable runWhenDone)
	{
		WorldCreator wc = new WorldCreator(name).environment(Environment.NETHER);
		plugin.worldManager.netherWorld = plugin.worldManager.createWorld(wc, runWhenDone);
	}
	
	public abstract boolean isFixedWorld();
}
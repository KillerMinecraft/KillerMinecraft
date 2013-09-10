package com.ftwinston.Killer;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class WorldGeneratorPlugin extends JavaPlugin
{
	public void onEnable()
	{
		Killer.registerWorldGenerator(this);
	}
	
	public abstract WorldGenerator createInstance();
	
	final void initialize(Killer plugin)
	{
		// keep the world options in alphabetic order
		String name = getName();
		for ( int i=0; i<WorldGenerator.worldGenerators.size(); i++ )
			if ( name.compareToIgnoreCase(WorldGenerator.worldGenerators.get(i).getName()) < 0 )
			{
				WorldGenerator.worldGenerators.add(i, this);
				return;
			}
		WorldGenerator.worldGenerators.add(this);
	}
}
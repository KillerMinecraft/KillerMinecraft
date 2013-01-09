package com.ftwinston.Killer;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class WorldOptionPlugin extends JavaPlugin
{
	public abstract WorldOption createInstance();
	
	final void initialize(Killer plugin)
	{
		String name = getName();
		if ( WorldOption.worldOptions.size() == 0 || name.equals(Settings.defaultWorldOption) )
		{
			defaultWorld = this;
			for ( Game game : plugin.games )
				game.setWorldOption(this);
		}
				
		// keep the world options in alphabetic order
		for ( int i=0; i<WorldOption.worldOptions.size(); i++ )
			if ( name.compareToIgnoreCase(WorldOption.worldOptions.get(i).getName()) < 0 )
			{
				WorldOption.worldOptions.add(i, this);
				return;
			}
		WorldOption.worldOptions.add(this);
	}
	
	private static WorldOptionPlugin defaultWorld;
	static WorldOptionPlugin getDefault() { return defaultWorld; }
}
package com.ftwinston.Killer;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class WorldOptionPlugin extends JavaPlugin
{
	public abstract WorldOption createInstance();
	
	final void initialize(Killer killer)
	{
		String name = getName();
		if ( WorldOption.worldOptions.size() == 0 || name.equals(Settings.defaultWorldOption) )
			killer.setWorldOption(this);
				
		// keep the world options in alphabetic order
		for ( int i=0; i<WorldOption.worldOptions.size(); i++ )
			if ( name.compareToIgnoreCase(WorldOption.worldOptions.get(i).getName()) < 0 )
			{
				WorldOption.worldOptions.add(i, this);
				return;
			}
		WorldOption.worldOptions.add(this);
	}
}
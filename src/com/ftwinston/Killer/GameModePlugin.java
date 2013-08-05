package com.ftwinston.Killer;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class GameModePlugin extends JavaPlugin
{
	public abstract GameMode createInstance();
	public abstract String[] getSignDescription();
	
	final void initialize(Killer plugin)
	{	
		// keep the game modes in alphabetic order
		String name = getName();
		for ( int i=0; i<GameMode.gameModes.size(); i++ )
			if ( name.compareToIgnoreCase(GameMode.gameModes.get(i).getName()) < 0 )
			{
				GameMode.gameModes.add(i, this);
				return;
			}
		GameMode.gameModes.add(this);
	}
}
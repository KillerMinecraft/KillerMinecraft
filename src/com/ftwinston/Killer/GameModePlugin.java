package com.ftwinston.Killer;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class GameModePlugin extends JavaPlugin
{
	public abstract GameMode createInstance();
	public abstract String[] getSignDescription();
	
	final void initialize(Killer killer)
	{
		String name = getName();
		if ( GameMode.gameModes.size() == 0 || name.equals(Settings.defaultGameMode) )
			killer.setGameMode(this);
				
		// keep the game modes in alphabetic order
		for ( int i=0; i<GameMode.gameModes.size(); i++ )
			if ( name.compareToIgnoreCase(GameMode.gameModes.get(i).getName()) < 0 )
			{
				GameMode.gameModes.add(i, this);
				return;
			}
		GameMode.gameModes.add(this);
	}
}
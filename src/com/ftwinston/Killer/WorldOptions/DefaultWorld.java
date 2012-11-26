package com.ftwinston.Killer.WorldOptions;

import com.ftwinston.Killer.Game;

import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;

public class DefaultWorld extends com.ftwinston.Killer.WorldOption
{
	public DefaultWorld()
	{
		super("Normal");
	}
	
	public boolean isFixedWorld() { return false; }
	
	public void createMainWorld(Game game, String name, Runnable runWhenDone)
	{
		WorldCreator wc = new WorldCreator(name).environment(Environment.NORMAL);
		game.setMainWorld(createWorld(wc, runWhenDone));
	}
}

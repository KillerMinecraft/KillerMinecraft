package com.ftwinston.Killer.WorldOptions;

import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import com.ftwinston.Killer.Game;

public class Superflat extends com.ftwinston.Killer.WorldOption
{
	public Superflat()
	{
		super("Superflat (difficult)");
	}
	
	public boolean isFixedWorld() { return false; }
	
	public void createMainWorld(Game game, String name, Runnable runWhenDone)
	{
		WorldCreator wc = new WorldCreator(name).environment(Environment.NORMAL).type(WorldType.FLAT);
		game.setMainWorld(createWorld(wc, runWhenDone));
	}
}

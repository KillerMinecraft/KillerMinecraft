package com.ftwinston.Killer.WorldOptions;

import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

public class Superflat extends com.ftwinston.Killer.WorldOption
{
	public Superflat()
	{
		super("Superflat (difficult)");
	}
	
	public boolean isFixedWorld() { return false; }
	
	public void createMainWorld(String name, Runnable runWhenDone)
	{
		WorldCreator wc = new WorldCreator(name).environment(Environment.NORMAL).type(WorldType.FLAT);
		setMainWorld(createWorld(wc, runWhenDone));
	}
}

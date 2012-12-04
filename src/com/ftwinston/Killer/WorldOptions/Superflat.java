package com.ftwinston.Killer.WorldOptions;

import org.bukkit.World.Environment;
import org.bukkit.WorldType;

import com.ftwinston.Killer.WorldHelper;

public class Superflat extends com.ftwinston.Killer.WorldOption
{
	public Superflat()
	{
		super("Superflat (difficult)");
	}
	
	public boolean isFixedWorld() { return false; }
	
	@Override
	public void setupWorld(WorldHelper world, Runnable runWhenDone)
	{
		if ( world.getEnvironment() == Environment.NORMAL )
			world.setWorldType(WorldType.FLAT);
		
		createWorld(world, runWhenDone);
	}
}

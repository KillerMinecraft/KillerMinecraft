package com.ftwinston.Killer.WorldOptions;

import com.ftwinston.Killer.WorldHelper;

public class DefaultWorld extends com.ftwinston.Killer.WorldOption
{
	public DefaultWorld()
	{
		super("Normal");
	}
	
	@Override
	public void setupWorld(WorldHelper world, Runnable runWhenDone)
	{	
		createWorld(world, runWhenDone);
	}
	
	public boolean isFixedWorld() { return false; }
}

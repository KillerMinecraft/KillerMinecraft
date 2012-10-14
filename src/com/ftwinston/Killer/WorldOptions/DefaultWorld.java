package com.ftwinston.Killer.WorldOptions;

import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;

import com.ftwinston.Killer.Settings;

public class DefaultWorld extends com.ftwinston.Killer.WorldOption
{
	public DefaultWorld()
	{
		super("Normal");
	}
	
	public boolean isFixedWorld() { return false; }
	
	public void create(Runnable runWhenDone)
	{
		WorldCreator wc = new WorldCreator(Settings.killerWorldName).environment(Environment.NORMAL);
		plugin.worldManager.mainWorld = plugin.getServer().createWorld(wc);
		
		plugin.worldManager.netherWorld = plugin.getServer().createWorld(new WorldCreator(Settings.killerWorldName + "_nether").environment(Environment.NETHER));
		
		runWhenDone.run();
	}
}

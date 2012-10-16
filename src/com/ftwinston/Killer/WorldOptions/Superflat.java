package com.ftwinston.Killer.WorldOptions;

import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import com.ftwinston.Killer.Settings;

public class Superflat extends com.ftwinston.Killer.WorldOption
{
	public Superflat()
	{
		super("Superflat (difficult)");
	}
	
	public boolean isFixedWorld() { return false; }
	
	public void create(Runnable runWhenDone)
	{
		WorldCreator wc = new WorldCreator(Settings.killerWorldName).environment(Environment.NORMAL).type(WorldType.FLAT);
		plugin.worldManager.mainWorld = plugin.getServer().createWorld(wc);
		
		plugin.worldManager.netherWorld = plugin.getServer().createWorld(new WorldCreator(Settings.killerWorldName + "_nether").environment(Environment.NETHER));
		
		runWhenDone.run();
	}
}

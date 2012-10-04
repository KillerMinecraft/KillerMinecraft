package com.ftwinston.Killer.WorldOptions;

import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;

import com.ftwinston.Killer.Settings;

public class DefaultWorld extends com.ftwinston.Killer.WorldOption
{
	public DefaultWorld(String name)
	{
		super(name);
	}
	
	public void create()
	{
		plugin.worldManager.mainWorld = plugin.getServer().createWorld(new WorldCreator(Settings.killerWorldName).environment(Environment.NORMAL));
		plugin.worldManager.netherWorld = plugin.getServer().createWorld(new WorldCreator(Settings.killerWorldName + "_nether").environment(Environment.NETHER));
	}
}

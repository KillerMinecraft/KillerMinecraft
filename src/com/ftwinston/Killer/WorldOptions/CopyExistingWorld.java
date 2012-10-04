package com.ftwinston.Killer.WorldOptions;

import java.io.File;
import java.io.IOException;

import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;

import com.ftwinston.Killer.Settings;
import com.google.common.io.Files;

public class CopyExistingWorld extends com.ftwinston.Killer.WorldOption
{
	public CopyExistingWorld(String name)
	{
		super(name);
	}
	
	public void create()
	{		
		File sourceDir = new File(plugin.getServer().getWorldContainer() + File.separator + name);
		File targetDir = new File(plugin.getServer().getWorldContainer() + File.separator + Settings.killerWorldName);
		
		try
		{
			Files.copy(sourceDir, targetDir);
		}
		catch (IOException ex)
		{
			plugin.log.warning("An error occurred copying the " + name + " world");
		}
		
		sourceDir = new File(plugin.getServer().getWorldContainer() + File.separator + name + "_nether");
		targetDir = new File(plugin.getServer().getWorldContainer() + File.separator + Settings.killerWorldName + "_nether");
		
		if ( sourceDir.exists() && sourceDir.isDirectory() )
			try
			{
				Files.copy(sourceDir, targetDir);
			}
			catch (IOException ex)
			{
				plugin.log.warning("An error occurred copying the " + name + " world");
			}
		
		plugin.worldManager.mainWorld = plugin.getServer().createWorld(new WorldCreator(Settings.killerWorldName).environment(Environment.NORMAL));
		plugin.worldManager.netherWorld = plugin.getServer().createWorld(new WorldCreator(Settings.killerWorldName + "_nether").environment(Environment.NETHER));
	}
}

package com.ftwinston.Killer.WorldOptions;

import java.util.Random;

import org.bukkit.World.Environment;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.Listener;
import org.bukkit.generator.BlockPopulator;

public class LotsaTraps extends com.ftwinston.Killer.WorldOption implements Listener
{
	public LotsaTraps()
	{
		super("Lotsa Traps");
	}
	
	public boolean isFixedWorld() { return false; }
	
	public void createMainWorld(String name, Runnable runWhenDone)
	{
		WorldCreator wc = new WorldCreator(name).environment(Environment.NORMAL);
		registerWorld(createWorld(wc, runWhenDone, new TrapPopulator()));
	}
	
	public class TrapPopulator extends BlockPopulator
	{
		public void populate(World world, Random random, Chunk chunk)
		{
			chunk.getBlock(8, 72, 8).setType(Material.GLOWSTONE);
		}
	}
}

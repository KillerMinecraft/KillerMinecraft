package com.ftwinston.Killer.WorldOptions;

import java.util.Random;

import org.bukkit.World.Environment;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.generator.BlockPopulator;

import com.ftwinston.Killer.WorldHelper;

public class LotsaTraps extends com.ftwinston.Killer.WorldOption implements Listener
{
	public LotsaTraps()
	{
		super("Lotsa Traps");
	}
	
	public boolean isFixedWorld() { return false; }
	
	@Override
	public void setupWorld(WorldHelper world, Runnable runWhenDone)
	{
		if ( world.getEnvironment() == Environment.NORMAL )
			world.getExtraPopulators().add(new TrapPopulator());
		
		createWorld(world, runWhenDone);
	}
		
	public class TrapPopulator extends BlockPopulator
	{
		public void populate(World world, Random random, Chunk chunk)
		{
			chunk.getBlock(8, 72, 8).setType(Material.GLOWSTONE);
		}
	}
}

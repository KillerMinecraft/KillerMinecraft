package com.ftwinston.Killer.WorldOptions;

import java.util.Random;

import org.bukkit.World.Environment;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.generator.BlockPopulator;

import com.ftwinston.Killer.Option;
import com.ftwinston.Killer.WorldConfig;

public class LotsaTraps extends com.ftwinston.Killer.WorldOption implements Listener
{
	@Override
	public Option[] setupOptions()
	{
		return new Option[0];
	}
	
	@Override
	public void setupWorld(WorldConfig world, Runnable runWhenDone)
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

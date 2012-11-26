package com.ftwinston.Killer.WorldOptions;

import java.util.Random;

import org.bukkit.World.Environment;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Listener;
import org.bukkit.generator.BlockPopulator;

import com.ftwinston.Killer.Game;

public class LavaLand extends com.ftwinston.Killer.WorldOption implements Listener
{
	public LavaLand()
	{
		super("Lava Land");
	}
	
	public boolean isFixedWorld() { return false; }
	
	public void createMainWorld(Game game, String name, Runnable runWhenDone)
	{
		WorldCreator wc = new WorldCreator(name).environment(Environment.NORMAL);
		game.setMainWorld(createWorld(wc, runWhenDone, new LavaSeaPopulator(), new ExtraLavaPopulator()));
	}
	
	public class LavaSeaPopulator extends BlockPopulator
	{
		public void populate(World world, Random random, Chunk chunk)
		{
			// replace the sea with lava ... in two passes, so it doesn't all turn to obsidian
			int seaLevel = chunk.getWorld().getSeaLevel();
			for ( int x=0; x<16; x++ )
				for ( int z=0; z<16; z++ )
					for ( int y=seaLevel; y>0; y-- )
					{
						Block b = chunk.getBlock(x,y,z);
						if ( b.getType() == Material.STATIONARY_WATER || b.getType() == Material.WATER || b.getType() == Material.ICE )
							b.setType(Material.OBSIDIAN);
						else if ( b.getType() != Material.OBSIDIAN && b.getType() != Material.AIR )
							break;
					}
			
			for ( int x=0; x<16; x++ )
				for ( int z=0; z<16; z++ )
					for ( int y=seaLevel; y>0; y-- )
					{
						Block b = chunk.getBlock(x,y,z);
						if ( b.getType() == Material.OBSIDIAN )
							b.setType(Material.STATIONARY_LAVA);
						else if ( b.getType() != Material.AIR )
							break;
					}
			
			// without now looking at adjacent (GENERATED) chunks, we'd end up with the edges of ocean chunks being obsidian
			
			int otherX = chunk.getX() - 1, otherZ = chunk.getZ();
			if ( world.isChunkLoaded(otherX, otherZ) )
			{
				Chunk other = world.getChunkAt(otherX, otherZ);
				for ( int z=0; z<16; z++ )
					for ( int y=seaLevel; y>0; y-- )
					{
						Block b = other.getBlock(15, y, z);
						if ( b.getType() == Material.OBSIDIAN || b.getType() == Material.STATIONARY_WATER || b.getType() == Material.WATER || b.getType() == Material.ICE )
							b.setType(Material.STATIONARY_LAVA);
						else if ( b.getType() != Material.AIR )
							break;
					}
			}
			
			otherX = chunk.getX() + 1; otherZ = chunk.getZ();
			if ( world.isChunkLoaded(otherX, otherZ) )
			{
				Chunk other = world.getChunkAt(otherX, otherZ);
				for ( int z=0; z<16; z++ )
					for ( int y=seaLevel; y>0; y-- )
					{
						Block b = other.getBlock(0, y, z);
						if ( b.getType() == Material.OBSIDIAN || b.getType() == Material.STATIONARY_WATER || b.getType() == Material.WATER || b.getType() == Material.ICE )
							b.setType(Material.STATIONARY_LAVA);
						else if ( b.getType() != Material.AIR )
							break;
					}
			}
			
			otherX = chunk.getX(); otherZ = chunk.getZ() - 1;
			if ( world.isChunkLoaded(otherX, otherZ) )
			{
				Chunk other = world.getChunkAt(otherX, otherZ);
				for ( int x=0; x<16; x++ )
					for ( int y=seaLevel; y>0; y-- )
					{
						Block b = other.getBlock(x, y, 15);
						if ( b.getType() == Material.OBSIDIAN || b.getType() == Material.STATIONARY_WATER || b.getType() == Material.WATER || b.getType() == Material.ICE )
							b.setType(Material.STATIONARY_LAVA);
						else if ( b.getType() != Material.AIR )
							break;
					}
			}
			
			otherX = chunk.getX(); otherZ = chunk.getZ() + 1;
			if ( world.isChunkLoaded(otherX, otherZ) )
			{
				Chunk other = world.getChunkAt(otherX, otherZ);
				for ( int x=0; x<16; x++ )
					for ( int y=seaLevel; y>0; y-- )
					{
						Block b = other.getBlock(x, y, 0);
						if ( b.getType() == Material.OBSIDIAN || b.getType() == Material.STATIONARY_WATER || b.getType() == Material.WATER || b.getType() == Material.ICE )
							b.setType(Material.STATIONARY_LAVA);
						else if ( b.getType() != Material.AIR )
							break;
					}
			}
		}
	}
	
	public class ExtraLavaPopulator extends BlockPopulator
	{
		public void populate(World world, Random random, Chunk chunk)
		{			
			if ( random.nextDouble() < 0.33 )
				return; // 2/3 chance of adding extra lava to a chunk
			
			// pick a random point in the chunk. trace downwards through the air until we hit something.
			// If it's not leaves or log (we're avoiding those, to avoid massive fire spread), or liquid (don't do it on the ocean), create lava above it
			int x = random.nextInt(16); int z = random.nextInt(16);
			for ( int y=128; y>0; y-- )
			{
				Block b = chunk.getBlock(x,y,z); 
				if ( b.getType() != Material.AIR && b.getType() != Material.SNOW) // snow lying on top of trees shouldn't change anything
				{
					if ( b.getType() != Material.LEAVES && b.getType() != Material.LOG && !b.isLiquid())
						 b.getRelative(BlockFace.UP).setType(Material.LAVA);
					break;
				}
			}
		}
	}
}

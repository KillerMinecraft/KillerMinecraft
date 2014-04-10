package com.ftwinston.KillerMinecraft;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.generator.BlockPopulator;

class StagingWorldGenerator extends org.bukkit.generator.ChunkGenerator
{
	private static final int groundMinY = 32, groundMaxY = 64, groundMinX = -22, groundMaxX = 22, groundMinZ = -22, groundMaxZ = 23;
	
	@Override
    public List<BlockPopulator> getDefaultPopulators(World world)
	{
        return Arrays.asList((BlockPopulator)new StagingWorldPopulator());
    }
	
    @Override
    public boolean canSpawn(World world, int x, int z)
	{
        return x >= -1 && x < 1 && z >= -1 && z < 1;
    }
    
	public byte[][] generateBlockSections(World world, Random random, int cx, int cz, BiomeGrid biomes)
	{
		return new byte[1][];
	}
	
	public class StagingWorldPopulator extends org.bukkit.generator.BlockPopulator
	{		
		public Block getBlockAbs(Chunk chunk, int absX, int y, int absZ)
		{
			int chunkX = absX - chunk.getX() * 16, chunkZ = absZ - chunk.getZ() * 16;
			
			if ( chunkX >= 0 && chunkX < 16 && chunkZ >= 0 && chunkZ < 16 )
				return chunk.getBlock(chunkX, y, chunkZ);
			return null;
		}
		
		public void populate(World world, Random random, Chunk chunk)
		{
			if ( chunk.getX() >= 2 || chunk.getX() < -2 || chunk.getZ() >= 2 || chunk.getZ() < -2 )
				return;
			
			Block b;
			final int rSquared = (groundMaxX+2) * (groundMaxX+2);
			
			for ( int x = groundMinX; x <= groundMaxX; x++ )
				for ( int z = groundMinZ; z <= groundMaxZ; z++ )
				{
					for ( int y = groundMinY; y < groundMaxY; y++ )
					{
						if ( !insideSphere(x, y, z, 0, groundMaxY+10, 0, rSquared) )
							continue;
						
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(Material.DIRT);
					}
					
					if ( !insideSphere(x,groundMaxY,z, 0, groundMaxY+10, 0, rSquared) )
						continue;
					b = getBlockAbs(chunk, x, groundMaxY, z);
					if ( b != null )
						b.setType(Material.GRASS);
				}
			
			// do a weird circly biome pattern
			int[] data = null; Location treeLoc = null;
			int cx = chunk.getX(), cz = chunk.getZ();
			
			if ( cx == -1 && cz == -1 )
			{
				data = new int[] { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,2,0,0,0,0,0,0,0,0,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,1,0,2,2,0,0,0,0,0,0,0,0,0,0,1,1,1,0,2,2,0,0,0,0,0,0,0,0,0,0,1,1,1,0,2,2,0,0,0,0,0,0,0,0,0,1,1,1,1 };
				treeLoc = new Location(world, -13, groundMaxY+1, -13);
			}
			else if ( cx == 0 && cz == -1 )
			{
				data = new int[] { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,1,1,0,0,0,0,0,0,0,0,0,0,0,2,2,0,1,1,1,1,0,0,0,0,0,0,0,0,0,0,2,2,1,1,1,1,0,0,0,0,0,0,0,0,0,0,2,2,1,1,1,1,1,0,0,0,0,0,0,0,0,0,2,2 };
				treeLoc = new Location(world, 13, groundMaxY+1, -13);
			}
			else if ( cx == -1 && cz == 0 )
			{
				data = new int[] { 0,2,2,0,0,0,0,0,0,0,0,0,1,1,1,1,0,2,2,0,0,0,0,0,0,0,0,0,1,1,1,1,0,2,2,0,0,0,0,0,0,0,0,0,0,1,1,1,0,2,2,0,0,0,0,0,0,0,0,0,0,1,1,1,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,1,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2 };
				treeLoc = new Location(world, -13, groundMaxY+1, 13);
			}
			else if ( cx == 0 && cz == 0 )
			{
				data = new int[] { 0,1,1,1,1,0,0,0,0,0,0,0,0,0,2,2,1,1,1,1,1,0,0,0,0,0,0,0,0,0,2,2,1,1,1,1,0,0,0,0,0,0,0,0,0,0,2,2,1,1,1,1,0,0,0,0,0,0,0,0,0,0,2,2,1,1,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,0,0,0,0,0,0,0,2,2,2,2,2,2,2,0,0,0,0,0,0,0,0,0,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0 };
				treeLoc = new Location(world, 13, groundMaxY+1, 13);

				b = getBlockAbs(chunk, 0, groundMaxY+1, 0);
				b.setType(Material.TORCH);
				
				b = getBlockAbs(chunk, 0, groundMaxY+1, 4);
				setupFloorSign(b, BlockFace.SOUTH, "", "type this:", "/killer");	
			}
			else if ( cx == 0 && cz == -2 )
				treeLoc = new Location(world, 0, groundMaxY+1, -18);
			else if ( cx == 0 && cz == 1 )
				treeLoc = new Location(world, 0, groundMaxY+1, 18);
			else if ( cx == -2 && cz == 0 )
				treeLoc = new Location(world, -18, groundMaxY+1, 0);
			else if ( cx == 1 && cz == 0 )
				treeLoc = new Location(world, 18, groundMaxY+1, 0);

			if ( treeLoc != null )
			{
				world.generateTree(treeLoc, TreeType.TALL_REDWOOD);
				world.getBlockAt(treeLoc).getRelative(BlockFace.DOWN).setType(Material.WATER);
			}
			
			if ( data == null )
				for ( int z=0; z<16; z++ )
					for ( int x=0; x<16; x++ )
						world.setBiome(chunk.getX() * 16 + x, chunk.getZ() * 16 + z, Biome.PLAINS);
			else
			{
				Biome[] biomes = new Biome[] { Biome.PLAINS, Biome.SWAMPLAND, Biome.DESERT };
				for ( int z=0; z<16; z++ )
					for ( int x=0; x<16; x++ )
						world.setBiome(chunk.getX() * 16 + x, chunk.getZ() * 16 + z,  biomes[data[x + z * 16]]);
			}
		}

		private boolean insideSphere(int x, int y, int z, int x0, int y0, int z0, int rSquared)
		{
			return (x-x0)*(x-x0) + (y-y0)*(y-y0) + (z-z0)*(z-z0) <= rSquared;
		}
	}
	
	public static void setupFloorSign(Block b, BlockFace orientation, String... lines)
	{
		b.setType(Material.SIGN_POST);
		org.bukkit.block.Sign state = (org.bukkit.block.Sign)b.getState();
		
		org.bukkit.material.Sign data = (org.bukkit.material.Sign)state.getData();
		data.setFacingDirection(orientation);
		state.setData(data);
		
		for ( int i=0; i<4 && i<lines.length; i++ )
			state.setLine(i, lines[i]);
		for ( int i=lines.length; i<4; i++ )
			state.setLine(i, "");
		state.update(true);
	}
}
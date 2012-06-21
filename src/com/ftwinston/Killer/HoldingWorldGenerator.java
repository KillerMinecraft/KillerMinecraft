package com.ftwinston.Killer;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;

public class HoldingWorldGenerator extends org.bukkit.generator.ChunkGenerator
{
	public byte[][] generateBlockSections(World world, Random random, int cx, int cz, BiomeGrid biomes)
	{
		byte[][] result = new byte[1][];
		if ( cx == 0 && cz == 0)
		{
			for ( int x = 0; x < 16; x++ )
				for ( int z = 0; z < 16; z++ )
					setBlock(result, x, 0, z, bedrock);

			for ( int y=1; y<15; y++ )
			{
				for ( int x = 0; x < 16; x++ )
				{
					setBlock(result, x, y, 0, bedrock);
					setBlock(result, x, y, 15, bedrock);
				}
				for ( int z = 0; z < 16; z++ )
				{
					setBlock(result, 0, y, z, bedrock);
					setBlock(result, 15, y, z, bedrock);
				}
			}
			
			setBlock(result, 1, 15, 1, glowstone);
			setBlock(result, 1, 15, 14, glowstone);
			setBlock(result, 14, 15, 1, glowstone);
			setBlock(result, 14, 15, 14, glowstone);
		}
		return result;
	}
	
	final byte bedrock = 7, glowstone = 89;
	private void setBlock(byte[][] result, int x, int y, int z, byte blkid)
	{
        if (result[y >> 4] == null)
            result[y >> 4] = new byte[4096];
            
        result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
    }
	
	public Location getFixedSpawnLocation(World world, Random random)
	{
		return new Location(world, 8, 1, 8);
	}
}

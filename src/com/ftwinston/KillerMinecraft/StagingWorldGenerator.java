package com.ftwinston.KillerMinecraft;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.generator.BlockPopulator;

class StagingWorldGenerator extends org.bukkit.generator.ChunkGenerator
{
	private static final int groundMinY = 32, groundMaxY = 64, groundMinX = -22, groundMaxX = 22, groundMinZ = -22, groundMaxZ = 23;
	private static final int game1WallX = 10, game2WallX = -10, gameWallMaxY = 68; 
	private static final int game1ButtonX = 9, game2ButtonX = -9, gameButtonY = 66;
	
	@Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
		world.setSpawnLocation(0, 65, 0);
        return Arrays.asList((BlockPopulator)new StagingWorldPopulator(this));
    }
	
    @Override
    public boolean canSpawn(World world, int x, int z) {
        return x >= -1 && x < 1 && z >= -1 && z < 1;
    }
    
	public byte[][] generateBlockSections(World world, Random random, int cx, int cz, BiomeGrid biomes)
	{
		return new byte[1][];
	}
	
	public class StagingWorldPopulator extends org.bukkit.generator.BlockPopulator
	{
		StagingWorldGenerator gen;
		
		public StagingWorldPopulator(StagingWorldGenerator gen)
		{
			this.gen = gen;
		}
		
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
			
			for ( int y=groundMaxY+1; y <= gameWallMaxY; y++ )
			{
				for ( int z = -1; z <= 1; z++ )
				{	
					b = getBlockAbs(chunk, game1WallX, y, z);
					if ( b != null )
					{
						b.setType(Material.QUARTZ_BLOCK);
						b.setData((byte)1);
					}
				
					b = getBlockAbs(chunk, game2WallX, y, z);
					if ( b != null )
					{
						b.setType(Material.QUARTZ_BLOCK);
						b.setData((byte)1);
					}
				}
				
				int[] zs = new int[] { -5, -2, 2, 5 };
				
				for ( int iz = 0; iz < zs.length; iz++ )
				{	
					b = getBlockAbs(chunk, game1WallX, y, zs[iz]);
					if ( b != null )
					{
						b.setType(Material.QUARTZ_BLOCK);
						b.setData((byte)2);
					}
				
					b = getBlockAbs(chunk, game2WallX, y, zs[iz]);
					if ( b != null )
					{
						b.setType(Material.QUARTZ_BLOCK);
						b.setData((byte)2);
					}
				}
			}
			
			for ( int y=groundMaxY+1; y <= groundMaxY+3; y++ )
			{
				b = getBlockAbs(chunk, game1WallX, y, -6);
				if ( b != null )
					b.setType(Material.QUARTZ_BLOCK);
			
				b = getBlockAbs(chunk, game1WallX, y, 6);
				if ( b != null )
					b.setType(Material.QUARTZ_BLOCK);
				
				b = getBlockAbs(chunk, game2WallX, y, -6);
				if ( b != null )
					b.setType(Material.QUARTZ_BLOCK);
			
				b = getBlockAbs(chunk, game2WallX, y, 6);
				if ( b != null )
					b.setType(Material.QUARTZ_BLOCK);
			}
	
			for ( int y=groundMaxY+1; y <= groundMaxY+2; y++ )
			{
				b = getBlockAbs(chunk, game1WallX, y, -7);
				if ( b != null )
					b.setType(Material.QUARTZ_BLOCK);
			
				b = getBlockAbs(chunk, game1WallX, y, 7);
				if ( b != null )
					b.setType(Material.QUARTZ_BLOCK);
				
				b = getBlockAbs(chunk, game2WallX, y, -7);
				if ( b != null )
					b.setType(Material.QUARTZ_BLOCK);
			
				b = getBlockAbs(chunk, game2WallX, y, 7);
				if ( b != null )
					b.setType(Material.QUARTZ_BLOCK);
			}
	
			b = getBlockAbs(chunk, game1WallX, groundMaxY+1, -8);
			if ( b != null )
				b.setType(Material.QUARTZ_BLOCK);
		
			b = getBlockAbs(chunk, game1WallX, groundMaxY+1, 8);
			if ( b != null )
				b.setType(Material.QUARTZ_BLOCK);
			
			b = getBlockAbs(chunk, game2WallX, groundMaxY+1, -8);
			if ( b != null )
				b.setType(Material.QUARTZ_BLOCK);
		
			b = getBlockAbs(chunk, game2WallX, groundMaxY+1, 8);
			if ( b != null )
				b.setType(Material.QUARTZ_BLOCK);
			
			
			
			b = getBlockAbs(chunk, game1WallX, groundMaxY+1, -9);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x2);
			}
			
			b = getBlockAbs(chunk, game1WallX, groundMaxY+1, 9);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x3);
			}
			
			b = getBlockAbs(chunk, game2WallX, groundMaxY+1, -9);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x2);
			}
			
			b = getBlockAbs(chunk, game2WallX, groundMaxY+1, 9);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x3);
			}
			
			
			b = getBlockAbs(chunk, game1WallX, groundMaxY+2, -8);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x2);
			}
			
			b = getBlockAbs(chunk, game1WallX, groundMaxY+2, 8);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x3);
			}
			
			b = getBlockAbs(chunk, game2WallX, groundMaxY+2, -8);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x2);
			}
			
			b = getBlockAbs(chunk, game2WallX, groundMaxY+2, 8);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x3);
			}
			
			
			b = getBlockAbs(chunk, game1WallX, groundMaxY+3, -7);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x2);
			}
			
			b = getBlockAbs(chunk, game1WallX, groundMaxY+3, 7);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x3);
			}
			
			b = getBlockAbs(chunk, game2WallX, groundMaxY+3, -7);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x2);
			}
			
			b = getBlockAbs(chunk, game2WallX, groundMaxY+3, 7);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x3);
			}
			
			
			b = getBlockAbs(chunk, game1WallX, groundMaxY+4, -6);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x2);
			}
			
			b = getBlockAbs(chunk, game1WallX, groundMaxY+4, 6);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x3);
			}
			
			b = getBlockAbs(chunk, game2WallX, groundMaxY+4, -6);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x2);
			}
			
			b = getBlockAbs(chunk, game2WallX, groundMaxY+4, 6);
			if ( b != null )
			{
				b.setType(Material.QUARTZ_STAIRS);
				b.setData((byte)0x3);
			}
			
			
			b = getBlockAbs(chunk, game1WallX, groundMaxY+2, -3);
			if ( b != null )
			{
				b.setType(Material.TORCH);
				b.setData((byte)0x4);
			}
			
			b = getBlockAbs(chunk, game1WallX, groundMaxY+2, 3);
			if ( b != null )
			{
				b.setType(Material.TORCH);
				b.setData((byte)0x3);
			}
			
			b = getBlockAbs(chunk, game2WallX, groundMaxY+2, -3);
			if ( b != null )
			{
				b.setType(Material.TORCH);
				b.setData((byte)0x4);
			}
			
			b = getBlockAbs(chunk, game2WallX, groundMaxY+2, 3);
			if ( b != null )
			{
				b.setType(Material.TORCH);
				b.setData((byte)0x3);
			}
			
			
			
			for ( int z = -2; z<= 2; z+=2 )
			{
				b = getBlockAbs(chunk, game1ButtonX, gameButtonY, z);
				if ( b != null )
				{
					b.setType(Material.STONE_BUTTON);
					b.setData((byte)0x2);
				}
				
				b = getBlockAbs(chunk, game2ButtonX, gameButtonY, z);
				if ( b != null )
				{
					b.setType(Material.STONE_BUTTON);
					b.setData((byte)0x1);
				}
			}

			
			b = getBlockAbs(chunk, game1ButtonX, gameButtonY-1, -2);
			if ( b != null ) // game 1 join
				setupWallSign(b, (byte)0x4, "");
			
			b = getBlockAbs(chunk, game2ButtonX, gameButtonY-1, 2);
			if ( b != null ) // game 2 join
				setupWallSign(b, (byte)0x5, "");
			
			b = getBlockAbs(chunk, game1ButtonX, gameButtonY-1, 0);
			if ( b != null ) // game 1 config
				setupWallSign(b, (byte)0x4, "");
			
			b = getBlockAbs(chunk, game2ButtonX, gameButtonY-1, 0);
			if ( b != null ) // game 2 config
				setupWallSign(b, (byte)0x5, "");
			
			b = getBlockAbs(chunk, game1ButtonX, gameButtonY-1, 2);
			if ( b != null ) // game 1 start
				setupWallSign(b, (byte)0x4, "");
			
			b = getBlockAbs(chunk, game2ButtonX, gameButtonY-1, -2);
			if ( b != null ) // game 2 start
				setupWallSign(b, (byte)0x5, "");
			
			b = getBlockAbs(chunk, game1ButtonX, gameButtonY+1, 0);
			if ( b != null ) // game 1 status
				setupWallSign(b, (byte)0x4, "");
			
			b = getBlockAbs(chunk, game2ButtonX, gameButtonY+1, 0);
			if ( b != null ) // game 2 status
				setupWallSign(b, (byte)0x5, "");
			
			// game 1 map frames
			b = getBlockAbs(chunk, game1ButtonX+1, gameButtonY, -1);
			if ( b != null )
			{
				ItemFrame frame = (ItemFrame)b.getWorld().spawnEntity(b.getLocation(), EntityType.ITEM_FRAME);
				frame.teleport(b.getRelative(BlockFace.WEST).getLocation());
				frame.setFacingDirection(BlockFace.WEST, true);	
			}

			b = getBlockAbs(chunk, game1ButtonX+1, gameButtonY, 1);
			if ( b != null )
			{
				ItemFrame frame = (ItemFrame)b.getWorld().spawnEntity(b.getLocation(), EntityType.ITEM_FRAME);
				frame.teleport(b.getRelative(BlockFace.WEST).getLocation());
				frame.setFacingDirection(BlockFace.WEST, true);
			}
			
			// game 2 map frames
			b = getBlockAbs(chunk, game2ButtonX-1, gameButtonY, 1);
			if ( b != null )
			{
				b.getWorld().spawnEntity(b.getLocation(), EntityType.ITEM_FRAME);
			}
			
			b = getBlockAbs(chunk, game2ButtonX-1, gameButtonY, -1);
			if ( b != null )
			{
				b.getWorld().spawnEntity(b.getLocation(), EntityType.ITEM_FRAME);
			}
		}

		private boolean insideSphere(int x, int y, int z, int x0, int y0, int z0, int rSquared)
		{
			return (x-x0)*(x-x0) + (y-y0)*(y-y0) + (z-z0)*(z-z0) <= rSquared;
		}
	}
	
	public static void setupFloorSign(Block b, byte orientation, String... lines)
	{
		b.setType(Material.SIGN_POST);
		b.setData(orientation);
		setSignText(b, lines);
	}

	
	public static void setupWallSign(Block b, byte orientation, String... lines)
	{
		b.setType(Material.WALL_SIGN);
		b.setData(orientation);
		setSignText(b, lines);
	}
	
	public static void setSignText(Block b, String... lines)
	{
		Sign s = (Sign)b.getState();
		for ( int i=0; i<4 && i<lines.length; i++ )
			s.setLine(i, lines[i]);
		for ( int i=lines.length; i<4; i++ )
			s.setLine(i, "");
		s.update();
	}
}

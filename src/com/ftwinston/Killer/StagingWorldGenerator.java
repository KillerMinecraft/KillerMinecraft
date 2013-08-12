package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.generator.BlockPopulator;

class StagingWorldGenerator extends org.bukkit.generator.ChunkGenerator
{
	private static final int groundMinY = 32, groundMaxY = 64, groundMinX = -22, groundMaxX = 22, groundMinZ = -22, groundMaxZ = 23;
	private static final int game1WallX = 10, game2WallX = -10, gameWallMaxY = 68; 
	private static final int game1ButtonX = 9, game2ButtonX = -9, gameButtonY = 66;
	
	@Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
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
		StagingWorldGenerator gen; boolean hackSpawn = true;
		
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
			if ( hackSpawn )
			{
				world.setSpawnLocation(0, 65, 0);
				hackSpawn = false;
			}
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
			if ( b != null )
				setupWallSign(b, (byte)0x4, "", "Join", "Game 1");
			
			b = getBlockAbs(chunk, game2ButtonX, gameButtonY-1, 2);
			if ( b != null )
				setupWallSign(b, (byte)0x5, "", "Join", "Game 2");
			
			b = getBlockAbs(chunk, game1ButtonX, gameButtonY-1, 0);
			if ( b != null )
				setupWallSign(b, (byte)0x4, "", "Configure", "Game 1");
			
			b = getBlockAbs(chunk, game2ButtonX, gameButtonY-1, 0);
			if ( b != null )
				setupWallSign(b, (byte)0x5, "", "Configure", "Game 2");
			
			b = getBlockAbs(chunk, game1ButtonX, gameButtonY-1, 2);
			if ( b != null )
				setupWallSign(b, (byte)0x4, "", "Start", "Game 1");
			
			b = getBlockAbs(chunk, game2ButtonX, gameButtonY-1, -2);
			if ( b != null )
				setupWallSign(b, (byte)0x5, "", "Start", "Game 2");
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
	
	public static void setSignLine(Block b, int line, String text)
	{
		Sign s = (Sign)b.getState();
		s.setLine(line,  text);
		s.update();
	}
	
	public static void fitTextOnSign(Sign s, String text)
	{
		if ( text.length() <= 15 )
		{
			s.setLine(2, text);
			s.setLine(3, "");
			return;
		}
		
		String[] lines = splitTextForSign(text);
		
		if ( lines[2] != null )
		{
			s.setLine(1, lines[0]);
			s.setLine(2, lines[1]);
			s.setLine(3, lines[2]);
		}
		else
		{
			s.setLine(1, "");
			s.setLine(2, lines[0]);
			if ( lines[1] != null )
				s.setLine(3, lines[1]);
			else
				s.setLine(3, "");
		}
		s.update();
	}
	
	public static String[] splitTextForSign(String text)
	{
		String[] words = text.split(" ");
		String[] lines = new String[3];
		int lineNum = -1;
		
		for ( String word : words )
		{
			// see if this word fits onto the current line. If not, move to the next line and place it there
			if ( lineNum == -1 || lines[lineNum].length() + 1 + word.length() > 15 )
			{
				lineNum++;
				if ( lineNum >= 3 )
					break;
				lines[lineNum] = word;
			}
			else
				lines[lineNum] += " " + word;
		}
		
		return lines;
	}
	
	public static String padSignLeft(String text)
	{
		while ( text.length() < 15 )
			text = " " + text;
		return text;
	}

	public static String getQuantityText(int num)
	{
		switch ( num )
		{
		case 0:
			return "None";
		case 1:
			return "Few";
		case 2:
			return "Some";
		case 3:
			return "Many";
		case 4:
			return "Too Many";
		default:
			return "???";
		}
	}
	
	// returns a 2D array of booleans, describing the blocks to use to write the text
	// first dimension is variable-length, and is horizontal, from left to right
	// second dimension is always of length 5, and is vertical, from bottom to top
	// most characters are 3 blocks wide (with 1 block spacing), but I is 1 block wide,
	// W and M are 5 blocks wide, and N is 4 blocks wide.
	public static boolean[][] writeBlockText(String text)
	{
		text = text.toUpperCase();
		
		ArrayList<boolean[]> columns = new ArrayList<boolean[]>();
		for (char ch : text.toCharArray())
		{
			switch (ch)
			{
				case ' ':
					columns.add(new boolean[5]);
					columns.add(new boolean[5]);
					columns.add(new boolean[5]);
					break;
				case 'A':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { false, false, true, false, true });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case 'B':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { true, false, true, false, true });
					columns.add(new boolean[] { false, true, false, true, false });
					columns.add(new boolean[5]);
					break;
				case 'C':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { true, false, false, false, true });
					columns.add(new boolean[] { true, false, false, false, true });
					columns.add(new boolean[5]);
					break;
				case 'D':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { true, false, false, false, true });
					columns.add(new boolean[] { false, true, true, true, false });
					columns.add(new boolean[5]);
					break;
				case 'E':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { true, false, true, false, true });
					columns.add(new boolean[] { true, false, false, false, true });
					columns.add(new boolean[5]);
					break;
				case 'F':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { false, false, true, false, true });
					columns.add(new boolean[] { false, false, false, false, true });
					columns.add(new boolean[5]);
					break;
				case 'G':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { true, false, false, false, true });
					columns.add(new boolean[] { true, true, true, false, true });
					columns.add(new boolean[5]);
					break;
				case 'H':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { false, false, true, false, false });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case 'I':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case 'J':
					columns.add(new boolean[] { true, true, false, false, false });
					columns.add(new boolean[] { true, false, false, false, false });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case 'K':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { false, false, true, false, false });
					columns.add(new boolean[] { true, true, false, true, true });
					columns.add(new boolean[5]);
					break;
				case 'L':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { true, false, false, false, false });
					columns.add(new boolean[] { true, false, false, false, false });
					columns.add(new boolean[5]);
					break;
				case 'M':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { false, false, false, false, true });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { false, false, false, false, true });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case 'N':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { false, false, true, true, false });
					columns.add(new boolean[] { false, true, false, false, false });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case 'O':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { true, false, false, false, true });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case 'P':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { false, false, true, false, true });
					columns.add(new boolean[] { false, false, true, true, true });
					columns.add(new boolean[5]);
					break;
				case 'Q':
					columns.add(new boolean[] { false, true, true, true, false });
					columns.add(new boolean[] { true, true, false, false, true });
					columns.add(new boolean[] { true, false, true, true, false });
					columns.add(new boolean[5]);
					break;
				case 'R':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { false, false, true, false, true });
					columns.add(new boolean[] { true, true, false, true, true });
					columns.add(new boolean[5]);
					break;
				case 'S':
					columns.add(new boolean[] { true, false, true, true, true });
					columns.add(new boolean[] { true, false, true, false, true });
					columns.add(new boolean[] { true, true, true, false, true });
					columns.add(new boolean[5]);
					break;
				case 'T':
					columns.add(new boolean[] { false, false, false, false, true });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { false, false, false, false, true });
					columns.add(new boolean[5]);
					break;
				case 'U':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { true, false, false, false, false });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case 'V':
					columns.add(new boolean[] { false, false, true, true, true });
					columns.add(new boolean[] { true, true, false, false, false });
					columns.add(new boolean[] { false, false, true, true, true });
					columns.add(new boolean[5]);
					break;
				case 'W':
					columns.add(new boolean[] { false, true, true, true, true });
					columns.add(new boolean[] { true, false, false, false, false });
					columns.add(new boolean[] { false, true, true, false, false });
					columns.add(new boolean[] { true, false, false, false, false });
					columns.add(new boolean[] { false, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case 'X':
					columns.add(new boolean[] { true, true, false, true, true });
					columns.add(new boolean[] { false, false, true, false, false });
					columns.add(new boolean[] { true, true, false, true, true });
					columns.add(new boolean[5]);
					break;
				case 'Y':
					columns.add(new boolean[] { true, false, false, true, true });
					columns.add(new boolean[] { true, false, true, false, false });
					columns.add(new boolean[] { false, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case 'Z':
					columns.add(new boolean[] { true, true, false, false, true });
					columns.add(new boolean[] { true, false, true, false, true });
					columns.add(new boolean[] { true, false, false, true, true });
					columns.add(new boolean[5]);
					break;
				case '1':
					columns.add(new boolean[] { true, false, false, true, false });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { true, false, false, false, false });
					columns.add(new boolean[5]);
					break;
				case '2':
					columns.add(new boolean[] { true, true, false, false, true });
					columns.add(new boolean[] { true, false, true, false, true });
					columns.add(new boolean[] { true, false, false, true, false });
					columns.add(new boolean[5]);
					break;
				case '3':
					columns.add(new boolean[] { true, false, false, false, true });
					columns.add(new boolean[] { true, false, true, false, true });
					columns.add(new boolean[] { false, true, true, true, false });
					columns.add(new boolean[5]);
					break;
				case '4':
					columns.add(new boolean[] { false, false, true, true, true });
					columns.add(new boolean[] { false, false, true, false, false });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case '5':
					columns.add(new boolean[] { true, false, true, true, true });
					columns.add(new boolean[] { true, false, true, false, true });
					columns.add(new boolean[] { false, true, false, false, true });
					columns.add(new boolean[5]);
					break;
				case '6':
					columns.add(new boolean[] { true, true, true, true, false });
					columns.add(new boolean[] { true, false, true, false, true });
					columns.add(new boolean[] { true, true, true, false, true });
					columns.add(new boolean[5]);
					break;
				case '7':
					columns.add(new boolean[] { false, false, false, false, true });
					columns.add(new boolean[] { true, true, true, false, true });
					columns.add(new boolean[] { false, false, false, true, true });
					columns.add(new boolean[5]);
					break;
				case '8':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { true, false, true, false, true });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case '9':
					columns.add(new boolean[] { false, false, true, true, true });
					columns.add(new boolean[] { false, false, true, false, true });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case '0':
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[] { true, false, false, false, true });
					columns.add(new boolean[] { true, true, true, true, true });
					columns.add(new boolean[5]);
					break;
				case '.':
					columns.add(new boolean[] { true, false, false, false, false });
					columns.add(new boolean[5]);
					break;
				case ',':
					columns.add(new boolean[] { true, false, false, false, false });
					columns.add(new boolean[] { false, true, false, false, false });
					columns.add(new boolean[5]);
					break;
				case '!':
					columns.add(new boolean[] { true, false, true, true, true });
					columns.add(new boolean[5]);
					break;
				case '?':
				default:
					columns.add(new boolean[] { false, false, false, false, true });
					columns.add(new boolean[] { true, false, true, false, true });
					columns.add(new boolean[] { false, false, false, true, false });
					columns.add(new boolean[5]);
					break;
			}
		}
		
		return columns.toArray(new boolean[0][]);
	}
	
	public static String capitalize(String str)
	{
		String first = str.substring(0, 1), rest = str.substring(1);
		return first.toUpperCase() + rest.toLowerCase();
	}
}

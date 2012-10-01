package com.ftwinston.Killer;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.generator.BlockPopulator;

public class StagingWorldGenerator extends org.bukkit.generator.ChunkGenerator
{
	private final int numRandomWorldOptions = 1;
	
	private int numGameModes, maxGameModeOptions, numWorldOptions;
	private boolean allowRandomWorlds;
	private String[] customWorldNames;
	
	int endX, endZ, worldEndX, optionsEndX, gameModeEndZ;
	int forceStartButtonX, forceStartButtonZ;
	
	boolean gameModeSelectionClosedOff, worldSelectionClosedOff, gameModeOptionsClosedOff;
	
	public StagingWorldGenerator()
	{
		// not really sure how these should be set
		String[] customWorldNames = new String[] { "Hello", "Goodbye" };
		boolean allowRandomWorldGeneration = true;
		
		
		
		numGameModes = GameMode.gameModes.size();
        maxGameModeOptions = 0;
        for ( GameMode mode : GameMode.gameModes.values() )
        	if ( mode.getOptions().size() > maxGameModeOptions )
        		maxGameModeOptions = mode.getOptions().size();
		
		this.customWorldNames = customWorldNames;
		this.allowRandomWorlds = allowRandomWorldGeneration;
		
		if ( allowRandomWorldGeneration )
			numWorldOptions = customWorldNames.length + numRandomWorldOptions;
		else
			numWorldOptions = customWorldNames.length;
		// if random world generation is disabled, and no custom world names are provided ... what then?
		
		
		// now set up helper values for where the various "extensible" things end
		endX = Math.max(Math.max(numWorldOptions, maxGameModeOptions) * 2 + 8, 20);
		endZ = Math.max(numGameModes * 2 + 12, 22);
						
		forceStartButtonX = endX - 1;
		forceStartButtonZ = endZ / 2;
		
		worldEndX = numWorldOptions * 2 + 8;
		optionsEndX = maxGameModeOptions == 0 ? 4 : maxGameModeOptions * 2 + 8;
		gameModeEndZ = numGameModes * 2 + 8;
		
		// todo: decide these, based on config or whatever
		gameModeSelectionClosedOff = numGameModes < 2 ;
		worldSelectionClosedOff = numWorldOptions < 2;
		gameModeOptionsClosedOff = maxGameModeOptions < 2;
	}
	
	public static int getWorldCenterZ()
	{
		return Math.max(GameMode.gameModes.size() + 6, 11);
	}
	
	@Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return Arrays.asList((BlockPopulator)new StagingWorldPopulator(this));
    }
	
    @Override
    public boolean canSpawn(World world, int x, int z) {
        return x >= 0 && z >= 0 && x <= endX / 16 && z <= endZ / 16;
    }
    
	public byte[][] generateBlockSections(World world, Random random, int cx, int cz, BiomeGrid biomes)
	{
		return new byte[1][];
	}
	
	public Location getFixedSpawnLocation(World world, Random random)
	{
		Location loc = new Location(world, 8, 2, forceStartButtonZ - 2 + random.nextDouble() * 4);
		loc.setYaw(0); // if 0 actually works, this isn't needed. But we want them to face -x, at any rate
		return loc;
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
			if ( chunk.getX() < 0 || chunk.getZ() < 0 || chunk.getX() > gen.endX / 16 || chunk.getZ() > gen.endZ / 16 )
				return;
			
			Material mainFloor = Material.SMOOTH_BRICK;
			Material ceiling = Material.GLOWSTONE;
			Material walls = Material.SMOOTH_BRICK;
			Material closedOffWall = Material.IRON_FENCE;
			
			Material slab = Material.STEP;
			Material loweredFloor = Material.NETHERRACK;
			
			Material lamp = Material.REDSTONE_LAMP_OFF;
			Material button = Material.STONE_BUTTON;
			Material sign = Material.WALL_SIGN;
			Block b;
		
			//
			// the world selection area
			//
			
			// floor
			for ( int x = 4; x < gen.worldEndX; x++ )
				for ( int z = 0; z < 5; z++ )
				{
					b = getBlockAbs(chunk, x, 1, z);
					if ( b != null )
						b.setType(mainFloor);
				}

			// ceiling
			for ( int x = 4; x < gen.worldEndX; x++ )
				for ( int z = 0; z < 5; z++ )
				{
					b = getBlockAbs(chunk, x, 7, z);
					if ( b != null )
						b.setType(ceiling);
				}
			
			// west wall
			for ( int z = 1; z < 5; z++ )
				for ( int y = 2; y < 7; y++ )
				{
					b = getBlockAbs(chunk, 4, y, z);
					if ( b != null )
						b.setType(walls);
				}
			
			// east wall
			for ( int z = 1; z < 5; z++ )
				for ( int y = 2; y < 7; y++ )
				{
					b = getBlockAbs(chunk, gen.worldEndX, y, z);
					if ( b != null )
						b.setType(walls);
				}
			
			// south wall, if should be closed off
			if ( gen.worldSelectionClosedOff )
				for ( int x = 5; x < gen.worldEndX; x++ )
					for ( int y = 2; y < 7; y++ )
					{
						b = getBlockAbs(chunk, x, y, 5);
						if ( b != null )
							b.setType(closedOffWall);
					}
			
			// north wall
			for ( int x = 4; x < gen.worldEndX; x++ )
				for ( int y = 2; y < 7; y++ )
				{
					b = getBlockAbs(chunk, x, y, 0);
					if ( b != null )
						b.setType(walls);
				}
			
			// now all the buttons/signs etc IN the north wall
			int num = 0;
			int worldX = 7;
			while ( worldX < gen.worldEndX - 2 && num < gen.numWorldOptions )
			{
				b = getBlockAbs(chunk, worldX, 2, 1);
				if ( b != null )
				{
					b.setType(button);
					b.setData((byte)0x3);
				}
				
				b = getBlockAbs(chunk, worldX, 3, 1);
				if ( b != null )
				{
					b.setType(sign);
					b.setData((byte)0x3);
					Sign s = (Sign)b.getState();
					s.setLine(1, "§fWorld:");
					
					if ( gen.allowRandomWorlds && num == 0 )
						s.setLine(2, "§fDefault Random");
					else
					{
						String name = gen.allowRandomWorlds ? gen.customWorldNames[num - 1] : gen.customWorldNames[num - 1];
						if ( name.length() > 12 )
						{
							String[] words = name.split(" ");
							s.setLine(2, "§f" + words[0]);
							if ( words.length > 1)
							{
								s.setLine(3, "§f" + words[1]);
								if ( words.length > 2)
									s.setLine(4, "§f" + words[2]);
							}
						}
						else
							s.setLine(2, "§f" + name);
					}
					s.update();
				}
				
				b = getBlockAbs(chunk, worldX, 3, 0);
				if ( b != null )
					b.setType(lamp);
				
				worldX += 2;
				num ++;
			}
			
			
			//
			// the game mode selection area
			//
			
			// floor
			for ( int x = 0; x < 5; x++ )
				for ( int z = 4; z < gen.gameModeEndZ; z++ )
				{
					b = getBlockAbs(chunk, x, 1, z);
					if ( b != null )
						b.setType(mainFloor);
				}
			
			// ceiling
			for ( int x = 0; x < 5; x++ )
				for ( int z = 4; z < gen.gameModeEndZ; z++ )
				{
					b = getBlockAbs(chunk, x, 7, z);
					if ( b != null )
						b.setType(ceiling);
				}
			
			// north wall
			for ( int x = 1; x < 5; x++ )
				for ( int y = 2; y < 7; y++ )
				{
					b = getBlockAbs(chunk, x, y, 4);
					if ( b != null )
						b.setType(walls);
				}
			
			// south wall, if needed
			for ( int x = 1; x < 5; x++ )
				for ( int y = 2; y < 7; y++ )
				{
					b = getBlockAbs(chunk, x, y, gen.gameModeEndZ);
					if ( b != null )
						b.setType(walls);
				}
			
			// east wall, if should be closed off
			if ( gen.gameModeSelectionClosedOff )
				for ( int z = 4; z < gen.gameModeEndZ; z++ )
					for ( int y = 2; y < 7; y++ )
					{
						b = getBlockAbs(chunk, 5, y, z);
						if ( b != null )
							b.setType(closedOffWall);
					}
			
			// west wall
			for ( int z = 4; z < gen.gameModeEndZ; z++ )
				for ( int y = 2; y < 7; y++ )
				{
					b = getBlockAbs(chunk, 0, y, z);
					if ( b != null )
						b.setType(walls);
				}
			
			// now all the buttons/signs etc IN the west wall
			num = 0;
			int modeZ = 7;
			Object[] modes = GameMode.gameModes.values().toArray();
			
			while ( modeZ < gen.gameModeEndZ - 2 && num < gen.numGameModes )
			{
				b = getBlockAbs(chunk, 1, 2, modeZ);
				if ( b != null )
				{
					b.setType(button);
					b.setData((byte)0x1);
				}
				
				b = getBlockAbs(chunk, 1, 3, modeZ);
				if ( b != null )
				{
					b.setType(sign);
					b.setData((byte)0x5);
					Sign s = (Sign)b.getState();
					s.setLine(1, "§fGame mode:");
					
					String name = ((GameMode)modes[num]).getName();
					if ( name.length() > 12 )
					{
						String[] words = name.split(" ");
						s.setLine(2, "§f" + words[0]);
						if ( words.length > 1)
						{
							s.setLine(3, "§f" + words[1]);
							if ( words.length > 2)
								s.setLine(4, "§f" + words[2]);
						}
					}
					else
						s.setLine(2, "§f" + name);
					s.update();
				}
				
				b = getBlockAbs(chunk, 0, 3, modeZ);
				if ( b != null )
					b.setType(lamp);
				
				modeZ += 2;
				num ++;
			}
			
			
			//
			// the main room
			//
			
			// ceiling
			for ( int x=5; x<=gen.endX; x++ )
				if ( x > 5 && x < 10  )
					continue; // leave a big hole above here, so that players won't spawn on the roof. Ah, minecraft. Joy.
				else
					for ( int z=5; z<gen.endZ - 4; z++ )
					{
						b = getBlockAbs(chunk, x, 7, z);
						if ( b != null )
							b.setType(ceiling);
					}
			
			// high-level floor on north
			for ( int x=5; x<gen.endX; x++ )
				for ( int z=5; z<7; z++ )
				{
					b = getBlockAbs(chunk, x, 1, z);
					if ( b != null )
						b.setType(mainFloor);
				}
			
			// high-level floor on west
			for ( int x=5; x<10; x++ )
				for ( int z=7; z<gen.endZ - 6; z++ )
				{
					b = getBlockAbs(chunk, x, 1, z);
					if ( b != null )
						b.setType(mainFloor);
				}
			
			// high-level floor on south
			for ( int x=5; x<gen.endX; x++ )
				for ( int z=gen.endZ - 6; z<gen.endZ - 4; z++ )
				{
					b = getBlockAbs(chunk, x, 1, z);
					if ( b != null )
						b.setType(mainFloor);
				}
			
			// half steps on north
			for ( int x=10; x<gen.endX; x++ )
			{
				b = getBlockAbs(chunk, x, 1, 7);
				if ( b != null )
				{
					b.setType(slab);
					b.setData((byte)0x5);
				}
			}
			
			// half steps on west
			for ( int z=8; z<gen.endZ - 7; z++ )
			{
				b = getBlockAbs(chunk, 10, 1, z);
				if ( b != null )
				{
					b.setType(slab);
					b.setData((byte)0x5);
				}
			}
			
			// half-steps on south
			for ( int x=10; x<gen.endX; x++ )
			{
				b = getBlockAbs(chunk, x, 1, gen.endZ - 7);
				if ( b != null )
				{
					b.setType(slab);
					b.setData((byte)0x5);
				}
			}
			
			// lowered floor
			for ( int x = 11; x <= gen.endX; x ++ )
				for ( int z = 8; z < gen.endZ - 6; z++ )
				{
					b = getBlockAbs(chunk, x, 0, z);
					if ( b != null )
						b.setType(loweredFloor);
				}
			
			// any extra wall required on the north
			for ( int x = gen.worldEndX + 1; x < gen.endX; x ++ )
				for ( int y = 2; y < 7; y++ )
				{
					b = getBlockAbs(chunk, x, y, 4);
					if ( b != null )
						b.setType(walls);
				}
			
			// any extra wall required on the south
			for ( int x = gen.optionsEndX + 1; x < gen.endX; x ++ )
				for ( int y = 2; y < 7; y++ )
				{
					b = getBlockAbs(chunk, x, y, gen.endZ - 4);
					if ( b != null )
						b.setType(walls);
				}
		
			// east wall
			for ( int z = 4; z < gen.endZ - 4; z++ )
				for ( int y = 1; y < 7; y++ )
				{
					b = getBlockAbs(chunk, gen.endX, y, z);
					if ( b != null )
						b.setType(walls);
				}
			
			//
			// the game mode option selection area
			//
			
			// floor
			for ( int x = 4; x < gen.optionsEndX; x++ )
				for ( int z = gen.endZ - 4; z <= gen.endZ; z++ )
				{
					b = getBlockAbs(chunk, x, 1, z);
					if ( b != null )
						b.setType(mainFloor);
				}
			
			// ceiling
			for ( int x = 4; x < gen.optionsEndX; x++ )
				for ( int z = gen.endZ - 4; z <= gen.endZ; z++ )
				{
					b = getBlockAbs(chunk, x, 7, z);
					if ( b != null )
						b.setType(ceiling);
				}
			
			// west wall
			for ( int z = gen.endZ - 4; z <= gen.endZ; z++ )
				for ( int y = 2; y < 7; y++ )
				{
					b = getBlockAbs(chunk, 4, y, z);
					if ( b != null )
						b.setType(walls);
				}
			
			// east wall
			for ( int z = gen.endZ - 4; z <= gen.endZ; z++ )
				for ( int y = 2; y < 7; y++ )
				{
					b = getBlockAbs(chunk, gen.optionsEndX, y, z);
					if ( b != null )
						b.setType(walls);
				}
			
			// north wall, if should be closed off
			if ( gen.gameModeOptionsClosedOff )
				for ( int x = 5; x < gen.optionsEndX; x++ )
					for ( int y = 2; y < 7; y++ )
					{
						b = getBlockAbs(chunk, x, y, gen.endZ - 4);
						if ( b != null )
							b.setType(closedOffWall);
					}
			
			// south wall
			for ( int x = 4; x < gen.optionsEndX; x++ )
				for ( int y = 2; y < 7; y++ )
				{
					b = getBlockAbs(chunk, x, y, gen.endZ);
					if ( b != null )
						b.setType(walls);
				}
			
			// now all the buttons/signs etc IN the south wall
			num = 0;
			int optionX = 7;
			while ( optionX < gen.optionsEndX - 2 && num < gen.maxGameModeOptions )
			{
				b = getBlockAbs(chunk, optionX, 2, gen.endZ - 1);
				if ( b != null )
				{
					b.setType(button);
					b.setData((byte)0x4);
				}
				
				b = getBlockAbs(chunk, optionX, 3, gen.endZ - 1);
				if ( b != null )
				{
					b.setType(sign);
					b.setData((byte)0x2);
					Sign s = (Sign)b.getState();
					s.setLine(1, "§fOption:");
					s.setLine(2, "§f???");
					s.update();
				}
				
				b = getBlockAbs(chunk, optionX, 3, gen.endZ);
				if ( b != null )
					b.setType(lamp);
				
				worldX += 2;
				num ++;
			}
		}
	}
}

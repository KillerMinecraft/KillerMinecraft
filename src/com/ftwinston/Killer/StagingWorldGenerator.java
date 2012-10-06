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
	private int maxGameModeOptions;
	
	int endX, endZ, worldEndX, optionsEndX, gameModeEndZ;
	boolean gameModeSelectionClosedOff, worldSelectionClosedOff, gameModeOptionsClosedOff;
	
	public StagingWorldGenerator()
	{
        maxGameModeOptions = 0;
        for ( GameMode mode : GameMode.gameModes )
        	if ( mode.getOptions().size() > maxGameModeOptions )
        		maxGameModeOptions = mode.getOptions().size();
		
		if ( Settings.customWorldNames.size() == 0 )
			Settings.allowRandomWorlds = true; // If no custom worlds (are left), always allow random worlds
		
		// now set up helper values for where the various "extensible" things end
		endX = Math.max(Math.max(WorldOption.options.size(), maxGameModeOptions) * 2 + 8, 14);
		endZ = Math.max(GameMode.gameModes.size() * 3 + 12, 22);
						
		startButtonX = endX - 1;
		startButtonZ = endZ / 2;
		overrideButtonZ = startButtonZ + 1;
		cancelButtonZ = startButtonZ - 1;
		gameModeButtonX = 1;
		worldOptionZ = 1;
		gameModeOptionZ = endZ - 1;
		
		worldEndX = WorldOption.options.size() * 2 + 8;
		optionsEndX = maxGameModeOptions == 0 ? 4 : maxGameModeOptions * 2 + 8;
		gameModeEndZ = GameMode.gameModes.size() * 3 + 8;
		
		gameModeSelectionClosedOff = GameMode.gameModes.size() < 2 ;
		worldSelectionClosedOff = WorldOption.options.size() < 2;
		gameModeOptionsClosedOff = maxGameModeOptions < 2;
	}
	
	public static int startButtonX, startButtonZ, overrideButtonZ, cancelButtonZ, gameModeButtonX, worldOptionZ, gameModeOptionZ;
	
	public static byte colorOptionOn = 5 /* lime */, colorOptionOff = 14 /* red*/,
		colorStartButton = 1 /* orange */, colorOverrideButton = 1 /* red */, colorCancelButton = 9 /* green */;
	
	public static int getGameModeZ(int num) { return 8 + num * 3; }
	public static int getWorldOptionX(int num) { return 7 + num * 2; }
	public static int getGameModeOptionX(int num) { return 7 + num * 2; }
	
	public static int getGameModeNumFromZ(int z) { return (z - 8) / 3; }
	public static int getWorldOptionNumFromX(int x) { return (x - 7) / 2; }
	public static int getGameModeOptionNumFromX(int x) { return (x - 7) / 2; }
	
	
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
		Location loc = new Location(world, 8.5, 34, startButtonZ + 0.5);
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
			Material closedOffWall = Material.SMOOTH_BRICK; // Material.IRON_FENCE
			
			Material slab = Material.STEP;
			Material loweredFloor = Material.NETHERRACK;
			
			Material wool = Material.WOOL;
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
					b = getBlockAbs(chunk, x, 33, z);
					if ( b != null )
						b.setType(mainFloor);
				}

			// ceiling
			for ( int x = 4; x < gen.worldEndX; x++ )
				for ( int z = 0; z < 5; z++ )
				{
					b = getBlockAbs(chunk, x, 39, z);
					if ( b != null )
						b.setType(ceiling);
				}
			
			// west wall
			for ( int z = 1; z < 5; z++ )
				for ( int y = 34; y < 39; y++ )
				{
					b = getBlockAbs(chunk, 4, y, z);
					if ( b != null )
						b.setType(walls);
				}
			
			// east wall
			for ( int z = 1; z < 5; z++ )
				for ( int y = 34; y < 39; y++ )
				{
					b = getBlockAbs(chunk, gen.worldEndX, y, z);
					if ( b != null )
						b.setType(walls);
				}
			
			// south wall, if should be closed off
			if ( gen.worldSelectionClosedOff )
				for ( int x = 5; x < gen.worldEndX; x++ )
					for ( int y = 34; y < 39; y++ )
					{
						b = getBlockAbs(chunk, x, y, 4);
						if ( b != null )
							b.setType(closedOffWall);
					}
			
			// north wall
			for ( int x = 4; x < gen.worldEndX; x++ )
				for ( int y = 34; y < 39; y++ )
				{
					b = getBlockAbs(chunk, x, y, 0);
					if ( b != null )
						b.setType(walls);
				}
			
			// now all the buttons/signs etc IN the north wall
			for ( int num=0; num < WorldOption.options.size(); num++ )
			{
				int worldX = getWorldOptionX(num);
				b = getBlockAbs(chunk, worldX, 35, 1);
				if ( b != null )
				{
					b.setType(button);
					b.setData((byte)0x3);
				}
				
				b = getBlockAbs(chunk, worldX, 36, 1);
				if ( b != null )
				{
					b.setType(sign);
					b.setData((byte)0x3);
					Sign s = (Sign)b.getState();
					s.setLine(0, "§fWorld:");
					
					String name = WorldOption.get(num).getName().replace('_', ' ');
					if ( name.length() > 12 )
					{
						String[] words = name.split(" ");
						s.setLine(1, "§f" + words[0]);
						if ( words.length > 1)
						{
							s.setLine(2, "§f" + words[1]);
							if ( words.length > 2)
								s.setLine(3, "§f" + words[2]);
						}
					}
					else
						s.setLine(1, "§f" + name);
					s.update();
				}
				
				b = getBlockAbs(chunk, worldX, 35, 0);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(WorldOption.options.size() == 1 ? colorOptionOn : colorOptionOff);
				}
			}
			
			
			//
			// the game mode selection area
			//
			
			// floor
			for ( int x = 0; x < 5; x++ )
				for ( int z = 4; z < gen.gameModeEndZ; z++ )
				{
					b = getBlockAbs(chunk, x, 33, z);
					if ( b != null )
						b.setType(mainFloor);
				}
			
			// ceiling
			for ( int x = 0; x < 5; x++ )
				for ( int z = 4; z < gen.gameModeEndZ; z++ )
				{
					b = getBlockAbs(chunk, x, 39, z);
					if ( b != null )
						b.setType(ceiling);
				}
			
			// north wall
			for ( int x = 1; x < 5; x++ )
				for ( int y = 34; y < 39; y++ )
				{
					b = getBlockAbs(chunk, x, y, 4);
					if ( b != null )
						b.setType(walls);
				}
			
			// south wall
			for ( int x = 1; x < 5; x++ )
				for ( int y = 34; y < 39; y++ )
				{
					b = getBlockAbs(chunk, x, y, gen.gameModeEndZ);
					if ( b != null )
						b.setType(walls);
				}
			
			// east wall, if should be closed off
			if ( gen.gameModeSelectionClosedOff )
				for ( int z = 4; z < gen.gameModeEndZ; z++ )
					for ( int y = 34; y < 39; y++ )
					{
						b = getBlockAbs(chunk, 5, y, z);
						if ( b != null )
							b.setType(closedOffWall);
					}
			
			// west wall
			for ( int z = 4; z < gen.gameModeEndZ; z++ )
				for ( int y = 34; y < 39; y++ )
				{
					b = getBlockAbs(chunk, 0, y, z);
					if ( b != null )
						b.setType(walls);
				}
			
			// now all the buttons/signs etc IN the west wall
			for ( int num=0; num < GameMode.gameModes.size(); num++ )
			{
				int modeZ = getGameModeZ(num);
				b = getBlockAbs(chunk, 1, 35, modeZ);
				if ( b != null )
				{
					b.setType(button);
					b.setData((byte)0x1);
				}
				
				GameMode gameMode = GameMode.get(num);
				b = getBlockAbs(chunk, 1, 36, modeZ);
				if ( b != null )
				{
					b.setType(sign);
					b.setData((byte)0x5);
					Sign s = (Sign)b.getState();
					s.setLine(0, "§fGame mode:");
					
					String name = gameMode.getName();
					if ( name.length() > 12 )
					{
						String[] words = name.split(" ");
						s.setLine(1, "§f" + words[0]);
						if ( words.length > 1)
						{
							s.setLine(2, "§f" + words[1]);
							if ( words.length > 2)
								s.setLine(3, "§f" + words[2]);
						}
					}
					else
						s.setLine(1, "§f" + name);
					s.update();
				}
				
				b = getBlockAbs(chunk, 0, 35, modeZ);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(GameMode.gameModes.size() == 1 ? colorOptionOn : colorOptionOff);
				}
				
				// now the game mode DESCRIPTION signs
				b = getBlockAbs(chunk, 1, 36, modeZ-1);
				if ( b != null )
				{
					b.setType(sign);
					b.setData((byte)0x5);
					Sign s = (Sign)b.getState();
					
					String[] descLines = gameMode.getSignDescription();
					s.setLine(0, descLines.length > 0 ? descLines[0] : "");
					s.setLine(1, descLines.length > 1 ? descLines[1] : "");
					s.setLine(2, descLines.length > 2 ? descLines[2] : "");
					s.setLine(3, descLines.length > 3 ? descLines[3] : "");
					s.update();
				}
				
				b = getBlockAbs(chunk, 1, 35, modeZ-1);
				if ( b != null )
				{
					b.setType(sign);
					b.setData((byte)0x5);
					Sign s = (Sign)b.getState();
					
					String[] descLines = gameMode.getSignDescription();
					s.setLine(0, descLines.length > 4 ? descLines[4] : "");
					s.setLine(1, descLines.length > 5 ? descLines[5] : "");
					s.setLine(2, descLines.length > 6 ? descLines[6] : "");
					s.setLine(3, descLines.length > 7 ? descLines[7] : "");
					s.update();
				}
				
				b = getBlockAbs(chunk, 1, 34, modeZ-1);
				if ( b != null )
				{
					b.setType(sign);
					b.setData((byte)0x5);
					Sign s = (Sign)b.getState();
					
					String[] descLines = gameMode.getSignDescription();
					s.setLine(0, descLines.length > 8 ? descLines[8] : "");
					s.setLine(1, descLines.length > 9 ? descLines[9] : "");
					s.setLine(2, descLines.length > 10 ? descLines[10] : "");
					s.setLine(3, descLines.length > 11 ? descLines[11] : "");
					s.update();
				}
			}
			
			
			//
			// the main room
			//
			
			// ceiling
			for ( int x=5; x<=gen.endX; x++ )
				for ( int z=5; z<gen.endZ - 4; z++ )
				{
					b = getBlockAbs(chunk, x, 39, z);
					if ( b != null )
						b.setType(ceiling);
				}
			
			// high-level floor on north
			for ( int x=5; x<gen.endX; x++ )
				for ( int z=5; z<7; z++ )
				{
					b = getBlockAbs(chunk, x, 33, z);
					if ( b != null )
						b.setType(mainFloor);
				}
			
			// high-level floor on west
			for ( int x=5; x<10; x++ )
				for ( int z=7; z<gen.endZ - 6; z++ )
				{
					b = getBlockAbs(chunk, x, 33, z);
					if ( b != null )
						b.setType(mainFloor);
				}
			
			// high-level floor on south
			for ( int x=5; x<gen.endX; x++ )
				for ( int z=gen.endZ - 6; z<gen.endZ - 4; z++ )
				{
					b = getBlockAbs(chunk, x, 33, z);
					if ( b != null )
						b.setType(mainFloor);
				}
			
			// half steps on north
			for ( int x=10; x<gen.endX; x++ )
			{
				b = getBlockAbs(chunk, x, 33, 7);
				if ( b != null )
				{
					b.setType(slab);
					b.setData((byte)0x5);
				}
			}
			
			// half steps on west
			for ( int z=8; z<gen.endZ - 7; z++ )
			{
				b = getBlockAbs(chunk, 10, 33, z);
				if ( b != null )
				{
					b.setType(slab);
					b.setData((byte)0x5);
				}
			}
			
			// half-steps on south
			for ( int x=10; x<gen.endX; x++ )
			{
				b = getBlockAbs(chunk, x, 33, gen.endZ - 7);
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
					b = getBlockAbs(chunk, x, 32, z);
					if ( b != null )
						b.setType(loweredFloor);
				}
			
			// any extra wall required on the north
			for ( int x = gen.worldEndX + 1; x < gen.endX; x ++ )
				for ( int y = 34; y < 39; y++ )
				{
					b = getBlockAbs(chunk, x, y, 4);
					if ( b != null )
						b.setType(walls);
				}
			
			// any extra wall required on the south
			for ( int x = gen.optionsEndX + 1; x < gen.endX; x ++ )
				for ( int y = 34; y < 39; y++ )
				{
					b = getBlockAbs(chunk, x, y, gen.endZ - 4);
					if ( b != null )
						b.setType(walls);
				}
		
			// east wall
			for ( int z = 4; z < gen.endZ - 4; z++ )
				for ( int y = 33; y < 39; y++ )
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
					b = getBlockAbs(chunk, x, 33, z);
					if ( b != null )
						b.setType(mainFloor);
				}
			
			// ceiling
			for ( int x = 4; x < gen.optionsEndX; x++ )
				for ( int z = gen.endZ - 4; z <= gen.endZ; z++ )
				{
					b = getBlockAbs(chunk, x, 39, z);
					if ( b != null )
						b.setType(ceiling);
				}
			
			// west wall
			for ( int z = gen.endZ - 4; z <= gen.endZ; z++ )
				for ( int y = 34; y < 39; y++ )
				{
					b = getBlockAbs(chunk, 4, y, z);
					if ( b != null )
						b.setType(walls);
				}
			
			// east wall
			for ( int z = gen.endZ - 4; z <= gen.endZ; z++ )
				for ( int y = 34; y < 39; y++ )
				{
					b = getBlockAbs(chunk, gen.optionsEndX, y, z);
					if ( b != null )
						b.setType(walls);
				}
			
			// north wall, if should be closed off
			if ( gen.gameModeOptionsClosedOff )
				for ( int x = 5; x < gen.optionsEndX; x++ )
					for ( int y = 34; y < 39; y++ )
					{
						b = getBlockAbs(chunk, x, y, gen.endZ - 4);
						if ( b != null )
							b.setType(closedOffWall);
					}
			
			// south wall
			for ( int x = 4; x < gen.optionsEndX; x++ )
				for ( int y = 34; y < 39; y++ )
				{
					b = getBlockAbs(chunk, x, y, gen.endZ);
					if ( b != null )
						b.setType(walls);
				}
			
			// now all the buttons/signs etc IN the south wall
			for ( int num=0; num < gen.maxGameModeOptions; num++ )
			{
				int optionX = getGameModeOptionX(num);
				b = getBlockAbs(chunk, optionX, 35, gen.endZ - 1);
				if ( b != null )
				{
					b.setType(button);
					b.setData((byte)0x4);
				}
				
				b = getBlockAbs(chunk, optionX, 36, gen.endZ - 1);
				if ( b != null )
				{
					b.setType(sign);
					b.setData((byte)0x2);
					Sign s = (Sign)b.getState();
					s.setLine(1, "§fOption:");
					s.setLine(2, "§f???");
					s.update();
				}
				
				b = getBlockAbs(chunk, optionX, 35, gen.endZ);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(colorOptionOff);
				}
			}
		}
	}
}

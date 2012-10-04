package com.ftwinston.Killer;

import java.io.File;
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
	
	private int maxGameModeOptions, numWorldOptions;
	
	int endX, endZ, worldEndX, optionsEndX, gameModeEndZ;
	boolean gameModeSelectionClosedOff, worldSelectionClosedOff, gameModeOptionsClosedOff;
	
	public StagingWorldGenerator()
	{
        maxGameModeOptions = 0;
        for ( GameMode mode : GameMode.gameModes )
        	if ( mode.getOptions().size() > maxGameModeOptions )
        		maxGameModeOptions = mode.getOptions().size();
		
		// check custom worlds exist, if they don't, remove them
		for ( int i=0; i<Settings.customWorldNames.size(); i++ )
		{
			File folder = new File(Killer.instance.getServer().getWorldContainer() + File.separator + Settings.customWorldNames.get(i));
			if ( Settings.customWorldNames.get(i).length() > 0 && folder.exists() && folder.isDirectory() )
				continue;
			
			Settings.customWorldNames.remove(i);
			i--;
		}
		if ( Settings.customWorldNames.size() == 0 )
			Settings.allowRandomWorlds = true; // If no custom worlds (are left), always allow random worlds
		
		if ( Settings.allowRandomWorlds )
			numWorldOptions = Settings.customWorldNames.size() + numRandomWorldOptions;
		else
			numWorldOptions = Settings.customWorldNames.size();
		// if random world generation is disabled, and no custom world names are provided ... what then?
		
		
		// now set up helper values for where the various "extensible" things end
		endX = Math.max(Math.max(numWorldOptions, maxGameModeOptions) * 2 + 8, 20);
		endZ = Math.max(GameMode.gameModes.size() * 3 + 12, 22);
						
		startButtonX = endX - 1;
		forceStartButtonZ = endZ / 2;
		overrideButtonZ = forceStartButtonZ + 1;
		cancelButtonZ = forceStartButtonZ - 1;
		gameModeButtonX = 1;
		worldOptionZ = 1;
		gameModeOptionZ = endZ - 1;
		
		worldEndX = numWorldOptions * 2 + 8;
		optionsEndX = maxGameModeOptions == 0 ? 4 : maxGameModeOptions * 2 + 8;
		gameModeEndZ = GameMode.gameModes.size() * 3 + 8;
		
		// todo: decide these, based on config or whatever
		gameModeSelectionClosedOff = GameMode.gameModes.size() < 2 ;
		worldSelectionClosedOff = numWorldOptions < 2;
		gameModeOptionsClosedOff = maxGameModeOptions < 2;
	}
	
	public static int startButtonX, forceStartButtonZ, overrideButtonZ, cancelButtonZ, gameModeButtonX, worldOptionZ, gameModeOptionZ;
	
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
		Location loc = new Location(world, 8.5, 2, forceStartButtonZ + 0.5);
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
					s.setLine(0, "§fWorld:");
					
					if ( Settings.allowRandomWorlds && num == 0 )
						s.setLine(1, "§fNormal Random");
					else
					{
						String name = Settings.customWorldNames.get(Settings.allowRandomWorlds ? num - 1 : num);
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
			
			// south wall
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
			int modeZ = 8;
						
			while ( modeZ < gen.gameModeEndZ - 2 && num < GameMode.gameModes.size() )
			{
				b = getBlockAbs(chunk, 1, 3, modeZ);
				if ( b != null )
				{
					b.setType(button);
					b.setData((byte)0x1);
				}
				
				GameMode gameMode = GameMode.get(num);
				b = getBlockAbs(chunk, 1, 4, modeZ);
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
				
				b = getBlockAbs(chunk, 0, 3, modeZ);
				if ( b != null )
					b.setType(lamp);
				
				// now the game mode DESCRIPTION signs
				b = getBlockAbs(chunk, 1, 4, modeZ-1);
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
				
				b = getBlockAbs(chunk, 1, 3, modeZ-1);
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
				
				b = getBlockAbs(chunk, 1, 2, modeZ-1);
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
				
				modeZ += 3;
				num ++;
			}
			
			
			//
			// the main room
			//
			
			// ceiling
			for ( int x=5; x<=gen.endX; x++ )
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

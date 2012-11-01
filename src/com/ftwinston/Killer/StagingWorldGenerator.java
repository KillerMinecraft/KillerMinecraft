package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.plugin.Plugin;

public class StagingWorldGenerator extends org.bukkit.generator.ChunkGenerator
{
	public static int maxGameModeOptions;
	
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
		
		progressStartZ = 3; progressEndZ = endZ - 7;
		
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
		gameModeOptionsClosedOff = true; // always closed off initially, until a game mode is chosen
	}
	
	public static int startButtonX, startButtonZ, overrideButtonZ, cancelButtonZ, gameModeButtonX, worldOptionZ, gameModeOptionZ, progressStartZ, progressEndZ;
	
	public static byte colorOptionOn = 5 /* lime */, colorOptionOff = 14 /* red*/,
		colorStartButton = 4 /* yellow */, colorOverrideButton = 1 /* red */, colorCancelButton = 9 /* green */;
	
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

			Material floor = Material.REDSTONE_LAMP_ON;
			Material wall = Material.SMOOTH_BRICK;
			Material ceiling = Material.DOUBLE_STEP;
			Material ceilingPanel = Material.STEP;
			byte ceilingPanelDataValue = 0x8;
			
			Material wool = Material.WOOL;
			Material button = Material.STONE_BUTTON;
			Material sign = Material.WALL_SIGN;
			Block b;
			
			int wallMinX = 0, wallMaxX = 22, wallMinZ = 0, wallMaxZ = 29, floorY = 32, ceilingMinY = 43, ceilingMaxY = 49;
			byte textColorInfo = 5, textColorGame = 4, textColorChoose = 1;
			
			// floor ... now with redstone torches underneath
			for ( int x=wallMinX+1; x<wallMaxX; x++ )
				for ( int z=wallMinZ; z<wallMaxZ; z++ )
					for ( int y=floorY; y>floorY-2; y-- )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							//b.setType((x + 1) % 4 == 0 && (z + 1) % 4 == 0 ? Material.GLOWSTONE : floor);
							b.setType(floor);
						
						b = getBlockAbs(chunk, x, y-2, z);
						if ( b != null )
							b.setType(Material.STONE);
						
						b = getBlockAbs(chunk, x, y-1, z);
						if ( b != null )
						{
							b.setType(Material.REDSTONE_TORCH_ON);
							b.setData((byte)5);
						}
					}
			
			// front wall
			for ( int x=wallMinX+1; x<wallMaxX; x++ )
				for ( int y=floorY; y<=ceilingMaxY; y++ )
					for ( int z=wallMinZ; z>wallMinZ-2; z-- )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(wall);
					}
			
			// back wall
			for ( int x=wallMinX+1; x<wallMaxX; x++ )
				for ( int y=floorY; y<=ceilingMinY; y++ )
					for ( int z=wallMaxZ; z<wallMaxZ+2; z++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(wall);
					}
			
			// sloped ceiling
			int z = wallMaxZ-1;
			for ( int y=ceilingMinY; y<=ceilingMaxY; y++ )
			{
				for ( int x=wallMinX+1; x<wallMaxX; x++ )
				{
					b = getBlockAbs(chunk, x, y, z);
					if ( b != null )
						b.setType(ceiling);
					b = getBlockAbs(chunk, x, y, z-1);
					if ( b != null )
						b.setType(ceiling);
					b = getBlockAbs(chunk, x, y, z-2);
					if ( b != null )
						b.setType(ceiling);
					b = getBlockAbs(chunk, x, y, z-3);
					if ( b != null )
						b.setType(ceiling);
					
					b = getBlockAbs(chunk, x, y+1, z);
					if ( b != null )
						b.setType(ceiling);
					b = getBlockAbs(chunk, x, y+1, z-1);
					if ( b != null )
						b.setType(ceiling);
					b = getBlockAbs(chunk, x, y+1, z-2);
					if ( b != null )
						b.setType(ceiling);
					b = getBlockAbs(chunk, x, y+1, z-3);
					if ( b != null )
						b.setType(ceiling);
					
					b = getBlockAbs(chunk, x, y-1, z);
					if ( b != null )
					{
						b.setType(ceilingPanel);
						b.setData(ceilingPanelDataValue);
					}
					b = getBlockAbs(chunk, x, y-1, z-1);
					if ( b != null )
					{
						b.setType(ceilingPanel);
						b.setData(ceilingPanelDataValue);
					}
				}
				z -= 4;
			}
			
			// side walls with sloped tops
			int yMax = ceilingMinY;
			for ( z = wallMaxZ; z>=wallMinZ; z-=4 )
			{
				for ( int y=floorY; y<=yMax; y++ )
				{
					for ( int x=wallMinX; x>wallMinX-2; x-- )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(wall);
						b = getBlockAbs(chunk, x, y, z-1);
						if ( b != null )
							b.setType(wall);
						b = getBlockAbs(chunk, x, y, z-2);
						if ( b != null )
							b.setType(wall);
						b = getBlockAbs(chunk, x, y, z-3);
						if ( b != null )
							b.setType(wall);
					}
					for ( int x=wallMaxX; x<wallMaxX+2; x++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(wall);
						b = getBlockAbs(chunk, x, y, z-1);
						if ( b != null )
							b.setType(wall);
						b = getBlockAbs(chunk, x, y, z-2);
						if ( b != null )
							b.setType(wall);
						b = getBlockAbs(chunk, x, y, z-3);
						if ( b != null )
							b.setType(wall);
					}
				}
				yMax++;
			}
			
			// extra light, to make the high text more visible
			for ( int x=wallMinX+1; x<wallMaxX; x++ )
			{
				b = getBlockAbs(chunk, x, ceilingMaxY-1, wallMinZ+5);
				if ( b != null )
					b.setType(Material.GLOWSTONE);
			}
			
			// write on the walls
			boolean[][] text = writeBlockText("SETUP");
			for ( int i=0; i<text.length; i++ )
				for ( int j=0; j<text[i].length; j++ )
				{
					b = getBlockAbs(chunk, i + wallMinX + 2, j + floorY + 11, wallMinZ);
					if ( b != null )
						if ( text[i][j] )
						{
							b.setType(wool);
							b.setData(textColorGame);
						}
						else
							b.setType(wall);
				}
			text = writeBlockText("GAME");
			for ( int i=0; i<text.length; i++ )
				for ( int j=0; j<text[i].length; j++ )
				{
					b = getBlockAbs(chunk, i + wallMinX + 3, j + floorY + 5, wallMinZ);
					if ( b != null )
						if ( text[i][j] )
						{
							b.setType(wool);
							b.setData(textColorGame);
						}
						else
							b.setType(wall);
				}
			
			text = writeBlockText("INFO");
			for ( int i=0; i<text.length; i++ )
				for ( int j=0; j<text[i].length; j++ )
				{
					b = getBlockAbs(chunk, wallMinX, j + floorY + 5, wallMinZ + text.length + 4 - i);
					if ( b != null )
						if ( text[i][j] )
						{
							b.setType(wool);
							b.setData(textColorInfo);
						}
						else
							b.setType(wall);
				}
			
			text = writeBlockText("CHOOSE");
			for ( int i=0; i<text.length; i++ )
				for ( int j=0; j<text[i].length; j++ )
				{
					b = getBlockAbs(chunk, wallMaxX, j + floorY + 5, wallMinZ + 4 + i);
					if ( b != null )
						if ( text[i][j] )
						{
							b.setType(wool);
							b.setData(textColorChoose);
						}
						else
							b.setType(wall);
				}
			
/*		
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
					WorldOption option = WorldOption.get(num);
					
					s.setLine(0, option.isFixedWorld() ? "Custom world:" : "Random world:");
					StagingWorldGenerator.fitTextOnSign(s, option.getName().replace('_', ' '));
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
					s.setLine(0, "§1Game mode:");
					
					String name = gameMode.getName();
					if ( name.length() > 12 )
					{
						String[] words = name.split(" ");
						s.setLine(1, "§1" + words[0]);
						if ( words.length > 1)
						{
							s.setLine(2, "§1" + words[1]);
							if ( words.length > 2)
								s.setLine(3, "§1" + words[2]);
						}
					}
					else
						s.setLine(1, "§1" + name);
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
				}*/
		}
	}

	public static void fitTextOnSign(Sign s, String text)
	{
		if ( text.length() <= 15 )
		{
			s.setLine(2, text);
			return;
		}
		
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
		
		if ( lines[2] != null )
		{
			s.setLine(1, lines[0]);
			s.setLine(2, lines[1]);
			s.setLine(3, lines[2]);
		}
		else
		{
			s.setLine(2, lines[0]);
			if ( lines[1] != null )
				s.setLine(3, lines[1]);
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
}

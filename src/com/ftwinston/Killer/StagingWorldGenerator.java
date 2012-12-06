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
	public static final int floorY = 32, ceilingY = 38, wallMaxX = -2, wallMinCorridorX = -8, wallMinInfoX = -13, backWallMinX = -19, wallMinCorridorZ = 1, wallMinInfoZ = 21, wallMaxZ = wallMinInfoZ + 8, buttonY = floorY + 2,
			mainButtonX = wallMinCorridorX + 1, optionButtonX = wallMaxX - 1, gameModeButtonZ = wallMinInfoZ - 1, gameModeConfigButtonZ = gameModeButtonZ - 2, worldButtonZ = gameModeConfigButtonZ - 3,
			worldConfigButtonZ = worldButtonZ - 2,  monstersButtonZ = worldConfigButtonZ - 3, animalsButtonZ = monstersButtonZ - 2, globalOptionButtonZ = animalsButtonZ - 3, startButtonX = wallMaxX - 3, startButtonZ = wallMinCorridorZ + 1,
			overrideButtonX = startButtonX + 1, cancelButtonX = startButtonX - 1, exitButtonX = -12,
			waitingButtonZ = wallMaxZ + 2, waitingSpleefButtonX = wallMaxX-3, waitingMonsterButtonX = waitingSpleefButtonX - 5,
			spleefY = floorY-2, spleefMaxX = wallMaxX + 2, spleefMinX = spleefMaxX - 15, spleefMinZ = wallMaxZ + 9, spleefMaxZ = spleefMinZ + 15, spleefPressurePlateZ = spleefMinZ-2;
	
	public static final byte colorOptionOn = 5 /* lime */, colorOptionOff = 14 /* red*/,
		colorStartButton = 4 /* yellow */, colorOverrideButton = 1 /* orange */, colorCancelButton = 9 /* teal */,
		signBackColor = 7 /* grey */, colorExitButton = 15 /* black */;
	
	public static int getOptionButtonZ(int num, boolean forGameModes) { return wallMinCorridorZ + 2 + num * (forGameModes ? 3 : 2); }
	
	public static int getOptionNumFromZ(int z, boolean forGameModes) { return (z - wallMinCorridorZ - 2) / (forGameModes ? 3 : 2); }
	
	@Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return Arrays.asList((BlockPopulator)new StagingWorldPopulator(this));
    }
	
    @Override
    public boolean canSpawn(World world, int x, int z) {
        return x == 0 && z == 1;
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
			if ( chunk.getX() >= 1 || chunk.getZ() < 0 || chunk.getX() < -3 || chunk.getZ() > 4 )
				return;

			Material floor = Material.STONE;
			Material wall = Material.SMOOTH_BRICK;
			Material ceiling = Material.GLOWSTONE;
			
			Material wool = Material.WOOL;
			Material button = Material.STONE_BUTTON;
			Block b;
			
			// floor
			for ( int x=wallMaxX+1; x>wallMinCorridorX-2; x-- )
				for ( int z=wallMinCorridorZ-1; z<wallMaxZ+4; z++ )
					for ( int y=floorY; y>floorY-2; y-- )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(floor);
					}
			for ( int x=wallMinCorridorX+1; x>wallMinInfoX-2; x-- )
				for ( int z=wallMinInfoZ-1; z<wallMaxZ+4; z++ )
					for ( int y=floorY; y>floorY-2; y-- )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(floor);
					}
			
			// ceiling
			for ( int x=wallMaxX+1; x>wallMinCorridorX-2; x-- )
				for ( int z=wallMinCorridorZ-1; z<wallMaxZ+4; z++ )
					for ( int y=ceilingY; y<ceilingY+2; y++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(ceiling);
					}
			for ( int x=wallMinCorridorX+1; x>wallMinInfoX-2; x-- )
				for ( int z=wallMinInfoZ-1; z<wallMaxZ+4; z++ )
					for ( int y=ceilingY; y<ceilingY+2; y++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(ceiling);
					}
			
			// walls
			for ( int x=wallMaxX+1; x>=wallMaxX; x-- )
				for ( int z=wallMinCorridorZ-1; z<wallMaxZ; z++ )
					for ( int y=floorY+1; y<ceilingY; y++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(wall);
					}
			for ( int x=wallMinCorridorX; x>wallMinCorridorX-2; x-- )
				for ( int z=wallMinCorridorZ-1; z<wallMinInfoZ; z++ )
					for ( int y=floorY+1; y<ceilingY; y++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(wall);
					}
			
			int backWallMaxX = wallMaxX + 7;
			for ( int x=backWallMaxX; x>=backWallMinX-1; x-- )
				for ( int z=wallMaxZ; z<wallMaxZ+4; z++ )
					for ( int y=floorY+1; y<ceilingY; y++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(wall);
					}
			for ( int x=wallMinCorridorX; x>=wallMinInfoX; x-- )
				for ( int z=wallMinInfoZ; z>wallMinInfoZ-2; z-- )
					for ( int y=floorY+1; y<ceilingY; y++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(wall);
					}
			for ( int x=wallMinInfoX; x>wallMinInfoX-2; x-- )
				for ( int z=wallMinInfoZ-1; z<wallMaxZ+2; z++ )
					for ( int y=floorY+1; y<ceilingY; y++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(wall);
					}
			
			// extra bit of ceiling light towards the spleef arena 
			for ( int x=backWallMaxX; x>=backWallMinX-1; x-- )
				for ( int z=wallMaxZ; z<wallMaxZ+4; z++ )
					for ( int y=ceilingY; y<ceilingY+2; y++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(ceiling);
					}
			
			// help signs on the "info" wall
			for ( int y=floorY + 1; y < buttonY + 2; y++ )
			{
				b = getBlockAbs(chunk, wallMinInfoX + 3, y, wallMinInfoZ);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(signBackColor);
				}
			}
			b = getBlockAbs(chunk, wallMinInfoX + 3, buttonY + 1, wallMinInfoZ + 1);
			if ( b != null )
				setupSign(b, (byte)0x3, "Welcome to", "Killer", "Minecraft!");
			b = getBlockAbs(chunk, wallMinInfoX + 3, buttonY, wallMinInfoZ + 1);
			if ( b != null )
				setupSign(b, (byte)0x3, "This is the", "staging world.", "It's used to", "set up games.");
			b = getBlockAbs(chunk, wallMinInfoX + 3, buttonY - 1, wallMinInfoZ + 1);
			if ( b != null )
				setupSign(b, (byte)0x3, "Read your", "instruction", "book if you", "need any help.");
			
			// buttons and signs on the setup wall
			for ( int y=floorY + 1; y < floorY + 4; y++ )
			{
				b = getBlockAbs(chunk, wallMinCorridorX, y, gameModeButtonZ-1);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(signBackColor);
				}
			}
			b = getBlockAbs(chunk, mainButtonX, buttonY + 1, gameModeButtonZ-1);
			if ( b != null )
			{
				b.setType(Material.WALL_SIGN);
				b.setData((byte)0x5);
				Sign s = (Sign)b.getState();
				s.setLine(0, "Game mode:");
				
				fitTextOnSign(s, Killer.instance.getGameMode().getName());
				s.update();
			}
			b = getBlockAbs(chunk, mainButtonX, buttonY, gameModeButtonZ-1);
			if ( b != null )
				setupSign(b, (byte)0x5, "<-- change     ", "", "", "  configure -->");
			b = getBlockAbs(chunk, wallMinCorridorX, buttonY, gameModeButtonZ);
			if ( b != null )
			{
				b.setType(wool);
				b.setData(colorOptionOff);
			}
			b = getBlockAbs(chunk, mainButtonX, buttonY, gameModeButtonZ);
			if ( b != null )
			{
				b.setType(button);
				b.setData((byte)0x1);
			}
			
			b = getBlockAbs(chunk, wallMinCorridorX, buttonY, gameModeConfigButtonZ);
			if ( b != null )
			{
				b.setType(wool);
				b.setData(colorOptionOff);
			}
			b = getBlockAbs(chunk, mainButtonX, buttonY, gameModeConfigButtonZ);
			if ( b != null )
			{
				b.setType(button);
				b.setData((byte)0x1);
			}
			
			
			for ( int y=floorY + 1; y < floorY + 4; y++ )
			{
				b = getBlockAbs(chunk, wallMinCorridorX, y, worldButtonZ-1);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(signBackColor);
				}
			}
			b = getBlockAbs(chunk, mainButtonX, buttonY+1, worldButtonZ-1);
			if ( b != null )
			{
				b.setType(Material.WALL_SIGN);
				b.setData((byte)0x5);
				Sign s = (Sign)b.getState();
				s.setLine(0, "World:");
				
				fitTextOnSign(s, Killer.instance.getWorldOption().getName());
				s.update();
			}
			b = getBlockAbs(chunk, mainButtonX, buttonY, worldButtonZ-1);
			if ( b != null )
				setupSign(b, (byte)0x5, "<-- change     ", "", "", "  configure -->");
			b = getBlockAbs(chunk, wallMinCorridorX, buttonY, worldButtonZ);
			if ( b != null )
			{
				b.setType(wool);
				b.setData(colorOptionOff);
			}
			b = getBlockAbs(chunk, mainButtonX, buttonY, worldButtonZ);
			if ( b != null )
			{
				b.setType(button);
				b.setData((byte)0x1);
			}
			
			b = getBlockAbs(chunk, wallMinCorridorX, buttonY, worldConfigButtonZ);
			if ( b != null )
			{
				b.setType(wool);
				b.setData(colorOptionOff);
			}
			b = getBlockAbs(chunk, mainButtonX, buttonY, worldConfigButtonZ);
			if ( b != null )
			{
				b.setType(button);
				b.setData((byte)0x1);
			}
			
			for ( int y=floorY + 1; y < floorY + 4; y++ )
			{
				b = getBlockAbs(chunk, wallMinCorridorX, y, monstersButtonZ - 1);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(signBackColor);
				}
			}
			b = getBlockAbs(chunk, mainButtonX, buttonY + 1, monstersButtonZ - 1);
			if ( b != null )
				setupSign(b, (byte)0x5, "Monsters:      ", padSignLeft(getQuantityText(Killer.instance.monsterNumbers)), "Animals:       ", padSignLeft(getQuantityText(Killer.instance.monsterNumbers)));
			b = getBlockAbs(chunk, mainButtonX, buttonY, monstersButtonZ - 1);
			if ( b != null )
				setupSign(b, (byte)0x5, "<-- monsters   ", "", "", "    animals -->");
			b = getBlockAbs(chunk, wallMinCorridorX, buttonY, monstersButtonZ);
			if ( b != null )
			{
				b.setType(wool);
				b.setData(colorOptionOff);
			}
			b = getBlockAbs(chunk, mainButtonX, buttonY, monstersButtonZ);
			if ( b != null )
			{
				b.setType(button);
				b.setData((byte)0x1);
			}
			
			b = getBlockAbs(chunk, wallMinCorridorX, buttonY, animalsButtonZ);
			if ( b != null )
			{
				b.setType(wool);
				b.setData(colorOptionOff);
			}
			b = getBlockAbs(chunk, mainButtonX, buttonY, animalsButtonZ);
			if ( b != null )
			{
				b.setType(button);
				b.setData((byte)0x1);
			}

			for ( int y=floorY + 1; y < floorY + 4; y++ )
			{
				b = getBlockAbs(chunk, wallMinCorridorX, y, globalOptionButtonZ);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(signBackColor);
				}
			}
			b = getBlockAbs(chunk, mainButtonX, buttonY + 1, globalOptionButtonZ);
			if ( b != null )
				setupSign(b, (byte)0x5, "", "Global", "options");
				
			b = getBlockAbs(chunk, wallMinCorridorX, buttonY, globalOptionButtonZ);
			if ( b != null )
			{
				b.setType(wool);
				b.setData(colorOptionOff);
			}
			b = getBlockAbs(chunk, mainButtonX, buttonY, globalOptionButtonZ);
			if ( b != null )
			{
				b.setType(button);
				b.setData((byte)0x1);
			}
			
			// start button and red surround
			for ( int x = wallMaxX+1; x>wallMinCorridorX; x-- )
				for ( int z = wallMinCorridorZ; z>wallMinCorridorZ-2; z-- )
					for ( int y=floorY+1; y<ceilingY; y++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
						{
							b.setType(wool);
							b.setData(colorOptionOff);
						}
					}
			b = getBlockAbs(chunk, startButtonX, buttonY + 1, startButtonZ);
			if ( b != null )
				setupSign(b, (byte)0x3, "", "Start", "Game");
			b = getBlockAbs(chunk, startButtonX, buttonY, wallMinCorridorZ);
			if ( b != null )
			{
				b.setType(wool);
				b.setData(colorStartButton);
			}
			b = getBlockAbs(chunk, startButtonX, buttonY, startButtonZ);
			if ( b != null )
			{
				b.setType(button);
				b.setData((byte)0x3);
			}
			
			// spleef arena setup room
			for ( int x=waitingSpleefButtonX; x>=waitingMonsterButtonX; x-- )
				for ( int z=wallMaxZ+1; z<wallMaxZ+4; z++ )
					for ( int y=floorY+1; y<ceilingY; y++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(Material.AIR);
					}
			for ( int x=waitingSpleefButtonX-2; x>=waitingSpleefButtonX-3; x-- )
				for ( int y=floorY+1; y<=floorY+2; y++ )
				{
					b = getBlockAbs(chunk, x, y, wallMaxZ);
					if ( b != null )
						b.setType(Material.AIR);
				}
				
			// signs over spleef setup room door
			b = getBlockAbs(chunk, waitingSpleefButtonX - 2, floorY + 3, wallMaxZ-1);
			if ( b != null )
				setupSign(b, (byte)0x2, "Waiting        ", "for         ", "other         ", "players?       ");
			b = getBlockAbs(chunk, waitingSpleefButtonX - 3, floorY + 3, wallMaxZ-1);
			if ( b != null )
				setupSign(b, (byte)0x2, "       Distract", "       yourself", "       in here!");
				
			// spleef setup buttons
			for ( int y=floorY + 1; y < floorY + 4; y++ )
			{
				b = getBlockAbs(chunk, waitingSpleefButtonX+1, y, waitingButtonZ);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(signBackColor);
				}
			}
			b = getBlockAbs(chunk, waitingSpleefButtonX, buttonY+1, waitingButtonZ);
			if ( b != null )
				setupSign(b, (byte)0x4, "", "Spleef", "Arena");
			b = getBlockAbs(chunk, waitingSpleefButtonX+1, buttonY, waitingButtonZ);
			if ( b != null )
			{
				b.setType(wool);
				b.setData(colorOptionOn);
			}
			b = getBlockAbs(chunk, waitingSpleefButtonX, buttonY, waitingButtonZ);
			if ( b != null )
			{
				b.setType(button);
				b.setData((byte)0x2);
			}
			
			for ( int y=floorY + 1; y < floorY + 4; y++ )
			{
				b = getBlockAbs(chunk, waitingMonsterButtonX-1, y, waitingButtonZ);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(signBackColor);
				}
			}
			b = getBlockAbs(chunk, waitingMonsterButtonX, buttonY+1, waitingButtonZ);
			if ( b != null )
				setupSign(b, (byte)0x5, "", "Monster", "Arena");
			b = getBlockAbs(chunk, waitingMonsterButtonX-1, buttonY, waitingButtonZ);
			if ( b != null )
			{
				b.setType(wool);
				b.setData(colorOptionOff);
			}
			b = getBlockAbs(chunk, waitingMonsterButtonX, buttonY, waitingButtonZ);
			if ( b != null )
			{
				b.setType(button);
				b.setData((byte)0x1);
			}
			
			// spleef arena itself
			for ( int x=spleefMinX; x<=spleefMaxX; x++ )
				for ( int z=spleefMinZ; z<=spleefMaxZ; z++ )
				{
					b = getBlockAbs(chunk, x, spleefY, z);
					if ( b != null )
						b.setType(Material.DIRT);
				}
			
			// floor to fall onto, to ensure monsters die
			for ( int x = spleefMinX - 7; x <= spleefMaxX + 7; x++ )
				for ( int z=spleefMinZ - 7; z <= spleefMaxZ + 7; z++ )
				{
					b = getBlockAbs(chunk, x, 0, z);
					if ( b != null )
						b.setType(Material.OBSIDIAN);					
				}
				
			// spleef viewing gallery
			for ( int x = spleefMinX - 5; x <= spleefMaxX + 5; x++ )
			{
				for ( int z=spleefMinZ - 3; z>spleefMinZ - 6; z-- )
				{
					b = getBlockAbs(chunk, x, floorY, z);
					if ( b != null )
						b.setType(floor);
					for ( int y=floorY-1; y>floorY-3; y-- )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(Material.GLOWSTONE);
					}
				}
				for ( int z=spleefMaxZ + 3; z<spleefMaxZ + 6; z++ )
				{
					b = getBlockAbs(chunk, x, floorY, z);
					if ( b != null )
						b.setType(floor);
					for ( int y=floorY-1; y>floorY-3; y-- )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(Material.GLOWSTONE);
					}
				}
			}
			for ( int z=spleefMinZ - 2; z<=spleefMaxZ + 2; z++ )
			{
				for ( int x = spleefMinX - 3; x > spleefMinX - 6; x-- )
				{
					b = getBlockAbs(chunk, x, floorY, z);
					if ( b != null )
						b.setType(floor);
					for ( int y=floorY-1; y>floorY-3; y-- )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(Material.GLOWSTONE);
					}
				}
				for ( int x = spleefMaxX + 3; x < spleefMaxX + 6; x++ )
				{
					b = getBlockAbs(chunk, x, floorY, z);
					if ( b != null )
						b.setType(floor);
					for ( int y=floorY-1; y>floorY-3; y-- )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(Material.GLOWSTONE);
					}
				}
			}
			
			// inner railings
			for ( int z=spleefMinZ - 3; z<=spleefMaxZ + 3; z++ )
			{
				b = getBlockAbs(chunk, spleefMaxX+3, floorY+1, z);
				if ( b != null )
					b.setType(Material.FENCE);
				b = getBlockAbs(chunk, spleefMinX-3, floorY+1, z);
				if ( b != null )
					b.setType(Material.FENCE);
			}
			for ( int x=spleefMinX - 3; x<=spleefMaxX + 3; x++ )
			{
				b = getBlockAbs(chunk, x, floorY+1, spleefMaxZ+3);
				if ( b != null )
					b.setType(Material.FENCE);
			
				if ( x < waitingSpleefButtonX - 1 && x > waitingSpleefButtonX - 4 )
					continue; // gap for the doors
				b = getBlockAbs(chunk, x, floorY+1, spleefMinZ-3);
				if ( b != null )
					b.setType(Material.FENCE);
			}
			
			// outer railing
			for ( int z=spleefMinZ - 5; z<=spleefMaxZ + 5; z++ )
			{
				b = getBlockAbs(chunk, spleefMaxX+5, floorY+1, z);
				if ( b != null )
					b.setType(Material.FENCE);
				b = getBlockAbs(chunk, spleefMinX-5, floorY+1, z);
				if ( b != null )
					b.setType(Material.FENCE);
			}
			for ( int x=spleefMinX - 5; x<=spleefMaxX + 5; x++ )
			{
				b = getBlockAbs(chunk, x, floorY+1, spleefMaxZ+5);
				if ( b != null )
					b.setType(Material.FENCE);
			}
			
			// pressure plates (and floor underneath them)
			for ( int x = waitingSpleefButtonX - 2; x>waitingSpleefButtonX - 4; x-- )
			{
				b = getBlockAbs(chunk, x, floorY, spleefPressurePlateZ);
				if ( b != null )
					b.setType(floor);
				
				b = getBlockAbs(chunk, x, floorY+1, spleefPressurePlateZ);
				if ( b != null )
					b.setType(Material.STONE_PLATE);
			}
			
			// "exit" sign & button
			if ( !Killer.instance.stagingWorldIsServerDefault )
			{
				int exitButtonZ = wallMaxZ - 4; 
				b = getBlockAbs(chunk, exitButtonX, buttonY+1, exitButtonZ);
				if ( b != null )
					setupSign(b, (byte)0x5, "Push to exit", "Killer and", "return to the", "main world");
				
				b = getBlockAbs(chunk, exitButtonX-1, buttonY, exitButtonZ);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(colorExitButton);
				}
				
				b = getBlockAbs(chunk, exitButtonX, buttonY, exitButtonZ);
				if ( b != null )
				{
					b.setType(button);
					b.setData((byte)0x1);
				}
			}
		}
	}

	public static void setupSign(Block b, byte orientation, String... lines)
	{
		b.setType(Material.WALL_SIGN);
		b.setData(orientation);
		Sign s = (Sign)b.getState();
		
		for ( int i=0; i<4 && i<lines.length; i++ )
			s.setLine(i, lines[i]);
		for ( int i=lines.length; i<4; i++ )
			s.setLine(i, "");
		s.update();
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
			s.setLine(1, "");
			s.setLine(2, lines[0]);
			if ( lines[1] != null )
				s.setLine(3, lines[1]);
			else
				s.setLine(3, "");
		}
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
}

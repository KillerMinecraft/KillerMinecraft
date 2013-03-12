package com.ftwinston.Killer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
	public static final int wallMaxX = 9, wallMinX = 3, wallMinZ = 1, startButtonZ = wallMinZ + 1, mainButtonX = wallMinX + 1,
			optionButtonX = wallMaxX - 1, animalsButtonZ = startButtonZ + 2, monstersButtonZ = animalsButtonZ + 1,
			difficultyButtonZ = monstersButtonZ + 1, globalOptionButtonZ = startButtonZ + 6, worldConfigButtonZ = globalOptionButtonZ + 3,
			worldButtonZ = worldConfigButtonZ + 2, gameModeConfigButtonZ = worldButtonZ + 3, gameModeButtonZ = gameModeConfigButtonZ + 2,
			startButtonX = wallMaxX - 3, overrideButtonX = startButtonX + 1, cancelButtonX = startButtonX - 1,
			arenaSpleefButtonX = startButtonX + 2, arenaMonsterButtonX = startButtonX - 2, spleefY = 29, spleefMaxX = startButtonX + 8,
			spleefMinX = spleefMaxX - 16;
	public static int arenaButtonZ, spleefMinZ, spleefMaxZ, spleefPressurePlateZ, exitPortalZ, playerLimitZ;
	
	public static final byte colorOptionOn = 5 /* lime */, colorOptionOff = 14 /* red*/,
		colorStartButton = 4 /* yellow */, colorOverrideButton = 1 /* orange */, colorCancelButton = 9 /* teal */,
		signBackColor = 7 /* grey */, colorDoorway = 15 /* black */;
	
	public static int getOptionButtonZ(int num, boolean forGameModes) { return wallMinZ + 2 + num * (forGameModes ? 3 : 2); }
	
	public static int getOptionNumFromZ(int z, boolean forGameModes) { return (z - wallMinZ - 2) / (forGameModes ? 3 : 2); }
	
	public static final int baseFloorY = 32, floorSeparation = 9, buttonOffset = 2, ceilingOffset = 6;
	public static int getFloorY(int gameNum)
	{
		if ( Settings.maxSimultaneousGames == 1 )
			return baseFloorY;
		return baseFloorY + floorSeparation + gameNum * floorSeparation;
	}
	
	public static int getButtonY(int gameNum) { return getFloorY(gameNum) + buttonOffset; }
	
	private static int wallMaxZ = gameModeButtonZ + 8;
	public static int getWallMaxZ() { return wallMaxZ; }
	public static void setWallMaxZ(int max)
	{
		wallMaxZ = max;
		arenaButtonZ = wallMaxZ + 2; spleefMinZ = wallMaxZ + 9; spleefMaxZ = spleefMinZ + 16; spleefPressurePlateZ = spleefMinZ-2; exitPortalZ = wallMaxZ - 2; playerLimitZ = wallMaxZ - 1;
		
		worldInfo = new ArrayList<String>();
		
		worldInfo.add("volume " + (startButtonX-1) + " " + (baseFloorY+1) + " " + (gameModeButtonZ+7) + " "
					+ (startButtonX+1) + " " + (baseFloorY+1) + " " + (gameModeButtonZ+9) + " spawn 180");
		
		worldInfo.add("volume -100 0 -100 100 128 100 protected");
		
		worldInfo.add("volume " + spleefMinX + " " + spleefY + " " + spleefMinZ + " "
					+ spleefMaxX + " " + (spleefY+3) + " " + spleefMaxZ + " arena 0 SPLEEF");
	}

	public static int getGamePortalX(int i)
	{
		return startButtonX - (Settings.maxSimultaneousGames * 3 + Settings.maxSimultaneousGames + 1)/2 + 4 * i + 2;
	}
	
	public static int getGamePortalZ() { return getWallMaxZ() - 13; }
	
	static List<String> worldInfo;
	public void saveWorldInfo(Killer plugin, World stagingWorld)
	{
		File infoFile = new File(stagingWorld.getWorldFolder(), "killer.txt");
		try
		{
			infoFile.createNewFile();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
			return;
		}
		
		if ( !infoFile.exists() || !infoFile.canWrite() )
		{
			plugin.log.warning("Cant find/write killer.txt in staging world folder!");
			return;
		}
		
		FileWriter fw;
		try
		{
			fw = new FileWriter(infoFile);
		} catch (IOException e)
		{
			e.printStackTrace();
			return;
		}

		try
		{
			BufferedWriter bw = new BufferedWriter(fw);
			
			for ( String line : worldInfo )
			{
				bw.write(line);
				bw.write('\n');
			}					
						
			bw.close();
			fw.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
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
			if ( chunk.getX() >= 2 || chunk.getZ() < 0 || chunk.getX() < -1 || chunk.getZ() > 4 )
				return;

			Material floor = Material.STONE;
			Material wall = Material.SMOOTH_BRICK;
			Material ceiling = Material.GLOWSTONE;
			
			Material wool = Material.WOOL;
			Material button = Material.STONE_BUTTON;
			Material command = Material.COMMAND;
			Block b;
			
			for ( int game = 0; game < Settings.maxSimultaneousGames; game++ )
			{
				int floorY = getFloorY(game), buttonY = getButtonY(game), ceilingY = floorY + ceilingOffset;
			
				// floor & ceiling
				for ( int x=wallMaxX+1; x>wallMinX-2; x-- )
					for ( int z=wallMinZ-1; z<getWallMaxZ()+4; z++ )
					{
						for ( int y=floorY; y>floorY-2; y-- )
						{
							b = getBlockAbs(chunk, x, y, z);
							if ( b != null )
								b.setType(floor);
						}
						
						for ( int y=ceilingY; y<ceilingY+2; y++ )
						{
							b = getBlockAbs(chunk, x, y, z);
							if ( b != null )
								b.setType(ceiling);
						}
					}
				
				// walls
				for ( int x=wallMaxX+1; x>=wallMaxX; x-- )
					for ( int z=wallMinZ-1; z<getWallMaxZ(); z++ )
						for ( int y=floorY+1; y<ceilingY; y++ )
						{
							b = getBlockAbs(chunk, x, y, z);
							if ( b != null )
								b.setType(wall);
						}
				for ( int x=wallMinX; x>wallMinX-2; x-- )
					for ( int z=wallMinZ-1; z<getWallMaxZ(); z++ )
						for ( int y=floorY+1; y<ceilingY; y++ )
						{
							b = getBlockAbs(chunk, x, y, z);
							if ( b != null )
								b.setType(wall);
						}
				
				// buttons and signs on the setup wall
				for ( int y=floorY + 1; y < floorY + 4; y++ )
				{
					b = getBlockAbs(chunk, wallMinX, y, gameModeButtonZ-1);
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
					worldInfo.add("sign " + b.getX() + " " + b.getY() + " " + b.getZ() + " GAME_MODE " + game);
				}
				b = getBlockAbs(chunk, mainButtonX, buttonY, gameModeButtonZ-1);
				if ( b != null )
					setupWallSign(b, (byte)0x5, "<-- change     ", "", "", "  configure -->");
				b = getBlockAbs(chunk, wallMinX, buttonY, gameModeButtonZ);
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
				b = getBlockAbs(chunk, wallMinX-1, buttonY, gameModeButtonZ);
				if ( b != null )
				{
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " show modes");
				}
				
				b = getBlockAbs(chunk, wallMinX, buttonY, gameModeConfigButtonZ);
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
				b = getBlockAbs(chunk, wallMinX-1, buttonY, gameModeConfigButtonZ);
				if ( b != null )
				{
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " show modeconfig");
				}
				
				
				for ( int y=floorY + 1; y < floorY + 4; y++ )
				{
					b = getBlockAbs(chunk, wallMinX, y, worldButtonZ-1);
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
					worldInfo.add("sign " + b.getX() + " " + b.getY() + " " + b.getZ() + " WORLD_OPTION " + game);
				}
				b = getBlockAbs(chunk, mainButtonX, buttonY, worldButtonZ-1);
				if ( b != null )
					setupWallSign(b, (byte)0x5, "<-- change     ", "", "", "  configure -->");
				b = getBlockAbs(chunk, wallMinX, buttonY, worldButtonZ);
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
				b = getBlockAbs(chunk, wallMinX-1, buttonY, worldButtonZ);
				if ( b != null )
				{
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " show worlds");
				}
				
				b = getBlockAbs(chunk, wallMinX, buttonY, worldConfigButtonZ);
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
				b = getBlockAbs(chunk, wallMinX-1, buttonY, worldConfigButtonZ);
				if ( b != null )
				{
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " show worldconfig");
				}
				
				b = getBlockAbs(chunk, wallMinX, buttonY, difficultyButtonZ);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(colorOptionOff);
				}
				
				b = getBlockAbs(chunk, mainButtonX, buttonY, difficultyButtonZ);
				if ( b != null )
				{
					b.setType(Material.WALL_SIGN);
					b.setData((byte)0x5);
					worldInfo.add("sign " + b.getX() + " " + b.getY() + " " + b.getZ() + " DIFFICULTY " + game);
					
					b = b.getRelative(0, 1, 0);
					b.setType(Material.WOOD_BUTTON);
					b.setData((byte)0x1);
					
					b = b.getRelative(0, -2, 0);
					b.setType(Material.WOOD_BUTTON);
					b.setData((byte)0x1);
				}
				b = getBlockAbs(chunk, wallMinX-1, buttonY, difficultyButtonZ);
				if ( b != null )
				{
					b = b.getRelative(0, 1, 0);
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " difficulty up");
					
					b = b.getRelative(0, -2, 0);
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " difficulty down");
				}
				
				b = getBlockAbs(chunk, wallMinX, buttonY, monstersButtonZ);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(colorOptionOff);
				}
				
				b = getBlockAbs(chunk, mainButtonX, buttonY, monstersButtonZ);
				if ( b != null )
				{					
					b.setType(Material.WALL_SIGN);
					b.setData((byte)0x5);
					worldInfo.add("sign " + b.getX() + " " + b.getY() + " " + b.getZ() + " MONSTERS " + game);
					
					b = b.getRelative(0, 1, 0);
					b.setType(Material.WOOD_BUTTON);
					b.setData((byte)0x1);
					
					b = b.getRelative(0, -2, 0);
					b.setType(Material.WOOD_BUTTON);
					b.setData((byte)0x1);
				}
				b = getBlockAbs(chunk, wallMinX-1, buttonY, monstersButtonZ);
				if ( b != null )
				{
					b = b.getRelative(0, 1, 0);
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " monsters up");
					
					b = b.getRelative(0, -2, 0);
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " monsters down");
				}
				
				b = getBlockAbs(chunk, wallMinX, buttonY, animalsButtonZ);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(colorOptionOff);
				}
				
				b = getBlockAbs(chunk, mainButtonX, buttonY, animalsButtonZ);
				if ( b != null )
				{
					b.setType(Material.WALL_SIGN);
					b.setData((byte)0x5);
					worldInfo.add("sign " + b.getX() + " " + b.getY() + " " + b.getZ() + " ANIMALS " + game);
					
					b = b.getRelative(0, 1, 0);
					b.setType(Material.WOOD_BUTTON);
					b.setData((byte)0x1);
					
					b = b.getRelative(0, -2, 0);
					b.setType(Material.WOOD_BUTTON);
					b.setData((byte)0x1);
				}
				b = getBlockAbs(chunk, wallMinX-1, buttonY, animalsButtonZ);
				if ( b != null )
				{
					b = b.getRelative(0, 1, 0);
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " animals up");
					
					b = b.getRelative(0, -2, 0);
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " animals down");
				}

				for ( int y=floorY + 1; y < floorY + 4; y++ )
				{
					b = getBlockAbs(chunk, wallMinX, y, globalOptionButtonZ);
					if ( b != null )
					{
						b.setType(wool);
						b.setData(signBackColor);
					}
				}
				b = getBlockAbs(chunk, mainButtonX, buttonY + 1, globalOptionButtonZ);
				if ( b != null )
					setupWallSign(b, (byte)0x5, "", "Global", "options");
					
				b = getBlockAbs(chunk, wallMinX, buttonY, globalOptionButtonZ);
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
				b = getBlockAbs(chunk, wallMinX-1, buttonY, globalOptionButtonZ);
				if ( b != null )
				{
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " show misc");
				}
				
				// start button and red surround
				for ( int x = wallMaxX-1; x>wallMinX; x-- )
					for ( int z = wallMinZ; z>wallMinZ-2; z-- )
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
					setupWallSign(b, (byte)0x3, "", "Start", "Game");
				b = getBlockAbs(chunk, startButtonX, buttonY, wallMinZ);
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
				b = getBlockAbs(chunk, startButtonX, buttonY, wallMinZ-1);
				if ( b != null )
				{
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " start");
				}
				b = getBlockAbs(chunk, startButtonX-1, buttonY, wallMinZ-1);
				if ( b != null )
				{
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " cancel");
				}
				b = getBlockAbs(chunk, startButtonX+1, buttonY, wallMinZ-1);
				if ( b != null )
				{
					b.setType(command);
					Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " confirm");
				}
				
				// if there's only one setup room, it links to the spleef arena. Otherwise, they're all separate.
				if ( Settings.maxSimultaneousGames != 1 )
				{
					int wallMaxZ = getWallMaxZ();
					// fill in end wall
					for ( int x=wallMaxX+1; x>=wallMinX-1; x-- )
						for ( int z=wallMaxZ; z<wallMaxZ+4; z++ )
							for ( int y=floorY+1; y<ceilingY; y++ )
							{
								b = getBlockAbs(chunk, x, y, z);
								if ( b != null )
									b.setType(wall);
							}
					
					// set up an exit portal
					for ( int z=wallMaxZ+1; z<wallMaxZ+3; z++ )
						for ( int y=floorY; y<floorY+4; y++ )
						{
							for ( int x=startButtonX-2; x<=startButtonX+2; x++ )
							{
								b = getBlockAbs(chunk, x, y, z);
								if ( b != null )
								{
									b.setType(wool);
									b.setData(colorDoorway);
								}
							}
						}
					for ( int y=floorY-1; y<floorY+8; y++ )
					{
						for ( int x=startButtonX-4; x<=startButtonX+4; x++ )
						{
							b = getBlockAbs(chunk, x, y, wallMaxZ+3);
							if ( b != null )
							{
								b.setType(wool);
								b.setData(colorDoorway);
							}
						}
					}
					
					for ( int z=wallMaxZ; z<wallMaxZ+3; z++ )
						for ( int y=floorY+1; y<floorY+3; y++ )
						{
							b = getBlockAbs(chunk, startButtonX, y, z);
							if ( b != null )
								b.setType(Material.AIR);
						}
					
					for ( int x=startButtonX-1; x<=startButtonX+1; x++ )
					{
						b = getBlockAbs(chunk, x, floorY+1, wallMaxZ+1);
						if ( b != null )
							b.setType(Material.TRIPWIRE);
					}
					b = getBlockAbs(chunk, startButtonX+2, floorY+1, wallMaxZ+1);
					if ( b != null )
					{
						b.setType(Material.TRIPWIRE_HOOK);
						b.setData((byte)5);
					}
					b = getBlockAbs(chunk, startButtonX-2, floorY+1, wallMaxZ+1);
					if ( b != null )
					{
						b.setType(Material.TRIPWIRE_HOOK);
						b.setData((byte)7);
					}
					
					b = getBlockAbs(chunk, startButtonX, floorY+3, wallMaxZ-1);
					if ( b != null )
						if ( Killer.instance.stagingWorldIsServerDefault)
							setupWallSign(b, (byte)0x2, "Arena", "and", "game selection");
						else
							setupWallSign(b, (byte)0x2, "Arena,", "game selection", "and exit to", "main world");
					
					// game number
					boolean[][] text = writeBlockText(Integer.toString(game + 1));
					int yMin = floorY + 1, zMax = gameModeButtonZ + text.length;
					for ( int i=0; i<text.length; i++ )
						for ( int j=0; j<text[i].length; j++ )
						{
							if ( !text[i][j] )
								continue;
							b = getBlockAbs(chunk, wallMinX, yMin + j, zMax - i);
							if ( b != null )
								b.setType(Material.IRON_BLOCK);
						}
					
					// help signs on the floor
					b = getBlockAbs(chunk, startButtonX - 1, floorY + 1, gameModeButtonZ + 5);
					if ( b != null )
						setupFloorSign(b, (byte)0xF, "This is the", "setup room.", "Configure your", "game here.");
					
					b = getBlockAbs(chunk, startButtonX + 1, floorY + 1, gameModeButtonZ + 5);
					if ( b != null )
						setupFloorSign(b, (byte)0x1, "Read your", "instruction", "book if you", "need any help.");
					
					// player limit controls
					if ( Settings.allowPlayerLimits )
					{
						b = getBlockAbs(chunk, mainButtonX + 1, floorY + 2, playerLimitZ);
						if ( b != null )
						{
							b.setType(Material.LEVER);
							b.setData((byte)0x4);
						}
						
						b = getBlockAbs(chunk, mainButtonX, floorY + 2, playerLimitZ);
						if ( b != null )
						{
							setupWallSign(b, (byte)0x2, "No player limit", "is set.", "Pull lever to", "apply a limit.");
							
							b = b.getRelative(0, 1, 0);
							b.setType(Material.WOOD_BUTTON);
							b.setData((byte)0x4);
							
							b = b.getRelative(0, -2, 0);
							b.setType(Material.WOOD_BUTTON);
							b.setData((byte)0x4);
						}
						

						b = getBlockAbs(chunk, mainButtonX, floorY + 2, wallMaxZ+1);
						if ( b != null )
						{
							b = b.getRelative(0, 1, 0);
							b.setType(command);
							Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " playerlimit up");
							
							b = b.getRelative(0, -2, 0);
							b.setType(command);
							Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup game " + (game+1) + " playerlimit down");
						}
					}
				}
				else
				{
					// "exit" portal
					if ( !Killer.instance.stagingWorldIsServerDefault )
						createExitPortal(chunk, wallMinX, floorY, exitPortalZ);
					
					// help signs on the floor
					b = getBlockAbs(chunk, startButtonX, floorY + 1, gameModeButtonZ + 4);
					if ( b != null )
						setupFloorSign(b, (byte)0x0, "Welcome to", "Killer", "Minecraft!");
					
					b = getBlockAbs(chunk, startButtonX - 2, floorY + 1, gameModeButtonZ + 5);
					if ( b != null )
						setupFloorSign(b, (byte)0xF, "This is the", "staging world.", "It's used to", "set up games.");
					
					b = getBlockAbs(chunk, startButtonX + 2, floorY + 1, gameModeButtonZ + 5);
					if ( b != null )
						setupFloorSign(b, (byte)0x1, "Read your", "instruction", "book if you", "need any help.");
				}
			}

			int floorY = baseFloorY, buttonY = floorY + buttonOffset, ceilingY = floorY + ceilingOffset;
			int backWallMinX = wallMinX - 10, backWallMaxX = wallMaxX + 10, selectionWallMinX = backWallMinX;
			if ( Settings.maxSimultaneousGames != 1 )
			{// game selection room
				int selectionWallMinZ = getGamePortalZ()+1;
				selectionWallMinX = startButtonX-(Settings.maxSimultaneousGames * 3 + Settings.maxSimultaneousGames + 1)/2;
				int selectionWallMaxX = selectionWallMinX + Settings.maxSimultaneousGames * 3 + Settings.maxSimultaneousGames + 1;
				backWallMinX = Math.min(selectionWallMinX-1, backWallMinX);
				backWallMaxX = Math.max(selectionWallMaxX+1, backWallMaxX);
				
				// floor & ceiling
				for ( int x=selectionWallMaxX+1; x>=selectionWallMinX-1; x-- )
					for ( int z=selectionWallMinZ-1; z<getWallMaxZ()+4; z++ )
					{
						for ( int y=floorY; y>floorY-2; y-- )
						{
							b = getBlockAbs(chunk, x, y, z);
							if ( b != null )
								b.setType(floor);
						}
						
						for ( int y=ceilingY; y<ceilingY+2; y++ )
						{
							b = getBlockAbs(chunk, x, y, z);
							if ( b != null )
								b.setType(ceiling);
						}
					}
				
				// side walls
				for ( int x=selectionWallMaxX+1; x>=selectionWallMaxX; x-- )
					for ( int z=selectionWallMinZ-1; z<getWallMaxZ(); z++ )
						for ( int y=floorY+1; y<ceilingY; y++ )
						{
							b = getBlockAbs(chunk, x, y, z);
							if ( b != null )
								b.setType(wall);
						}
				for ( int x=selectionWallMinX; x>selectionWallMinX-2; x-- )
					for ( int z=selectionWallMinZ-1; z<getWallMaxZ(); z++ )
						for ( int y=floorY+1; y<ceilingY; y++ )
						{
							b = getBlockAbs(chunk, x, y, z);
							if ( b != null )
								b.setType(wall);
						}
				
				// end wall
				for ( int x=selectionWallMinX-1; x<=selectionWallMaxX+1; x++ )
					for ( int y=floorY-1; y<=ceilingY+1; y++ )
					{
						for ( int z=selectionWallMinZ-4; z<selectionWallMinZ; z++ )
						{
							b = getBlockAbs(chunk, x, y, z);
							if ( b != null )
							{
								b.setType(wool);
								b.setData(colorDoorway);
							}
						}
					}
				for ( int x=selectionWallMinX; x<=selectionWallMaxX; x++ )
					for ( int y=floorY+1; y<ceilingY; y++ )
					{
						b = getBlockAbs(chunk, x, y, selectionWallMinZ);
						if ( b != null )
							b.setType(wall);
					}
				
				// doorways
				for ( int i=0; i<Settings.maxSimultaneousGames; i++ )
				{
					int doorX = getGamePortalX(i);
					for ( int z=selectionWallMinZ; z>selectionWallMinZ-3; z-- )
						for ( int y=floorY+1; y<=floorY+2; y++ )
						{
							b = getBlockAbs(chunk, doorX, y, z);
							if ( b != null )
								b.setType(Material.AIR);
						}
					
						b = getBlockAbs(chunk, doorX, floorY+3, selectionWallMinZ+1);
						if ( b != null )
							setupWallSign(b, (byte)0x3, "", "Game " + (i+1), "", "");
				}

				// tripwire
				for ( int x=selectionWallMinX; x<=selectionWallMaxX; x++ )
				{
					b = getBlockAbs(chunk, x, floorY+1, selectionWallMinZ-1);
					if ( b != null )
						b.setType(Material.TRIPWIRE);
				}

				b = getBlockAbs(chunk, selectionWallMinX, floorY+1, selectionWallMinZ-1);
				if ( b != null )
				{
					b.setType(Material.TRIPWIRE_HOOK);
					b.setData((byte)7);
				}
				
				b = getBlockAbs(chunk, selectionWallMaxX, floorY+1, selectionWallMinZ-1);
				if ( b != null )
				{
					b.setType(Material.TRIPWIRE_HOOK);
					b.setData((byte)5);
				}
				
				// help signs on the floor
				b = getBlockAbs(chunk, startButtonX, floorY + 1, gameModeButtonZ + 4);
				if ( b != null )
					setupFloorSign(b, (byte)0x0, "Welcome to", "Killer", "Minecraft!");
				
				b = getBlockAbs(chunk, startButtonX - 2, floorY + 1, gameModeButtonZ + 5);
				if ( b != null )
					setupFloorSign(b, (byte)0xF, "Each door ahead", "of you leads to", "a different", "Killer game.");
				
				b = getBlockAbs(chunk, startButtonX + 2, floorY + 1, gameModeButtonZ + 5);
				if ( b != null )
					setupFloorSign(b, (byte)0x1, "If a game isn't", "active, you'll", "be taken to the", "game setup room");
			}
			
			// now the spleef arena, which are in the same place, regardless
			
			// longer walls next to the spleef arena
			for ( int x=backWallMaxX; x>=backWallMinX; x-- )
				for ( int z=getWallMaxZ(); z<getWallMaxZ()+4; z++ )
					for ( int y=floorY+1; y<ceilingY; y++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(wall);
					}
			
			for ( int x=backWallMaxX; x>=backWallMinX; x-- )
				for ( int z=getWallMaxZ(); z<getWallMaxZ()+4; z++ )
					for ( int y=ceilingY; y<ceilingY+2; y++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(ceiling);
					}

			// "exit" portal
			if ( !Killer.instance.stagingWorldIsServerDefault && Settings.maxSimultaneousGames != 1 )
				createExitPortal(chunk, selectionWallMinX, floorY, exitPortalZ);
			
			// spleef arena setup room
			for ( int x=arenaSpleefButtonX; x>=arenaMonsterButtonX; x-- )
				for ( int z=getWallMaxZ()+1; z<getWallMaxZ()+4; z++ )
					for ( int y=floorY+1; y<ceilingY; y++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
							b.setType(Material.AIR);
					}

			// spleef setup room door
			for ( int x=startButtonX-1; x<=startButtonX+1; x++ )
			{
				b = getBlockAbs(chunk, x, floorY + 1, getWallMaxZ());
				if ( b != null )				
					b.setType(Material.AIR);
				b = getBlockAbs(chunk, x, floorY + 2, getWallMaxZ());
				if ( b != null )				
					b.setType(Material.AIR);
			}
			b = getBlockAbs(chunk, startButtonX, floorY + 3, getWallMaxZ()-1);
			if ( b != null )				
				setupWallSign(b, (byte)0x2, "Waiting for", "others?", "Play in", "this arena!");
				
			// spleef setup buttons
			for ( int y=floorY + 1; y < floorY + 4; y++ )
			{
				b = getBlockAbs(chunk, arenaSpleefButtonX+1, y, arenaButtonZ);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(signBackColor);
				}
			}
			b = getBlockAbs(chunk, arenaSpleefButtonX, buttonY+1, arenaButtonZ);
			if ( b != null )
				setupWallSign(b, (byte)0x4, "", "Spleef", "Arena");
			b = getBlockAbs(chunk, arenaSpleefButtonX+1, buttonY, arenaButtonZ);
			if ( b != null )
			{
				b.setType(wool);
				b.setData(colorOptionOn);
			}
			b = getBlockAbs(chunk, arenaSpleefButtonX, buttonY, arenaButtonZ);
			if ( b != null )
			{
				b.setType(button);
				b.setData((byte)0x2);
			}
			b = getBlockAbs(chunk, arenaSpleefButtonX+2, buttonY, arenaButtonZ);
			if ( b != null )
			{
				b.setType(command);
				Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup arena 0 SPLEEF");
			}
			
			for ( int y=floorY + 1; y < floorY + 4; y++ )
			{
				b = getBlockAbs(chunk, arenaMonsterButtonX-1, y, arenaButtonZ);
				if ( b != null )
				{
					b.setType(wool);
					b.setData(signBackColor);
				}
			}
			b = getBlockAbs(chunk, arenaMonsterButtonX, buttonY+1, arenaButtonZ);
			if ( b != null )
				setupWallSign(b, (byte)0x5, "", "Monster", "Arena");
			b = getBlockAbs(chunk, arenaMonsterButtonX-1, buttonY, arenaButtonZ);
			if ( b != null )
			{
				b.setType(wool);
				b.setData(colorOptionOff);
			}
			b = getBlockAbs(chunk, arenaMonsterButtonX, buttonY, arenaButtonZ);
			if ( b != null )
			{
				b.setType(button);
				b.setData((byte)0x1);
			}
			b = getBlockAbs(chunk, arenaMonsterButtonX-2, buttonY, arenaButtonZ);
			if ( b != null )
			{
				b.setType(command);
				Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup arena 0 SURVIVAL");
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
			
				if ( x == startButtonX )
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
			
			// pressure plate (and floor underneath)
			b = getBlockAbs(chunk, startButtonX, floorY, spleefPressurePlateZ);
			if ( b != null )
				b.setType(floor);
			
			b = getBlockAbs(chunk, startButtonX, floorY+1, spleefPressurePlateZ);
			if ( b != null )
				b.setType(Material.STONE_PLATE);
			
			b = getBlockAbs(chunk, startButtonX, floorY-1, spleefPressurePlateZ);
			if ( b != null )
			{
				b.setType(command);
				Killer.instance.craftBukkit.setCommandBlockCommand(b, "setup arena 0 equip @p");
			}
		}

		private void createExitPortal(Chunk chunk, int wallX, int floorY,int portalZ)
		{
			Block b;
			for ( int x=wallX-1; x>wallX-4; x-- )
				for ( int y=floorY; y<floorY+4; y++ )
				{
					for ( int z=portalZ-2; z<=portalZ+2; z++ )
					{
						b = getBlockAbs(chunk, x, y, z);
						if ( b != null )
						{
							b.setType(Material.WOOL);
							b.setData(colorDoorway);
						}
					}
				}
			
			for ( int x=wallX; x>wallX-3; x-- )
				for ( int y=floorY+1; y<floorY+3; y++ )
				{
					b = getBlockAbs(chunk, x, y, portalZ);
					if ( b != null )
						b.setType(Material.AIR);
				}
			
			for ( int z=portalZ-1; z<=portalZ+1; z++ )
			{
				b = getBlockAbs(chunk, wallX-1, floorY+1, z);
				if ( b != null )
					b.setType(Material.TRIPWIRE);
			}
			b = getBlockAbs(chunk, wallX-1, floorY+1, portalZ+2);
			if ( b != null )
			{
				b.setType(Material.TRIPWIRE_HOOK);
				b.setData((byte)6);	
			}
			b = getBlockAbs(chunk, wallX-1, floorY+1, portalZ-2);
			if ( b != null )
			{
				b.setType(Material.TRIPWIRE_HOOK);
				b.setData((byte)4);
			}
			
			b = getBlockAbs(chunk, wallX+1, floorY+3, portalZ);
			if ( b != null )
				setupWallSign(b, (byte)0x5, "Exit Killer", "and return to", "the server's", "main world");
		}
	}
	
	public static void setupFloorSign(Block b, byte orientation, String... lines)
	{
		b.setType(Material.SIGN_POST);
		b.setData(orientation);
		Sign s = (Sign)b.getState();
		
		for ( int i=0; i<4 && i<lines.length; i++ )
			s.setLine(i, lines[i]);
		for ( int i=lines.length; i<4; i++ )
			s.setLine(i, "");
		s.update();
	}

	
	public static void setupWallSign(Block b, byte orientation, String... lines)
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

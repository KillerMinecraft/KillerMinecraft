package com.ftwinston.Killer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

class StagingWorldManager
{
	public StagingWorldManager(Killer plugin, World world)
	{
		this.plugin = plugin;
		stagingWorld = world;
		
		currentGameOptions = new StagingWorldOption[plugin.games.length];
		for ( int i=0; i<currentGameOptions.length; i++ )
			currentGameOptions[i] = StagingWorldOption.NONE;
		
		gameSigns = new ArrayList<EnumMap<GameSign, Location>>();
		for ( int i=0; i<plugin.games.length; i++ )
			gameSigns.add(new EnumMap<GameSign, Location>(GameSign.class));
		
		protectedVolumes = new ArrayList<Volume>();
		arenas = new ArrayList<Arena>();
	
		initWorldInfo();
		setupSignDefaults();
		
		if ( spawn == null )
		{
			Location loc = stagingWorld.getSpawnLocation();
			spawn = new Volume(
				loc.getBlockX() - 1, loc.getBlockY(), loc.getBlockZ() - 1,
				loc.getBlockX() + 1, loc.getBlockY(), loc.getBlockZ() + 1
			);
			spawnYaw = 0;
		}
	}

	private void initWorldInfo()
	{
		File infoFile = new File(stagingWorld.getWorldFolder(), "killer.txt");
		if ( !infoFile.exists() || !infoFile.canRead() )
		{
			plugin.log.warning("Cant find/read killer.txt in staging world folder!");
			return;
		}
		
		FileReader fr;
		try
		{
			fr = new FileReader(infoFile);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return;
		}

		try
		{
			BufferedReader br = new BufferedReader(fr);
			String line;
			while ( (line = br.readLine()) != null )
				addWorldInfo(line);
			
			br.close();
			fr.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void setupSignDefaults()
	{
		for ( int game=0; game<plugin.games.length; game++ )
			for ( Entry<GameSign, Location> entry : gameSigns.get(game).entrySet() )
				initSign(game, entry.getKey(), entry.getValue().getBlock());
	}
	
	private void initSign(int game, GameSign type, Block block)
	{
		switch ( type )
		{
		case DIFFICULTY:
			StagingWorldGenerator.setSignLine(block, 1, "Difficulty:");
			break;
		case MONSTERS:
			StagingWorldGenerator.setSignLine(block, 1, "Monsters:");
			break;
		case ANIMALS:
			StagingWorldGenerator.setSignLine(block, 1, "Animals:");
			break;
		case GAME_MODE:
			StagingWorldGenerator.setSignLine(block, 0, "Game mode:");
			break;
		case WORLD_OPTION:
			StagingWorldGenerator.setSignLine(block, 0, "World:");
			break;
		}
		
		updateSign(plugin.games[game], type, block);
	}

	private void updateSign(Game game, GameSign type, Block block)
	{
		switch ( type )
		{
		case DIFFICULTY:
			StagingWorldGenerator.setSignLine(block, 2, StagingWorldGenerator.capitalize(game.getDifficulty().name()));
			break;
		case MONSTERS:
			StagingWorldGenerator.setSignLine(block, 2, StagingWorldGenerator.getQuantityText(game.monsterNumbers));
			break;
		case ANIMALS:
			StagingWorldGenerator.setSignLine(block, 2, StagingWorldGenerator.getQuantityText(game.animalNumbers));
			break;
		case GAME_MODE:
			StagingWorldGenerator.fitTextOnSign((Sign)block.getState(), game.getGameMode().getName());
			break;
		case WORLD_OPTION:
			if ( game.getGameMode().allowWorldOptionSelection() )
			{// if this game mode doesn't let you choose the world option, set the default world option and update that sign
				StagingWorldGenerator.fitTextOnSign((Sign)block.getState(), game.getWorldOption().getName());	
			}
			else // otherwise, ensure that sign is back to normal
			{
				game.setWorldOption(WorldOptionPlugin.getDefault());
				StagingWorldGenerator.fitTextOnSign((Sign)block.getState(), ChatColor.BOLD + "Disabled by " + ChatColor.BOLD + "game mode");
			}
			break;
		}
	}
	
	public void updateSign(Game game, GameSign type)
	{
		Block sign = getSign(game, type);
		if ( sign != null )
			updateSign(game, type, sign);
	}
	
	Killer plugin;
	World stagingWorld;
	
	Random random = new Random();
	public Location getStagingWorldSpawnPoint()
	{
		Location loc = spawn.getRandomLocation(stagingWorld, random);
		loc.setYaw(spawnYaw);
		return loc;
	}

	Location getGameSetupSpawnLocation(int gameNum)
	{
		Location loc = getStagingWorldSpawnPoint();
		loc.setY(StagingWorldGenerator.getFloorY(gameNum) + 1);
		return loc;
	}

	public enum StagingWorldOption
	{
		NONE,
		GAME_MODE,
		GAME_MODE_CONFIG,
		WORLD,
		WORLD_CONFIG,
		GLOBAL_OPTION,
	}
	
	private StagingWorldOption[] currentGameOptions;
	public StagingWorldOption getCurrentOption(Game game)
	{
		return currentGameOptions[game.getNumber()];
	}
	
	public void setCurrentOption(Game game, StagingWorldOption option)
	{		
		if ( option == currentGameOptions[game.getNumber()] )
			return;
		
		currentGameOptions[game.getNumber()] = option;
		
		int buttonY = StagingWorldGenerator.getButtonY(game.getNumber());
		
		// disable whatever's currently on
		switch ( option )
		{
		case GAME_MODE:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinX, buttonY, StagingWorldGenerator.gameModeButtonZ).setData(StagingWorldGenerator.colorOptionOff);
			break;
		case GAME_MODE_CONFIG:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinX, buttonY, StagingWorldGenerator.gameModeConfigButtonZ).setData(StagingWorldGenerator.colorOptionOff);
			break;
		case WORLD:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinX, buttonY, StagingWorldGenerator.worldButtonZ).setData(StagingWorldGenerator.colorOptionOff);
			break;
		case WORLD_CONFIG:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinX, buttonY, StagingWorldGenerator.worldConfigButtonZ).setData(StagingWorldGenerator.colorOptionOff);
			break;
		case GLOBAL_OPTION:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinX, buttonY, StagingWorldGenerator.globalOptionButtonZ).setData(StagingWorldGenerator.colorOptionOff);
			break;
		}
		hideSetupOptionButtons(game);
		
		String[] labels;
		boolean[] values;
		Option[] options;
		
		// now set up the new option
		switch ( option )
		{
		case GAME_MODE:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinX, buttonY, StagingWorldGenerator.gameModeButtonZ).setData(StagingWorldGenerator.colorOptionOn);
			
			labels = new String[GameMode.gameModes.size()];
			values = new boolean[labels.length];
			for ( int i=0; i<labels.length; i++ )
			{
				GameModePlugin mode = GameMode.gameModes.get(i); 
				labels[i] = mode.getName();
				values[i] = mode.getName().equals(game.getGameMode().getName());
			}
			showSetupOptionButtons(game, "Game mode:", true, labels, values);
			break;
		case GAME_MODE_CONFIG:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinX, buttonY, StagingWorldGenerator.gameModeConfigButtonZ).setData(StagingWorldGenerator.colorOptionOn);
			
			options = game.getGameMode().getOptions();
			labels = new String[options.length];
			values = new boolean[options.length];
			for ( int i=0; i<options.length; i++ )
			{
				labels[i] = options[i].getName();
				values[i] = options[i].isEnabled();
			}
			showSetupOptionButtons(game, "Mode option:", false, labels, values);
			break;
		case WORLD:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinX, buttonY, StagingWorldGenerator.worldButtonZ).setData(StagingWorldGenerator.colorOptionOn);
			
			labels = new String[WorldOption.worldOptions.size()];
			values = new boolean[labels.length];
			for ( int i=0; i<labels.length; i++ )
			{
				WorldOptionPlugin worldOption = WorldOption.worldOptions.get(i); 
				labels[i] = worldOption.getName();
				values[i] = worldOption.getName().equals(game.getWorldOption().getName());
			}
			showSetupOptionButtons(game, "World:", false, labels, values);
			break;
		case WORLD_CONFIG:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinX, buttonY, StagingWorldGenerator.worldConfigButtonZ).setData(StagingWorldGenerator.colorOptionOn);
			
			options = game.getWorldOption().getOptions();
			labels = new String[options.length];
			values = new boolean[options.length];
			for ( int i=0; i<options.length; i++ )
			{
				labels[i] = options[i].getName();
				values[i] = options[i].isEnabled();
			}
			showSetupOptionButtons(game, "World option:", false, labels, values);
			break;
		case GLOBAL_OPTION:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinX, buttonY, StagingWorldGenerator.globalOptionButtonZ).setData(StagingWorldGenerator.colorOptionOn);
			labels = new String[] { "Craftable monster eggs", "Easier dispenser recipe", "Eyes of ender find nether fortresses" };
			values = new boolean[] { true, true, true, true };
			showSetupOptionButtons(game, "Global option:", false, labels, values);
			break;
		}
	}
	
	public void gameOptionButtonPressed(Game game, StagingWorldOption option, int num)
	{
		boolean[] newValues; Option[] options;
		switch ( option )
		{
		case GAME_MODE:
			boolean prevAllowedWorldOptions = game.getGameMode().allowWorldOptionSelection();
			
			// change mode
			GameModePlugin gameMode = GameMode.get(num);
			if ( game.getGameMode().getName().equals(gameMode.getName()) )
				return;
			game.setGameMode(gameMode);
			
			// update block colors
			newValues = new boolean[GameMode.gameModes.size()];
			for ( int i=0; i<newValues.length; i++ )
				newValues[i] = i == num;
			updateSetupOptionButtons(game, newValues, true);
			
			updateSign(game, GameSign.GAME_MODE);
			
			if ( prevAllowedWorldOptions != game.getGameMode().allowWorldOptionSelection() )
				updateSign(game, GameSign.WORLD_OPTION);
			break;
		case GAME_MODE_CONFIG:
			// toggle this option
			game.getGameMode().toggleOption(num);
			options = game.getGameMode().getOptions();
			
			// update block colors
			newValues = new boolean[options.length];
			for ( int i=0; i<newValues.length; i++ )
				newValues[i] = options[i].isEnabled();
			updateSetupOptionButtons(game, newValues, false);
			break;
		case WORLD:
			// change option
			WorldOptionPlugin worldOption = WorldOption.get(num);
			if ( game.getWorldOption().getName().equals(worldOption.getName()) )
				return;
			game.setWorldOption(worldOption);
			
			// update block colors
			newValues = new boolean[WorldOption.worldOptions.size()];
			for ( int i=0; i<newValues.length; i++ )
				newValues[i] = i == num;
			updateSetupOptionButtons(game, newValues, false);
			
			updateSign(game, GameSign.WORLD_OPTION);
			break;
		case WORLD_CONFIG:
			// toggle this option
			game.getWorldOption().toggleOption(num);
			options = game.getWorldOption().getOptions();
			
			// update block colors
			newValues = new boolean[options.length];
			for ( int i=0; i<newValues.length; i++ )
				newValues[i] = options[i].isEnabled();
			updateSetupOptionButtons(game, newValues, false);
			break;
		case GLOBAL_OPTION:
			if ( num == 0 )
				game.toggleMonsterEggRecipes();
			else if ( num == 1 )
				game.toggleDispenserRecipe();
			else if ( num == 2 )
				game.toggleEnderEyeRecipe();

			newValues = new boolean[] { game.isMonsterEggRecipeEnabled(), game.isDispenserRecipeEnabled(), game.isEnderEyeRecipeEnabled() };
			updateSetupOptionButtons(game, newValues, false);
			break;
		}
	}
	
	// signs related to specific games
	public enum GameSign
	{
		DIFFICULTY,
		MONSTERS,
		ANIMALS,
		GAME_MODE,
		WORLD_OPTION,
	}
	
	private void addWorldInfo(String line)
	{
		String[] parts = line.split(" ");
		
		if ( parts[0].equals("sign") )
		{
			if ( parts.length < 6 )
			{
				plugin.log.warning("Too few sign parameters in killer.txt: " + line);
				return;
			}
			
			int x, y, z, num;
			try
			{
				x = Integer.parseInt(parts[1]);
				y = Integer.parseInt(parts[2]);
				z = Integer.parseInt(parts[3]);
				num = Integer.parseInt(parts[5]);
			}
			catch ( NumberFormatException ex )
			{
				plugin.log.warning("Can't read sign coordinates from killer.txt: " + line);
				return;
			}
			
			if ( num < 0 || num >= plugin.games.length )
			{
				plugin.log.warning("Invalid game number in killer.txt: " + line);
				return;
			}
			
			GameSign type;
			try
			{
				type = GameSign.valueOf(parts[4]);
			}
			catch ( IllegalArgumentException ex )
			{
				plugin.log.warning("Unrecognised sign type in killer.txt: " + line);
				return;
			}
			
			gameSigns.get(num).put(type, new Location(stagingWorld, x, y, z));
		}
		else if ( parts[0].equals("volume") )
		{
			if ( parts.length < 8 )
			{
				plugin.log.warning("Too few volume parameters in killer.txt: " + line);
				return;
			}
			
			int x1, y1, z1, x2, y2, z2;
			try
			{
				x1 = Integer.parseInt(parts[1]);
				y1 = Integer.parseInt(parts[2]);
				z1 = Integer.parseInt(parts[3]);
				x2 = Integer.parseInt(parts[4]);
				y2 = Integer.parseInt(parts[5]);
				z2 = Integer.parseInt(parts[6]);
			}
			catch ( NumberFormatException ex )
			{
				plugin.log.warning("Can't read volume coordinates from killer.txt: " + line);
				return;
			}
			
			// volume x1 y1 z1 x2 y2 z2 spawn 180
			// volume x1 y1 z1 x2 y2 z2 protected
			// volume x1 y1 z1 x2 y2 z2 arena 1 SPLEEF
			
			if ( parts[7].equals("spawn") )
			{
				if ( parts.length < 9 )
				{
					plugin.log.warning("Too few volume parameters for spawn in killer.txt: " + line);
					return;
				}
			
				int yaw;
				try
				{
					yaw = Integer.parseInt(parts[8]);
				}
				catch ( NumberFormatException ex )
				{
					plugin.log.warning("Can't read yaw for spawn in killer.txt: " + line);
					return;
				}
				
				spawn = new Volume(x1, y1, z1, x2, y2, z2);
				spawnYaw = yaw;
			}
			else if ( parts[7].equals("protected") )
			{
				protectedVolumes.add(new Volume(x1, y1, z1, x2, y2, z2));
			}
			else if ( parts[7].equals("arena") )
			{
				if ( parts.length < 10 )
				{
					plugin.log.warning("Too few volume parameters for arena in killer.txt: " + line);
					return;
				}
				
				int num;
				try
				{
					num = Integer.parseInt(parts[8]);
				}
				catch ( NumberFormatException ex )
				{
					plugin.log.warning("Can't read arena number from killer.txt: " + line);
					return;
				}
				
				Arena.Mode mode;
				try
				{
					mode = Arena.Mode.valueOf(parts[9]);
				}
				catch ( IllegalArgumentException ex )
				{
					plugin.log.warning("Unrecognised arena mode in killer.txt: " + line);
					return;
				}
				
				arenas.add(new Arena(plugin, num, new Volume(x1, y1, z1, x2, y2, z2), mode));
			}
			else
				plugin.log.warning("unrecognised volume line in killer.txt: " + line);
		}
		else
			plugin.log.warning("unrecognised line in killer.txt: " + line);
	}
	
	ArrayList<EnumMap<GameSign, Location>> gameSigns;
	public Block getSign(Game game, GameSign option)
	{
		Location loc = gameSigns.get(game.getNumber()).get(option);
		if ( loc == null )
			return null;
		return loc.getBlock();
	}
	
	ArrayList<Volume> protectedVolumes;
	Volume spawn = null;
	int spawnYaw;
	
	ArrayList<Arena> arenas;
	
	public boolean isProtected(Block b)
	{
		for ( Volume v : protectedVolumes )
			if ( v.contains(b) )
				return true;
		return false;
	}
	
	public boolean isProtected(Location l)
	{
		for ( Volume v : protectedVolumes )
			if ( v.contains(l) )
				return true;
		return false;
	}
	
	public Arena getArena(int num)
	{
		if ( num < 0 || num >= arenas.size() )
			return null;
		return arenas.get(num);
	}
	
	public Arena getArena(Location loc)
	{
		for ( Arena arena : arenas )
			if ( arena.paddedVolume.contains(loc) )
				return arena;
		return null;
	}
	
	class Volume
	{
		int x1, y1, z1, x2, y2, z2;
		public Volume(int x1, int y1, int z1, int x2, int y2, int z2)
		{
			if ( x1 <= x2 )
			{
				this.x1 = x1; this.x2 = x2;
			}
			else
			{
				this.x1 = x2; this.x2 = x1;
			}
			
			if ( y1 <= y2 )
			{
				this.y1 = y1; this.y2 = y2;
			}
			else
			{
				this.y1 = y2; this.y2 = y1;
			}
			
			if ( z1 <= z2 )
			{
				this.z1 = z1; this.z2 = z2;
			}
			else
			{
				this.z1 = z2; this.z2 = z1;
			}
		}
		
		public boolean contains(Block b)
		{
			return b.getX() >= x1 && b.getX() <= x2
				&& b.getY() >= y1 && b.getY() <= y2
				&& b.getZ() >= z1 && b.getZ() <= z2;
		}
		
		public boolean contains(Location l)
		{
			return l.getBlockX() >= x1 && l.getBlockX() <= x2
				&& l.getBlockY() >= y1 && l.getBlockY() <= y2
				&& l.getBlockZ() >= z1 && l.getBlockZ() <= z2;
		}

		public Volume expand(int i)
		{
			return new Volume(x1-i, y1-i, z1-i, x2+i, y2+i, z2+i);
		}

		public Location getRandomLocation(World world, Random r)
		{
			return new Location(world, x1 + (x2-x1) * r.nextDouble(), y1 + (y2-y1) * r.nextDouble(), z1 + (z2-z1) * r.nextDouble());
		}
	}
	
	private void showSetupOptionButtons(Game game, String heading, boolean forGameMode, String[] labels, boolean[] values)
	{
		Block b;
		int buttonY = StagingWorldGenerator.getButtonY(game.getNumber());
		for ( int i=0; i<labels.length; i++ )
		{
			int buttonZ = StagingWorldGenerator.getOptionButtonZ(i, forGameMode);
			int maxZ = forGameMode ? buttonZ + 1: buttonZ;
			
			for ( int y=buttonY-1; y<=buttonY+1; y++ )
				for ( int z=buttonZ; z<=maxZ; z++ )
				{
					b = stagingWorld.getBlockAt(StagingWorldGenerator.wallMaxX, y, z);
					b.setType(Material.WOOL);
					b.setData(StagingWorldGenerator.signBackColor);
				}
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.wallMaxX, buttonY, buttonZ);
			b.setData(values[i] ? StagingWorldGenerator.colorOptionOn : StagingWorldGenerator.colorOptionOff);
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.optionButtonX, buttonY, buttonZ);
			b.setType(Material.STONE_BUTTON);
			b.setData((byte)0x2);
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.optionButtonX, buttonY+1, buttonZ);
			b.setType(Material.WALL_SIGN);
			b.setData((byte)0x4);
			Sign s = (Sign)b.getState();
			s.setLine(0, heading);
			
			StagingWorldGenerator.fitTextOnSign(s, labels[i]);
			
			if ( !forGameMode )
				continue;
			
			// show game mode description signs
			String[] descLines = GameMode.get(i).getSignDescription();
			if ( descLines == null )
				continue;
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.optionButtonX, buttonY+1, maxZ);
			StagingWorldGenerator.setupWallSign(b, (byte)0x4,
				descLines.length > 0 ? descLines[0] : "",
				descLines.length > 1 ? descLines[1] : "",
				descLines.length > 2 ? descLines[2] : "",
				descLines.length > 3 ? descLines[3] : "");
				
			b = stagingWorld.getBlockAt(StagingWorldGenerator.optionButtonX, buttonY, maxZ);
			StagingWorldGenerator.setupWallSign(b, (byte)0x4,
				descLines.length > 4 ? descLines[4] : "",
				descLines.length > 5 ? descLines[5] : "",
				descLines.length > 6 ? descLines[6] : "",
				descLines.length > 7 ? descLines[7] : "");
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.optionButtonX, buttonY-1, maxZ);
			StagingWorldGenerator.setupWallSign(b, (byte)0x4,
				descLines.length > 8 ? descLines[8] : "",
				descLines.length > 9 ? descLines[9] : "",
				descLines.length > 10 ? descLines[10] : "",
				descLines.length > 11 ? descLines[11] : "");
		}
	}

	private void hideSetupOptionButtons(Game game)
	{
		Block b;
		int minZ = StagingWorldGenerator.getOptionButtonZ(0, false);
		int maxZ = StagingWorldGenerator.getWallMaxZ()-1;
		int floorY = StagingWorldGenerator.getFloorY(game.getNumber());
		int buttonY = StagingWorldGenerator.getButtonY(game.getNumber());
		
		for ( int z=minZ; z<=maxZ; z++ )
			for ( int y=buttonY-1; y<=buttonY+1; y++ )
			{
				if ( z != StagingWorldGenerator.gameModeButtonZ + 5 || y != floorY + 1 )
				{// don't remove the welcome sign
					b = stagingWorld.getBlockAt(StagingWorldGenerator.optionButtonX, y, z);
					b.setType(Material.AIR);
				}
			
				b = stagingWorld.getBlockAt(StagingWorldGenerator.wallMaxX, y, z);
				b.setType(Material.SMOOTH_BRICK);
			}
	}
	
	private void updateSetupOptionButtons(Game game, boolean[] values, boolean forGameMode)
	{
		Block b;
		int buttonY = StagingWorldGenerator.getButtonY(game.getNumber());
		for ( int i=0; i<values.length; i++ )
		{
			int z = StagingWorldGenerator.getOptionButtonZ(i, forGameMode);
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.wallMaxX, buttonY, z);
			b.setData(values[i] ? StagingWorldGenerator.colorOptionOn : StagingWorldGenerator.colorOptionOff);
		}
	}
	
	public void playerInteraction(Game game, Player player, Action action, Block block)
	{
		if ( action == Action.PHYSICAL )
			handlePhysicalInteraction(game, player, action, block);
		/*else if ( block.getType() == Material.WOOL ) // clicking the surround of a button activates that button
		{
			if ( block.getX() == StagingWorldGenerator.wallMinX )
			{
				block = block.getRelative(1, 0, 0);
				if ( block.getType() == Material.STONE_BUTTON )
				{
					handleButtonPress(game, player, action, block);
					plugin.craftBukkit.pushButton(block);
				}
			}
			else if ( block.getX() == StagingWorldGenerator.wallMaxX )
			{
				block = block.getRelative(-1, 0, 0);
				if ( block.getType() == Material.STONE_BUTTON )
				{
					handleButtonPress(game, player, action, block);
					plugin.craftBukkit.pushButton(block);
				}
			}
		}*/
	}
	
	private void handlePhysicalInteraction(final Game game, final Player player, final Action action, final Block block)
	{
		if ( block.getType() != Material.TRIPWIRE && block.getType() != Material.STONE_PLATE )
			return;
		if ( block.getZ() == StagingWorldGenerator.getGamePortalZ() )
		{
			for ( int i=0; i<Settings.maxSimultaneousGames; i++ )
				if ( block.getX() == StagingWorldGenerator.getGamePortalX(i) )
				{
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,  new Runnable() {
						@Override
						public void run() {
							resetTripWire(block);
						}
					});
					break;
				}
		}
		else if ( block.getZ() == StagingWorldGenerator.getWallMaxZ() + 1 && game != null && Settings.maxSimultaneousGames != 1 )
		{// exit this game, back to the staging world spawn
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,  new Runnable() {
				@Override
				public void run() {
					player.teleport(getStagingWorldSpawnPoint());
					resetTripWire(block);
				}
			});
		}
		else if ( block.getZ() == StagingWorldGenerator.exitPortalZ && !plugin.stagingWorldIsServerDefault )
		{// exit killer minecraft altogether
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,  new Runnable() {
				@Override
				public void run() {
					plugin.playerManager.movePlayerOutOfKillerGame(player);
					resetTripWire(block);
				}
			});
		}
	}
	
	public enum StartButtonState
	{
		START,
		CONFIRM,
		WAIT_FOR_DELETION,
		WAIT_FOR_GENERATION,
		GENERATING,
		IN_PROGRESS,
	}
	
	public void showStartButtons(Game game, StartButtonState state)
	{
		int buttonY = StagingWorldGenerator.getButtonY(game.getNumber());
		
		Block bStart = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, buttonY, StagingWorldGenerator.startButtonZ);
		Block sStart = bStart.getRelative(BlockFace.UP);
		Block backStart = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, buttonY, StagingWorldGenerator.wallMinZ);
		
		Block bOverride = stagingWorld.getBlockAt(StagingWorldGenerator.overrideButtonX, buttonY, StagingWorldGenerator.startButtonZ);
		Block sOverride = bOverride.getRelative(BlockFace.UP);
		Block backOverride = stagingWorld.getBlockAt(StagingWorldGenerator.overrideButtonX, buttonY, StagingWorldGenerator.wallMinZ);
		
		Block bCancel = stagingWorld.getBlockAt(StagingWorldGenerator.cancelButtonX, buttonY, StagingWorldGenerator.startButtonZ);
		Block sCancel = bCancel.getRelative(BlockFace.UP);
		Block backCancel = stagingWorld.getBlockAt(StagingWorldGenerator.cancelButtonX, buttonY, StagingWorldGenerator.wallMinZ);
		
		Block sHighInfo = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, buttonY + 2, StagingWorldGenerator.startButtonZ);
		
		boolean showMiddleButton = false, showSideButtons = false;
		
		switch ( state )
		{
		case CONFIRM:
			showMiddleButton = false; showSideButtons = true;
			StagingWorldGenerator.setupWallSign(sOverride, (byte)0x3, "", "Push to start", "the game anyway", "");
			StagingWorldGenerator.setupWallSign(sCancel, (byte)0x3, "", "Push to cancel", "and choose", "something else");
			StagingWorldGenerator.setupWallSign(sHighInfo, (byte)0x3, "This mode needs", "at least " + game.getGameMode().getMinPlayers(), "players. You", "only have " + game.getOnlinePlayers().size() + ".");
			break;
		case START:
			showMiddleButton = true; showSideButtons = false;
			StagingWorldGenerator.setupWallSign(sStart, (byte)0x3, "", "Push to", "start the game", "");
			break;
		case WAIT_FOR_DELETION:
			showMiddleButton = false; showSideButtons = false;
			StagingWorldGenerator.setupWallSign(sStart, (byte)0x3, "Please wait for", "the last game's", "worlds to be", "deleted...");
			break;
		case WAIT_FOR_GENERATION:
			showMiddleButton = false; showSideButtons = false;
			StagingWorldGenerator.setupWallSign(sStart, (byte)0x3, "Please wait", "for other games", "to finish", "generating...");
			break;
		case GENERATING:
			showMiddleButton = false; showSideButtons = false;
			StagingWorldGenerator.setupWallSign(sStart, (byte)0x3, "", "Generating...");
			break;
		case IN_PROGRESS:
			showMiddleButton = false; showSideButtons = false;
			StagingWorldGenerator.setupWallSign(sStart, (byte)0x3, "This game is in", "progress. You", "should not", "be here!");
			break;
		}
		
		if ( showSideButtons )
		{
			bOverride.setType(Material.STONE_BUTTON);
			bOverride.setData((byte)0x3);
			
			bCancel.setType(Material.STONE_BUTTON);
			bCancel.setData((byte)0x3);
			
			backOverride.setData(StagingWorldGenerator.colorOverrideButton);
			backCancel.setData(StagingWorldGenerator.colorCancelButton);
		}
		else
		{
			backOverride.setData(StagingWorldGenerator.colorOptionOff);
			backCancel.setData(StagingWorldGenerator.colorOptionOff);
			
			bOverride.setType(Material.AIR);
			bCancel.setType(Material.AIR);
			sOverride.setType(Material.AIR);
			sCancel.setType(Material.AIR);
			sHighInfo.setType(Material.AIR);
		}
		
		if ( showMiddleButton )
		{
			bStart.setType(Material.STONE_BUTTON);
			bStart.setData((byte)0x3);
			backStart.setData(StagingWorldGenerator.colorStartButton);
		}
		else
		{
			bStart.setType(Material.AIR);
			if ( showSideButtons )
				sStart.setType(Material.AIR);
			backStart.setData(StagingWorldGenerator.colorOptionOff);
		}
	}
	
	public void showWorldGenerationIndicator(Game game, float completion)
	{
		int minZ = StagingWorldGenerator.wallMinZ + 2, maxZ = StagingWorldGenerator.getWallMaxZ() - 2;
		int maxCompleteZ = (int)((maxZ - minZ) * completion + 0.5f) + minZ;
		if ( completion != 0f )
			maxCompleteZ += 2;
	
		int x = StagingWorldGenerator.wallMaxX, y = StagingWorldGenerator.getButtonY(game.getNumber()) + 1;
		for ( int z = minZ; z <= maxZ; z++ )
		{
			Block b = stagingWorld.getBlockAt(x, y, z);
			b.setType(Material.WOOL);
			if ( z < maxCompleteZ )
				b.setData(StagingWorldGenerator.colorOptionOn);
			else
				b.setData(StagingWorldGenerator.colorOptionOff);
		}
	}
	
	public void removeWorldGenerationIndicator(Game game)
	{
		int minZ = StagingWorldGenerator.wallMinZ + 2, maxZ = StagingWorldGenerator.getWallMaxZ() - 2;
		
		int x = StagingWorldGenerator.wallMaxX, y = StagingWorldGenerator.getButtonY(game.getNumber()) + 1;
		for ( int z = minZ; z <= maxZ; z++ )
		{
			Block b = stagingWorld.getBlockAt(x, y, z);
			b.setType(Material.SMOOTH_BRICK);
		}
	}
	
	public void updateGameInfoSigns(Game game)
	{
		if ( Settings.maxSimultaneousGames == 1 || game == null )
			return;
		
		int numPlayers = game.getOnlinePlayers().size(), numTotal = numPlayers + game.getOfflinePlayers().size();
		boolean gameLocked = false, gameFull = false;
		int buttonY = StagingWorldGenerator.getButtonY(game.getNumber());
		Block b = stagingWorld.getBlockAt(StagingWorldGenerator.mainButtonX, buttonY, StagingWorldGenerator.playerLimitZ);
		
		if ( game.usesPlayerLimit() )
		{
			if ( game.getPlayerLimit() == 0 )
			{
				game.setPlayerLimit(Math.max(numPlayers, 2));
				gameFull = numPlayers >= 2;
				gameLocked = gameFull && !game.getGameState().usesGameWorlds;
			}
			else if ( numPlayers == 0 && !game.getGameState().usesGameWorlds )
			{
				game.setUsesPlayerLimit(false);
				gameLocked = false;
				gameFull = !game.getGameState().usesGameWorlds;
				
				// reset the switch
				Block lever = b.getRelative(1, 0, 0); 
				lever.setData((byte)0x4);
			}
			else
			{
				gameFull = game.usesPlayerLimit() && numTotal >= game.getPlayerLimit();
				gameLocked = gameFull && !game.getGameState().usesGameWorlds;
			}
			
			if ( game.usesPlayerLimit() )
			{
				int numFree = game.getPlayerLimit() - numTotal;
				StagingWorldGenerator.setupWallSign(b, (byte)0x2, "Player limit:", "§l" + game.getPlayerLimit() + " players", "", gameFull ? "§4Game is full" : (numFree == 1 ? "1 free slot" : numFree + " free slots"));
			}
		}
		
		if ( !game.usesPlayerLimit() && Settings.allowPlayerLimits )
			StagingWorldGenerator.setupWallSign(b, (byte)0x2, "No player limit", "is set.", "Pull lever to", "apply a limit.");
		
		lockGame(game, gameLocked || (game.getGameState().usesGameWorlds && !Settings.allowLateJoiners && !Settings.allowSpectators));
		
		int portalX = StagingWorldGenerator.getGamePortalX(game.getNumber());
		int signZ = StagingWorldGenerator.getGamePortalZ() + 2;
		
		b = stagingWorld.getBlockAt(portalX-1, StagingWorldGenerator.baseFloorY+2, signZ);
		
		String strPlayers = numPlayers == 1 ? "1 player" : numPlayers + " players";
		if ( game.getGameState().usesGameWorlds )
		{
			String[] mode = StagingWorldGenerator.splitTextForSign(game.getGameMode().getName());
			
			StagingWorldGenerator.setupWallSign(b, (byte)0x3, strPlayers, "* In Progress *", mode.length == 1 ? "" : mode[0], mode[mode.length > 1 ? 1 : 0]);
		}
		else if ( numPlayers > 0 )
			StagingWorldGenerator.setupWallSign(b, (byte)0x3, strPlayers, "", "* In Setup *");
		else
		{
			StagingWorldGenerator.setupWallSign(b, (byte)0x3, strPlayers, "", "* Vacant *");
			
			// reset difficulty and monster numbers back to defaults
			if ( game.getDifficulty() != Game.defaultDifficulty) 
			{
				game.setDifficulty(Game.defaultDifficulty);
				Block sign = plugin.stagingWorld.getBlockAt(StagingWorldGenerator.mainButtonX, buttonY, StagingWorldGenerator.difficultyButtonZ);
				StagingWorldGenerator.setupWallSign(sign, (byte)0x5, "", "Difficulty:", StagingWorldGenerator.capitalize(game.getDifficulty().name()), "");
			}
			if ( game.monsterNumbers != Game.defaultMonsterNumbers) 
			{
				game.monsterNumbers = Game.defaultMonsterNumbers; 
				Block sign = plugin.stagingWorld.getBlockAt(StagingWorldGenerator.mainButtonX, buttonY, StagingWorldGenerator.monstersButtonZ);
				StagingWorldGenerator.setupWallSign(sign, (byte)0x5, "", "Monsters:", StagingWorldGenerator.getQuantityText(game.monsterNumbers), "");
			}
			if ( game.animalNumbers != Game.defaultAnimalNumbers) 
			{
				game.animalNumbers = Game.defaultAnimalNumbers;
				Block sign = plugin.stagingWorld.getBlockAt(StagingWorldGenerator.mainButtonX, buttonY, StagingWorldGenerator.animalsButtonZ);
				StagingWorldGenerator.setupWallSign(sign, (byte)0x5, "", "Animals:", StagingWorldGenerator.getQuantityText(game.animalNumbers), "");
			}
		}
		
		b = stagingWorld.getBlockAt(portalX+1, StagingWorldGenerator.baseFloorY+2, signZ);
		if ( game.getGameState().usesGameWorlds )
		{
			if ( !Settings.allowLateJoiners && !Settings.allowSpectators )
			{
				StagingWorldGenerator.setupWallSign(b, (byte)0x3, "Game already", "started, no", "spectators", "allowed");
			}
			else if ( gameFull )
				StagingWorldGenerator.setupWallSign(b, (byte)0x3, "This game", "is full:", "enter portal to", "specatate game");
			else
			{
				String actionStr;
				if ( gameFull || !Settings.allowLateJoiners )
					actionStr = "spectate game";
				else
					actionStr = "join game";
				StagingWorldGenerator.setupWallSign(b, (byte)0x3, "", "enter portal to", actionStr);
			}
		}
		else if ( gameLocked )
			StagingWorldGenerator.setupWallSign(b, (byte)0x3, "This game", "is full:", "no one else", "can join it");
		else
			StagingWorldGenerator.setupWallSign(b, (byte)0x3, "", "enter portal to", "set up a game");
	}
	
	private void resetTripWire(Block b)
	{
		b.setData((byte)0);
	}

	private void lockGame(Game game, boolean locked)
	{
		if ( Settings.maxSimultaneousGames <= 1 || (!Settings.allowPlayerLimits && locked) )
			return;
		
		Block wayIn = stagingWorld.getBlockAt(StagingWorldGenerator.getGamePortalX(game.getNumber()), StagingWorldGenerator.baseFloorY+1, StagingWorldGenerator.getGamePortalZ()+1);
		
		boolean wasLocked = wayIn.getType() != Material.AIR;
		if ( wasLocked == locked )
			return;
		
		if ( locked )
		{
			wayIn.setTypeIdAndData(Material.IRON_DOOR_BLOCK.getId(), (byte)0x3, false);
			wayIn = wayIn.getRelative(BlockFace.UP);
			wayIn.setTypeIdAndData(Material.IRON_DOOR_BLOCK.getId(), (byte)0x8, false);
			stagingWorld.playSound(wayIn.getLocation(), Sound.DOOR_CLOSE, 0.25f, 0f);
		}
		else
		{
			wayIn.setType(Material.AIR);
			wayIn.getRelative(BlockFace.UP).setType(Material.AIR);
			stagingWorld.playSound(wayIn.getLocation(), Sound.DOOR_OPEN, 0.25f, 0f);
		}
	}
}

package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.Random;

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
		
		protectedVolumes = new ArrayList<Volume>();
		arenas = new ArrayList<Arena>();
			
		if ( spawn == null )
		{
			Location loc = stagingWorld.getSpawnLocation();
			spawn = new Volume(
				loc.getBlockX() - 1, loc.getBlockY(), loc.getBlockZ() - 1,
				loc.getBlockX() + 1, loc.getBlockY(), loc.getBlockZ() + 1
			);
			spawnYaw = 0;
		}
		
		for ( int i=0; i<arenas.size(); i++ )
			arenas.get(i).updateIndicators();
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
			
			game.updateSigns(GameSign.GAME_MODE);
			
			if ( prevAllowedWorldOptions != game.getGameMode().allowWorldOptionSelection() )
				game.updateSigns(GameSign.WORLD_OPTION);
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
			
			game.updateSigns(GameSign.WORLD_OPTION);
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
		PLAYER_LIMIT,
		STATUS,
		JOIN_ACTION,
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
	
	void showSetupOptionButtons(Game game, String heading, boolean forGameMode, String[] labels, boolean[] values)
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

	void hideSetupOptionButtons(Game game)
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
	
	public void handlePhysicalInteraction(final Game game, final Player player, final Action action, final Block block)
	{
		if ( block.getType() != Material.TRIPWIRE && block.getType() != Material.STONE_PLATE )
			return;
		if ( block.getZ() == StagingWorldGenerator.getGamePortalZ() )
		{
			for ( int i=0; i<Settings.numGames; i++ )
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
		else if ( block.getZ() == StagingWorldGenerator.getWallMaxZ() + 1 && game != null && Settings.numGames != 1 )
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
	
	public void playerNumberChanged(Game game)
	{
		if ( Settings.numGames == 1 || game == null )
			return;
		
		int numPlayers = game.getOnlinePlayers().size(), numTotal = numPlayers + game.getOfflinePlayers().size();
		int buttonY = StagingWorldGenerator.getButtonY(game.getNumber());
		Block b = stagingWorld.getBlockAt(StagingWorldGenerator.mainButtonX, buttonY, StagingWorldGenerator.playerLimitZ);
		
		if ( game.usesPlayerLimit() )
		{
			if ( game.getPlayerLimit() == 0 )
			{
				game.setPlayerLimit(Math.max(numPlayers, 2));
				game.setLocked(numPlayers >= 2 && !game.getGameState().usesGameWorlds);
			}
			else if ( numPlayers == 0 && !game.getGameState().usesGameWorlds )
			{
				game.setUsesPlayerLimit(false);
				game.setLocked(false);
				
				// reset the switch
				Block lever = b.getRelative(1, 0, 0); 
				lever.setData((byte)0x4);
			}
			else
			{
				game.setLocked(numTotal >= game.getPlayerLimit() && !game.getGameState().usesGameWorlds);
			}
		}
		else
			game.setLocked(false);
		
		game.updateSigns(GameSign.PLAYER_LIMIT);
		
		lockGame(game, game.isLocked() || (game.getGameState().usesGameWorlds && !Settings.allowLateJoiners && !Settings.allowSpectators));
		
		game.updateSigns(GameSign.STATUS);
		game.updateSigns(GameSign.JOIN_ACTION);
	}
	
	private void resetTripWire(Block b)
	{
		b.setData((byte)0);
	}

	private void lockGame(Game game, boolean locked)
	{
		if ( Settings.numGames <= 1 || (!Settings.allowPlayerLimits && locked) )
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

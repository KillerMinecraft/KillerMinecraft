package com.ftwinston.Killer;

import java.util.Random;

import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import com.ftwinston.Killer.Game.GameState;

class StagingWorldManager
{
	public StagingWorldManager(Killer plugin, World world)
	{
		this.plugin = plugin;
		stagingWorld = world;
	}
	Killer plugin;
	World stagingWorld;
	
	Random random = new Random();
	public Location getStagingWorldSpawnPoint()
	{
		return new Location(stagingWorld, StagingWorldGenerator.startButtonX + random.nextDouble() * 3 - 1, StagingWorldGenerator.baseFloorY + 1, StagingWorldGenerator.gameModeButtonZ + 8.5 - random.nextDouble() * 1.5, 180, 0);
	}

	private Location getGameSetupSpawnLocation(int gameNum)
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
	
	private StagingWorldOption currentOption = StagingWorldOption.NONE;
	public void setCurrentOption(Game game, StagingWorldOption option)
	{
		if ( option == currentOption )
			return;
		
		int buttonY = StagingWorldGenerator.getButtonY(game.getNumber());
		
		// disable whatever's currently on
		switch ( currentOption )
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
		
		currentOption = option;
		String[] labels;
		boolean[] values;
		Option[] options;
		
		// now set up the new option
		switch ( currentOption )
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
			s.update();
			
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
		else if ( block.getType() == Material.STONE_BUTTON || block.getType() == Material.LEVER || block.getType() == Material.WOOD_BUTTON )
			handleButtonPress(game, player, action, block);
		else if ( block.getType() == Material.WOOL ) // clicking the surround of a button activates that button
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
		}
	}
	
	public void handleButtonPress(Game game, Player player, Action action, Block block)
	{
		if ( game != null && !game.getGameState().canChangeGameSetup )
			return;
		
		int buttonY = StagingWorldGenerator.getButtonY(game == null ? -1 : game.getNumber());
		int x = block.getX(), y = block.getY(), z = block.getZ();
		
		// first; levers, and those buttons that we allow rapid re-pressing on
		if ( z == StagingWorldGenerator.playerLimitZ && game != null )
		{
			if ( action == Action.RIGHT_CLICK_BLOCK && x == StagingWorldGenerator.startButtonX - 1 )
			{
				game.setUsesPlayerLimit(!game.usesPlayerLimit());
				updateGameInfoSigns(game);
			}
			else if ( x == StagingWorldGenerator.mainButtonX && Settings.allowPlayerLimits && game.usesPlayerLimit() )
			{
				int limit = game.getPlayerLimit();
				if ( y == buttonY + 1 )
				{
					if ( limit < Settings.maxPlayerLimit )
					{
						game.setPlayerLimit(limit+1);
						updateGameInfoSigns(game);
					}
				}
				else if ( y == buttonY - 1 )
				{
					if ( limit > Settings.minPlayerLimit )
					{
						game.setPlayerLimit(limit - 1);
						updateGameInfoSigns(game);
					}
				}
			}
		}
		else if ( x == StagingWorldGenerator.mainButtonX && game != null )
		{
			if ( z == StagingWorldGenerator.monstersButtonZ )
			{
				if ( y == buttonY + 1 )
				{
					if ( game.monsterNumbers < Game.maxQuantityNum )
					{
						game.monsterNumbers++;
						StagingWorldGenerator.setupWallSign(block.getRelative(0,-1,0), (byte)0x5, "", "Monsters:", StagingWorldGenerator.getQuantityText(game.monsterNumbers), "");
					}
				}
				else if ( y == buttonY - 1 )
				{
					if ( game.monsterNumbers > Game.minQuantityNum )
					{
						game.monsterNumbers--;
						StagingWorldGenerator.setupWallSign(block.getRelative(0,1,0), (byte)0x5, "", "Monsters:", StagingWorldGenerator.getQuantityText(game.monsterNumbers), "");
					}
				}
			}
			else if ( z == StagingWorldGenerator.animalsButtonZ )
			{
				if ( y == buttonY + 1 )
				{
					if ( game.animalNumbers < Game.maxQuantityNum )
					{
						game.animalNumbers++;
						StagingWorldGenerator.setupWallSign(block.getRelative(0,-1,0), (byte)0x5, "", "Animals:", StagingWorldGenerator.getQuantityText(game.animalNumbers), "");
					}
				}
				else if ( y == buttonY - 1 )
				{
					if ( game.animalNumbers > Game.minQuantityNum )
					{
						game.animalNumbers--;
						StagingWorldGenerator.setupWallSign(block.getRelative(0,1,0), (byte)0x5, "", "Animals:", StagingWorldGenerator.getQuantityText(game.animalNumbers), "");
					}
				}
			}
			else if ( z == StagingWorldGenerator.difficultyButtonZ )
			{
				if ( y == buttonY + 1 )
				{
					if ( game.getDifficulty().getValue() < Difficulty.HARD.getValue() )
					{
						game.setDifficulty(Difficulty.getByValue(game.getDifficulty().getValue()+1));
						StagingWorldGenerator.setupWallSign(block.getRelative(0,-1,0), (byte)0x5, "", "Difficulty:", StagingWorldGenerator.capitalize(game.getDifficulty().name()), "");
					}
				}
				else if ( y == buttonY - 1 )
				{
					if ( game.getDifficulty().getValue() > Difficulty.PEACEFUL.getValue() )
					{
						game.setDifficulty(Difficulty.getByValue(game.getDifficulty().getValue()-1));
						StagingWorldGenerator.setupWallSign(block.getRelative(0,1,0), (byte)0x5, "", "Difficulty:", StagingWorldGenerator.capitalize(game.getDifficulty().name()), "");
					}
				}
			}
		}
		
		// hereafter, only buttons that we don't allow rapid re-pressing on
		if ( (block.getData() & 0x8) != 0 )
			return; // this button was already pressed, abort! 
		
		if ( z == StagingWorldGenerator.arenaButtonZ )
		{
			if ( x == StagingWorldGenerator.arenaSpleefButtonX )
			{
				stagingWorld.getBlockAt(StagingWorldGenerator.arenaSpleefButtonX+1, buttonY, z).setData(StagingWorldGenerator.colorOptionOn);
				stagingWorld.getBlockAt(StagingWorldGenerator.arenaMonsterButtonX-1, buttonY, z).setData(StagingWorldGenerator.colorOptionOff);
				plugin.arenaManager.monsterArenaModeEnabled = false;
			}
			else if ( x == StagingWorldGenerator.arenaMonsterButtonX )
			{
				stagingWorld.getBlockAt(StagingWorldGenerator.arenaSpleefButtonX+1, buttonY, z).setData(StagingWorldGenerator.colorOptionOff);
				stagingWorld.getBlockAt(StagingWorldGenerator.arenaMonsterButtonX-1, buttonY, z).setData(StagingWorldGenerator.colorOptionOn);
				plugin.arenaManager.monsterArenaModeEnabled = true;
			}
				
			plugin.arenaManager.endMonsterArena();
		}
		else if ( x == StagingWorldGenerator.mainButtonX )
		{
			if ( z == StagingWorldGenerator.gameModeButtonZ )
				setCurrentOption(game, currentOption == StagingWorldOption.GAME_MODE ? StagingWorldOption.NONE : StagingWorldOption.GAME_MODE);
			else if ( z == StagingWorldGenerator.gameModeConfigButtonZ )
				setCurrentOption(game, currentOption == StagingWorldOption.GAME_MODE_CONFIG ? StagingWorldOption.NONE : StagingWorldOption.GAME_MODE_CONFIG);
			else if ( z == StagingWorldGenerator.worldButtonZ )
				setCurrentOption(game, currentOption == StagingWorldOption.WORLD ? StagingWorldOption.NONE : StagingWorldOption.WORLD);
			else if ( z == StagingWorldGenerator.worldConfigButtonZ )
				setCurrentOption(game, currentOption == StagingWorldOption.WORLD_CONFIG ? StagingWorldOption.NONE : StagingWorldOption.WORLD_CONFIG);
			else if ( z == StagingWorldGenerator.globalOptionButtonZ )
				setCurrentOption(game, currentOption == StagingWorldOption.GLOBAL_OPTION ? StagingWorldOption.NONE : StagingWorldOption.GLOBAL_OPTION);
		}
		else if ( x == StagingWorldGenerator.optionButtonX )
		{
			int num = StagingWorldGenerator.getOptionNumFromZ(z, currentOption == StagingWorldOption.GAME_MODE);
			
			boolean[] newValues; Block b; Sign s; Option[] options;
			switch ( currentOption )
			{
			case GAME_MODE:
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
				
				// update game mode sign
				b = stagingWorld.getBlockAt(StagingWorldGenerator.mainButtonX, buttonY+1, StagingWorldGenerator.gameModeButtonZ-1);
				s = (Sign)b.getState();
				StagingWorldGenerator.fitTextOnSign(s, gameMode.getName());
				s.update();
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
				
				// update world option sign
				b = stagingWorld.getBlockAt(StagingWorldGenerator.mainButtonX, buttonY+1, StagingWorldGenerator.worldButtonZ-1);
				s = (Sign)b.getState();
				StagingWorldGenerator.fitTextOnSign(s, worldOption.getName());
				s.update();
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
		else if ( z == StagingWorldGenerator.startButtonZ )
		{
			if ( x == StagingWorldGenerator.startButtonX )
			{
				if ( game.getOnlinePlayers().size() >= game.getGameMode().getMinPlayers() )
				{
					setCurrentOption(game, StagingWorldOption.NONE);
					game.setGameState(GameState.waitingToGenerate);
				}
				else
					game.setGameState(GameState.stagingWorldConfirm);
			}
			else if ( x == StagingWorldGenerator.overrideButtonX )
			{
				setCurrentOption(game, StagingWorldOption.NONE);
				game.setGameState(GameState.waitingToGenerate);
			}
			else if ( x == StagingWorldGenerator.cancelButtonX )
				game.setGameState(GameState.stagingWorldSetup);
		}
	}
	
	private void handlePhysicalInteraction(final Game game, final Player player, final Action action, final Block block)
	{
		if ( block.getType() != Material.TRIPWIRE && block.getType() != Material.STONE_PLATE )
			return;
		if ( block.getZ() == StagingWorldGenerator.spleefPressurePlateZ )
		{
			plugin.arenaManager.pressurePlatePressed(player);
		}
		else if ( block.getZ() == StagingWorldGenerator.getGamePortalZ() )
		{
			for ( int i=0; i<Settings.maxSimultaneousGames; i++ )
				if ( block.getX() == StagingWorldGenerator.getGamePortalX(i) )
				{
					final Game game2 = plugin.games[i];
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,  new Runnable() {
						@Override
						public void run() {
							if ( game2.getGameState().usesGameWorlds )
								plugin.playerManager.teleport(player, game2.getGameMode().getSpawnLocation(player));
							else
								player.teleport(getGameSetupSpawnLocation(game2.getNumber()));
							plugin.playerManager.putPlayerInGame(player, game2);
							resetTripWire(block);
							updateGameInfoSigns(game2);
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
					plugin.playerManager.removePlayerFromGame(player, game);
					updateGameInfoSigns(game);
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

package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Recipe;

import com.ftwinston.Killer.PlayerManager.Info;
import com.ftwinston.Killer.StagingWorldManager.GameSign;
import com.ftwinston.Killer.StagingWorldManager.StagingWorldOption;
import com.ftwinston.Killer.StagingWorldManager.StartButtonState;

public class Game
{
	static LinkedList<Game> generationQueue = new LinkedList<Game>();

	Killer plugin;
	private int number, helpMessageProcess, compassProcess, spectatorFollowProcess;
	
	public Game(Killer killer, int gameNumber)
	{
		plugin = killer;
		number = gameNumber;
	}
	
	public void initStagingWorld()
	{
		updateIndicators();
		
		for ( Entry<GameSign, ArrayList<Location>> entry : signs.entrySet() )
			for ( Location loc : entry.getValue() )
				initSign(entry.getKey(), loc.getBlock());
	}
	
	public int getNumber() { return number; }

	private GameMode gameMode = null;
	GameMode getGameMode() { return gameMode; }
	void setGameMode(GameModePlugin plugin)
	{
		GameMode mode = plugin.createInstance();
		mode.initialize(this, plugin);
		gameMode = mode;
	}
	
	private WorldOption worldOption = null;
	WorldOption getWorldOption() { return worldOption; }
	void setWorldOption(WorldOptionPlugin plugin)
	{
		WorldOption world = plugin.createInstance();
		world.initialize(this, plugin);
		worldOption = world;
	}

	private TreeMap<String, Info> playerInfo = new TreeMap<String, Info>();
	public Map<String, Info> getPlayerInfo() { return playerInfo; }
	
	static final int minQuantityNum = 0, maxQuantityNum = 4;
	
	static final int defaultMonsterNumbers = 2, defaultAnimalNumbers = 2; 
	int monsterNumbers = defaultMonsterNumbers, animalNumbers = defaultAnimalNumbers;
	
	static final Difficulty defaultDifficulty = Difficulty.HARD;
	private Difficulty difficulty = defaultDifficulty;
	public Difficulty getDifficulty() { return difficulty; }
	public void setDifficulty(Difficulty d) { difficulty = d; }
	
	public void startProcesses()
	{
		final Game game = this;
		
		// start sending out help messages explaining the game rules
		helpMessageProcess = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run()
			{
				getGameMode().sendGameModeHelpMessage();
			}
		}, 0, 550L); // send every 25 seconds
		
		compassProcess = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
        	public void run()
        	{
	        	for ( Player player : getGameMode().getOnlinePlayers(new PlayerFilter().alive()) )
	        		if ( player.getInventory().contains(Material.COMPASS) )
	        		{// does this need a null check on the target?
	        			player.setCompassTarget(plugin.playerManager.getCompassTarget(game, player));
	        		}
        	}
        }, 20, 10);
	        			
		spectatorFollowProcess = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
        	public void run()
        	{
	        	for ( Player player : getGameMode().getOnlinePlayers(new PlayerFilter().notAlive()) )
	        	{
	        		PlayerManager.Info info = playerInfo.get(player.getName());
	        		if (info.target != null )
	        			plugin.playerManager.checkFollowTarget(game, player, info.target);
	        	}
        	}
        }, 40, 40);
	}
	
	public void stopProcesses()
	{
		if ( helpMessageProcess != -1 )
		{
			plugin.getServer().getScheduler().cancelTask(helpMessageProcess);
			helpMessageProcess = -1;
		}
		
		if ( compassProcess != -1 )
		{
			plugin.getServer().getScheduler().cancelTask(compassProcess);
			compassProcess = -1;
		}
		
		if ( spectatorFollowProcess != -1 )
		{
			plugin.getServer().getScheduler().cancelTask(spectatorFollowProcess);
			spectatorFollowProcess = -1;
		}
	}
	
	enum GameState
	{
		stagingWorldSetup(false, true), // in staging world, players need to choose mode/world
		worldDeletion(false, true), // in staging world, hide start buttons, delete old world, then show start button again
		stagingWorldConfirm(false, true), // in staging world, players have chosen a game mode that requires confirmation (e.g. they don't have the recommended player number)
		waitingToGenerate(false, false),
		worldGeneration(false, false), // in staging world, game worlds are being generated
		active(true, false), // game is active, in game world
		finished(true, false); // game is finished, but not yet restarted
		
		public final boolean usesGameWorlds, canChangeGameSetup;
		GameState(boolean useGameWorlds, boolean canChangeGameSetup)
		{
			this.usesGameWorlds = useGameWorlds;
			this.canChangeGameSetup = canChangeGameSetup;
		}
	}
	
	private GameState gameState = GameState.stagingWorldSetup;
	GameState getGameState() { return gameState; }
	void setGameState(GameState newState)
	{
		GameState prevState = gameState;
		gameState = newState;
		
		if ( prevState.usesGameWorlds != newState.usesGameWorlds )
		{
			plugin.stagingWorldManager.playerNumberChanged(this);
			
			if ( newState.usesGameWorlds )
				startProcesses();
			else
				stopProcesses();
		}
		
		if ( newState == GameState.worldDeletion )
		{
			// if the stats manager is tracking, then the game didn't finish "properly" ... this counts as an "aborted" game
			if ( plugin.statsManager.isTracking(number) )
				plugin.statsManager.gameFinished(number, getGameMode(), getWorldOption(), getGameMode().getOnlinePlayers(new PlayerFilter().alive()).size(), true);
			
			HandlerList.unregisterAll(getGameMode()); // stop this game mode listening for events

			// don't show the start buttons until the old world finishes deleting
			plugin.stagingWorldManager.showStartButtons(this, StartButtonState.WAIT_FOR_DELETION);
			plugin.stagingWorldManager.removeWorldGenerationIndicator(this);
			
			for ( Player player : getOnlinePlayers() )
				if ( player.getWorld() != plugin.stagingWorld )
					plugin.playerManager.putPlayerInStagingWorld(player);
			
			plugin.playerManager.reset(this);
			
			plugin.worldManager.deleteKillerWorlds(this, new Runnable() {
				@Override
				public void run() {
					setGameState(GameState.stagingWorldSetup);
				}
			});
		}
		else if ( newState == GameState.stagingWorldSetup )
		{
			plugin.stagingWorldManager.showStartButtons(this, StartButtonState.START);
		}
		else if ( newState == GameState.stagingWorldConfirm )
		{
			plugin.stagingWorldManager.showStartButtons(this, StartButtonState.CONFIRM);
		}
		else if ( newState == GameState.waitingToGenerate )
		{
			// if nothing in the queue (so nothing currently generating), generate immediately
			if ( generationQueue.peek() == null ) 
				newState = gameState = GameState.worldGeneration;
			else
				plugin.stagingWorldManager.showStartButtons(this, StartButtonState.WAIT_FOR_GENERATION);
			
			generationQueue.addLast(this); // always add to the queue (head of the queue is currently generating)
		}
		if( newState == GameState.worldGeneration )
		{
			plugin.getServer().getPluginManager().registerEvents(getGameMode(), plugin);
			plugin.stagingWorldManager.showStartButtons(this, StartButtonState.GENERATING);
			
			final Game game = this;
			plugin.worldManager.generateWorlds(this, worldOption, new Runnable() {
				@Override
				public void run() {
					setGameState(GameState.active);
					plugin.stagingWorldManager.showStartButtons(game, StartButtonState.IN_PROGRESS);
					
					if ( generationQueue.peek() != game )
						return; // just in case, only the "head" game should trigger this logic
					
					generationQueue.remove(); // remove from the queue when its done generating ... can't block other games
					Game nextInQueue = generationQueue.peek();
					if ( nextInQueue != null ) // start the next queued game generating
						nextInQueue.setGameState(GameState.worldGeneration);
				}
			});
		}
		else if ( newState == GameState.active )
		{
			// if the stats manager is tracking, then the game didn't finish "properly" ... this counts as an "aborted" game
			int numPlayers = getGameMode().getOnlinePlayers(new PlayerFilter().alive()).size();
			if ( plugin.statsManager.isTracking(number) )
				plugin.statsManager.gameFinished(number, getGameMode(), getWorldOption(), numPlayers, true);
			plugin.statsManager.gameStarted(number, numPlayers);
			
			if ( prevState.usesGameWorlds )
			{
				for ( World world : worlds )
				{
					plugin.worldManager.removeAllItems(world);
					world.setTime(0);
				}
			}

			getGameMode().startGame(!prevState.usesGameWorlds);
		}
		else if ( newState == GameState.finished )
		{
			plugin.statsManager.gameFinished(number, getGameMode(), getWorldOption(), getGameMode().getOnlinePlayers(new PlayerFilter().alive()).size(), false);
		}
	}
	
	private boolean dispenserRecipeEnabled = true;
	void toggleDispenserRecipe()
	{
		dispenserRecipeEnabled = !dispenserRecipeEnabled;
		
		if ( dispenserRecipeEnabled )
		{
			plugin.getServer().addRecipe(plugin.dispenserRecipe);
			return;
		}
		
		Iterator<Recipe> iterator = plugin.getServer().recipeIterator();
        while (iterator.hasNext())
        	if ( plugin.isDispenserRecipe(iterator.next()) )
        	{
        		iterator.remove();
        		return;
        	}
	}
	
	boolean isDispenserRecipeEnabled() { return dispenserRecipeEnabled; }
	
	private boolean enderEyeRecipeEnabled = true;
	void toggleEnderEyeRecipe()
	{
		enderEyeRecipeEnabled = !enderEyeRecipeEnabled;
		
		if ( enderEyeRecipeEnabled )
		{
			plugin.getServer().addRecipe(plugin.enderRecipe);
			return;
		}
		
		Iterator<Recipe> iterator = plugin.getServer().recipeIterator();
        while (iterator.hasNext())
        	if ( plugin.isEnderEyeRecipe(iterator.next()) )
        	{
        		iterator.remove();
        		return;
        	}
	}
	
	boolean isEnderEyeRecipeEnabled() { return enderEyeRecipeEnabled; }

	private boolean monsterEggsEnabled = true;
	void toggleMonsterEggRecipes()
	{
		monsterEggsEnabled = !monsterEggsEnabled;
		
		if ( monsterEggsEnabled )
		{
			for ( Recipe recipe : plugin.monsterRecipes )
				plugin.getServer().addRecipe(recipe);
			return;
		}
		
		Iterator<Recipe> iterator = plugin.getServer().recipeIterator();
		while (iterator.hasNext())
        {
			if ( plugin.isMonsterEggRecipe(iterator.next()) )
            	iterator.remove();
    	}
	}
	
	boolean isMonsterEggRecipeEnabled() { return monsterEggsEnabled; }
	
	private int playerLimit = 0;
	private boolean hasPlayerLimit = false, locked = false;
	boolean usesPlayerLimit() { return hasPlayerLimit; }
	void setUsesPlayerLimit(boolean val) { hasPlayerLimit = val; if ( !val ) playerLimit = 0; }
	int getPlayerLimit() { return playerLimit; }
	void setPlayerLimit(int limit) { playerLimit = limit; }
	
	boolean isLocked() { return locked; }
	void setLocked(boolean value) { locked = value; }
		
	private List<World> worlds = new ArrayList<World>();
	List<World> getWorlds() { return worlds; }
	
	String getWorldName() { return Settings.killerWorldName + number; }
	
	boolean forcedGameEnd = false;
	void endGame(CommandSender actionedBy)
	{
		if ( actionedBy != null )
			getGameMode().broadcastMessage(actionedBy.getName() + " ended the game. You've been moved to the staging world to allow you to set up a new one...");
		else
			getGameMode().broadcastMessage("The game has ended. You've been moved to the staging world to allow you to set up a new one...");
		
		setGameState(GameState.worldDeletion);
		plugin.stagingWorldManager.playerNumberChanged(this);
	}
	
	void restartGame(CommandSender actionedBy)
	{
		if ( actionedBy != null )
			getGameMode().broadcastMessage(actionedBy.getName() + " is restarting the game...");
		else
			getGameMode().broadcastMessage("Game is restarting...");
		
		setGameState(GameState.active);
	}

	public List<Player> getOnlinePlayers()
	{		
		return getOnlinePlayers(new PlayerFilter());
	}
	
	public List<Player> getOnlinePlayers(PlayerFilter filter)
	{
		return filter.setGame(this).getOnlinePlayers();
	}
	
	public List<OfflinePlayer> getOfflinePlayers()
	{
		return getOfflinePlayers(new PlayerFilter());
	}
	
	public List<OfflinePlayer> getOfflinePlayers(PlayerFilter filter)
	{		
		return filter.offline().setGame(this).getPlayers();
	}
	
	public List<OfflinePlayer> getPlayers()
	{
		return getPlayers(new PlayerFilter());
	}
	
	public List<OfflinePlayer> getPlayers(PlayerFilter filter)
	{		
		return filter.setGame(this).getPlayers();
	}
	
	public void broadcastMessage(String message)
	{
		for ( Player player : getOnlinePlayers() )
			player.sendMessage(message);
	}
	
	public void broadcastMessage(PlayerFilter recipients, String message)
	{
		for ( Player player : getOnlinePlayers(recipients) )
			player.sendMessage(message);
	}
	
	public String calculateColoredName(Player player)
	{
		Info info = getPlayerInfo().get(player.getName());
		
		if ( getGameMode().teamAllocationIsSecret() || !info.isAlive() )
			return player.getPlayerListName();
		
		String listName = player.getPlayerListName();
		ChatColor color = getGameMode().getTeamChatColor(info.getTeam()); 
		if ( listName.length() > 15 )
			return color + listName.substring(0, 15);
		
		return color + listName;
	}
	
	private StagingWorldOption currentOption = StagingWorldOption.NONE;
	private EnumMap<GameSign, ArrayList<Location>> signs = new EnumMap<GameSign, ArrayList<Location>>(GameSign.class);
	private EnumMap<StagingWorldOption, ArrayList<Location>> indicators = new EnumMap<StagingWorldOption, ArrayList<Location>>(StagingWorldOption.class);
	
	public StagingWorldOption getCurrentOption() { return currentOption; }
	public void setCurrentOption(StagingWorldOption option)
	{		
		if ( option == currentOption )
			return;
		
		// disable whatever's currently on
		updateIndicator(currentOption, false);
		plugin.stagingWorldManager.hideSetupOptionButtons(this);
		
		currentOption = option;
		
		String[] labels;
		boolean[] values;
		Option[] options;
		
		// now set up the new option
		updateIndicator(currentOption, true);
		switch ( currentOption )
		{
		case GAME_MODE:
			labels = new String[GameMode.gameModes.size()];
			values = new boolean[labels.length];
			for ( int i=0; i<labels.length; i++ )
			{
				GameModePlugin mode = GameMode.gameModes.get(i); 
				labels[i] = mode.getName();
				values[i] = mode.getName().equals(getGameMode().getName());
			}
			plugin.stagingWorldManager.showSetupOptionButtons(this, "Game mode:", true, labels, values);
			break;
		case GAME_MODE_CONFIG:
			options = getGameMode().getOptions();
			labels = new String[options.length];
			values = new boolean[options.length];
			for ( int i=0; i<options.length; i++ )
			{
				labels[i] = options[i].getName();
				values[i] = options[i].isEnabled();
			}
			plugin.stagingWorldManager.showSetupOptionButtons(this, "Mode option:", false, labels, values);
			break;
		case WORLD:
			labels = new String[WorldOption.worldOptions.size()];
			values = new boolean[labels.length];
			for ( int i=0; i<labels.length; i++ )
			{
				WorldOptionPlugin worldOption = WorldOption.worldOptions.get(i); 
				labels[i] = worldOption.getName();
				values[i] = worldOption.getName().equals(getWorldOption().getName());
			}
			plugin.stagingWorldManager.showSetupOptionButtons(this, "World:", false, labels, values);
			break;
		case WORLD_CONFIG:
			options = getWorldOption().getOptions();
			labels = new String[options.length];
			values = new boolean[options.length];
			for ( int i=0; i<options.length; i++ )
			{
				labels[i] = options[i].getName();
				values[i] = options[i].isEnabled();
			}
			plugin.stagingWorldManager.showSetupOptionButtons(this, "World option:", false, labels, values);
			break;
		case GLOBAL_OPTION:
			labels = new String[] { "Craftable monster eggs", "Easier dispenser recipe", "Eyes of ender find nether fortresses" };
			values = new boolean[] { true, true, true, true };
			plugin.stagingWorldManager.showSetupOptionButtons(this, "Global option:", false, labels, values);
			break;
		}
	}
	
	public void addSign(int x, int y, int z, GameSign type)
	{
		if ( signs.containsKey(type) )
			signs.get(type).add(new Location(plugin.stagingWorld, x, y, z));
		else
		{
			ArrayList<Location> vals = new ArrayList<Location>();
			vals.add(new Location(plugin.stagingWorld, x, y, z));
			signs.put(type, vals);
		}
	}
	
	public void addIndicator(int x, int y, int z, StagingWorldOption type)
	{
		if ( indicators.containsKey(type) )
			indicators.get(type).add(new Location(plugin.stagingWorld, x, y, z));
		else
		{
			ArrayList<Location> vals = new ArrayList<Location>();
			vals.add(new Location(plugin.stagingWorld, x, y, z));
			indicators.put(type, vals);
		}
	}
	
	public void updateIndicators()
	{
		for ( Entry<StagingWorldOption, ArrayList<Location>> entry : indicators.entrySet() )
			for ( Location loc : entry.getValue() )
				plugin.stagingWorld.getBlockAt(loc).setData(currentOption == entry.getKey() ? StagingWorldGenerator.colorOptionOn : StagingWorldGenerator.colorOptionOff);
	}
	
	private void updateIndicator(StagingWorldOption option, boolean on)
	{
		ArrayList<Location> list = indicators.get(option);
		if ( list != null )
			for ( Location loc : list )
				loc.getBlock().setData(on ? StagingWorldGenerator.colorOptionOn : StagingWorldGenerator.colorOptionOff);
	}
	
	private void initSign(GameSign type, Block block)
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
		
		updateSign(type, block);
	}

	private void updateSign(GameSign type, Block block)
	{
		switch ( type )
		{
		case DIFFICULTY:
			StagingWorldGenerator.setSignLine(block, 2, StagingWorldGenerator.capitalize(getDifficulty().name()));
			break;
		case MONSTERS:
			StagingWorldGenerator.setSignLine(block, 2, StagingWorldGenerator.getQuantityText(monsterNumbers));
			break;
		case ANIMALS:
			StagingWorldGenerator.setSignLine(block, 2, StagingWorldGenerator.getQuantityText(animalNumbers));
			break;
		case GAME_MODE:
			StagingWorldGenerator.fitTextOnSign((Sign)block.getState(), getGameMode().getName());
			break;
		case WORLD_OPTION:
			if ( getGameMode().allowWorldOptionSelection() )
			{// if this game mode doesn't let you choose the world option, set the default world option and update that sign
				StagingWorldGenerator.fitTextOnSign((Sign)block.getState(), getWorldOption().getName());	
			}
			else // otherwise, ensure that sign is back to normal
			{
				setWorldOption(WorldOptionPlugin.getDefault());
				StagingWorldGenerator.fitTextOnSign((Sign)block.getState(), ChatColor.BOLD + "Disabled by " + ChatColor.BOLD + "game mode");
			}
			break;
		case PLAYER_LIMIT:
			if ( !Settings.allowPlayerLimits )
				return;
			
			if ( usesPlayerLimit() )
			{
				int numFree = getPlayerLimit() - getOnlinePlayers().size() - getOfflinePlayers().size();
				
				StagingWorldGenerator.setSignText(block, "Player limit:", "§l" + getPlayerLimit() + " players", "", numFree <= 0 ? "§4Game is full" : (numFree == 1 ? "1 free slot" : numFree + " free slots"));
			}
			else
				StagingWorldGenerator.setSignText(block, "No player limit", "is set.", "Pull lever to", "apply a limit.");
			break;
		case STATUS:
			int numPlayers = getOnlinePlayers().size();
			String strPlayers = numPlayers == 1 ? "1 player" : numPlayers + " players";
			if ( getGameState().usesGameWorlds )
			{
				String[] mode = StagingWorldGenerator.splitTextForSign(getGameMode().getName());
				
				StagingWorldGenerator.setSignText(block, strPlayers, "* In Progress *", mode.length == 1 ? "" : mode[0], mode[mode.length > 1 ? 1 : 0]);
			}
			else if ( numPlayers > 0 )
				StagingWorldGenerator.setSignText(block, strPlayers, "", "* In Setup *");
			else
			{
				StagingWorldGenerator.setSignText(block, strPlayers, "", "* Vacant *");
				
				// reset difficulty and monster numbers back to defaults
				if ( getDifficulty() != defaultDifficulty) 
				{
					setDifficulty(defaultDifficulty);
					updateSigns(GameSign.DIFFICULTY);
				}
				if ( monsterNumbers != defaultMonsterNumbers) 
				{
					monsterNumbers = defaultMonsterNumbers; 
					updateSigns(GameSign.MONSTERS);
				}
				if ( animalNumbers != defaultAnimalNumbers) 
				{
					animalNumbers = defaultAnimalNumbers;
					updateSigns(GameSign.ANIMALS);
				}
			}
			break;
		case JOIN_ACTION:
			if ( getGameState().usesGameWorlds )
			{
				if ( !Settings.allowLateJoiners && !Settings.allowSpectators )
				{
					StagingWorldGenerator.setSignText(block, "Game already", "started, no", "spectators", "allowed");
				}
				else if ( isLocked() )
					StagingWorldGenerator.setSignText(block, "This game", "is full:", "enter portal to", "specatate game");
				else
				{
					String actionStr;
					if ( isLocked() || !Settings.allowLateJoiners )
						actionStr = "spectate game";
					else
						actionStr = "join game";
					StagingWorldGenerator.setSignText(block, "", "enter portal to", actionStr);
				}
			}
			else if ( isLocked() )
				StagingWorldGenerator.setSignText(block, "This game", "is full:", "no one else", "can join it");
			else
				StagingWorldGenerator.setSignText(block, "", "enter portal to", "set up a game");
			break;
		}
	}
	
	public void updateSigns(GameSign type)
	{
		ArrayList<Location> thisType = signs.get(type);
		if ( thisType != null )
			for ( Location loc : thisType )
				updateSign(type, loc.getBlock());
	}
}

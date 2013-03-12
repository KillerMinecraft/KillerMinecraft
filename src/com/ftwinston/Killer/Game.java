package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Recipe;

import com.ftwinston.Killer.PlayerManager.Info;
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
			plugin.stagingWorldManager.updateGameInfoSigns(this);
			
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
	private boolean hasPlayerLimit = false;
	boolean usesPlayerLimit() { return hasPlayerLimit; }
	void setUsesPlayerLimit(boolean val) { hasPlayerLimit = val; if ( !val ) playerLimit = 0; }
	int getPlayerLimit() { return playerLimit; }
	void setPlayerLimit(int limit) { playerLimit = limit; }
		
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
		plugin.stagingWorldManager.updateGameInfoSigns(this);
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
}

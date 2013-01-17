package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Recipe;

import com.ftwinston.Killer.PlayerManager.Info;

public class Game
{
	Killer plugin;
	private int number, helpMessageProcess, compassProcess, spectatorFollowProcess;
	private boolean locked = false;
	
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
	
	static final int defaultMonsterNumbers = 2, defaultAnimalNumbers = 2; 
	int monsterNumbers = defaultMonsterNumbers, animalNumbers = defaultAnimalNumbers;
	
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
			plugin.stagingWorldManager.showWaitForDeletion(this);
			plugin.stagingWorldManager.removeWorldGenerationIndicator(this);
			
			for ( Player player : getOnlinePlayers() )
				if ( player.getWorld() != plugin.stagingWorld )
					plugin.playerManager.putPlayerInStagingWorld(player);
			
			plugin.playerManager.reset(this);
			
			plugin.worldManager.deleteKillerWorlds(this, new Runnable() {
				@Override
				public void run() { // we need this to set the state to stagingWorldReady when done
					setGameState(GameState.stagingWorldSetup);
				}
			});
		}
		else if ( newState == GameState.stagingWorldSetup )
		{
			plugin.stagingWorldManager.showStartButtons(this, false);
		}
		else if ( newState == GameState.stagingWorldConfirm )
		{
			plugin.stagingWorldManager.showStartButtons(this, true);
		}
		else if( newState == GameState.worldGeneration )
		{
			plugin.getServer().getPluginManager().registerEvents(getGameMode(), plugin);
			
			final Game game = this;
			plugin.worldManager.generateWorlds(this, worldOption, new Runnable() {
				@Override
				public void run() {
					// don't waste memory on monsters in the staging world
					if ( plugin.arenaManager.countPlayersInArena() == 0 )
						plugin.arenaManager.endMonsterArena();
					
					setGameState(GameState.active);
					plugin.stagingWorldManager.showStartButtons(game, false);
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
	
	boolean isLocked() { return locked; }
	void setLocked(boolean val) { locked = val; }
	
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
		return getOnlinePlayers(new PlayerFilter().setGame(this));
	}
	
	public List<Player> getOnlinePlayers(PlayerFilter filter)
	{
		return filter.setGame(this).getOnlinePlayers();
	}
	
	public List<OfflinePlayer> getOfflinePlayers(PlayerFilter filter)
	{		
		return filter.offline().setGame(this).getPlayers();
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
}

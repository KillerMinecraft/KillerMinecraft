package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Recipe;

public class Game
{
	Killer plugin;
	private int number;
	private int helpMessageProcess, compassProcess, spectatorFollowProcess;
	
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
	boolean setWorldOption(WorldOption w) { worldOption = w; return gameMode != null && worldOption != null; }
	
	int monsterNumbers = 2, animalNumbers = 2;
	
	public void start()
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
	        	for ( Player player : getGameMode().getOnlinePlayers(true) )
	        		if ( player.getInventory().contains(Material.COMPASS) )
	        		{// does this need a null check on the target?
	        			player.setCompassTarget(plugin.playerManager.getCompassTarget(game, player));
	        		}
        	}
        }, 20, 10);
	        			
		spectatorFollowProcess = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
        	public void run()
        	{
	        	for ( Player player : getGameMode().getOnlinePlayers(false) )
	        	{
	        		PlayerManager.Info info = plugin.playerManager.getInfo(player.getName());
	        		if (info.target != null )
	        			plugin.playerManager.checkFollowTarget(player, info.target);
	        	}
        	}
        }, 40, 40);
	}
	
	public void reset()
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
		stagingWorldReady(false, true), // in staging world, players need to push start
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
		
		if ( newState == GameState.stagingWorldSetup )
		{
			plugin.stagingWorldManager.showStartButtons(this, false);
		}
		else if ( newState == GameState.worldDeletion )
		{
			// if the stats manager is tracking, then the game didn't finish "properly" ... this counts as an "aborted" game
			if ( plugin.statsManager.isTracking(number) )
				plugin.statsManager.gameFinished(this, getGameMode().getOnlinePlayers(true).size(), 3, 0);
			
			HandlerList.unregisterAll(getGameMode()); // stop this game mode listening for events

			// don't show the start buttons until the old world finishes deleting
			plugin.stagingWorldManager.showWaitForDeletion(this);
			plugin.stagingWorldManager.removeWorldGenerationIndicator(this);
			
			for ( Player player : getOnlinePlayers() )
				if ( player.getWorld() != plugin.worldManager.stagingWorld )
					plugin.playerManager.teleport(player, plugin.stagingWorldManager.getStagingWorldSpawnPoint());
			
			plugin.playerManager.reset(number); // fixmulti
			
			plugin.worldManager.deleteKillerWorlds(this, new Runnable() {
				@Override
				public void run() { // we need this to set the state to stagingWorldReady when done
					setGameState(GameState.stagingWorldReady);
				}
			});
		}
		else if ( newState == GameState.stagingWorldReady )
		{
			plugin.stagingWorldManager.showStartButtons(this, false);
		}
		else if ( newState == GameState.stagingWorldConfirm )
		{
			plugin.stagingWorldManager.showStartButtons(this, true);
		}
		else if( newState == GameState.worldGeneration )
		{
			final Game game = this;
			plugin.worldManager.generateWorlds(this, worldOption, new Runnable() {
				@Override
				public void run() {
					// don't waste memory on monsters in the staging world
					if ( plugin.stagingWorldManager.countPlayersInArena() == 0 )
						plugin.stagingWorldManager.endMonsterArena();
					
					getGameMode().worldGenerationComplete(getMainWorld(), getNetherWorld());
					setGameState(GameState.active);
					plugin.stagingWorldManager.showStartButtons(game, false);
				}
			});
		}
		else if ( newState == GameState.active )
		{
			// if the stats manager is tracking, then the game didn't finish "properly" ... this counts as an "aborted" game
			if ( plugin.statsManager.isTracking(number) )
				plugin.statsManager.gameFinished(this, getGameMode().getOnlinePlayers(true).size(), 3, 0);
			
			if ( prevState.usesGameWorlds )
			{
				plugin.worldManager.removeAllItems(getMainWorld());
				getMainWorld().setTime(0);
			}
			else
				plugin.getServer().getPluginManager().registerEvents(getGameMode(), plugin);

			getGameMode().startGame();
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
	
	private World mainWorld = null, netherWorld = null;
	World getMainWorld() { return mainWorld; }
	World getNetherWorld() { return netherWorld; }
	public void setMainWorld(World w) { mainWorld = w; }
	public void setNetherWorld(World w) { netherWorld = w; }
	
	String getMainWorldName() { return Settings.killerWorldName + number + "_nether"; }
	String getNetherWorldName() { return Settings.killerWorldName + number; }
	
	boolean forcedGameEnd = false;
	void endGame(CommandSender actionedBy)
	{
		if ( actionedBy != null )
			getGameMode().broadcastMessage(actionedBy.getName() + " ended the game. You've been moved to the staging world to allow you to set up a new one...");
		else
			getGameMode().broadcastMessage("The game has ended. You've been moved to the staging world to allow you to set up a new one...");
		
		setGameState(GameState.worldDeletion); // stagingWorldReady cos the options from last time will still be selected
	}
	
	void restartGame(CommandSender actionedBy)
	{
		if ( actionedBy != null )
			getGameMode().broadcastMessage(actionedBy.getName() + " is restarting the game...");
		else
			getGameMode().broadcastMessage("Game is restarting...");
		
		setGameState(GameState.active);
	}
}

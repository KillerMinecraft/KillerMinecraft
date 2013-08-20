package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.Bukkit;
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
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.ftwinston.Killer.PlayerManager.Info;

public class Game
{
	static LinkedList<Game> generationQueue = new LinkedList<Game>();

	Killer plugin;
	private int number, helpMessageProcess, compassProcess, spectatorFollowProcess;
	private String name;
	
	public Game(Killer killer, int gameNumber)
	{
		plugin = killer;
		number = gameNumber;
	}
	
	public String getName() { return name; }
	void setName(String n) { name = n; }
	
	private Location startButton, joinButton, configButton;
	private Location statusSign, startSign, joinSign, configSign;
	
	void initButtons(Location join, Location config, Location start)
	{
		joinButton = join;
		configButton = config;
		startButton = start;
	}

	void initSigns(Location status, Location join, Location config, Location start)
	{
		statusSign = status;
		joinSign = join;
		configSign = config;
		startSign = start;
	}

	private static boolean isSign(Block b)
	{
		return b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN;
	}
	
	private void updateSign(Location loc, String... lines)
	{
		if ( loc == null )
			return;
		
		if (!plugin.craftBukkit.isChunkGenerated(loc.getChunk()))
		{
			signsNeedingUpdated.put(loc, lines);
			return;
		}
		
		writeSign(loc, lines);
	}
	
	static boolean writeSign(Location loc, String... lines)
	{
		Block b = loc.getBlock();
		if ( !isSign(b) )
		{
			Killer.instance.log.warning("Expected sign at " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + " but got " + b.getType().name());
			return false;
		}
		
		Sign s = (Sign)b.getState();
		for ( int i=0; i<4 && i<lines.length; i++ )
			s.setLine(i, lines[i]);
		for ( int i=lines.length; i<4; i++ )
			s.setLine(i, "");
		s.update();
		return true;
	}
	
	private int progressBarDx, progressBarDy, progressBarDz;
	private Location progressBarMinExtent, progressBarMaxExtent;
	boolean initProgressBar(Location start, String dir, int length, int breadth, int depth)
	{
		progressBarDx = 0; progressBarDy = 0; progressBarDz = 0;
		
		// determine the extent of the progress bar
		if ( dir.equalsIgnoreCase("+x") )
		{
			progressBarDx = 1;
			int xExtent = length;
			int yExtent = depth;
			int zExtent = breadth;
			
			progressBarMinExtent = new Location(start.getWorld(), start.getBlockX(), start.getBlockY() - yExtent/2, start.getBlockZ() - zExtent/2);
			progressBarMaxExtent = new Location(start.getWorld(), start.getBlockX() + xExtent, progressBarMinExtent.getBlockY() + yExtent - 1, progressBarMinExtent.getBlockZ() + zExtent - 1);
		}
		else if ( dir.equalsIgnoreCase("-x") )
		{
			progressBarDx = -1;
			int xExtent = length;
			int yExtent = depth;
			int zExtent = breadth;
			
			progressBarMinExtent = new Location(start.getWorld(), start.getBlockX() - xExtent, start.getBlockY() - yExtent/2, start.getBlockZ() - zExtent/2);
			progressBarMaxExtent = new Location(start.getWorld(), start.getBlockX(), progressBarMinExtent.getBlockY() + yExtent - 1, progressBarMinExtent.getBlockZ() + zExtent - 1);
		}
		else if ( dir.equalsIgnoreCase("+y") )
		{
			progressBarDy = 1;
			int xExtent = breadth;
			int yExtent = length;
			int zExtent = depth;
			
			progressBarMinExtent = new Location(start.getWorld(), start.getBlockX() - xExtent/2, start.getBlockY(), start.getBlockZ() - zExtent/2);
			progressBarMaxExtent = new Location(start.getWorld(), progressBarMinExtent.getBlockX() + xExtent - 1, start.getBlockY() + yExtent, progressBarMinExtent.getBlockZ() + zExtent - 1);
		}
		else if ( dir.equalsIgnoreCase("-y") )
		{
			progressBarDy = -1;
			int xExtent = breadth;
			int yExtent = length;
			int zExtent = depth;
			
			progressBarMinExtent = new Location(start.getWorld(), start.getBlockX() - xExtent/2, start.getBlockY() - yExtent, start.getBlockZ() - zExtent/2);
			progressBarMaxExtent = new Location(start.getWorld(), progressBarMinExtent.getBlockX() + xExtent - 1, start.getBlockY(), progressBarMinExtent.getBlockZ() + zExtent - 1);
		}
		else if ( dir.equalsIgnoreCase("+z") )
		{
			progressBarDz = 1;
			int xExtent = breadth;
			int yExtent = depth;
			int zExtent = length;
			
			progressBarMinExtent = new Location(start.getWorld(), start.getBlockX() - xExtent/2, start.getBlockY() - yExtent/2, start.getBlockZ());
			progressBarMaxExtent = new Location(start.getWorld(), progressBarMinExtent.getBlockX() + xExtent - 1, progressBarMinExtent.getBlockY() + yExtent - 1, start.getBlockZ() + zExtent);
		}
		else if ( dir.equalsIgnoreCase("-z") )
		{
			progressBarDz = -1;
			int xExtent = breadth;
			int yExtent = depth;
			int zExtent = length;

			progressBarMinExtent = new Location(start.getWorld(), start.getBlockX() - xExtent/2, start.getBlockY() - yExtent/2, start.getBlockZ() - zExtent);
			progressBarMaxExtent = new Location(start.getWorld(), progressBarMinExtent.getBlockX() + xExtent - 1, progressBarMinExtent.getBlockY() + yExtent - 1, start.getBlockZ());
		}
		else
		{
			Killer.instance.log.warning("Invalid progress bar direction for " + getName() + ": " + dir);
			return false;
		}
		
		// ensure that min/max really are the right way around
		if ( progressBarMinExtent.getBlockX() > progressBarMaxExtent.getBlockX() )
		{
			int tmp = progressBarMinExtent.getBlockX();
			progressBarMinExtent.setX(progressBarMaxExtent.getBlockX());
			progressBarMaxExtent.setX(tmp);
		}
		if ( progressBarMinExtent.getBlockY() > progressBarMaxExtent.getBlockY() )
		{
			int tmp = progressBarMinExtent.getBlockY();
			progressBarMinExtent.setY(progressBarMaxExtent.getBlockY());
			progressBarMaxExtent.setY(tmp);
		}
		if ( progressBarMinExtent.getBlockZ() > progressBarMaxExtent.getBlockZ() )
		{
			int tmp = progressBarMinExtent.getBlockZ();
			progressBarMinExtent.setZ(progressBarMaxExtent.getBlockZ());
			progressBarMaxExtent.setZ(tmp);
		}
		
		/*int minChunkX = progressBarMinExtent.getBlockX() >> 4, minChunkZ = progressBarMinExtent.getBlockZ() >> 4;
		int maxChunkX = progressBarMaxExtent.getBlockX() >> 4, maxChunkZ = progressBarMaxExtent.getBlockZ() >> 4;
		
		for ( int x=minChunkX; x<=maxChunkX; x++ )
			for ( int z=minChunkZ; z<=maxChunkZ; z++ )
				useStagingWorldChunk(new ChunkPosition(x,z));*/
		return true;
	}
	
	void drawProgressBar()
	{
		switch ( getGameState() )
		{
		case active:
		case finished:
			drawProgressBar(1f); break;
		case worldGeneration:
			break;
		default:
			drawProgressBar(0f); break;
		}
	}
	
	void drawProgressBar(float fraction)
	{
		if ( progressBarMinExtent == null )
			return;
		
		Material type = Material.WOOL;
		byte data;
		if ( fraction <= 0 )
		{
			fraction = 1;
			data = 0xE;
		}
		else
		{
			data = 0x5;
			if ( fraction > 1 )
				fraction = 1;
		}
		
		if ( progressBarDx != 0 )
		{
			if ( progressBarDx > 0 )
				drawProgressBar(type, data, progressBarMinExtent.getBlockX(), progressBarMinExtent.getBlockY(), progressBarMinExtent.getBlockZ(), progressBarMinExtent.getBlockX() + (int)((progressBarMaxExtent.getBlockX() - progressBarMinExtent.getBlockX()) * fraction), progressBarMaxExtent.getBlockY(), progressBarMaxExtent.getBlockZ());
			else
				drawProgressBar(type, data, progressBarMaxExtent.getBlockX() - (int)((progressBarMaxExtent.getBlockX() - progressBarMinExtent.getBlockX()) * fraction), progressBarMinExtent.getBlockY(), progressBarMinExtent.getBlockZ(), progressBarMaxExtent.getBlockX(), progressBarMaxExtent.getBlockY(), progressBarMaxExtent.getBlockZ());
		}
		else if ( progressBarDy != 0 )
		{
			if ( progressBarDy > 0 )
				drawProgressBar(type, data, progressBarMinExtent.getBlockX(), progressBarMinExtent.getBlockY(), progressBarMinExtent.getBlockZ(), progressBarMaxExtent.getBlockX(), progressBarMinExtent.getBlockY() + (int)((progressBarMaxExtent.getBlockY() - progressBarMinExtent.getBlockY()) * fraction), progressBarMaxExtent.getBlockZ());
			else
				drawProgressBar(type, data, progressBarMinExtent.getBlockX(), progressBarMaxExtent.getBlockY() - (int)((progressBarMaxExtent.getBlockY() - progressBarMinExtent.getBlockY()) * fraction), progressBarMinExtent.getBlockZ(), progressBarMaxExtent.getBlockX(), progressBarMaxExtent.getBlockY(), progressBarMaxExtent.getBlockZ());
		}
		else// if ( progressBarDz != 0 )
		{
			if ( progressBarDz > 0 )
				drawProgressBar(type, data, progressBarMinExtent.getBlockX(), progressBarMinExtent.getBlockY(), progressBarMinExtent.getBlockZ(), progressBarMaxExtent.getBlockX(), progressBarMaxExtent.getBlockY(), progressBarMinExtent.getBlockZ() + (int)((progressBarMaxExtent.getBlockZ() - progressBarMinExtent.getBlockZ()) * fraction));
			else
				drawProgressBar(type, data, progressBarMinExtent.getBlockX(), progressBarMinExtent.getBlockY(), progressBarMaxExtent.getBlockZ() - (int)((progressBarMaxExtent.getBlockZ() - progressBarMinExtent.getBlockZ()) * fraction), progressBarMaxExtent.getBlockX(), progressBarMaxExtent.getBlockY(), progressBarMaxExtent.getBlockZ());
		}
	}
	
	private void drawProgressBar(Material type, byte data, int x1, int y1, int z1, int x2, int y2, int z2)
	{
		for ( int x=x1; x<=x2; x++ )
			for ( int z=z1; z<=z2; z++ )
				for ( int y=y1; y<=y2; y++ )
				{
					Block b = plugin.stagingWorld.getBlockAt(x,y,z);
					b.setType(type);
					b.setData(data);
				}
	}
	
	static HashMap<Location, String[]> signsNeedingUpdated = new HashMap<Location, String[]>();
		
	public boolean checkButtonPressed(Location loc, Player player)
	{
		if ( loc.equals(joinButton) )
		{
			joinPressed(player);
			return true;
		}
		else if ( loc.equals(configButton) )
		{
			configPressed(player);
			return true;
		}
		else if ( loc.equals(startButton) )
		{
			startPressed(player);
			return true;
		}
		return false;
	}
	
	public void joinPressed(Player player) {
		if ( isPlayerInGame(player) )
		{
			removePlayerFromGame(player);
			return;
		}
		
		if ( getGameState().canChangeGameSetup )
		{
			for ( Game other : plugin.games )
				if ( other != this && other.isPlayerInGame(player) )
				{
					other.removePlayerFromGame(player);
					break;
				}
			
			addPlayerToGame(player);
		}
		else // TODO attempt late joining, if allowed by game
			player.sendMessage("This game is in progress, and can't currently be joined.");
	}
	
	public void addPlayerToGame(Player player)
	{
		Info info = getPlayerInfo().get(player.getName());
		boolean isNewPlayer;
		if ( info == null )
		{
			isNewPlayer = true;
			
			if ( !getGameState().usesGameWorlds )
				info = PlayerManager.instance.CreateInfo(true);
			else if ( !Settings.allowLateJoiners )
				info = PlayerManager.instance.CreateInfo(false);
			else
			{
				info = PlayerManager.instance.CreateInfo(true);
				plugin.statsManager.playerJoinedLate(getNumber());
			}
			getPlayerInfo().put(player.getName(), info);
			
			// this player is new for this game, so clear them down
			if ( getGameState().usesGameWorlds )
				PlayerManager.instance.resetPlayer(this, player);
		}
		else
			isNewPlayer = false;

		Scoreboard sb = getSetupScoreboard();
		player.setScoreboard(sb);
		player.sendMessage("You have joined " + getName());

		updatePlayerCount();
		
		if ( !getGameState().usesGameWorlds )
			return;

		// hide all spectators from this player
		for ( Player spectator : getOnlinePlayers(new PlayerFilter().notAlive().exclude(player)) )
			PlayerManager.instance.hidePlayer(player, spectator);

		getGameMode().playerJoinedLate(player, isNewPlayer);
		
		if ( !info.isAlive() )
		{
			String message = isNewPlayer ? "" : "Welcome Back to " + getName() + ". ";
			message += "You are now a spectator. You can fly, but can't be seen or interact. Type " + ChatColor.YELLOW + "/spec" + ChatColor.RESET + " to list available commands.";
			
			player.sendMessage(message);
			
			PlayerManager.instance.setAlive(this, player, false);
			
			// send this player to everyone else's scoreboards, because they're now invisible, and won't show otherwise
			for ( Player online : getOnlinePlayers() )
				if ( online != player && !online.canSee(player) )
					plugin.craftBukkit.sendForScoreboard(online, player, true);
		}
		else
			PlayerManager.instance.setAlive(this, player, true);
		if ( isNewPlayer )
			getGameMode().sendGameModeHelpMessage(player);
			
		if ( player.getInventory().contains(Material.COMPASS) )
		{// does this need a null check on the target?
			player.setCompassTarget(PlayerManager.instance.getCompassTarget(this, player));
		}
	}

	public void removePlayerFromGame(OfflinePlayer player)
	{
		getPlayerInfo().remove(player.getName());
		
		updatePlayerCount();
		
		if ( player.isOnline() )
		{
			Player online = (Player)player;
			online.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
			online.sendMessage("You have left " + getName());
		}
	}
	
	void updatePlayerCount()
	{
		if ( !getGameState().usesGameWorlds )
		{
			setupObjective.getScore(plugin.getServer().getOfflinePlayer("# players")).setScore(getPlayerInfo().size());
		}
		
		/*
		int numPlayers = getOnlinePlayers().size(), numTotal = numPlayers + getOfflinePlayers().size();
		int buttonY = StagingWorldGenerator.getButtonY(getNumber());
		Block b = plugin.stagingWorld.getBlockAt(StagingWorldGenerator.mainButtonX, buttonY, StagingWorldGenerator.playerLimitZ);

		if ( usesPlayerLimit() )
		{
			if ( getPlayerLimit() == 0 )
			{
				setPlayerLimit(Math.max(numPlayers, 2));
				setLocked(numPlayers >= 2 && !getGameState().usesGameWorlds);
			}
			else if ( numPlayers == 0 && !getGameState().usesGameWorlds )
			{
				setUsesPlayerLimit(false);
				setLocked(false);

				// reset the switch
				Block lever = b.getRelative(1, 0, 0);
				lever.setData((byte)0x4);
			}
			else
			{
				setLocked(numTotal >= getPlayerLimit() && !getGameState().usesGameWorlds);
			}
		}
		else
			setLocked(false);

		updateSigns(GameSign.PLAYER_LIMIT);

		lockGame(this, isLocked() || (getGameState().usesGameWorlds && !Settings.allowLateJoiners && !Settings.allowSpectators));*/
	}
	
	Scoreboard setupScoreboard = null; Team setupTeam; Objective setupObjective;
	public Scoreboard getSetupScoreboard()
	{
		if ( setupScoreboard == null )
		{
			 setupScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
			 setupObjective = setupScoreboard.registerNewObjective("setup", "dummy");
			 setupObjective.setDisplayName(getName());
			 setupObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
			 setupTeam = setupScoreboard.registerNewTeam("all");
			 setupTeam.setDisplayName("# players");
		}
		return setupScoreboard;
	}
	
	public void configPressed(Player player) {
		if ( !isPlayerInGame(player) )
		{
			player.sendMessage("You cannot configure this game because you are not a part of it.");
			return;
		}
		
		GameConfiguration.instance.showConversation(player, this);
	}
	
	public void startPressed(Player player) {
		if ( getGameState() != GameState.stagingWorldSetup )
		{
			player.sendMessage("This game cannot be started. You can only start a game currently being set up.");
			return;
		}
		
		if ( !isPlayerInGame(player) )
		{
			player.sendMessage("You cannot start this game because you are not a part of it.");
			return;
		}
		
		if ( configuringPlayer != null )
		{
			if ( configuringPlayer.equals(player.getName()) )
				player.sendMessage("You cannot start this game while you are currently configuring it.");
			else
				player.sendMessage("You cannot start this game because " + configuringPlayer + " is configuring it.");
			return;
		}
		
		plugin.log.info(player.getName() + " started " + getName());
		setGameState(GameState.waitingToGenerate);
	}

	public boolean isPlayerInGame(Player player)
	{
		return getPlayerInfo().get(player.getName()) != null;
	}
	
	private String configuringPlayer = null;
	public String getConfiguringPlayer() { return configuringPlayer; }
	public void setConfiguringPlayer(String p)
	{
		configuringPlayer = p;
		if ( p == null )
			updateSign(configSign, "", "Configure", getName());
		else
			updateSign(configSign, "", "Currently being", "configured");
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
	
	void startProcesses()
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
	
	void stopProcesses()
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
			if ( newState.usesGameWorlds )
				startProcesses();
			else
				stopProcesses();
		}
		
		switch ( newState )
		{
			case worldDeletion:
			{
				updateSign(statusSign, "", "§l" + getName(), "* vacant *");
				updateSign(joinSign, "", "Join", getName());
				updateSign(configSign, "", "Configure", getName());
				updateSign(startSign, "", "Start", getName());
				
				if ( prevState == newState )
					return;
				
				// if the stats manager is tracking, then the game didn't finish "properly" ... this counts as an "aborted" game
				if ( plugin.statsManager.isTracking(number) )
					plugin.statsManager.gameFinished(number, getGameMode(), getWorldOption(), getGameMode().getOnlinePlayers(new PlayerFilter().alive()).size(), true);
				
				HandlerList.unregisterAll(getGameMode()); // stop this game mode listening for events

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
				break;
			}
			case stagingWorldSetup:
			{
				updateSign(statusSign, "", "§l" + getName(), "* vacant *");
				updateSign(joinSign, "", "Join", getName());
				updateSign(configSign, "", "Configure", getName());
				updateSign(startSign, "", "Start", getName());
				
				drawProgressBar(0f);
				
				if ( prevState == newState )
					return;
				
				break;
			}
			case stagingWorldConfirm:
			{
				updateSign(statusSign, "", "§l" + getName(), "* vacant *");
				updateSign(joinSign, "", "Join", getName());
				updateSign(configSign, "", "Configure", getName());
				updateSign(startSign, "", "Please", "Confirm");
				break;
			}
			case waitingToGenerate:		
			{
				if ( prevState == newState )
					return;
				
				// if nothing in the queue (so nothing currently generating), generate immediately
				if ( generationQueue.peek() == null ) 
					setGameState(GameState.worldGeneration);
				else
				{
					updateSign(statusSign, "", "§l" + getName(), "* in queue *");
					updateSign(joinSign, "", "Join", getName());
					updateSign(configSign, "", "Configuration", "Locked");
					updateSign(startSign, "", "Please", "Wait");
				}
				
				generationQueue.addLast(this); // always add to the queue (head of the queue is currently generating)
				break;
			}
			case worldGeneration:
			{
				updateSign(statusSign, "", "§l" + getName(), "* generating *");
				updateSign(joinSign, "", "Join", getName());
				updateSign(configSign, "", "Configuration", "Locked");
				updateSign(startSign, "", "Please", "Wait");
				
				if ( prevState == newState )
					return;
				
				plugin.getServer().getPluginManager().registerEvents(getGameMode(), plugin);
				
				final Game game = this;
				plugin.worldManager.generateWorlds(this, worldOption, new Runnable() {
					@Override
					public void run() {
						setGameState(GameState.active);
						
						if ( generationQueue.peek() != game )
							return; // just in case, only the "head" game should trigger this logic
						
						generationQueue.remove(); // remove from the queue when its done generating ... can't block other games
						Game nextInQueue = generationQueue.peek();
						if ( nextInQueue != null ) // start the next queued game generating
							nextInQueue.setGameState(GameState.worldGeneration);
					}
				});
				break;
			}
			case active:
			{
				updateSign(statusSign, "", "§l" + getName(), "* in progress *", getOnlinePlayers() + " players");
				
				if ( !Settings.allowLateJoiners && !Settings.allowSpectators )
					updateSign(joinSign, "No late joiners", "or spectators", "allowed");
				else if ( isLocked() || !Settings.allowLateJoiners )
					updateSign(joinSign, "", "Spectate", getName());
				else
					updateSign(joinSign, "", "Join", getName());

				updateSign(configSign, "", "Configuration", "Locked");
				updateSign(startSign, "", "Already", "Started");
				
				drawProgressBar(1f);
				
				if ( prevState == newState )
					return;
				
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
				break;
			}
			case finished:
			{
				updateSign(statusSign, "", "§l" + getName(), "* finishing *");
				updateSign(joinSign, "", "Please", "Wait");
				updateSign(configSign, "", "Configuration", "Locked");
				updateSign(startSign, "", "Please", "Wait");
				
				if ( prevState == newState )
					return;
				
				plugin.statsManager.gameFinished(number, getGameMode(), getWorldOption(), getGameMode().getOnlinePlayers(new PlayerFilter().alive()).size(), false);
				break;
			}
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
	
	String getWorldName() { return Settings.killerWorldNamePrefix + (getNumber()+1); }
	
	boolean forcedGameEnd = false;
	void endGame(CommandSender actionedBy)
	{
		if ( actionedBy != null )
			getGameMode().broadcastMessage(actionedBy.getName() + " ended the game. You've been moved to the staging world to allow you to set up a new one...");
		else
			getGameMode().broadcastMessage("The game has ended. You've been moved to the staging world to allow you to set up a new one...");
		
		setGameState(GameState.worldDeletion);
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

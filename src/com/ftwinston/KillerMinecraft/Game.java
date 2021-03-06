package com.ftwinston.KillerMinecraft;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import com.ftwinston.KillerMinecraft.MenuManager.GameMenu;

import com.ftwinston.KillerMinecraft.Configuration.TeamInfo;

public class Game
{
	static LinkedList<Game> generationQueue = new LinkedList<Game>();

	KillerMinecraft plugin;
	private int number, helpMessageProcess, compassProcess;
	
	MenuManager menuManager;

	public Game(KillerMinecraft killer, int gameNumber)
	{
		plugin = killer;
		number = gameNumber;
		
		worldBorderSize = Settings.defaultWorldBorderSize;
		menuManager = new MenuManager(this);
		reset();
	}
	
	public String getName() { return "Game #" + number; }
	
	public int getNumber() { return number; }
	
	String hostPlayer = null;
	
	public enum GameState
	{
		INITIALIZING(false, false, false, false),
		EMPTY(false, false, true, true),
		SETUP(false, false, false, true),
		LOBBY(false, false, true, true),
		QUEUE_FOR_GENERATION(false, false, true, true),
		GENERATING(true, false, true, true),
		STARTING(true, true, true, false),
		ACTIVE(true, true, true, false),
		FINISHED(true, true, false, false),
		WORLD_DELETION(true, false, false, false);

		public final boolean usesWorlds, playersInWorld, canJoin, usesLobby;
		private GameState(boolean usesWorlds, boolean playersInWorld, boolean canJoin, boolean usesLobby)
		{
			this.usesWorlds = usesWorlds;
			this.playersInWorld = playersInWorld;
			this.canJoin = canJoin;
			this.usesLobby = usesLobby;
		}
	}
	
	private GameState gameState = GameState.INITIALIZING;
	GameState getGameState() { return gameState; }
	void setGameState(GameState newState)
	{
		if ( gameState == newState )
			return;
		
		if (currentVote != null)
		{
			currentVote.cancelTask();
			currentVote = null;
		}
		
		if ( newState == GameState.ACTIVE )
			startProcesses();
		else if ( gameState == GameState.ACTIVE )
			stopProcesses();
		
		gameState = newState;

		switch ( gameState )
		{
			case INITIALIZING:
			case EMPTY:
			{
				for ( OfflinePlayer player : getPlayers() )
					removePlayerFromGame(player);
				reset();
				break;
			}
			case SETUP:
				break;
			case WORLD_DELETION:
			{
				plugin.log.info("Deleting worlds for game " + getNumber() + "...");
				plugin.eventListener.unregisterEvents(getGameMode());
				
				if (overworldGenerator != null)
					plugin.eventListener.unregisterEvents(overworldGenerator);
				if (netherWorldGenerator != null)
					plugin.eventListener.unregisterEvents(netherWorldGenerator);
				if (endWorldGenerator != null)
					plugin.eventListener.unregisterEvents(endWorldGenerator);

				for ( Player player : getOnlinePlayers(new PlayerFilter().onlySpectators()) )
					Helper.stopSpectating(this, player);
				
				for ( Player player : getOnlinePlayers() )
				{
					if (player.isDead())
						plugin.craftBukkit.forceRespawn(player);
					
					removePlayerFromGame(player);
				}
				plugin.playerManager.playerDataChanged();
				
				plugin.worldManager.deleteGameWorlds(this, new Runnable() {
					@Override
					public void run() {
						setGameState(GameState.EMPTY);
					}
				});
				
				break;
			}
			case LOBBY:
			{
				menuManager.repopulateMenu(GameMenu.PLAYERS);
				break;
			}
			case QUEUE_FOR_GENERATION:		
			{
				// if nothing in the queue (so nothing currently generating), generate immediately
				if ( generationQueue.peek() == null )
					setGameState(GameState.GENERATING);
				else
					broadcastMessage("Your game is in a queue, and will start shortly");
				
				generationQueue.addLast(this); // always add to the queue (head of the queue is currently generating)
				break;
			}
			case GENERATING:
			{	
				registerEvents();
				
				final Game game = this;
				plugin.worldManager.generateWorlds(this, new Runnable() {
					@Override
					public void run() {
						setGameState(GameState.STARTING);
						
						if ( generationQueue.peek() != game )
							return; // just in case, only the "head" game should trigger this logic
						
						generationQueue.remove(); // remove from the queue when its done generating ... can't block other games
						Game nextInQueue = generationQueue.peek();
						if ( nextInQueue != null ) // start the next queued game generating
							nextInQueue.setGameState(GameState.GENERATING);
					}
				}, false);
				break;
			}
			case STARTING:
			{
				broadcastMessage("Finished generating.\nGame started!");
				scoreboard = getGameMode().createScoreboard();
				List<Player> players = getOnlinePlayers();
				
				// allocate unassigned players to teams, if game uses teams
				TeamInfo[] teams = getGameMode().getTeams();
				if ( teams != null && teams.length != 0 )
				{
					ArrayList<Player> unallocated = new ArrayList<Player>();
					for ( Player player : players )
						if ( getPlayerInfo(player).getTeam() == null )
							unallocated.add(player);
					
					getGameMode().allocateTeams(unallocated);
				}
				
				// now put players into the game
				for ( Player player : getOnlinePlayers(new PlayerFilter().includeSpectators()) )
				{
					plugin.playerManager.savePlayerData(player);
					player.setScoreboard(scoreboard);
					
					Location spawnLocation = getGameMode().getSpawnLocation(player);
					if (spawnLocation == null)
					{
						plugin.log.warning(getGameMode().name + " returned a null spawn point");
						spawnLocation = worlds.get(0).getSpawnLocation();
					}
					Helper.teleport(player, spawnLocation);
					
					if ( getPlayerInfo(player).isSpectator() )
						Helper.makeSpectator(this, player);
				}
				plugin.playerManager.playerDataChanged();

				getGameMode().startGame();
				setGameState(GameState.ACTIVE);
				break;
			}
			case ACTIVE:
			{
				for (Player player : getOnlinePlayers())
					setupHelpMessages(player);
				
				break;
			}
			case FINISHED:
			{
				getGameMode().gameFinished();
				
				plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
					@Override
					public void run() {
						setGameState(GameState.WORLD_DELETION);
					}
				}, 220); // 11 second delay
				break;
			}
		}
		
		menuManager.updateMenus();
	}
	
	private void registerEvents()
	{
		plugin.eventListener.registerEvents(getGameMode());
		if ( getGameMode().allowWorldGeneratorSelection() )
		{				
			if (overworldGenerator != null)
				plugin.eventListener.registerEvents(overworldGenerator);
			if (netherWorldGenerator != null)
				plugin.eventListener.registerEvents(netherWorldGenerator);
			if (endWorldGenerator != null)
				plugin.eventListener.registerEvents(endWorldGenerator);
		}
	}

	private double worldBorderSize = 0;
	public double getWorldBorderSize() { return worldBorderSize; }
	public void setWorldBorderSize(double size) { worldBorderSize = size; }
	
	private boolean isPrivate;
	public boolean isPrivate() { return isPrivate; }
	public void setPrivate(boolean b) { isPrivate = b; }
	
	public boolean canJoin()
	{
		if ( !Settings.allowLateJoiners && getGameState().playersInWorld )
			return false;
		return gameState.canJoin && !isPrivate && !isLocked() && (!usesPlayerLimit() || getPlayers().size() < playerLimit);
	}
	
	public void reset()
	{
		boolean wasEmpty = gameState == GameState.EMPTY;
		gameState = GameState.INITIALIZING;
		
		worldBorderSize = Settings.defaultWorldBorderSize;
		isPrivate = false;
		hostPlayer = null;
		
		setDifficulty(defaultDifficulty);
		setGameMode(plugin.defaultGameMode);
		
		overworldGenerator = netherWorldGenerator = endWorldGenerator = null;
		if (gameMode.allowWorldGeneratorSelection())
		{
			for (Environment worldType : Environment.values())
				if (gameMode.usesWorldType(worldType))
					setWorldGenerator(worldType, WorldGenerator.getDefault(worldType));
		}
		
		monsterNumbers = defaultMonsterNumbers;
		animalNumbers = defaultAnimalNumbers;
		menuManager.moduleSetupComplete = false;
		menuManager.repopulateMenu(GameMenu.PLAYERS);

		if (wasEmpty)
		{
			menuManager.updateMenus();
			gameState = GameState.EMPTY;
		}
		else
			setGameState(GameState.EMPTY);
	}
	
	public class PlayerInfo
	{
		public PlayerInfo(boolean spectator) { spec = spectator; team = null; }
		
		private boolean spec;
		private TeamInfo team;
		public TeamInfo getTeam() { return team; }
		
		public void setTeam(TeamInfo team)
		{
			this.team = team;
		}
		
		// am I a survivor or a spectator?
		public boolean isSpectator() { return spec; }
		
		public void setSpectator(boolean b)
		{
			spec = b;
		}
		
		public ListIterator<String> helpMessageIterator = null;
	}

	public void addPlayerToGame(Player player)
	{
		Game prevGame = plugin.getGameForPlayer(player);
		if ( prevGame != null )
			if ( prevGame == this )
				return;
			else
				prevGame.removePlayerFromGame(player);
		
		broadcastMessage(ChatColor.YELLOW + "[Killer] " + player.getName() + " joined the game");
		plugin.gamesByPlayer.put(player.getName(), this);
		playerInfo.put(player.getName(), new PlayerInfo(false));
		
		if ( Settings.filterScoreboard )
		{
			// add to the scoreboard of everyone in this game
			for ( Player other : getOnlinePlayers(new PlayerFilter().exclude(player)) )
				plugin.craftBukkit.sendForScoreboard(other, player, true);
						
			// remove everyone not in this game from my scoreboard
			for ( Player other : plugin.getServer().getOnlinePlayers() )
				if ( plugin.getGameForPlayer(other) != this )
					plugin.craftBukkit.sendForScoreboard(player, other, false);
		}
		
		if (getGameState() == GameState.EMPTY)
		{
			hostPlayer = player.getName();
			setGameState(GameState.SETUP);
			return;
		}
		
		if (getGameState().playersInWorld)
		{
			plugin.playerManager.savePlayerData(player);
			plugin.playerManager.playerDataChanged();
			
			player.setScoreboard(scoreboard);
			
			getGameMode().playerJoinedLate(player);

			setupHelpMessages(player);
			sendHelpMessage(player);
			
			Location spawnLocation = getGameMode().getSpawnLocation(player);
			if (spawnLocation == null)
			{
				plugin.log.warning(getGameMode().name + " returned a null spawn point");
				spawnLocation = worlds.get(0).getSpawnLocation();
			}
			Helper.teleport(player, spawnLocation);
						
			if ( player.getInventory().contains(Material.COMPASS) )
			{
				player.setCompassTarget(getCompassTarget(player));
			}
		}
		
		menuManager.updateGameIcon();
	}
	
	public void removePlayerFromGame(OfflinePlayer player)
	{
		removePlayerFromGame(player, false);
	}

	public void removePlayerFromGame(OfflinePlayer player, boolean disconnected)
	{
		if ( getGameState() == GameState.ACTIVE )
			getGameMode().playerQuit(player);
		
		getGameMode().setTeam(player, null);
			
		plugin.gamesByPlayer.remove(player.getName());
		
		boolean persistent = gameMode.isPersistent();
		
		PlayerInfo info = persistent ? playerInfo.get(player.getName()) : playerInfo.remove(player.getName());
		
		if ( getGameState().playersInWorld )
			broadcastMessage(ChatColor.YELLOW + "[Killer] " + player.getName() + " left the game");
		
		if ( player.isOnline() )
		{	
			Player online = (Player)player;
			online.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
			online.sendMessage("You have left " + getName());

			if ( !disconnected && !getGameState().usesLobby && plugin.playerManager.restorePlayerData(online) )
				plugin.playerManager.playerDataChanged();
			
			if ( Settings.filterScoreboard )
			{
				// remove from the scoreboard of everyone in this game
				for ( Player other : getOnlinePlayers(new PlayerFilter().exclude(player)) )
					plugin.craftBukkit.sendForScoreboard(other, online, false);
				
				// add everyone NOT in this game onto this player's scoreboard
				for ( Player other : plugin.getServer().getOnlinePlayers() )
					if ( plugin.getGameForPlayer(other) != this )
						plugin.craftBukkit.sendForScoreboard(online, other, true);
			}
			
			// if they're a spectator, let them be seen again
			if ( info.isSpectator() )
				Helper.stopSpectating(this, online);

			// this player just quit, so let them see all spectators again (when those players leave the game)
			if ( getGameState() != GameState.WORLD_DELETION )
				for ( Player spec : getOnlinePlayers(new PlayerFilter().onlySpectators()) )
					online.showPlayer(spec);
		}
		
		if ( getGameState() == GameState.WORLD_DELETION )
			return;
		else if ( getOnlinePlayers().size() == 0 && !persistent )
			finishGame();
		else if ( player.getName() == hostPlayer && !getGameState().usesWorlds )
		{
			for ( Player other : getOnlinePlayers() )
				if (other != player)
					other.closeInventory();
				
			broadcastMessage("The host has left, so the game has been cancelled.");
			setGameState(GameState.EMPTY);
			return;
		}
		
		menuManager.updateGameIcon();
	}
		
	final void setupHelpMessages(Player player)
	{
		PlayerInfo info = getPlayerInfo(player); 
		info.helpMessageIterator = getGameMode().getHelpMessages(info.getTeam()).listIterator();
	}

	final void sendHelpMessages()
	{
		for (Player player : getOnlinePlayers())
			sendHelpMessage(player);
	}
	
	final boolean sendHelpMessage(Player player)
	{
		PlayerInfo info = getPlayerInfo().get(player.getName());
		
		if (info.helpMessageIterator != null && info.helpMessageIterator.hasNext())
		{
			boolean firstMessage = !info.helpMessageIterator.hasPrevious();
			
			String message = info.helpMessageIterator.next(); 
			if (message != null)
			{
				if (firstMessage)
					message = ChatColor.YELLOW + getName() + ChatColor.RESET + "\n" + message; // put the game mode name on the front of the first message
				
				player.sendMessage(message);
				return true;
			}
		}
		return false;
	}

	Vote currentVote = null;
	
	public void startVote(final Vote vote)	
	{
		if (currentVote != null)
			return;
		
		currentVote = vote;
		vote.start();
	}
	
	void startGame() {
		if ( getGameState() != GameState.LOBBY )
			return;
		
		broadcastMessage(ChatColor.YELLOW + hostPlayer + " started the game");
		setGameState(GameState.QUEUE_FOR_GENERATION);
	}

	void finishGame()
	{	
		if ( !plugin.isEnabled() )
			setGameState(GameState.EMPTY);
		
		if ( getGameState() != GameState.ACTIVE )
		{
			if ( getGameState().usesWorlds )
			{
				if ( getGameState() != GameState.WORLD_DELETION ) 
					setGameState(GameState.WORLD_DELETION);
			}
			else
				setGameState(GameState.EMPTY);
			return;
		}
		
		setGameState(GameState.FINISHED);
	}
	
	private GameMode gameMode = null;
	GameMode getGameMode() { return gameMode; }

	Scoreboard scoreboard;
	boolean setGameMode(GameModePlugin plugin)
	{
		GameModePlugin prev = gameMode == null ? null : (GameModePlugin)gameMode.getPlugin();
		if ( prev == plugin )
			return false;
		
		gameMode = plugin.createInstance();
		gameMode.initialize(this, plugin);
		
		if (gameMode.allowWorldGeneratorSelection())
		{
			for (Environment worldType : Environment.values())
			{
				if (gameMode.usesWorldType(worldType))
					setWorldGenerator(worldType, WorldGenerator.getDefault(worldType));
				else
					setWorldGenerator(worldType, null);
			}
		}
		else
		{
			overworldGenerator = netherWorldGenerator = endWorldGenerator = null;
		}
		
		menuManager.updateMenus();

		for ( Player player : getOnlinePlayers() )
			gameMode.setTeam(player, null);
		return true;
	}
	
	private WorldGenerator overworldGenerator = null, netherWorldGenerator = null, endWorldGenerator = null;
	WorldGenerator getWorldGenerator(Environment worldType)
	{
		switch (worldType)
		{
		case NORMAL:
			return overworldGenerator;
		case NETHER:
			return netherWorldGenerator;
		case THE_END:
			return endWorldGenerator;
		default:
			return null;
		}
	}
	
	void setWorldGenerator(Environment worldType, WorldGeneratorPlugin plugin)
	{
		WorldGenerator prev = getWorldGenerator(worldType);
		WorldGeneratorPlugin prevPlugin = prev == null ? null : (WorldGeneratorPlugin)prev.getPlugin();
		if (prevPlugin == plugin)
			return;
		
		WorldGenerator generator = plugin == null ? null : plugin.createInstance();
		if (generator != null)
			generator.initialize(this, plugin);
		
		switch (worldType)
		{
		case NORMAL:
			overworldGenerator = generator; break;
		case NETHER:
			netherWorldGenerator = generator; break;
		case THE_END:
			endWorldGenerator = generator; break;
		}
	}

	private TreeMap<String, PlayerInfo> playerInfo = new TreeMap<String, PlayerInfo>();
	public Map<String, PlayerInfo> getPlayerInfo() { return playerInfo; }
	public PlayerInfo getPlayerInfo(String player) { return playerInfo.get(player); }
	public PlayerInfo getPlayerInfo(OfflinePlayer player) { return getPlayerInfo(player.getName()); }

	static final int minQuantityNum = 0, maxQuantityNum = 4;
	
	static final int defaultMonsterNumbers = 2, defaultAnimalNumbers = 2; 
	int monsterNumbers = defaultMonsterNumbers, animalNumbers = defaultAnimalNumbers;

	static final Difficulty defaultDifficulty = Difficulty.HARD;
	private Difficulty difficulty = defaultDifficulty;
	public Difficulty getDifficulty() { return difficulty; }
	public void setDifficulty(Difficulty d) { difficulty = d; }

	void startProcesses()
	{
		// start sending out help messages explaining the game rules
		helpMessageProcess = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run()
			{
				sendHelpMessages();
			}
		}, 0, 550L); // send every 25 seconds
		
		compassProcess = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
        	public void run()
        	{
	        	for ( Player player : getGameMode().getOnlinePlayers() )
	        		if ( player.getInventory().contains(Material.COMPASS) )
	        			player.setCompassTarget(getCompassTarget(player));
        	}
        }, 20, 10);
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
	}
	
	Location getCompassTarget(Player player)
	{
		Location target = getGameMode().getCompassTarget(player);
		if ( target != null )
			return target;
		
		target = player.getBedSpawnLocation();
		if ( target != null && target.getWorld() == player.getWorld() )
			return target;
		
		return player.getWorld().getSpawnLocation();
	}

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

	public LinkedList<Player> getOnlinePlayers()
	{		
		return getOnlinePlayers(new PlayerFilter());
	}
	
	public LinkedList<Player> getOnlinePlayers(PlayerFilter filter)
	{
		return filter.setGame(this).getOnlinePlayers();
	}
	
	public LinkedList<OfflinePlayer> getOfflinePlayers()
	{
		return getOfflinePlayers(new PlayerFilter());
	}
	
	public LinkedList<OfflinePlayer> getOfflinePlayers(PlayerFilter filter)
	{		
		return filter.offline().setGame(this).getPlayers();
	}
	
	public LinkedList<OfflinePlayer> getPlayers()
	{
		return getPlayers(new PlayerFilter());
	}
	
	public LinkedList<OfflinePlayer> getPlayers(PlayerFilter filter)
	{		
		return filter.setGame(this).getPlayers();
	}
	
	public void broadcastMessage(String message)
	{
		for ( Player player : getOnlinePlayers(new PlayerFilter().includeSpectators()) )
			player.sendMessage(message);
	}
	
	public void broadcastMessage(PlayerFilter recipients, String message)
	{
		for ( Player player : getOnlinePlayers(recipients) )
			player.sendMessage(message);
	}

	public void loadPersistentData(final ConfigurationSection section)
	{
		// set game mode and world generator plugins
		GameModePlugin mode = GameMode.getByName(section.getString("gameMode"));
		if (mode == null)
		{
			plugin.log.warning("Cannot find \"" + section.getString("gameMode") + "\" game mode, used by persistent game");
			return;
		}
		setGameMode(mode);
		loadPersistentModuleOptions(getGameMode(), section.getConfigurationSection("gameModeOptions"));
		
		EnumMap<Environment, String> fieldNames = new EnumMap<>(Environment.class);
		fieldNames.put(Environment.NORMAL, "worldGenerator");
		fieldNames.put(Environment.NETHER, "netherGenerator");
		fieldNames.put(Environment.THE_END, "endGenerator");
		
		for (Environment worldType : Environment.values())
		{
			String fieldName = fieldNames.get(worldType);
			
			WorldGeneratorPlugin gen = WorldGenerator.getByName(worldType, section.getString(fieldName));
			if (gen != null)
			{
				setWorldGenerator(worldType, gen);
				loadPersistentModuleOptions(getWorldGenerator(worldType), section.getConfigurationSection(fieldName + "Options"));
			}
		}

		// load player data
		ConfigurationSection players = section.getConfigurationSection("players");
		TeamInfo[] teams = getGameMode().getTeams();
		
		for (String playerName : players.getKeys(false))
		{
			ConfigurationSection playerSection = players.getConfigurationSection(playerName);
			if (playerSection == null)
				continue;
			
			boolean spectator = playerSection.getBoolean("spectator", false);
			PlayerInfo info = new PlayerInfo(spectator);
			
			int teamNum = playerSection.getInt("team", -1);
			if (teamNum != -1 && teams != null && teamNum < teams.length)
				info.setTeam(teams[teamNum]);
			
			playerInfo.put(playerName,  info);
		}
		
		// "create" server worlds that will link up to existing worlds' data
		plugin.worldManager.generateWorlds(this, new Runnable() {
			@Override
			public void run() {
				// recreate the scoreboard
				scoreboard = getGameMode().createScoreboard();
				
				// lastly, load any game mode internal data
				if (getGameMode().isPersistent())
					((PersistentGameMode)getGameMode()).loadPersistentData(section.getConfigurationSection("mode"));

				setGameState(GameState.ACTIVE);
				registerEvents();
			}
		}, true);
	}
	
	private void loadPersistentModuleOptions(KillerModule module, ConfigurationSection configData)
	{
		if (configData == null || module.options == null || module.options.length == 0)
			return;
		
		for (Option o : module.options)
		{
			String keyName = o.getName().replace(" ", "_");
			String saved = configData.getString(keyName);
			if (saved != null)
				if (!o.trySetValue(saved))
					plugin.log.warning("Got invalid value saved for " + module.getName() + "'s " + keyName + " value: " + saved);
		}
	}

	public void savePersistentData(ConfigurationSection section)
	{
		GameMode mode = getGameMode();
		section.set("gameMode", mode.getName());
		if (mode.options != null && mode.options.length > 0)
		{
			ConfigurationSection options = section.createSection("gameModeOptions");
			savePersistentModuleOptions(mode, options);
		}
		
		EnumMap<Environment, String> fieldNames = new EnumMap<>(Environment.class);
		fieldNames.put(Environment.NORMAL, "worldGenerator");
		fieldNames.put(Environment.NETHER, "netherGenerator");
		fieldNames.put(Environment.THE_END, "endGenerator");
		
		for (Environment worldType : Environment.values())
		{
			WorldGenerator gen = getWorldGenerator(worldType); 
			if (gen != null)
			{
				String fieldName = fieldNames.get(worldType);
				section.set(fieldName, gen.getName());
				if (gen.options != null && gen.options.length > 0)
				{
					ConfigurationSection options = section.createSection(fieldName + "Options");
					savePersistentModuleOptions(gen, options);
				}
			}
		}
		
		// save player data
		ConfigurationSection players = section.createSection("players");
		TeamInfo[] teams = getGameMode().getTeams();
		for (Map.Entry<String, PlayerInfo> entry : playerInfo.entrySet())
		{
			ConfigurationSection playerSection = players.createSection(entry.getKey());			
			PlayerInfo info = entry.getValue();
			
			playerSection.set("spectator", info.isSpectator());

			TeamInfo team = info.getTeam(); 
			if (team == null)
				continue;
			
			for (int i=0; i<teams.length; i++)
				if (teams[i] == team)
				{
					playerSection.set("team", i);
					break;
				}
		}
		
		// lastly, save any game mode internal data
		if (getGameMode().isPersistent())
			((PersistentGameMode)getGameMode()).savePersistentData(section.getConfigurationSection("mode"));
	}

	private void savePersistentModuleOptions(KillerModule module, ConfigurationSection configDestination)
	{
		for (Option o : module.options)
		{
			String keyName = o.getName().replace(" ", "_");
			configDestination.set(keyName, o.getValueString());
		}
	}
}

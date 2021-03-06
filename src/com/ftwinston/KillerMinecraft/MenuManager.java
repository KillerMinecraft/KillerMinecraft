package com.ftwinston.KillerMinecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.ftwinston.KillerMinecraft.Game.GameState;
import com.ftwinston.KillerMinecraft.Game.PlayerInfo;
import com.ftwinston.KillerMinecraft.Configuration.MenuItem;
import com.ftwinston.KillerMinecraft.Configuration.TeamInfo;


class MenuManager
{
	enum GameMenu
	{
		SETUP,
		GAME_MODE_SELECT,
		GAME_MODE_CONFIG,
		WORLD_GEN_SELECT,
		WORLD_GEN_CONFIG,
		//MUTATORS,
		PLAYERS,
		DIFFICULTY,
		WORLD_BORDERS,

		SPECIFIC_OPTION_CHOICE,

		LOBBY,
		SPECTATOR_LOBBY,
		TEAM_SELECTION,
		ACTIVE,
	}
	
	EnumMap<GameMenu, Inventory> inventories = new EnumMap<GameMenu, Inventory>(GameMenu.class);
	HashMap<Inventory, MenuItem[]> menuItems = new HashMap<Inventory, MenuItem[]>();
	
	public static Inventory rootMenu;
	protected static MenuItem[] rootMenuItems = new MenuItem[9]; 
	
	Game game;
	MenuItem gameRootMenuItem;
	private static ItemStack helpItem, helpBook, backItem, quitItem;

	public MenuManager(final Game game)
	{
		this.game = game;
		
		gameRootMenuItem = new MenuItem(rootMenu, game.getNumber() - 1, new ItemStack(Material.BUCKET, 1)) {
			@Override
			public void runWhenClicked(Player player) {
				if ( game.canJoin() )
				{
					game.addPlayerToGame(player);
					if (game.getGameState() == GameState.ACTIVE)
						player.closeInventory();
					else
						show(player);
				}
				else
					player.sendMessage(ChatColor.RED + "You can't join this game");
			}
			
			@Override
			public void recalculateStack()
			{
				Material mat;
				
				if ( game.getGameState() == GameState.EMPTY )
					mat = Material.BUCKET;
				else if ( game.getGameState() == GameState.SETUP )
					mat = Material.MILK_BUCKET;
				else
					mat = game.canJoin() ? Material.WATER_BUCKET : Material.LAVA_BUCKET;
				
				ItemStack stack = new ItemStack(mat, 1);
				setNameAndLore(stack, game.getName(), describeGame(game));
				setStack(stack);
			}
		};
		
		addItemToMenu(null, gameRootMenuItem);
		
		inventories.put(GameMenu.SETUP, createSetupMenu());
		inventories.put(GameMenu.LOBBY, createLobbyMenu());
		inventories.put(GameMenu.SPECTATOR_LOBBY, createSpectatorLobbyMenu());
		inventories.put(GameMenu.TEAM_SELECTION, createTeamMenu());
		inventories.put(GameMenu.ACTIVE, createActiveMenu());
		
		inventories.put(GameMenu.GAME_MODE_SELECT, createGameModeSelectionMenu());
		inventories.put(GameMenu.GAME_MODE_CONFIG, createGameModeConfigMenu());
		inventories.put(GameMenu.WORLD_GEN_SELECT, createWorldGenSelectionMenu());
		inventories.put(GameMenu.WORLD_GEN_CONFIG, createWorldGenConfigMenu());
		//inventories.put(GameMenu.MUTATORS, createMutatorsMenu());
		inventories.put(GameMenu.PLAYERS, createPlayersMenu());
		inventories.put(GameMenu.DIFFICULTY, createDifficultyMenu());
		inventories.put(GameMenu.WORLD_BORDERS, createWorldBordersMenu());
	}
	
	static void addItemToMenu(MenuManager instance, MenuItem item) 
	{
		if (instance == null)
		{
			rootMenuItems[item.getSlot()] = item;
			return;
		}
		
		// add this to some data structure, so we can call its runOnClick later
		Inventory menu = item.getMenu();
		MenuItem[] items;
		if (instance.menuItems.containsKey(menu))
			items = instance.menuItems.get(menu);
		else
		{
			items = new MenuItem[9];
			instance.menuItems.put(menu, items);
		}
		
		items[item.getSlot()] = item;
	}
	
	static void createRootMenu()
	{
		rootMenu = Bukkit.createInventory(null, 9, "Killer Minecraft: Join a Game");
		
		helpItem = new ItemStack(Material.BOOK, 1);
		setNameAndLore(helpItem, "Help", "Click for a book that explains", "how Killer Minecraft works");
		
		helpBook = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta metaData = (BookMeta)helpBook.getItemMeta();
		metaData.setTitle("Help");
		metaData.setAuthor("Killer Minecraft");
		metaData.addPage("This book should explain how Killer Minecraft works.", "But it doesn't really. Not yet.", "Sorry.");
		helpBook.setItemMeta(metaData);
		
		addItemToMenu(null, new MenuItem(rootMenu, 8, helpItem) {
			@Override
			public void runWhenClicked(Player player) {
				player.setItemOnCursor(helpBook);
			}
		});
		
		// these items not used in the root menu, but are used (statically) in other menus
		quitItem = new ItemStack(Material.TNT, 1);
		setNameAndLore(quitItem, "Quit", "Click to exit this game");
		
		backItem = new ItemStack(Material.WOOD_DOOR);
		setNameAndLore(backItem, highlightStyle + "Go back", "Return to the previous menu");
		
		ItemStack becomeSpectatorItem = new ItemStack(Material.EYE_OF_ENDER, 1);
		setNameAndLore(becomeSpectatorItem, "Spectate", "Click to become a spectator", "in this game"); // clicking marks your playerInfo as spectator, and moves you to the "spectator lobby"
	}

	private static String[] describeGame(Game game)
	{
		switch ( game.getGameState() )
		{
		case EMPTY:
			return new String[] { "This game is empty.", "Click to configure a new game." };
		case SETUP:
			return new String[] { ChatColor.GRAY + "Hosted by " + game.hostPlayer, "This game is being configured.", "You can't yet join." };
		case LOBBY:
		case QUEUE_FOR_GENERATION:
			return new String[] { ChatColor.GRAY + "Hosted by " + game.hostPlayer, "This game has not started.", game.canJoin() ? "Click to join." : "You can't join." }; 
		case GENERATING:
			return new String[] { ChatColor.GRAY + "Hosted by " + game.hostPlayer, "This game is starting.", game.canJoin() ? "Click to join." : "You can't join." };
		case ACTIVE:
			return new String[] { ChatColor.GRAY + "Hosted by " + game.hostPlayer, "This game is active.", game.canJoin() ? "Click to join." : "You can't join." };
		case FINISHED:
		case WORLD_DELETION:
			return new String[] { ChatColor.GRAY + "Hosted by " + game.hostPlayer, "This game has finished.", "Please wait." };
		default:
			return new String[0];
		}
	}
	
	private static String[] describeSettings(Game game)
	{
		if (!game.getGameMode().allowWorldGeneratorSelection())
			return new String[] { "Game mode: " + game.getGameMode().getName() };
		
		ArrayList<String> settings = new ArrayList<String>();
		settings.add("Game mode: " + game.getGameMode().getName());
		
		WorldGenerator world = game.getWorldGenerator(Environment.NORMAL);
		if (world != null)
			settings.add("World: " + world.getName());

		world = game.getWorldGenerator(Environment.NETHER);
		if (world != null)
			settings.add("Nether: " + world.getName());

		world = game.getWorldGenerator(Environment.THE_END);
		if (world != null)
			settings.add("End: " + world.getName());
		
		String[] help = new String[0];
		return settings.toArray(help);
	}
	
	private static String listAllSettings(Game game)
	{
		String output = "Game mode: " + game.getGameMode().getName();
		
		for ( Option o : game.getGameMode().options )
			if ( !o.isHidden() )
				output += "\n  " + o.getName() + ": " + o.getValueString();
		
		if ( game.getGameMode().allowWorldGeneratorSelection() )
		{
			WorldGenerator world = game.getWorldGenerator(Environment.NORMAL);
			if (world != null)
			{
				output += "\nWorld generator: " + world.getName();
				for ( Option o : world.options )
					if ( !o.isHidden() )
						output += "\n  " + o.getName() + ": " + o.getValueString();	
			}
			
			world = game.getWorldGenerator(Environment.NETHER);
			if (world != null)
			{
				output += "\nNether generator: " + world.getName();
				for ( Option o : world.options )
					if ( !o.isHidden() )
						output += "\n  " + o.getName() + ": " + o.getValueString();	
			}
			
			world = game.getWorldGenerator(Environment.THE_END);
			if (world != null)
			{
				output += "\nEnd generator: " + world.getName();
				for ( Option o : world.options )
					if ( !o.isHidden() )
						output += "\n  " + o.getName() + ": " + o.getValueString();	
			}
		}
		
		// player settings
		if ( game.usesPlayerLimit() )
			output += "\nPlayer limit: " + game.getPlayerLimit();
		else
			output += "\nNo player limit";
		
		if ( game.isLocked() )
			output += " (game is locked)";

		output += "\nDifficulty: " + difficultyNames[game.getDifficulty().ordinal()];

		return output;
	}
	
	private static String describeNumber(int num, String singular, String plural)
	{
		if ( num == 1 )
			return "is 1 " + singular;
		else
			return "are " + num + " " + plural;
	}
	
	private static String[] countPlayers(Game game)
	{
		String line1 = "There " + describeNumber(game.getOnlinePlayers().size(), "player", "players") + " in this game.";
		String line2 = null, line3 = null;

		int num = game.getOnlinePlayers(new PlayerFilter().onlySpectators()).size();
		if ( num > 0 )
			line2 = "There " + describeNumber(num, "spectator", "spectators") + ".";
		
		if ( game.isLocked() )
		{
			String msg = "This game is locked: no one can join."; 
			if (line2 == null)
				line2 = msg;
			else
				line3 = msg;
		}
		else if ( game.usesPlayerLimit() )
		{
			String msg = "The limit is " + game.getPlayerLimit() + " players.";
			if (line2 == null)
				line2 = msg;
			else
				line3 = msg;
		}
		
		if ( line3 != null )
			return new String[] { line1, line2, line3 };
		else if ( line2 != null )
			return new String[] { line1, line2 };
		return new String[] { line1 };
	}
	
	private static String listPlayers(Game game)
	{
		List<Player> players = game.getOnlinePlayers();
		String output = "Players in this game:\n";
		boolean first = true;
		for ( Player player : players )
		{
			if ( first )
				first = false;
			else
				output += ", ";
			output += player.getName();
		}
		
		players = game.getOnlinePlayers(new PlayerFilter().onlySpectators());
		if ( players.size() == 0 )
			return output;
		
		output += "\nSpectating this game:\n";
		
		first = true;
		for ( Player player : players )
		{
			if ( first )
				first = false;
			else
				output += ", ";
			output += player.getName();
		}
		
		return output;
	}
	
	GameMenu getNextSetupMenu(GameMenu current)
	{
		switch (current)
		{
		case GAME_MODE_SELECT:
			if (moduleSetupComplete)
				return GameMenu.GAME_MODE_CONFIG;
			
			if (shouldShowWorldSelection(Environment.NORMAL))
			{
				worldGeneratorType = Environment.NORMAL;
				return GameMenu.WORLD_GEN_SELECT;
			}
		case WORLD_GEN_SELECT:
			if (moduleSetupComplete)
				return GameMenu.WORLD_GEN_CONFIG;
			
			switch (worldGeneratorType)
			{
			case NORMAL:
				if (shouldShowWorldSelection(Environment.NETHER))
				{
					worldGeneratorType = Environment.NETHER;
					return GameMenu.WORLD_GEN_SELECT;
				}
			case NETHER:
				if (shouldShowWorldSelection(Environment.THE_END))
				{
					worldGeneratorType = Environment.THE_END;
					return GameMenu.WORLD_GEN_SELECT;
				}
			case THE_END:
				//return GameMenu.MUTATORS;
			}
		default:
			break;
		}
		
		moduleSetupComplete = true;
		return GameMenu.SETUP;
	}
	
	GameMenu getPrevSetupMenu(GameMenu current)
	{
		if (moduleSetupComplete)
		{
			if (current == GameMenu.GAME_MODE_SELECT)
				return GameMenu.GAME_MODE_CONFIG;
			else if (current == GameMenu.WORLD_GEN_SELECT)
				return GameMenu.WORLD_GEN_CONFIG;
			
			return GameMenu.SETUP;
		}
		
		if (current == GameMenu.WORLD_GEN_SELECT)
			switch (worldGeneratorType)
			{
			case THE_END:
				if (shouldShowWorldSelection(Environment.NETHER))
				{
					worldGeneratorType = Environment.NETHER;
					return GameMenu.WORLD_GEN_SELECT;
				}
			case NETHER:
				if (shouldShowWorldSelection(Environment.NORMAL))
				{
					worldGeneratorType = Environment.NORMAL;
					return GameMenu.WORLD_GEN_SELECT;
				}
			case NORMAL:
				
			}
		
		return GameMenu.GAME_MODE_SELECT;
	}
	
	private Inventory createSetupMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer Minecraft: Game setup");
		
		addItemToMenu(this, new MenuItem(menu, 0, null) {
			@Override
			public void runWhenClicked(Player player) {
				Option[] options = game.getGameMode().options;
				GameMenu menu = options == null || options.length == 0 ? GameMenu.GAME_MODE_SELECT : GameMenu.GAME_MODE_CONFIG;
				show(player, menu);
			}
			
			@Override
			public void recalculateStack() {
				Option[] options = game.getGameMode().options;
				String label = options == null || options.length == 0 ? "Change game mode" : "Configure game mode";
				
				ItemStack stack = new ItemStack(game.getGameMode().getPlugin().getMenuIcon());
				setNameAndLore(stack, label, highlightStyle + "Current mode: " + game.getGameMode().getName(), "The game mode is the main set of rules,", "and controls every aspect of a game.");
				setStack(stack);
			}
		});

		addItemToMenu(this, new MenuItem(menu, 1, null) {
			@Override
			public void runWhenClicked(Player player) {
				worldGeneratorType = Environment.NORMAL;
				repopulateMenu(GameMenu.WORLD_GEN_SELECT);
				repopulateMenu(GameMenu.WORLD_GEN_CONFIG);
				show(player, GameMenu.WORLD_GEN_CONFIG);
			}
			
			@Override
			public void recalculateStack()
			{
				WorldGenerator generator = game.getWorldGenerator(Environment.NORMAL);
				if (!shouldShowWorldSelection(Environment.NORMAL))
				{
					Option[] options = generator == null ? null : generator.options;
					if (options == null || options.length == 0)
					{
						setStack(null);
						return;
					}
				}
				
				String generatorName = generator == null ? "<none>" : generator.getName(); 
				ItemStack stack = new ItemStack(generator.getPlugin().getMenuIcon());
				setNameAndLore(stack, "Configure World Generator", highlightStyle + "Current generator: " + generatorName, "Change how the overworld is", "generated in your game");
				setStack(stack);
			}
		});

		addItemToMenu(this, new MenuItem(menu, 2, null) {
			@Override
			public void runWhenClicked(Player player) {
				worldGeneratorType = Environment.NETHER;
				repopulateMenu(GameMenu.WORLD_GEN_SELECT);
				repopulateMenu(GameMenu.WORLD_GEN_CONFIG);
				show(player, GameMenu.WORLD_GEN_CONFIG);
			}
			
			@Override
			public void recalculateStack()
			{
				WorldGenerator generator = game.getWorldGenerator(Environment.NETHER);
				if (!shouldShowWorldSelection(Environment.NETHER))
				{
					Option[] options = generator == null ? null : generator.options;
					if (options == null || options.length == 0)
					{
						setStack(null);
						return;
					}
				}
				String generatorName = generator == null ? "<none>" : generator.getName();
				ItemStack stack = new ItemStack(Material.NETHERRACK);
				setNameAndLore(stack, "Configure Nether Generator", highlightStyle + "Current generator: " + generatorName, "Change how the nether is", "generated in your game");
				setStack(stack);
			}
		});

		addItemToMenu(this, new MenuItem(menu, 3, null) {
			@Override
			public void runWhenClicked(Player player) {
				worldGeneratorType = Environment.THE_END;
				repopulateMenu(GameMenu.WORLD_GEN_SELECT);
				repopulateMenu(GameMenu.WORLD_GEN_CONFIG);
				show(player, GameMenu.WORLD_GEN_CONFIG);
			}
			
			@Override
			public void recalculateStack()
			{
				WorldGenerator generator = game.getWorldGenerator(Environment.THE_END);
				if (!shouldShowWorldSelection(Environment.THE_END))
				{
					Option[] options = generator == null ? null : generator.options;
					if (options == null || options.length == 0)
					{
						setStack(null);
						return;
					}
				}
				String generatorName = generator == null ? "<none>" : generator.getName();
				ItemStack stack = new ItemStack(Material.ENDER_STONE);
				setNameAndLore(stack, "Configure End Generator", highlightStyle + "Current generator: " + generatorName, "Change how the End is", "generated in your game");
				setStack(stack);
			}
		});
		
		/*
		final ItemStack rootMutators = new ItemStack(Material.EXP_BOTTLE);
		setNameAndLore(rootMutators, "Select Mutators", "Mutators change specific aspects", "of a game, but aren't specific", "to any particular game mode");
		addItemToMenu(this, new MenuItem(menu, 4, rootMutators) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_MUTATORS);
			}
		});
		*/

		if (Settings.worldBorderSizes.length > 1)
		{
			ItemStack worldBorders = new ItemStack(Material.GLASS);
			setNameAndLore(worldBorders, "World Borders", "Limit the size of", "game worlds");
			addItemToMenu(this, new MenuItem(menu, 5, worldBorders) {
				@Override
				public void runWhenClicked(Player player) {
					show(player, GameMenu.WORLD_BORDERS);
				}
			});
		}
		
		ItemStack difficulty = new ItemStack(Material.SKULL_ITEM, 1, (short)4); // creeper head
		setNameAndLore(difficulty, "Gameplay Difficulty", "Controls the minecraft difficulty level.", "Hard is recommended in most cases.");
		addItemToMenu(this, new MenuItem(menu, 6, difficulty) {
			@Override
			public void runWhenClicked(Player player) {
				repopulateMenu(GameMenu.DIFFICULTY);
				show(player, GameMenu.DIFFICULTY);
			}
		});
		
		ItemStack rootPlayerNumbers = new ItemStack(Material.SKULL_ITEM, 1, (short)3); // steve head
		setNameAndLore(rootPlayerNumbers, "Player limits", "Specify the maximum number of", "players allowed into the game");
		addItemToMenu(this, new MenuItem(menu, 7, rootPlayerNumbers) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.PLAYERS);
			}
		});
		
		final ItemStack rootOpen = new ItemStack(Material.IRON_DOOR);
		setNameAndLore(rootOpen, "Open game lobby", "Open this game, so", "that players can join");
		addItemToMenu(this, new MenuItem(menu, 8, rootOpen) {
			@Override
			public void runWhenClicked(Player player) {
				if ( game.getGameState() == GameState.SETUP )
					game.setGameState(GameState.LOBBY);
				
				player.closeInventory();
				show(player);
			}
			
			@Override
			public void recalculateStack() {
				setStack(game.getGameState() == GameState.SETUP ? rootOpen : backItem);
			}
		});
		
		return menu;
	}

	private Inventory createGameModeSelectionMenu()
	{
		Inventory menu = Bukkit.createInventory(null, nearestNine(GameMode.gameModes.size() + 1), "Choose a Game Mode");
		
		addItemToMenu(this, new MenuItem(menu, 0, null) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, getPrevSetupMenu(GameMenu.GAME_MODE_SELECT));
			}
			
			@Override
			public void recalculateStack()
			{
				setStack(moduleSetupComplete ? backItem : null);
			}
		});
		
		for ( int i=0; i<GameMode.gameModes.size(); i++ )
		{
			final GameModePlugin mode = GameMode.get(i);
			
			addItemToMenu(this, new MenuItem(menu, i + 1, null) {
				@Override
				public void runWhenClicked(Player player) {
					if ( game.setGameMode(mode) )
						settingsChanged(" set the game mode to " + mode.getDisplayName());
					
					show(player, getNextSetupMenu(GameMenu.GAME_MODE_SELECT));
				}
				
				@Override
				public void recalculateStack() {
					boolean current = game.getGameMode().getPlugin() == mode;
					
					ItemStack stack = new ItemStack(mode.getMenuIcon());
					if ( current )
					{
						String[] desc = mode.getDescriptionText();
						ArrayList<String> lore = new ArrayList<String>(desc.length + 1);
						lore.add(highlightStyle + "Current game mode");
						for ( int j=0; j<desc.length; j++)
							lore.add(desc[j]);
						setNameAndLore(stack, mode.getDisplayName(), lore);
						stack = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(stack);
					}
					else
						setNameAndLore(stack, mode.getDisplayName(), mode.getDescriptionText());
					setStack(stack);
				}
			});
		}
		
		return menu;
	}
	
	private Inventory createGameModeConfigMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Game Mode configuration");

		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP);
			}
		});
		
		addItemToMenu(this, new MenuItem(menu, 1, null) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.GAME_MODE_SELECT);
			}
			
			@Override
			public void recalculateStack()
			{
				ItemStack stack = new ItemStack(game.getGameMode().getPlugin().getMenuIcon());
				setNameAndLore(stack, "Change game mode", highlightStyle + "Current mode: " + game.getGameMode().getName(), "Change to a different game mode");
				setStack(stack);
			}
		});
		
		for (int i=2; i<9; i++)
		{
			final int optionNum = i - 2;
			
			addItemToMenu(this, new MenuItem(menu, i, null) {
				@Override
				public void runWhenClicked(Player player) {
					Option[] options = game.getGameMode().options;
					if (optionNum >= options.length)
						return;
					
					Option option = options[optionNum];
					
					ItemStack[] choiceItems = option.optionClicked();
					if ( choiceItems != null )
						showChoiceOptionMenu(player, option, choiceItems, GameMenu.GAME_MODE_CONFIG);
					else
					{
						repopulateMenu(GameMenu.GAME_MODE_CONFIG);
						settingsChanged(" set the '" + option.getName() + "' setting to " + option.getValueString());
					}
				}
				
				@Override
				public void recalculateStack() {
					Option[] options = game.getGameMode().options;
					if (optionNum >= options.length)
					{
						setStack(null);
						return;
					}
					
					final Option option = options[optionNum];
					
					if ( option.isHidden() )
					{
						setStack(null);
						return;
					}

					ItemStack stack = option.getDisplayStack();
					setNameAndLore(stack, option.getName(), option.getDescription());
					setStack(stack);
				}
			});
		}
		return menu;
	}
	
	private boolean shouldShowWorldSelection(Environment worldType)
	{
		if (!game.getGameMode().allowWorldGeneratorSelection())
			return false;
		
		if (!game.getGameMode().usesWorldType(worldType))
			return false;
		
		return WorldGenerator.getGenerators(worldType).size() > 1;
	}
	
	boolean moduleSetupComplete = false;
	Environment worldGeneratorType = Environment.NORMAL;
	private Inventory createWorldGenSelectionMenu()
	{
		int maxGenerators = Math.max(Math.max(WorldGenerator.getGenerators(Environment.NORMAL).size(), WorldGenerator.getGenerators(Environment.NETHER).size()), WorldGenerator.getGenerators(Environment.THE_END).size());
		Inventory menu = Bukkit.createInventory(null, nearestNine(maxGenerators + 1), "Choose a World Generator");
		
		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, getPrevSetupMenu(GameMenu.WORLD_GEN_SELECT));
			}
		});
		
		for (int i=0; i<maxGenerators; i++)
		{
			final int generatorNumber = i;
			addItemToMenu(this, new MenuItem(menu, i + 1, null) {
				@Override
				public void runWhenClicked(Player player) {
					List<WorldGeneratorPlugin> worldGenerators = WorldGenerator.getGenerators(worldGeneratorType);
					if (generatorNumber >= worldGenerators.size())
						return;
					
					WorldGeneratorPlugin worldGenerator = worldGenerators.get(generatorNumber);
					game.setWorldGenerator(worldGeneratorType, worldGenerator);
					settingsChanged(" set the world generator to " + worldGenerator.getDisplayName());
					updateMenus();
					show(player, getNextSetupMenu(GameMenu.WORLD_GEN_SELECT));
				}
				
				@Override
				public void recalculateStack() {
					List<WorldGeneratorPlugin> worldGenerators = WorldGenerator.getGenerators(worldGeneratorType);
					if (generatorNumber >= worldGenerators.size())
					{
						setStack(null);
						return;
					}
					
					WorldGeneratorPlugin worldGenerator = worldGenerators.get(generatorNumber);
					boolean current = game.getWorldGenerator(worldGeneratorType).getPlugin() == worldGenerator;

					ItemStack stack = new ItemStack(worldGenerator.getMenuIcon());
					if ( current )
					{
						String[] desc = worldGenerator.getDescriptionText();
						ArrayList<String> lore = new ArrayList<String>(desc.length + 1);
						lore.add(highlightStyle + "Current world generator");
						for ( int j=0; j<desc.length; j++)
							lore.add(desc[j]);
						setNameAndLore(stack, worldGenerator.getDisplayName(), lore);
						stack = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(stack);
					}
					else
						setNameAndLore(stack, worldGenerator.getDisplayName(), worldGenerator.getDescriptionText());
					setStack(stack);
				}
			});
		}
		return menu;
	}
	
	private Inventory createWorldGenConfigMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "World Generator configuration");

		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP);
			}
		});
		
		addItemToMenu(this, new MenuItem(menu, 1, null) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.WORLD_GEN_SELECT);
			}
			
			@Override
			public void recalculateStack()
			{
				if (!shouldShowWorldSelection(worldGeneratorType))
				{
					setStack(null);
					return;
				}
				
				ItemStack stack = new ItemStack(game.getWorldGenerator(worldGeneratorType).getPlugin().getMenuIcon());
				setNameAndLore(stack, "Change generator", highlightStyle + "Current generator: " + game.getWorldGenerator(worldGeneratorType).getName(), "Change to a different generator");
				setStack(stack);
			}
		});
		
		
		for (int i=2; i<9; i++)
		{
			final int optionNum = i - 2;
			
			addItemToMenu(this, new MenuItem(menu, i, null) {
				@Override
				public void runWhenClicked(Player player) {
					Option[] options = game.getWorldGenerator(worldGeneratorType).options;
					if (optionNum >= options.length)
						return;
					
					Option option = options[optionNum];
					
					ItemStack[] choiceItems = option.optionClicked();
					if ( choiceItems != null )
						showChoiceOptionMenu(player, option, choiceItems, GameMenu.WORLD_GEN_CONFIG);
					else
					{
						repopulateMenu(GameMenu.WORLD_GEN_CONFIG);
						settingsChanged(" set the '" + option.getName() + "' setting to " + option.getValueString());
					}
				}
				
				@Override
				public void recalculateStack() {
					WorldGenerator generator = game.getWorldGenerator(worldGeneratorType); 
					Option[] options = generator == null ? null : generator.options;
					if (options == null || optionNum >= options.length)
					{
						setStack(null);
						return;
					}
					
					final Option option = options[optionNum];
					
					if ( option.isHidden() )
					{
						setStack(null);
						return;
					}
					
					ItemStack stack = option.getDisplayStack();
					setNameAndLore(stack, option.getName(), option.getDescription());
					setStack(stack);
				}
			});
		}
		return menu;
	}

	private Inventory createPlayersMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Player settings");
		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player); // go back to lobby or setup, depending on state
			}
		});
		
		addItemToMenu(this, new MenuItem(menu, 3, null) {
			@Override
			public void runWhenClicked(Player player)
			{
				int limit = Math.max(1, game.getPlayerLimit()-1);
				game.setPlayerLimit(limit);
				settingsChanged(" decreased the player limit to " + limit);
				repopulateMenu(GameMenu.PLAYERS);
			}
			
			@Override
			public void recalculateStack() {
				if (game.usesPlayerLimit() && game.getPlayerLimit() > 1)
				{
					ItemStack stack = new ItemStack(Material.BUCKET);
					setNameAndLore(stack, "Decrease player limit", ChatColor.YELLOW + "Current limit: " + game.getPlayerLimit(), "Reduce limit to " + (game.getPlayerLimit() - 1));
					setStack(stack);
				}
				else
					setStack(null);
			}
		});
		
		addItemToMenu(this, new MenuItem(menu, 4, null) {
			@Override
			public void runWhenClicked(Player player)
			{
				int limit = game.getPlayerLimit()+1;
				game.setPlayerLimit(limit);
				settingsChanged(" increased the player limit to " + limit);
				repopulateMenu(GameMenu.PLAYERS);
			}
			
			@Override
			public void recalculateStack() {
				if (game.usesPlayerLimit())
				{
					ItemStack stack = new ItemStack(Material.MILK_BUCKET);
					setNameAndLore(stack, "Increase player limit", ChatColor.YELLOW + "Current limit: " + game.getPlayerLimit(), "Increase limit to " + (game.getPlayerLimit() + 1));
					setStack(stack);
				}
				else
					setStack(null);
			}
		});
		
		addItemToMenu(this, new MenuItem(menu, 2, null) {
			@Override
			public void runWhenClicked(Player player)
			{
				if ( game.usesPlayerLimit() )
				{
					game.setUsesPlayerLimit(false);
					settingsChanged(" disabled the player limit");
				}
				else
				{
					int limit = game.getOnlinePlayers().size();
					game.setUsesPlayerLimit(true);
					game.setPlayerLimit(limit);
					settingsChanged(" enabled a player limit of " + limit);
				}
				repopulateMenu(GameMenu.PLAYERS);
			};
			
			@Override
			public void recalculateStack() {
				ItemStack stack = new ItemStack(Material.HOPPER);
				setNameAndLore(stack, "Use player limit", game.usesPlayerLimit() ? ChatColor.YELLOW + "Current limit: " + game.getPlayerLimit() : ChatColor.RED + "No limit set");
				
				if ( game.usesPlayerLimit() )
					stack = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(stack);

				setStack(stack);
			}
		});
		
		addItemToMenu(this, new MenuItem(menu, 7, null) {
			@Override
			public void runWhenClicked(Player player)
			{
				boolean locked = !game.isLocked();
				game.setLocked(locked);
				settingsChanged(locked ? " locked the game" : " unlocked the game");
				repopulateMenu(GameMenu.PLAYERS);
			};
			
			@Override
			public void recalculateStack() {
				if ( game.getGameState() == GameState.EMPTY || game.getGameState() == GameState.SETUP )
				{
					setStack(null);
					return;
				}

				ItemStack stack = new ItemStack(Material.IRON_FENCE);
				if ( game.isLocked() )
				{
					setNameAndLore(stack, "Unlock the game", "This game is locked, so", "no one else can join,", "even if players leave");
					stack = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(stack);
				}
				else
					setNameAndLore(stack, "Lock the game", "Lock this game, so that", "no one else can join,", "even if players leave");
				setStack(stack);
			}
		});
		
		addItemToMenu(this, new MenuItem(menu, 8, null) {
			@Override
			public void runWhenClicked(Player player)
			{
				teamSelectionEnabled = !teamSelectionEnabled;
				settingsChanged(teamSelectionEnabled ? " enabled team selection" : " disabled team selection");
				repopulateMenu(GameMenu.PLAYERS);
			};
			
			@Override
			public void recalculateStack() {
				// if game mode allows team selection, a toggle to allow manual team selection (and show the team selection scoreboard) ...
				// if disabled, players shouldn't see the scoreboard thing, teams will be auto-assigned
				if ( game.getGameMode().allowTeamSelection() )
				{
					ItemStack stack = new ItemStack(Material.DIODE);
					setNameAndLore(stack, "Allow team selection", "When enabled, players will be", "able to choose their own teams.", "When disabled, teams will be", "allocated randomly.");
					
					if ( teamSelectionEnabled )
						stack = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(stack);
					
					setStack(stack);
				}
				else
					setStack(null);
			}
		});
				
		return menu;
	}
	
	private Inventory createDifficultyMenu()
	{
		final Inventory menu = Bukkit.createInventory(null, 9, "Gameplay difficulty");
		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP);
			}
		});
		
		for (int i=0; i<difficultyNames.length; i++)
		{
			final int quantity = i;
			@SuppressWarnings("deprecation")
			final Difficulty difficulty = Difficulty.getByValue(quantity);
			
			addItemToMenu(this, new MenuItem(menu, i + 1, null) {
				@Override
				public void runWhenClicked(Player player) {
					game.setDifficulty(difficulty);						
					settingsChanged(" changed the gameplay difficulty to '" + difficultyNames[quantity] + "'");
					repopulateMenu(GameMenu.DIFFICULTY);
				}
				
				@Override
				public void recalculateStack() {
					boolean selected = game.getDifficulty() == difficulty;
					
					Material mat;
					switch (quantity)
					{
					default:
						mat = Material.STICK; break;
					case 1:
						mat = Material.WOOD_PICKAXE; break;
					case 2:
						mat = Material.IRON_PICKAXE; break;
					case 3:
						mat = Material.DIAMOND_PICKAXE; break;
					}

					ItemStack stack = new ItemStack(mat);
					
					setNameAndLore(stack, difficultyDescriptions[quantity], highlightStyle + "Current Setting");
					
					ItemMeta meta = stack.getItemMeta();
					meta.setDisplayName(difficultyNames[quantity]);
					stack.setItemMeta(meta);
					
					if (selected)
						stack = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(stack);
					
					setStack(stack);
				}
			});
		}
		
		return menu;
	}
	
	private Inventory createWorldBordersMenu()
	{
		final Inventory menu = Bukkit.createInventory(null, 9, "World Borders");
		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP);
			}
		});
		
		int pos = 1;
		for (double d : Settings.worldBorderSizes)
		{
			if (pos >= 9)
				break;
			
			final double size = d;

			MenuItem item = new MenuItem(menu, pos++, null) {
				@Override
				public void runWhenClicked(Player player) {
					game.setWorldBorderSize(size);
					if (size == 0)
						settingsChanged(" disabled world borders");
					else
						settingsChanged(" changed the world border size to '" + size + "'");
					repopulateMenu(GameMenu.WORLD_BORDERS);
				}
				
				@Override
				public void recalculateStack() {
					ItemStack stack;
					
					if (size == 0)
					{
						stack = new ItemStack(Material.BARRIER);
						setNameAndLore(stack, "No borders");
					}
					else
					{
						stack = new ItemStack(Material.BRICK);
						setNameAndLore(stack, size + " metres");
					}
					
					if (game.getWorldBorderSize() == size)
						stack = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(stack);
					
					setStack(stack);
				}
			};
			item.recalculateStack();
			addItemToMenu(this, item);
		}

		return menu;
	}
	ItemStack startItem;
	
	private Inventory createLobbyMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer Minecraft: " + game.getName());
		
		addItemToMenu(this, new MenuItem(menu, 3, null) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.TEAM_SELECTION);
			}
			
			@Override
			public void recalculateStack() {
				ItemStack stack = new ItemStack(Material.IRON_HELMET, 1);
				setNameAndLore(stack, "Choose Team", "Pick which team you will", "play on in this game");
				setStack(allowTeamSelection() ? stack : null);
			}
		});
		
		ItemStack becomeSpectatorItem = new ItemStack(Material.EYE_OF_ENDER, 1);
		setNameAndLore(becomeSpectatorItem, "Spectate", "Click to become a spectator", "in this game"); // clicking marks your playerInfo as spectator, and moves you to the "spectator lobby"
		addItemToMenu(this, new MenuItem(menu, 2, becomeSpectatorItem) {
			@Override
			public void runWhenClicked(Player player) {
				PlayerInfo info = game.getPlayerInfo(player);
				info.setSpectator(true);
				game.getGameMode().setTeam(player, null);
				show(player);
			}
		});

		addCommonLobbyItems(menu);
		
		addItemToMenu(this, new MenuItem(menu, 7, quitItem) {
			@Override
			public void runWhenClicked(Player player) {
				game.removePlayerFromGame(player);
				player.closeInventory();
			}
		});
		
		addItemToMenu(this, new MenuItem(menu, 8, helpItem) {
			@Override
			public void runWhenClicked(Player player) {
				player.setItemOnCursor(helpBook);
			}
		});
		
		return menu;
	}
	
	private void addCommonLobbyItems(Inventory menu)
	{
		addItemToMenu(this, new MenuItem(menu, 0, null) {
			@Override
			public void runWhenClicked(Player player) {
				if ( player.getName() == game.hostPlayer && game.getGameState() == GameState.LOBBY )
					show(player, GameMenu.SETUP);
				else
					player.sendMessage(listAllSettings(game));
			}
			@Override
			public void recalculateStack() {
				ItemStack stack = new ItemStack(Material.PAPER, 1);
				setNameAndLore(stack, "View Game Settings", describeSettings(game));
				setStack(stack);
			}
		});
		
		addItemToMenu(this, new MenuItem(menu, 1, null) {
			@Override
			public void runWhenClicked(Player player) {
				player.sendMessage(listPlayers(game));
			}
			
			@Override
			public void recalculateOnClick(MenuItem... items) {
				ItemStack stack = new ItemStack(Material.SKULL_ITEM, 1, (short)3); // steve head
				setNameAndLore(stack, "Players", countPlayers(game)); // clicking this should list the players in the game
				setStack(stack);
			}
		});

		addItemToMenu(this, new MenuItem(menu, 5, null) {
			@Override
			public void runWhenClicked(Player player) {
				if ( game.getGameState() != GameState.LOBBY )
					return;
				
				if ( player.getName() == game.hostPlayer )
				{
					int min = game.getGameMode().getMinPlayers();
					if ( game.getOnlinePlayers().size() < min )	
						player.sendMessage("Cannot start game: you need at least " + min + (min == 1 ? " player" : " players"));
					else
						game.startGame();
				}
				else
					game.broadcastMessage(ChatColor.YELLOW + player.getName() + " wants to start the game");
			}
			
			@Override
			public void recalculateStack() {
				ItemStack stack;
				if ( game.getGameState() == GameState.QUEUE_FOR_GENERATION )
				{
					stack = new ItemStack(Material.REDSTONE_TORCH_OFF, 1);
					setNameAndLore(stack, "Game Queued", "Waiting for another game", "to finish generating");
				}
				else if ( game.getGameState() == GameState.GENERATING )
				{
					stack = new ItemStack(Material.REDSTONE_TORCH_ON, 1);
					setNameAndLore(stack, "Generating Worlds", "This game will start shortly");
				}
				else
				{
					stack = new ItemStack(Material.TORCH, 1);
					setNameAndLore(stack, "Start Game", "Only the host can start the game");
				}
				
				setStack(stack);
			}
		});
	}
	
	private Inventory createSpectatorLobbyMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer: Spectating " + game.getName());
		
		addItemToMenu(this, new MenuItem(menu, 7, quitItem) {
			@Override
			public void runWhenClicked(Player player) {
				game.removePlayerFromGame(player);
				player.closeInventory();
			}
		});
		
		addItemToMenu(this, new MenuItem(menu, 8, helpItem) {
			@Override
			public void runWhenClicked(Player player) {
				player.setItemOnCursor(helpBook);
			}
		});

		addCommonLobbyItems(menu);

		addItemToMenu(this, new MenuItem(menu, 2, null) {
			@Override
			public void runWhenClicked(Player player) {
				if ( game.canJoin() )
				{
					PlayerInfo info = game.getPlayerInfo(player);
					info.setSpectator(false);
				}
				show(player);
			}
			
			@Override
			public void recalculateStack() {
				if (game.canJoin())
				{
					ItemStack stack = new ItemStack(Material.EYE_OF_ENDER, 1);
					setNameAndLore(stack, "Stop Spectating", "Click to become a player", "in this game");
					stack = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(stack);
					setStack(stack);
				}
				else
					setStack(null);
			}
		});
		
		return menu;
	}
	
	private Inventory createTeamMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer Minecraft: Pick a team");
		
		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player);
			}
		});
		
		ItemStack autoAssign = new ItemStack(Material.MOB_SPAWNER);
		setNameAndLore(autoAssign, "Auto assign", "Automatically assigns you to", "the team with the fewest", "players, or randomly in the", "event of a tie.");
		addItemToMenu(this, new MenuItem(menu, 1, autoAssign) {
			@Override
			public void runWhenClicked(Player player) {
				game.getGameMode().setTeam(player, null);
				game.broadcastMessage(player.getName() + " set their team to auto-assign");
			}
		});
		
		for (int slot = 2; slot < 9; slot++)
		{
			final int teamIndex = slot - 2;
			addItemToMenu(this, new MenuItem(menu, slot, null) {
				@Override
				protected void runWhenClicked(Player player)
				{
					TeamInfo[] teams = game.getGameMode().getTeams();
					if (teams == null || teams.length <= teamIndex)
					{
						setStack(null);
						return;
					}
					
					TeamInfo team = teams[teamIndex];
					
					game.getGameMode().setTeam(player, team);
					game.broadcastMessage(player.getName() + " joined the " + team.getChatColor() + team.getName());
				}
				
				@Override
				public void recalculateStack()
				{
					TeamInfo[] teams = game.getGameMode().getTeams();
					if (teams == null || teams.length <= teamIndex)
					{
						setStack(null);
						return;
					}
					
					TeamInfo team = teams[teamIndex];
					ItemStack stack = new ItemStack(Material.BANNER);
					BannerMeta banner = (BannerMeta)stack.getItemMeta();
					banner.setBaseColor(team.getDyeColor());
					stack.setItemMeta(banner);
					setNameAndLore(stack, team.getChatColor() + team.getName(), "Join the " + team.getName());
					setStack(stack);
				};
			});
		}
		
		return menu;
	}
	
	private Inventory createActiveMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer Minecraft: " + game.getName());

		ItemStack stack = new ItemStack(Material.BOOK_AND_QUILL);
		setNameAndLore(stack, "Vote: End game", "Click to start a vote", "to end the current game");
		addItemToMenu(this, new MenuItem(menu, 0, stack) {
			@Override
			public void runWhenClicked(Player player) {
				if (game.currentVote != null)
				{
					player.sendMessage("Cannot start a vote: another vote is in progress"); 
					return;
				}
				
				Vote vote = new Vote(game, "End the current game?", player) {
					protected void runOnYes() { game.finishGame(); }
					protected void runOnNo() {}
					protected void runOnDraw() {}
				};

				game.startVote(vote);
				player.closeInventory();
			}
		});

		if (Settings.allowLateJoiners)
		{
			addItemToMenu(this, new MenuItem(menu, 1, null) {
				@Override
				protected void runWhenClicked(Player player) {
					if (game.currentVote != null)
					{
						player.sendMessage("Cannot start a vote: another vote is in progress"); 
						return;
					}
					
					Vote vote;
					if (game.isPrivate())
						vote = new Vote(game, "Open the game to other players?", player) {
							protected void runOnYes() { game.setPrivate(false); game.setPlayerLimit(0); updateMenus(); }
							protected void runOnNo() {}
							protected void runOnDraw() {}
						};
					else
						vote = new Vote(game, "Prevent other players from joining the game?", player) {
							protected void runOnYes() { game.setPrivate(true); updateMenus(); }
							protected void runOnNo() {}
							protected void runOnDraw() {}
						};
					game.startVote(vote);
					player.closeInventory();
				};
				
				@Override
				public void recalculateStack() {
					stack = new ItemStack(Material.IRON_DOOR);
					if (game.isPrivate())
						setNameAndLore(stack, "Vote: Open game", "Click to start a vote", "to open this game to other players");
					else
						setNameAndLore(stack, "Vote: Lock game", "Click to start a vote", "to prevent other players from joining");					
					setStack(stack);
				};
			});
		}
		
		addItemToMenu(this, new MenuItem(menu, 8, quitItem) {
			@Override
			public void runWhenClicked(Player player) {
				game.removePlayerFromGame(player);
				player.closeInventory();
			}
		});

		return menu;
	}

	static final String[] animalDescriptions = new String[] { "No animals will spawn", "Reduced animal spawn rate", "Normal animal spawn rate", "High animal spawn rate", "Excessive animal spawn rate" };
	static final String[] monsterDescriptions = new String[] { "No monsters will spawn", "Reduced monster spawn rate", "Normal monster spawn rate", "High monster spawn rate", "Excessive monster spawn rate" };
	static final String[] difficultyNames = new String[] { "Peaceful", "Easy", "Medium", "Hard" };
	static final String[] difficultyDescriptions = new String[] { "No monsters, cannot starve, regenerate health", "Easy monsters, starving hurts a little", "Medium monsters, starving hurts more", "Hard monsters, starving can kill you" };
	private static String highlightStyle = "" + ChatColor.YELLOW + ChatColor.ITALIC;
	
	public static void show(Player player)
	{
		Game game = KillerMinecraft.instance.getGameForPlayer(player);
		if ( game == null )
		{
			menusByPlayer.put(player.getName(), rootMenu);
			player.openInventory(rootMenu);
		}
		else
			switch ( game.getGameState() )
			{
			case ACTIVE:
			case WORLD_DELETION:
				game.menuManager.show(player, GameMenu.ACTIVE); break;
			case EMPTY:
			case SETUP:
				game.menuManager.show(player, game.menuManager.moduleSetupComplete ? GameMenu.SETUP : GameMenu.GAME_MODE_SELECT); break;
			default:
				PlayerInfo info = game.getPlayerInfo(player.getName());
				game.menuManager.show(player, info.isSpectator() ? GameMenu.SPECTATOR_LOBBY : GameMenu.LOBBY); break;
			}
	}

	public void show(Player player, GameMenu menu)
	{
		Inventory inv = inventories.get(menu);
		if ( inv != null )
		{
			player.closeInventory();
			menusByPlayer.put(player.getName(), inv);
			player.openInventory(inv);
		}
	}
	
	private static void setNameAndLore(ItemStack item, String name, List<String> lore)
	{
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + name);
		meta.setLore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(meta);
	}
	
	private static void setNameAndLore(ItemStack item, String name, String... lore)
	{
		setNameAndLore(item, name, Arrays.asList(lore));
	}
	
	private int nearestNine(int num)
	{
		return 9*(int)Math.ceil(num/9.0);
	}
	
	private static Map<String, Inventory> menusByPlayer = new HashMap<String, Inventory>(); 
	
	public static boolean checkEvent(Player player, InventoryClickEvent event)
	{
		Inventory menu = menusByPlayer.get(player.getName());
		if ( menu == null )
			return false;
			
		if ( event.getRawSlot() >= event.getInventory().getSize() )
			return false; // click in player's own inventory

		ItemStack clicked = event.getCurrentItem();
		if ( clicked == null || clicked.getType() == Material.AIR || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY )
		{
			event.setCancelled(true);
			return false;
		}
		
		if ( menu == rootMenu )
		{
			event.setCancelled(true);
			rootMenuClicked(player, event.getRawSlot());
			return true;
		}
		
		Game game = KillerMinecraft.instance.getGameForPlayer(player);
		
		if ( game == null )
			return false;
		
		event.setCancelled(true);
		game.menuManager.menuClicked(player, menu, event.getRawSlot());
		return true;
	}

	public static boolean checkEvent(Player player, InventoryDragEvent event)
	{
		Inventory menu = menusByPlayer.get(player.getName());
		if ( menu == null )
			return false;
		
		for ( Integer i : event.getRawSlots() )
			if ( i.intValue() < event.getInventory().getSize() )
			{
				event.setCancelled(true);
				return true; // dragging into or out of menu inventory
			}
		
		return false;
	}
	
	public static void inventoryClosed(Player player)
	{
		menusByPlayer.remove(player.getName());
	}
	
	private void menuClicked(Player player, Inventory menu, int slot)
	{
		MenuItem[] items = menuItems.get(menu);

		if (slot < 0 || slot >= items.length)
			return;
		
		MenuItem item = items[slot];
		if (item != null && item.getStack() != null)
			item.clicked(player);
	}
	
	private static void rootMenuClicked(Player player, int slot)
	{
		if (slot < 0 || slot >= rootMenuItems.length)
			return;
		
		MenuItem item = rootMenuItems[slot];
		if (item != null && item.getStack() != null)
			item.clicked(player);
	}
	
	private boolean teamSelectionEnabled = true;

	public final boolean allowTeamSelection()
	{
		if ( !game.getGameState().usesLobby || !game.getGameMode().allowTeamSelection() || game.getGameMode().getTeams().length == 0 )
			return false;
		
		return teamSelectionEnabled;
	}
		
	private void showChoiceOptionMenu(Player player, Option option, ItemStack[] choiceItems, GameMenu goBackTo)
	{
		choiceOptionGoBackTo = goBackTo;
		currentOption = option;
		Inventory menu = Bukkit.createInventory(null, nearestNine(choiceItems.length + 2), option.getName());
		
		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player)
			{
				currentOption = null;
				inventories.put(GameMenu.SPECIFIC_OPTION_CHOICE, null);
				
				repopulateMenu(choiceOptionGoBackTo);
				show(player, choiceOptionGoBackTo);
			};
		});
		
		menu.setItem(0, backItem);	
		
		populateChoiceOptionMenu(choiceItems, option.getSelectedIndex(), menu);
		
		inventories.put(GameMenu.SPECIFIC_OPTION_CHOICE, menu);
		show(player, GameMenu.SPECIFIC_OPTION_CHOICE);
	}
	
	GameMenu choiceOptionGoBackTo;
	Option currentOption;

	private void populateChoiceOptionMenu(ItemStack[] choiceItems, int selectedIndex, Inventory menu)
	{
		for ( int i=0; i<choiceItems.length; i++ )
		{
			ItemStack item = choiceItems[i];
			if ( selectedIndex == i )
				item = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(item);
			
			addItemToMenu(this, new MenuItem(menu, i+2, item) {
				@Override
				public void runWhenClicked(Player player)
				{
					currentOption.setSelectedIndex(slot - 2);
					populateChoiceOptionMenu(currentOption.optionClicked(), currentOption.getSelectedIndex(), inventories.get(GameMenu.SPECIFIC_OPTION_CHOICE));
					settingsChanged(" set the '" + currentOption.getName() + "' setting to " + currentOption.getValueString());
				};
			});
		}
	}
		
	private void settingsChanged(String message)
	{
		// only broadcast when in the lobby
		if ( game.getGameState() == GameState.LOBBY )
			game.broadcastMessage(game.hostPlayer + message);
	}
	
	public void repopulateMenu(GameMenu menu)
	{
		Inventory inventory = inventories.get(menu);
		if (inventory == null)
			return;
		
		MenuItem[] items = menuItems.get(inventory);
		for (MenuItem item : items)
			if (item != null)
				item.recalculateStack();				
	}
	
	public void updateMenus()
	{
		if (game.getGameState() == GameState.INITIALIZING)
			return;

		repopulateMenu(GameMenu.LOBBY);
		repopulateMenu(GameMenu.SPECTATOR_LOBBY);
		repopulateMenu(GameMenu.SETUP);
		repopulateMenu(GameMenu.GAME_MODE_SELECT);
		repopulateMenu(GameMenu.GAME_MODE_CONFIG);
		repopulateMenu(GameMenu.WORLD_GEN_SELECT);
		repopulateMenu(GameMenu.WORLD_GEN_CONFIG);
		repopulateMenu(GameMenu.PLAYERS);
		repopulateMenu(GameMenu.TEAM_SELECTION);
		repopulateMenu(GameMenu.ACTIVE);
		
		updateGameIcon();
	}
	
	public void updateGameIcon()
	{		
		gameRootMenuItem.recalculateStack();
	}
}

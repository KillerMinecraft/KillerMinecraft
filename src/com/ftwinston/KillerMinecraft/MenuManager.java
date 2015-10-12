package com.ftwinston.KillerMinecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import com.ftwinston.KillerMinecraft.Game.GameState;
import com.ftwinston.KillerMinecraft.Game.PlayerInfo;
import com.ftwinston.KillerMinecraft.Configuration.MenuItem;
import com.ftwinston.KillerMinecraft.Configuration.TeamInfo;


class MenuManager
{
	enum GameMenu
	{
		SETUP_ROOT,
		SETUP_GAME_MODE,
		SETUP_GAME_MODE_CONFIG,
		SETUP_WORLD_GEN,
		SETUP_WORLD_GEN_CONFIG,
		SETUP_PLAYERS,
		SETUP_MONSTERS,
		SETUP_ANIMALS,

		SPECIFIC_OPTION_CHOICE,

		LOBBY,
		SPECTATOR_LOBBY,
		TEAM_SELECTION,
		ACTIVE,
	}
	
	private static final int
	ROOT_MENU_HELP_POS = 8,
	SETUP_MENU_MODE_POS = 0,
	SETUP_MENU_WORLD_POS = 1,
	//SETUP_MENU_MUTATOR_POS = 2,
	SETUP_MENU_PLAYERS_POS = 3,
	SETUP_MENU_MONSTERS_POS = 4,
	SETUP_MENU_ANIMALS_POS = 5,
	SETUP_MENU_OPEN_POS = 7,
	SETUP_MENU_QUIT_POS = 8,
	LOBBY_MENU_SETTINGS_POS = 0,
	LOBBY_MENU_PLAYER_LIST_POS = 1,
	LOBBY_MENU_SPECTATE_POS = 2,
	LOBBY_MENU_TEAM_POS = 3,
	LOBBY_MENU_START_POS = 5,
	LOBBY_MENU_QUIT_POS = 7,
	LOBBY_MENU_HELP_POS = 8,
	GAME_MODE_MENU_BACK_POS = 0,
	GAME_MODE_MENU_FIRST_POS = 2,
	MODULE_CONFIG_MENU_FIRST_POS = 2,
	WORLD_GEN_MENU_BACK_POS = 0,
	WORLD_GEN_MENU_FIRST_POS = 2,
	SETUP_PLAYERS_BACK_POS = 0,
	SETUP_PLAYERS_USE_PLAYER_LIMIT = 2,
	SETUP_PLAYERS_LIMIT_DOWN = 3,
	SETUP_PLAYERS_LIMIT_UP = 4,
	SETUP_PLAYERS_LOCK_GAME = 7,
	SETUP_PLAYERS_TEAM_SELECTION = 8,
	QUANTITY_MENU_BACK_POS = 0,
	CHOICE_OPTION_BACK_POS = 0,
	TEAM_MENU_AUTO_ASSIGN_POS = 0,
	TEAM_MENU_FIRST_TEAM_POS = 1,
	ACTIVE_MENU_QUIT_POS = 8;
	
	static final int
	QUANTITY_MENU_NONE_POS = 2;
	
	EnumMap<GameMenu, Inventory> inventories = new EnumMap<GameMenu, Inventory>(GameMenu.class);
	HashMap<Inventory, MenuItem[]> menuItems = new HashMap<Inventory, MenuItem[]>();
	
	public static Inventory rootMenu;
	private static MenuItem[] rootMenuItems = new MenuItem[9]; 
	
	Game game;
	private static ItemStack helpItem, helpBook, becomeSpectatorItem, stopSpectatingItem, backItem, quitItem, teamSelectionItem;

	public MenuManager(Game game)
	{
		this.game = game;
		inventories.put(GameMenu.SETUP_ROOT, createSetupMenu());
		inventories.put(GameMenu.LOBBY, createLobbyMenu());
		inventories.put(GameMenu.SPECTATOR_LOBBY, createSpectatorLobbyMenu());
		inventories.put(GameMenu.TEAM_SELECTION, createTeamMenu());
		inventories.put(GameMenu.ACTIVE, createActiveMenu());
		
		inventories.put(GameMenu.SETUP_GAME_MODE, createGameModeMenu());
		inventories.put(GameMenu.SETUP_WORLD_GEN, createWorldGenMenu());
		inventories.put(GameMenu.SETUP_PLAYERS, createPlayersMenu());
		inventories.put(GameMenu.SETUP_MONSTERS, createMonstersMenu());
		inventories.put(GameMenu.SETUP_ANIMALS, createAnimalsMenu());
	}
	
	static void addItemToMenu(MenuManager instance, Inventory menu, int slot, MenuItem item) 
	{	
		menu.setItem(slot, item == null ? null : item.getStack());
		
		if (instance == null)
		{
			rootMenuItems[slot] = item;
			return;
		}
		
		// and add this to some data structure, so we can call its runOnClick later
		MenuItem[] items;
		if (instance.menuItems.containsKey(menu))
			items = instance.menuItems.get(menu);
		else
		{
			items = new MenuItem[9];
			instance.menuItems.put(menu,  items);
		}
		
		items[slot] = item;
	}
	
	static void createRootMenu()
	{
		rootMenu = Bukkit.createInventory(null, 9, "Killer Minecraft: All games");
		
		helpItem = new ItemStack(Material.BOOK, 1);
		setNameAndLore(helpItem, "Help", "Click for a book that explains", "how Killer Minecraft works");
		
		helpBook = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta metaData = (BookMeta)helpBook.getItemMeta();
		metaData.setTitle("Help");
		metaData.setAuthor("Killer Minecraft");
		metaData.addPage("This book should explain how Killer Minecraft works.", "But it doesn't really. Not yet.", "Sorry.");
		helpBook.setItemMeta(metaData);
		
		addItemToMenu(null, rootMenu, ROOT_MENU_HELP_POS, new MenuItem(helpItem){
			@Override
			public void runWhenClicked(Player player) {
				player.setItemOnCursor(helpBook);
			}
		});
		
		
		// these items not used in the root menu, but are used (statically) in other menus
		quitItem = new ItemStack(Material.TNT, 1);
		setNameAndLore(quitItem, "Quit", "Click to exit this game");
		
		teamSelectionItem = new ItemStack(Material.IRON_HELMET, 1);
		setNameAndLore(teamSelectionItem, "Choose Team", "Pick which team you will", "play on in this game");

		backItem = new ItemStack(Material.WOOD_DOOR);
		setNameAndLore(backItem, highlightStyle + "Go back", "Return to the previous menu");
		
		becomeSpectatorItem = new ItemStack(Material.EYE_OF_ENDER, 1);
		setNameAndLore(becomeSpectatorItem, "Spectate", "Click to become a spectator", "in this game"); // clicking marks your playerInfo as spectator, and moves you to the "spectator lobby"
		
		stopSpectatingItem = new ItemStack(Material.EYE_OF_ENDER, 1);
		setNameAndLore(stopSpectatingItem, "Stop Spectating", "Click to become a player", "in this game");
		stopSpectatingItem = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(stopSpectatingItem);
	}
	
	private static void updateGameInRootMenu(final Game game)
	{
		int gameMenuIndex = game.getNumber() - 1;
		MenuItem item = rootMenuItems[gameMenuIndex];
		
		if (item == null)
		{
			ItemStack stack = new ItemStack(Material.BUCKET, 1);
			item = new MenuItem(stack) {
				@Override
				public void runWhenClicked(Player player) {
					if ( game.canJoin() )
					{
						game.addPlayerToGame(player);
						show(player);
					}
					else
						player.sendMessage(ChatColor.RED + "You can't join this game");
				}
			};
		}
		
		Material mat;
		
		if ( game.getGameState() == GameState.EMPTY )
			mat = Material.BUCKET;
		else if ( game.getGameState() == GameState.SETUP )
			mat = Material.MILK_BUCKET;
		else
			mat = game.canJoin() ? Material.WATER_BUCKET : Material.LAVA_BUCKET;
		
		ItemStack stack = new ItemStack(mat, 1);
		setNameAndLore(stack, game.getName(), describeGame(game));
		item.setStack(stack);
		
		addItemToMenu(null, rootMenu, gameMenuIndex, item);
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
		if ( game.getGameMode().allowWorldGeneratorSelection() )
			return new String[] { "Game mode: " + game.getGameMode().getName(), "World: " + game.getWorldGenerator().getName() };
		return new String[] { "Game mode: " + game.getGameMode().getName() };
	}
	
	private static String listAllSettings(Game game)
	{
		String output = "Game mode: " + game.getGameMode().getName();
		
		for ( Option o : game.getGameMode().options )
			if ( !o.isHidden() )
				output += "\n  " + o.getName() + ": " + o.getValueString();
		
		if ( game.getGameMode().allowWorldGeneratorSelection() )
		{
			output += "\nWorld generator: " + game.getWorldGenerator().getName();
			for ( Option o : game.getWorldGenerator().options )
				if ( !o.isHidden() )
					output += "\n  " + o.getName() + ": " + o.getValueString();	
		}
		
		// player settings
		if ( game.usesPlayerLimit() )
			output += "\nPlayer limit: " + game.getPlayerLimit();
		else
			output += "\nNo player limit";
		
		if ( game.isLocked() )
			output += " (game is locked)";
		
		// monster & animal numbers
		output += "\nMonster numbers: " + getQuantityText(game.monsterNumbers) + "\nAnimal numbers: " + getQuantityText(game.animalNumbers);
				
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

	private ItemStack rootGameMode, rootWorldGen, /*rootMutators,*/ rootPlayerNumbers, rootMonsters, rootAnimals, rootOpen;
	
	private Inventory createSetupMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer Minecraft: Game setup");
		
		rootGameMode = new ItemStack(Material.CAKE);
		setNameAndLore(rootGameMode, "Change Game Mode", "");
		addItemToMenu(this, menu, SETUP_MENU_MODE_POS, new MenuItem(rootGameMode){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_GAME_MODE);
			}
		});
		
		rootWorldGen = new ItemStack(Material.GRASS);
		setNameAndLore(rootWorldGen, "Change World Generator", "");
		addItemToMenu(this, menu, SETUP_MENU_WORLD_POS, new MenuItem(rootWorldGen){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_WORLD_GEN);
			}
		});
		/*
		rootMutators = new ItemStack(Material.EXP_BOTTLE);
		setNameAndLore(rootMutators, "Select Mutators", "Mutators change specific aspects", "of a game, but aren't specific", "to any particular game mode");
		addItemToMenu(this, menu, SETUP_MENU_MUTATOR_POS, new MenuItem(rootMutators){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_MUTATORS);
			}
		});
		*/
		rootPlayerNumbers = new ItemStack(Material.SKULL_ITEM, 1, (short)3); // steve head
		setNameAndLore(rootPlayerNumbers, "Player limits", "Specify the maximum number of", "players allowed into the game");
		addItemToMenu(this, menu, SETUP_MENU_PLAYERS_POS, new MenuItem(rootPlayerNumbers){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_PLAYERS);
			}
		});
		
		rootMonsters = new ItemStack(Material.SKULL_ITEM, 1, (short)4); // creeper head
		setNameAndLore(rootMonsters, "Monster Numbers", "Control the number of", "monsters that spawn");
		addItemToMenu(this, menu, SETUP_MENU_MONSTERS_POS, new MenuItem(rootMonsters){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_MONSTERS);
			}
		});
		
		rootAnimals = new ItemStack(Material.EGG);
		setNameAndLore(rootAnimals, "Animal Numbers", "Control the number of", "animals that spawn");
		addItemToMenu(this, menu, SETUP_MENU_ANIMALS_POS, new MenuItem(rootAnimals){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_ANIMALS);
			}
		});
		
		rootOpen = new ItemStack(Material.IRON_DOOR);
		setNameAndLore(rootOpen, "Open game lobby", "Open this game, so", "that players can join");
		addItemToMenu(this, menu, SETUP_MENU_OPEN_POS, new MenuItem(rootOpen){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.LOBBY);
			}
		});

		return menu;
	}

	private Inventory createGameModeMenu()
	{
		Inventory menu = Bukkit.createInventory(null, nearestNine(GameMode.gameModes.size() + 2), "Game mode selection");
		
		addItemToMenu(this, menu, GAME_MODE_MENU_BACK_POS, new MenuItem(backItem){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_ROOT);
			}
		});
		
		for ( int i=0; i<GameMode.gameModes.size(); i++ )
		{
			final GameModePlugin mode = GameMode.get(i);
			ItemStack stack = setupGameModeItem(mode, false);
			
			addItemToMenu(this, menu, i + GAME_MODE_MENU_FIRST_POS, new MenuItem(stack){
				@Override
				public void runWhenClicked(Player player) {
					if ( game.setGameMode(mode) )
						settingsChanged(" set the game mode to " + mode.getName());
					
					show(player, GameMenu.SETUP_GAME_MODE_CONFIG);
				}
			});
		}
		
		return menu;
	}
	
	private ItemStack setupGameModeItem(GameModePlugin mode, boolean current) 
	{
		ItemStack item = new ItemStack(mode.getMenuIcon());
		if ( current )
		{
			String[] desc = mode.getDescriptionText();
			ArrayList<String> lore = new ArrayList<String>(desc.length + 1);
			lore.add(highlightStyle + "Current game mode");
			for ( int j=0; j<desc.length; j++)
				lore.add(desc[j]);
			setNameAndLore(item, mode.getName(), lore);
			item = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(item);
		}
		else
			setNameAndLore(item, mode.getName(), mode.getDescriptionText());
		
		return item;
	}
	
	private Inventory createGameModeConfigMenu(GameMode mode)
	{
		return createModuleOptionsMenu(mode, GameMenu.SETUP_GAME_MODE, GameMenu.SETUP_GAME_MODE_CONFIG);
	}
	
	private Inventory createWorldGenMenu()
	{
		Inventory menu = Bukkit.createInventory(null, nearestNine(WorldGenerator.worldGenerators.size() + 2), "World generator selection");
		
		addItemToMenu(this, menu, WORLD_GEN_MENU_BACK_POS, new MenuItem(backItem){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_ROOT);
			}
		});
		
		for ( int i=0; i<WorldGenerator.worldGenerators.size(); i++ )
		{
			final WorldGeneratorPlugin world = WorldGenerator.get(i);
			ItemStack stack = setupWorldGenItem(world, false);
			
			addItemToMenu(this, menu, i + WORLD_GEN_MENU_FIRST_POS, new MenuItem(stack){
				@Override
				public void runWhenClicked(Player player) { 
					game.setWorldGenerator(world);
					settingsChanged(" set the world generator to " + world.getName());
					
					show(player, GameMenu.SETUP_WORLD_GEN_CONFIG);
				}
			});
		}
		return menu;
	}

	private ItemStack setupWorldGenItem(WorldGeneratorPlugin world, boolean current)
	{
		ItemStack item = new ItemStack(world.getMenuIcon());
		if ( current )
		{
			String[] desc = world.getDescriptionText();
			ArrayList<String> lore = new ArrayList<String>(desc.length + 1);
			lore.add(highlightStyle + "Current world generator");
			for ( int j=0; j<desc.length; j++)
				lore.add(desc[j]);
			setNameAndLore(item, world.getName(), lore);
			item = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(item);
		}
		else
			setNameAndLore(item, world.getName(), world.getDescriptionText());
		return item;
	}

	private Inventory createWorldGenConfigMenu(WorldGenerator world)
	{
		return createModuleOptionsMenu(world, GameMenu.SETUP_WORLD_GEN, GameMenu.SETUP_WORLD_GEN_CONFIG);
	}
	
	private Inventory createModuleOptionsMenu(KillerModule module, GameMenu backMenu, GameMenu currentMenu)
	{
		Inventory menu = Bukkit.createInventory(null, nearestNine(module.options.length + MODULE_CONFIG_MENU_FIRST_POS), module.getName() + " options");
		generateOptionMenuItems(menu, module.options, backMenu, currentMenu);
		return menu;
	}
	
	private void generateOptionMenuItems(final Inventory menu, final Option[] options, final GameMenu backMenu, final GameMenu currentMenu)
	{
		addItemToMenu(this, menu, 0, new MenuItem(backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, backMenu);
			}
		});
		
		for ( int i=0; i<options.length; i++ )
		{
			final Option option = options[i];
			if ( option.isHidden() )
			{
				menu.setItem(i + MODULE_CONFIG_MENU_FIRST_POS, null);
				continue;
			}
			ItemStack item = option.getDisplayStack();
			setNameAndLore(item, option.getName(), option.getDescription());
			
			addItemToMenu(this, menu, i + MODULE_CONFIG_MENU_FIRST_POS, new MenuItem(item){
				@Override
				public void runWhenClicked(Player player) {
					ItemStack[] choiceItems = option.optionClicked();
					if ( choiceItems != null )
						showChoiceOptionMenu(player, option, choiceItems, currentMenu);
					else
					{
						generateOptionMenuItems(menu, options, backMenu, currentMenu);
						settingsChanged(" set the '" + option.getName() + "' setting to " + option.getValueString());
						updateMenus();
					}
				}
			});
		}
	}

	private Inventory createPlayersMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Player settings");
		addItemToMenu(this, menu, SETUP_PLAYERS_BACK_POS, new MenuItem(backItem){
			@Override
			public void runWhenClicked(Player player) {
				show(player); // go back to lobby or setup, depending on state
			}
		});
		
		return menu;
	}
	
	private Inventory createMonstersMenu()
	{
		final Inventory menu = Bukkit.createInventory(null, 9, "Monster numbers");
		addItemToMenu(this, menu, QUANTITY_MENU_BACK_POS, new MenuItem(backItem){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_ROOT);
			}
		});
		
		final MenuManager manager = this;
		for ( int i=0; i<=Game.maxQuantityNum; i++ )
		{
			ItemStack item = createQuantityItem(i, game.monsterNumbers == i, monsterDescriptions[i]);
			final int quantity = i;
			
			addItemToMenu(this, menu, i + QUANTITY_MENU_NONE_POS, new MenuItem(item){
				@Override
				public void runWhenClicked(Player player) {
					int prevQuantity = game.monsterNumbers;
					game.monsterNumbers = quantity;
					
					ItemStack item = createQuantityItem(quantity, true, monsterDescriptions[quantity]);
					setStack(item);
					addItemToMenu(manager, menu, quantity + QUANTITY_MENU_NONE_POS, this);
					
					MenuItem[] items = menuItems.get(menu);
					MenuItem prevItem = items[prevQuantity + QUANTITY_MENU_NONE_POS];
					prevItem.setStack(createQuantityItem(prevQuantity, false, monsterDescriptions[prevQuantity]));
					addItemToMenu(manager, menu, prevQuantity + QUANTITY_MENU_NONE_POS, prevItem);
						
					settingsChanged(" changed the monster numbers to '" + getQuantityText(quantity) + "'");
				}
			});
		}
		
		return menu;
	}
	
	private Inventory createAnimalsMenu()
	{
		final Inventory menu = Bukkit.createInventory(null, 9, "Monster numbers");
		addItemToMenu(this, menu, QUANTITY_MENU_BACK_POS, new MenuItem(backItem){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_ROOT);
			}
		});
		
		final MenuManager manager = this;
		for ( int i=0; i<=Game.maxQuantityNum; i++ )
		{
			ItemStack item = createQuantityItem(i, game.animalNumbers == i, animalDescriptions[i]);
			final int quantity = i;
			
			addItemToMenu(this, menu, i + QUANTITY_MENU_NONE_POS, new MenuItem(item){
				@Override
				public void runWhenClicked(Player player) {
					int prevQuantity = game.animalNumbers;
					game.animalNumbers = quantity;
					
					ItemStack item = createQuantityItem(quantity, true, animalDescriptions[quantity]);
					setStack(item);
					addItemToMenu(manager, menu, quantity + QUANTITY_MENU_NONE_POS, this);
					
					MenuItem[] items = menuItems.get(menu);
					MenuItem prevItem = items[prevQuantity + QUANTITY_MENU_NONE_POS];
					prevItem.setStack(createQuantityItem(prevQuantity, false, animalDescriptions[prevQuantity]));
					addItemToMenu(manager, menu, prevQuantity + QUANTITY_MENU_NONE_POS, prevItem);
						
					settingsChanged(" changed the animal numbers to '" + getQuantityText(quantity) + "'");
				}
			});
		}
		
		return menu;
	}
	
	ItemStack gameSettingsItem, playerListItem, startItem;
	
	private Inventory createLobbyMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer Minecraft: " + game.getName());

		gameSettingsItem = new ItemStack(Material.PAPER, 1);
		
		playerListItem = new ItemStack(Material.SKULL_ITEM, 1, (short)3); // steve head

		becomeSpectatorItem = new ItemStack(Material.EYE_OF_ENDER, 1);
		setNameAndLore(becomeSpectatorItem, "Spectate", "Click to become a spectator", "in this game"); // clicking marks your playerInfo as spectator, and moves you to the "spectator lobby"
		addItemToMenu(this, menu, LOBBY_MENU_SPECTATE_POS, new MenuItem(becomeSpectatorItem){
			@Override
			public void runWhenClicked(Player player) {
				PlayerInfo info = game.getPlayerInfo(player);
				info.setSpectator(true);
				game.getGameMode().setTeam(player, null);
				updateMenus();
				show(player);
			}
		});
		
		addItemToMenu(this, menu, LOBBY_MENU_QUIT_POS, new MenuItem(quitItem){
			@Override
			public void runWhenClicked(Player player) {
				game.removePlayerFromGame(player);
				player.closeInventory();
			}
		});
		
		addItemToMenu(this, menu, LOBBY_MENU_HELP_POS, new MenuItem(helpItem){
			@Override
			public void runWhenClicked(Player player) {
				player.setItemOnCursor(helpBook);
			}
		});
		
		return menu;
	}
	
	private Inventory createSpectatorLobbyMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer: Spectating " + game.getName());
		
		addItemToMenu(this, menu, LOBBY_MENU_QUIT_POS, new MenuItem(quitItem){
			@Override
			public void runWhenClicked(Player player) {
				game.removePlayerFromGame(player);
				player.closeInventory();
			}
		});
		
		addItemToMenu(this, menu, LOBBY_MENU_HELP_POS, new MenuItem(helpItem){
			@Override
			public void runWhenClicked(Player player) {
				player.setItemOnCursor(helpBook);
			}
		});

		return menu;
	}
	
	private Inventory createTeamMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer Minecraft: Pick a team");
		
		ItemStack autoAssign = new ItemStack(Material.MOB_SPAWNER);
		setNameAndLore(autoAssign, "Auto assign", "Automatically assigns you to", "the team with the fewest", "players, or randomly in the", "event of a tie.");
		addItemToMenu(this, menu, TEAM_MENU_AUTO_ASSIGN_POS, new MenuItem(autoAssign){
			@Override
			public void runWhenClicked(Player player) {
				game.getGameMode().setTeam(player, null);
				game.broadcastMessage(player.getName() + " set their team to auto-assign");
			}
		});
		
		return menu;
	}
	
	protected void populateTeamMenu()
	{
		Inventory menu = inventories.get(GameMenu.TEAM_SELECTION);
		int slot = TEAM_MENU_FIRST_TEAM_POS;
		
		if ( allowTeamSelection() )
			for ( final TeamInfo team : game.getGameMode().getTeams() )
			{
				ItemStack item = new ItemStack(Material.LEATHER_HELMET, 1);
				LeatherArmorMeta meta = (LeatherArmorMeta)item.getItemMeta();
				meta.setColor(team.getArmorColor());
				item.setItemMeta(meta);
				
				setNameAndLore(item, team.getChatColor() + team.getName(), "Join the " + team.getName());
				
				addItemToMenu(this, menu, slot, new MenuItem(item){
					@Override
					public void runWhenClicked(Player player) {
						game.getGameMode().setTeam(player, team);
						game.broadcastMessage(player.getName() + " joined the " + team.getChatColor() + team.getName());
						show(player);
					}
				});
				
				slot++;
			}
		else // no team selection, so close the menu
			for ( HumanEntity viewer : menu.getViewers() )
				viewer.closeInventory();
		
		while ( slot < 9 )
		{
			addItemToMenu(this, menu, slot, null);
			slot++;
		}
	}
	
	private Inventory createActiveMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer Minecraft: " + game.getName());

		// TODO: call a vote?
		
		addItemToMenu(this, menu, ACTIVE_MENU_QUIT_POS, new MenuItem(quitItem){
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
	private static String highlightStyle = "" + ChatColor.YELLOW + ChatColor.ITALIC;
	
	public void updateMenus()
	{
		if ( game.getGameState().keepLobbyUpToDate )
			updateSetupAndLobbyMenus();
		
		// TODO: update any items that should change
		//Inventory menu = inventories.get(GameMenu.ACTIVE);


		// also update this game in the root menu
		updateGameInRootMenu(game);
	}
	
	private void updateSetupAndLobbyMenus()
	{
		// update setup menu items that should change
		Inventory menu = inventories.get(GameMenu.SETUP_ROOT);
		
		addItemToMenu(this, menu, SETUP_MENU_OPEN_POS, game.getGameState() == GameState.SETUP ? new MenuItem(rootOpen){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.LOBBY);
			}
		} : null);
		
		addItemToMenu(this, menu, SETUP_MENU_QUIT_POS, new MenuItem(game.getGameState() == GameState.SETUP ? quitItem : backItem){
			@Override
			public void runWhenClicked(Player player) {
				if ( game.getGameState() == GameState.SETUP )
				{
					game.removePlayerFromGame(player);
					player.closeInventory();
				}
				else
				{
					player.closeInventory();
					show(player, GameMenu.LOBBY);
				}
			}
		});

		addItemToMenu(this, menu, LOBBY_MENU_TEAM_POS, allowTeamSelection() ? new MenuItem(quitItem){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.TEAM_SELECTION);
			}
		} : null);
		
		setLore(rootGameMode, highlightStyle + "Current mode: " + game.getGameMode().getName(), "The game mode is the main set of rules,", "and controls every aspect of a game.");
		addItemToMenu(this, menu, SETUP_MENU_MODE_POS, new MenuItem(rootGameMode){
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_GAME_MODE);
			}
		});
		
		if ( game.getGameMode().allowWorldGeneratorSelection() )
		{
			setLore(rootWorldGen, highlightStyle + "Current generator: "+ game.getWorldGenerator().getName(), "The world generator controls", "the terrain in the game's world(s)");
			addItemToMenu(this, menu, SETUP_MENU_WORLD_POS, new MenuItem(rootWorldGen){
				@Override
				public void runWhenClicked(Player player) {
					show(player, GameMenu.SETUP_WORLD_GEN);
				}
			});
		}
		else
			addItemToMenu(this, menu, SETUP_MENU_WORLD_POS, null);
		
		
		// update lobby menu items that should change
		menu = inventories.get(GameMenu.LOBBY);
		setNameAndLore(gameSettingsItem, "View Game Settings", describeSettings(game));	// clicking this should write out more detailed settings to chat
		MenuItem lobbySettingsItem = new MenuItem(gameSettingsItem){
			@Override
			public void runWhenClicked(Player player) {
				if ( player.getName() == game.hostPlayer && game.getGameState() == GameState.LOBBY )
					show(player, GameMenu.SETUP_ROOT);
				else
					player.sendMessage(listAllSettings(game));
			}
		};
		addItemToMenu(this, menu, LOBBY_MENU_SETTINGS_POS, lobbySettingsItem);
		
		setNameAndLore(playerListItem, "Players", countPlayers(game)); // clicking this should list the players in the game
		MenuItem listPlayersItem = new MenuItem(playerListItem){
			@Override
			public void runWhenClicked(Player player) {
				player.sendMessage(listPlayers(game));
			}
		};
		addItemToMenu(this, menu, LOBBY_MENU_PLAYER_LIST_POS, listPlayersItem);
		
		if ( game.getGameState() == GameState.QUEUE_FOR_GENERATION )
		{
			startItem = new ItemStack(Material.REDSTONE_TORCH_OFF, 1);
			setNameAndLore(startItem, "Game Queued", "Waiting for another game", "to finish generating");
		}
		else if ( game.getGameState() == GameState.GENERATING )
		{
			startItem = new ItemStack(Material.REDSTONE_TORCH_ON, 1);
			setNameAndLore(startItem, "Generating Worlds", "This game will start shortly");
		}
		else
		{
			startItem = new ItemStack(Material.TORCH, 1);
			setNameAndLore(startItem, "Start Game", "Only the host can start the game");
		}
		
		MenuItem gameStartItem = new MenuItem(startItem){
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
		};
		addItemToMenu(this, menu, LOBBY_MENU_START_POS, gameStartItem);
		
		
		menu = inventories.get(GameMenu.SPECTATOR_LOBBY);
		
		addItemToMenu(this, menu, LOBBY_MENU_SETTINGS_POS, lobbySettingsItem);
		
		addItemToMenu(this, menu, LOBBY_MENU_PLAYER_LIST_POS, listPlayersItem);
		
		addItemToMenu(this, menu, LOBBY_MENU_START_POS, gameStartItem);

		addItemToMenu(this, menu, LOBBY_MENU_SPECTATE_POS, game.canJoin() ? new MenuItem(stopSpectatingItem){
			@Override
			public void runWhenClicked(Player player) {
				if ( game.canJoin() )
				{
					PlayerInfo info = game.getPlayerInfo(player);
					info.setSpectator(false);
					updateMenus();
				}
				show(player);
			}
		} : null);
	}
	
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
				game.menuManager.show(player, GameMenu.SETUP_ROOT); break;
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
	
	public static String getQuantityText(int num)
	{
		switch ( num )
		{
		case 0:
			return "None";
		case 1:
			return "Few";
		case 2:
			return "Some";
		case 3:
			return "Many";
		case 4:
			return "Too Many";
		default:
			return "???";
		}
	}
	
	private ItemStack createQuantityItem(int quantity, boolean selected, String lore)
	{
		ItemStack item = new ItemStack(Material.STICK);
		
		if ( selected )
			setNameAndLore(item, getQuantityText(quantity), highlightStyle + "Current Setting", lore);
		else
			setNameAndLore(item, getQuantityText(quantity), lore);
		
		switch ( quantity )
		{
		case 1:
			item.setType(Material.WOOD_PICKAXE); break;
		case 2:
			item.setType(Material.STONE_PICKAXE); break;
		case 3:
			item.setType(Material.IRON_PICKAXE); break;
		case 4:
			item.setType(Material.DIAMOND_PICKAXE); break;
		}

		if ( selected )
			item = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(item);
		return item;
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
	
	private static void setLore(ItemStack item, List<String> lore)
	{
		ItemMeta meta = item.getItemMeta();
		meta.setLore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(meta);
	}
	
	private static void setLore(ItemStack item, String... lore)
	{
		setLore(item, Arrays.asList(lore));
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
		if (item != null)
			item.runWhenClicked(player);
	}
	
	private static void rootMenuClicked(Player player, int slot)
	{
		if (slot < 0 || slot >= rootMenuItems.length)
			return;
		
		MenuItem item = rootMenuItems[slot];
		if (item != null)
			item.runWhenClicked(player);
	}
	
	private boolean teamSelectionEnabled = true;

	public final boolean allowTeamSelection()
	{
		if ( !game.getGameState().keepLobbyUpToDate || !game.getGameMode().allowTeamSelection() || game.getGameMode().getTeams().length == 0 )
			return false;
		
		return teamSelectionEnabled;
	}
	
	void gameModeChanged(GameModePlugin prev, GameModePlugin current)
	{
		teamSelectionEnabled = true;
		repopulatePlayersMenu(); // repopulate players menu so that "team selection" item is present (or not) depending on new game mode	
		populateTeamMenu();
		
		// update which item gets the "current game mode" lore text on the game mode menu
		Inventory menu = inventories.get(GameMenu.SETUP_GAME_MODE);
		MenuItem[] items = menuItems.get(menu);
		
		if ( prev != null )
		{
			int itemSlot = GameMode.indexOf(prev);
			ItemStack stack = setupGameModeItem(prev, false);
			items[itemSlot].setStack(stack);
			addItemToMenu(this, menu, itemSlot, items[itemSlot]);
		}
		
		int itemSlot = GameMode.indexOf(current);
		ItemStack stack = setupGameModeItem(current, true);
		items[itemSlot].setStack(stack); // NPE!
		addItemToMenu(this, menu, itemSlot, items[itemSlot]);
		
		// actually populate the config menu!
		inventories.put(GameMenu.SETUP_GAME_MODE_CONFIG, createGameModeConfigMenu(game.getGameMode()));
	}
	
	void worldGenChanged(WorldGeneratorPlugin prev, WorldGeneratorPlugin current)
	{
		// update which item gets the "current world generator" lore text on the game mode menu
		Inventory menu = inventories.get(GameMenu.SETUP_WORLD_GEN);
		MenuItem[] items = menuItems.get(menu);

		if ( prev != null )
		{			
			int itemSlot = WorldGenerator.indexOf(prev);
			ItemStack stack = setupWorldGenItem(prev, false);
			items[itemSlot].setStack(stack);
			addItemToMenu(this, menu, itemSlot, items[itemSlot]);
		}
		
		int itemSlot = WorldGenerator.indexOf(current);
		ItemStack stack = setupWorldGenItem(current, true);
		items[itemSlot].setStack(stack);
		addItemToMenu(this, menu, itemSlot, items[itemSlot]);
		
		// actually populate the config menu!
		// ...
		inventories.put(GameMenu.SETUP_WORLD_GEN_CONFIG, createWorldGenConfigMenu(game.getWorldGenerator()));
	}
	
	private void showChoiceOptionMenu(Player player, Option option, ItemStack[] choiceItems, GameMenu goBackTo)
	{
		choiceOptionGoBackTo = goBackTo;
		currentOption = option;
		Inventory menu = Bukkit.createInventory(null, nearestNine(choiceItems.length + 2), option.getName());
		
		addItemToMenu(this, menu, CHOICE_OPTION_BACK_POS, new MenuItem(backItem) {
			@Override
			public void runWhenClicked(Player player)
			{
				currentOption = null;
				inventories.put(GameMenu.SPECIFIC_OPTION_CHOICE, null);
				
				if ( choiceOptionGoBackTo == GameMenu.SETUP_GAME_MODE_CONFIG )
					generateOptionMenuItems(inventories.get(choiceOptionGoBackTo), game.getGameMode().options, GameMenu.SETUP_GAME_MODE, choiceOptionGoBackTo);
				else if ( choiceOptionGoBackTo == GameMenu.SETUP_WORLD_GEN_CONFIG ) 
					generateOptionMenuItems(inventories.get(choiceOptionGoBackTo), game.getWorldGenerator().options, GameMenu.SETUP_WORLD_GEN, choiceOptionGoBackTo);
				
				show(player, choiceOptionGoBackTo);
			};
		});
		
		menu.setItem(CHOICE_OPTION_BACK_POS, backItem);	
		
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
			
			final int slot = i;
			addItemToMenu(this, menu, i+2, new MenuItem(item) {
				@Override
				public void runWhenClicked(Player player)
				{
					currentOption.setSelectedIndex(slot);
					populateChoiceOptionMenu(currentOption.optionClicked(), currentOption.getSelectedIndex(), inventories.get(GameMenu.SPECIFIC_OPTION_CHOICE));
					settingsChanged(" set the '" + currentOption.getName() + "' setting to " + currentOption.getValueString());
				
					updateMenus();
				};
			});
		}
	}
	
	void repopulatePlayersMenu()
	{
		Inventory menu = inventories.get(GameMenu.SETUP_PLAYERS);	

		ItemStack usePlayerLimit = new ItemStack(Material.HOPPER);
		setNameAndLore(usePlayerLimit, "Use player limit", game.usesPlayerLimit() ? ChatColor.YELLOW + "Current limit: " + game.getPlayerLimit() : ChatColor.RED + "No limit set");
		
		if ( game.usesPlayerLimit() )
		{
			usePlayerLimit = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(usePlayerLimit);
			if ( game.getPlayerLimit() > 1)
			{
				ItemStack playerLimitDown = new ItemStack(Material.BUCKET);
				setNameAndLore(playerLimitDown, "Decrease player limit", ChatColor.YELLOW + "Current limit: " + game.getPlayerLimit(), "Reduce limit to " + (game.getPlayerLimit() - 1));
				addItemToMenu(this, menu, SETUP_PLAYERS_LIMIT_DOWN, new MenuItem(playerLimitDown) {
					@Override
					public void runWhenClicked(Player player)
					{
						int limit = Math.max(1, game.getPlayerLimit()-1);
						game.setPlayerLimit(limit);
						settingsChanged(" decreased the player limit to " + limit);
						
						repopulatePlayersMenu();
						updateMenus();
					}
				});
			}
			else
				menu.setItem(SETUP_PLAYERS_LIMIT_DOWN, null);
			
			ItemStack playerLimitUp = new ItemStack(Material.MILK_BUCKET);
			setNameAndLore(playerLimitUp, "Increase player limit", ChatColor.YELLOW + "Current limit: " + game.getPlayerLimit(), "Increase limit to " + (game.getPlayerLimit() + 1));
			
			addItemToMenu(this, menu, SETUP_PLAYERS_LIMIT_UP, new MenuItem(playerLimitUp) {
				@Override
				public void runWhenClicked(Player player)
				{
					int limit = game.getPlayerLimit()+1;
					game.setPlayerLimit(limit);
					settingsChanged(" increased the player limit to " + limit);

					repopulatePlayersMenu();
					updateMenus();
				}
			});
		}
		else
		{
			menu.setItem(SETUP_PLAYERS_LIMIT_DOWN, null);
			menu.setItem(SETUP_PLAYERS_LIMIT_UP, null);
		}

		addItemToMenu(this, menu, SETUP_PLAYERS_USE_PLAYER_LIMIT, new MenuItem(usePlayerLimit) {
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
				

				repopulatePlayersMenu();
				updateMenus();
			};	
		});
		
		ItemStack lockGame;
		if ( game.getGameState() == GameState.EMPTY || game.getGameState() == GameState.SETUP )
			lockGame = null;
		else
		{
			lockGame = new ItemStack(Material.IRON_FENCE);
			if ( game.isLocked() )
			{
				setNameAndLore(lockGame, "Unlock the game", "This game is locked, so", "no one else can join,", "even if players leave");
				lockGame = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(lockGame);
			}
			else
				setNameAndLore(lockGame, "Lock the game", "Lock this game, so that", "no one else can join,", "even if players leave");
		}
		addItemToMenu(this, menu, SETUP_PLAYERS_LOCK_GAME, new MenuItem(lockGame) {
			@Override
			public void runWhenClicked(Player player)
			{
				boolean locked = !game.isLocked();
				game.setLocked(locked);
				settingsChanged(locked ? " locked the game" : " unlocked the game");

				repopulatePlayersMenu();
				updateMenus();
			};	
		});
		
		// if game mode allows team selection, a toggle to allow manual team selection (and show the team selection scoreboard) ...
		// if disabled, players shouldn't see the scoreboard thing, teams will be auto-assigned
		if ( game.getGameMode().allowTeamSelection() )
		{
			ItemStack teamSelection = new ItemStack(Material.DIODE);
			setNameAndLore(teamSelection, "Allow team selection", "When enabled, players will be", "able to choose their own teams.", "When disabled, teams will be", "allocated randomly.");
			
			if ( teamSelectionEnabled )
				teamSelection = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(teamSelection);
			
			menu.setItem(SETUP_PLAYERS_TEAM_SELECTION, teamSelection);
			addItemToMenu(this, menu, SETUP_PLAYERS_TEAM_SELECTION, new MenuItem(lockGame) {
				@Override
				public void runWhenClicked(Player player)
				{
					teamSelectionEnabled = !teamSelectionEnabled;
					settingsChanged(teamSelectionEnabled ? " enabled team selection" : " disabled team selection");
					
					repopulatePlayersMenu();
					updateMenus();
				};	
			});
		}
		else
			addItemToMenu(this, menu, SETUP_PLAYERS_TEAM_SELECTION, null);
	}
	
	private void settingsChanged(String message)
	{
		// only broadcast when in the lobby
		if ( game.getGameState() == GameState.LOBBY )
			game.broadcastMessage(game.hostPlayer + message);
	}
}

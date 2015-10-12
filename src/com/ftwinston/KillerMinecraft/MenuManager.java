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
	GAME_MODE_CONFIG_MENU_FIRST_POS = 2,
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
	
	public static Inventory rootMenu;
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
	
	static void createRootMenu()
	{
		rootMenu = Bukkit.createInventory(null, 9, "Killer Minecraft: All games");
		
		helpItem = new ItemStack(Material.BOOK, 1);
		setNameAndLore(helpItem, "Help", "Click for a book that explains", "how Killer Minecraft works");
		rootMenu.setItem(ROOT_MENU_HELP_POS, helpItem);
		
		helpBook = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta metaData = (BookMeta)helpBook.getItemMeta();
		metaData.setTitle("Help");
		metaData.setAuthor("Killer Minecraft");
		metaData.addPage("This book should explain how Killer Minecraft works.", "But it doesn't really. Not yet.", "Sorry.");
		helpBook.setItemMeta(metaData);
		
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
	
	private static void updateGameInRootMenu(Game game)
	{
		Material mat;
		
		if ( game.getGameState() == GameState.EMPTY )
			mat = Material.BUCKET;
		else if ( game.getGameState() == GameState.SETUP )
			mat = Material.MILK_BUCKET;
		else
			mat = game.canJoin() ? Material.WATER_BUCKET : Material.LAVA_BUCKET;
		
		ItemStack item = new ItemStack(mat, 1);
		setNameAndLore(item, game.getName(), describeGame(game));
		
		rootMenu.setItem(game.getNumber() - 1, item);
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
	private ItemStack[] gameModeItems, worldGenItems, monsterItems, animalItems;
	
	private Inventory createSetupMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer Minecraft: Game setup");
		
		rootGameMode = new ItemStack(Material.CAKE);
		rootWorldGen = new ItemStack(Material.GRASS);
		
		//rootMutators = new ItemStack(Material.EXP_BOTTLE);
		rootPlayerNumbers = new ItemStack(Material.SKULL_ITEM, 1, (short)3); // steve head
		rootMonsters = new ItemStack(Material.SKULL_ITEM, 1, (short)4); // creeper head
		rootAnimals = new ItemStack(Material.EGG);
		
		rootOpen = new ItemStack(Material.IRON_DOOR);
		
		setNameAndLore(rootGameMode, "Change Game Mode", "");
		setNameAndLore(rootWorldGen, "Change World Generator", "");
		
		//setNameAndLore(rootMutators, "Select Mutators", "Mutators change specific aspects", "of a game, but aren't specific", "to any particular game mode");
		setNameAndLore(rootPlayerNumbers, "Player limits", "Specify the maximum number of", "players allowed into the game");
		
		setNameAndLore(rootMonsters, "Monster Numbers", "Control the number of", "monsters that spawn");
		menu.setItem(SETUP_MENU_MONSTERS_POS, rootMonsters);
		
		setNameAndLore(rootAnimals, "Animal Numbers", "Control the number of", "animals that spawn");
		menu.setItem(SETUP_MENU_ANIMALS_POS, rootAnimals);
		
		setNameAndLore(rootOpen, "Open game lobby", "Open this game, so", "that players can join");
				
		menu.setItem(SETUP_MENU_MODE_POS, rootGameMode);
		
		//menu.setItem(SETUP_MENU_MUTATOR_POS, rootMutators);
		menu.setItem(SETUP_MENU_PLAYERS_POS, rootPlayerNumbers);

		return menu;
	}

	private Inventory createGameModeMenu()
	{
		Inventory menu = Bukkit.createInventory(null, nearestNine(GameMode.gameModes.size() + 2), "Game mode selection");
		
		menu.setItem(0, backItem);
		
		gameModeItems = new ItemStack[GameMode.gameModes.size()];
		for ( int i=0; i<GameMode.gameModes.size(); i++ )
		{
			GameModePlugin mode = GameMode.get(i);
			gameModeItems[i] = new ItemStack(mode.getMenuIcon());
			setupGameModeItemLore(menu, i, mode, false);
		}
		
		return menu;
	}
	
	private void setupGameModeItemLore(Inventory menu, int num, GameModePlugin mode, boolean current) 
	{
		ItemStack item = gameModeItems[num];
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

		menu.setItem(num + GAME_MODE_MENU_FIRST_POS, item);
	}
	
	private Inventory createGameModeConfigMenu(GameMode mode)
	{
		return createModuleOptionsMenu(mode);
	}
	
	private Inventory createWorldGenMenu()
	{
		Inventory menu = Bukkit.createInventory(null, nearestNine(WorldGenerator.worldGenerators.size() + 2), "World generator selection");
		menu.setItem(0, backItem);
		
		worldGenItems = new ItemStack[WorldGenerator.worldGenerators.size()];
		for ( int i=0; i<WorldGenerator.worldGenerators.size(); i++ )
		{
			WorldGeneratorPlugin world = WorldGenerator.get(i);
			
			ItemStack item = new ItemStack(world.getMenuIcon());
			worldGenItems[i] = item;
			setupWorldGenItemLore(menu, i, world, false);
		}
		return menu;
	}

	private void setupWorldGenItemLore(Inventory menu, int num, WorldGeneratorPlugin world, boolean current)
	{
		ItemStack item = worldGenItems[num];
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

		menu.setItem(num+2, item);
	}

	private Inventory createWorldGenConfigMenu(WorldGenerator world)
	{
		return createModuleOptionsMenu(world);
	}
	
	private Inventory createModuleOptionsMenu(KillerModule module)
	{
		Inventory menu = Bukkit.createInventory(null, nearestNine(module.options.length + 2), module.getName() + " options");
		generateOptionMenuItems(menu, module.options);
		return menu;
	}
	
	private void generateOptionMenuItems(Inventory menu, Option[] options)
	{
		menu.setItem(0, backItem);
		
		for ( int i=0; i<options.length; i++ )
		{
			Option option = options[i];
			if ( option.isHidden() )
			{
				menu.setItem(i+2, null);
				continue;
			}
			ItemStack item = option.getDisplayStack();
			setNameAndLore(item, option.getName(), option.getDescription());
			menu.setItem(i+2, item);
		}
	}

	private Inventory createPlayersMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Player settings");
		menu.setItem(0, backItem);
		return menu;
	}
	
	private Inventory createMonstersMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Monster numbers");
		menu.setItem(0, backItem);
		
		monsterItems = new ItemStack[Game.maxQuantityNum + 1];
		for ( int i=0; i<=Game.maxQuantityNum; i++ )
		{
			ItemStack item = monsterItems[i] = createQuantityItem(i, game.monsterNumbers == i, monsterDescriptions[i]);
			menu.setItem(i+2, item);
		}
		
		return menu;
	}
	
	private Inventory createAnimalsMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Animal numbers");
		menu.setItem(0, backItem);
		
		animalItems = new ItemStack[Game.maxQuantityNum + 1];
		for ( int i=0; i<=Game.maxQuantityNum; i++ )
		{
			ItemStack item = animalItems[i] = createQuantityItem(i, game.animalNumbers == i, animalDescriptions[i]);
			menu.setItem(i+2, item);
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
		menu.setItem(LOBBY_MENU_SPECTATE_POS, becomeSpectatorItem);
		
		menu.setItem(LOBBY_MENU_QUIT_POS, quitItem);
		menu.setItem(LOBBY_MENU_HELP_POS, helpItem);

		return menu;
	}
	
	private Inventory createSpectatorLobbyMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer: Spectating " + game.getName());
		
		menu.setItem(LOBBY_MENU_QUIT_POS, quitItem);
		
		menu.setItem(LOBBY_MENU_HELP_POS, helpItem);
		
		return menu;
	}
	
	private Inventory createTeamMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer Minecraft: Pick a team");
		
		ItemStack autoAssign = new ItemStack(Material.MOB_SPAWNER);
		setNameAndLore(autoAssign, "Auto assign", "Automatically assigns you to", "the team with the fewest", "players, or randomly in the", "event of a tie.");
		menu.setItem(TEAM_MENU_AUTO_ASSIGN_POS, autoAssign);
	
		return menu;
	}
	
	protected void populateTeamMenu()
	{
		Inventory menu = inventories.get(GameMenu.TEAM_SELECTION);
		int slot = TEAM_MENU_FIRST_TEAM_POS;
		
		if ( allowTeamSelection() )
			for ( TeamInfo team : game.getGameMode().getTeams() )
			{
				ItemStack item = new ItemStack(Material.LEATHER_HELMET, 1);
				LeatherArmorMeta meta = (LeatherArmorMeta)item.getItemMeta();
				meta.setColor(team.getArmorColor());
				item.setItemMeta(meta);
				
				setNameAndLore(item, team.getChatColor() + team.getName(), "Join the " + team.getName());
				menu.setItem(slot, item);
				slot++;
			}
		else // no team selection, so close the menu
			for ( HumanEntity viewer : menu.getViewers() )
				viewer.closeInventory();
		
		while ( slot < 9 )
		{
			menu.setItem(slot, null);
			slot++;
		}
	}
	
	private Inventory createActiveMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer Minecraft: " + game.getName());

		// TODO: call a vote?
		
		menu.setItem(ACTIVE_MENU_QUIT_POS, quitItem);

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
		
		if ( game.getGameState() == GameState.SETUP )
		{
			menu.setItem(SETUP_MENU_OPEN_POS, rootOpen);
			menu.setItem(SETUP_MENU_QUIT_POS, quitItem);
		}
		else
		{
			menu.setItem(SETUP_MENU_OPEN_POS, null);
			menu.setItem(SETUP_MENU_QUIT_POS, backItem);
		}
		
		if ( allowTeamSelection() )
			menu.setItem(LOBBY_MENU_TEAM_POS, teamSelectionItem);
		else
			menu.setItem(LOBBY_MENU_TEAM_POS, null);
		
		setLore(rootGameMode, highlightStyle + "Current mode: " + game.getGameMode().getName(), "The game mode is the main set of rules,", "and controls every aspect of a game.");
		menu.setItem(SETUP_MENU_MODE_POS, rootGameMode);
		
		if ( game.getGameMode().allowWorldGeneratorSelection() )
		{
			setLore(rootWorldGen, highlightStyle + "Current generator: "+ game.getWorldGenerator().getName(), "The world generator controls", "the terrain in the game's world(s)");
			inventories.get(GameMenu.SETUP_ROOT).setItem(SETUP_MENU_WORLD_POS, rootWorldGen);
		}
		else
			inventories.get(GameMenu.SETUP_ROOT).setItem(SETUP_MENU_WORLD_POS, null);
		
		
		// update lobby menu items that should change
		menu = inventories.get(GameMenu.LOBBY);
		setNameAndLore(gameSettingsItem, "View Game Settings", describeSettings(game));	// clicking this should write out more detailed settings to chat
		menu.setItem(LOBBY_MENU_SETTINGS_POS, gameSettingsItem);
		
		setNameAndLore(playerListItem, "Players", countPlayers(game)); // clicking this should list the players in the game
		menu.setItem(LOBBY_MENU_PLAYER_LIST_POS, playerListItem);
		
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
		menu.setItem(LOBBY_MENU_START_POS, startItem);
		
		
		menu = inventories.get(GameMenu.SPECTATOR_LOBBY);
		menu.setItem(LOBBY_MENU_SETTINGS_POS, gameSettingsItem);
		menu.setItem(LOBBY_MENU_PLAYER_LIST_POS, playerListItem);
		menu.setItem(LOBBY_MENU_START_POS, startItem);
		
		if ( game.canJoin() )
			menu.setItem(LOBBY_MENU_SPECTATE_POS, stopSpectatingItem); // clicking marks your playerInfo as NOT spectator, and moves you to the "proper lobby"
		else
			menu.setItem(LOBBY_MENU_SPECTATE_POS, null);
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
		for ( Map.Entry<GameMenu, Inventory> kvp : inventories.entrySet() )
			if ( kvp.getValue() == menu )
			{
				switch ( kvp.getKey() )
				{
				case SETUP_ROOT:
					setupMenuClicked(player, slot); break;					
				case SETUP_GAME_MODE:
					gameModeMenuClicked(player, slot); 
					break;
				case SETUP_GAME_MODE_CONFIG:
					gameModeConfigMenuClicked(player, slot); 
					break;
				case SETUP_WORLD_GEN:
					worldGenMenuClicked(player, slot); 
					break;
				case SETUP_WORLD_GEN_CONFIG:
					worldGenConfigMenuClicked(player, slot); 
					break;
				case SETUP_PLAYERS:
					setupPlayersMenuClicked(player, slot); 
					break;
				case SETUP_MONSTERS:
					monstersMenuClicked(player, slot); 
					break;
				case SETUP_ANIMALS:
					animalsMenuClicked(player, slot); 
					break;
				case SPECIFIC_OPTION_CHOICE:
					choiceOptionMenuClicked(player, slot);
					break;
					
				case LOBBY:
					lobbyMenuClicked(player, slot); break;
				case SPECTATOR_LOBBY:
					spectatorLobbyMenuClicked(player, slot); break;
				case TEAM_SELECTION:
					teamMenuClicked(player, slot); break;
				case ACTIVE:
					activeMenuClicked(player, slot); break;
				}
				break;
			}
	}
	
	private static void rootMenuClicked(Player player, int slot)
	{
		if (slot == ROOT_MENU_HELP_POS)
		{
			helpClicked(player);
			return;
		}
		
		Game[] games = KillerMinecraft.instance.games;
		if (slot < 0 || slot >= games.length)
			return;
	
		Game game = games[slot];
		if ( game.canJoin() )
		{
			game.addPlayerToGame(player);
			show(player);
		}
		else
			player.sendMessage(ChatColor.RED + "You can't join this game");
	}
	
	private void setupMenuClicked(Player player, int slot)
	{
		switch (slot)
		{
		case SETUP_MENU_MODE_POS:
			show(player, GameMenu.SETUP_GAME_MODE);
			break;
		case SETUP_MENU_WORLD_POS:
			show(player, GameMenu.SETUP_WORLD_GEN);
			break;
		/*case SETUP_MENU_MUTATOR_POS:
			show(player, GameMenu.SETUP_MUTATORS);
			break;*/
		case SETUP_MENU_PLAYERS_POS:
			show(player, GameMenu.SETUP_PLAYERS);
			break;
		case SETUP_MENU_MONSTERS_POS:
			show(player, GameMenu.SETUP_MONSTERS);
			break;
		case SETUP_MENU_ANIMALS_POS:
			show(player, GameMenu.SETUP_ANIMALS);
			break;
		case SETUP_MENU_OPEN_POS:
			game.setGameState(GameState.LOBBY);
			show(player, GameMenu.LOBBY);
			break;
		case SETUP_MENU_QUIT_POS:
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
			break;
		}
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
		if ( prev != null )
			setupGameModeItemLore(menu, GameMode.indexOf(prev), prev, false);
		setupGameModeItemLore(menu, GameMode.indexOf(current), current, true);
		
		// actually populate the config menu!
		inventories.put(GameMenu.SETUP_GAME_MODE_CONFIG, createGameModeConfigMenu(game.getGameMode()));
	}
	
	private void gameModeMenuClicked(Player player, int slot)
	{
		if ( slot == GAME_MODE_MENU_BACK_POS )
		{
			show(player, GameMenu.SETUP_ROOT);
			return;
		}
		
		GameModePlugin mode = GameMode.get(slot - GAME_MODE_MENU_FIRST_POS); 
		if ( game.setGameMode(mode) )
			settingsChanged(" set the game mode to " + mode.getName());
		
		show(player, GameMenu.SETUP_GAME_MODE_CONFIG);
	}
	
	private void gameModeConfigMenuClicked(Player player, int slot)
	{
		if ( slot == GAME_MODE_MENU_BACK_POS )
		{
			show(player, GameMenu.SETUP_GAME_MODE);
			return;
		}
		
		Option option = game.getGameMode().options[slot-GAME_MODE_CONFIG_MENU_FIRST_POS];
			
		ItemStack[] choiceItems = option.optionClicked();
		if ( choiceItems != null )
			showChoiceOptionMenu(player, option, choiceItems, GameMenu.SETUP_GAME_MODE_CONFIG);
		else
		{
			generateOptionMenuItems(inventories.get(GameMenu.SETUP_GAME_MODE_CONFIG), game.getGameMode().options);
			settingsChanged(" set the '" + option.getName() + "' setting to " + option.getValueString());
			updateMenus();
		}
	}
	
	void worldGenChanged(WorldGeneratorPlugin prev, WorldGeneratorPlugin current)
	{
		// update which item gets the "current world generator" lore text on the game mode menu
		Inventory menu = inventories.get(GameMenu.SETUP_WORLD_GEN);
		if ( prev != null )
			setupWorldGenItemLore(menu, WorldGenerator.indexOf(prev), prev, false);
		setupWorldGenItemLore(menu, WorldGenerator.indexOf(current), current, true);
		
		// actually populate the config menu!
		// ...
		inventories.put(GameMenu.SETUP_WORLD_GEN_CONFIG, createWorldGenConfigMenu(game.getWorldGenerator()));
	}
	
	private void worldGenMenuClicked(Player player, int slot)
	{
		if ( slot == WORLD_GEN_MENU_BACK_POS )
		{
			show(player, GameMenu.SETUP_ROOT);
			return;
		}
		
		WorldGeneratorPlugin gen = WorldGenerator.get(slot - WORLD_GEN_MENU_FIRST_POS); 
		game.setWorldGenerator(gen);
		settingsChanged(" set the world generator to " + gen.getName());
		
		show(player, GameMenu.SETUP_WORLD_GEN_CONFIG);
	}
	
	private void worldGenConfigMenuClicked(Player player, int slot)
	{
		if ( slot == WORLD_GEN_MENU_BACK_POS )
		{
			show(player, GameMenu.SETUP_WORLD_GEN);
			return;
		}
		
		Option option = game.getWorldGenerator().options[slot-2];
			
		ItemStack[] choiceItems = option.optionClicked();
		if ( choiceItems != null )
			showChoiceOptionMenu(player, option, choiceItems, GameMenu.SETUP_WORLD_GEN_CONFIG);
		else
		{
			generateOptionMenuItems(inventories.get(GameMenu.SETUP_WORLD_GEN_CONFIG), game.getWorldGenerator().options);
			settingsChanged(" set the '" + option.getName() + "' setting to " + option.getValueString());
			updateMenus();
		}
	}
	
	private void showChoiceOptionMenu(Player player, Option option, ItemStack[] choiceItems, GameMenu goBackTo)
	{
		choiceOptionGoBackTo = goBackTo;
		currentOption = option;
		Inventory menu = Bukkit.createInventory(null, nearestNine(choiceItems.length + 2), option.getName());
		menu.setItem(CHOICE_OPTION_BACK_POS, backItem);	
		
		populateChoiceOptionMenu(choiceItems, option.getSelectedIndex(), menu);
		
		inventories.put(GameMenu.SPECIFIC_OPTION_CHOICE, menu);
		show(player, GameMenu.SPECIFIC_OPTION_CHOICE);
	}
	
	GameMenu choiceOptionGoBackTo;
	Option currentOption;
	private void choiceOptionMenuClicked(Player player, int itemSlot)
	{
		if ( itemSlot == CHOICE_OPTION_BACK_POS )
		{
			currentOption = null;
			inventories.put(GameMenu.SPECIFIC_OPTION_CHOICE, null);
			
			if ( choiceOptionGoBackTo == GameMenu.SETUP_GAME_MODE_CONFIG )
				generateOptionMenuItems(inventories.get(choiceOptionGoBackTo), game.getGameMode().options);
			else if ( choiceOptionGoBackTo == GameMenu.SETUP_WORLD_GEN_CONFIG ) 
				generateOptionMenuItems(inventories.get(choiceOptionGoBackTo), game.getWorldGenerator().options);
			
			show(player, choiceOptionGoBackTo);
			return;
		}
		
		currentOption.setSelectedIndex(itemSlot-2);
		populateChoiceOptionMenu(currentOption.optionClicked(), currentOption.getSelectedIndex(), inventories.get(GameMenu.SPECIFIC_OPTION_CHOICE));
		settingsChanged(" set the '" + currentOption.getName() + "' setting to " + currentOption.getValueString());
	
		updateMenus();
	}

	private void populateChoiceOptionMenu(ItemStack[] choiceItems, int selectedIndex, Inventory menu)
	{
		for ( int i=0; i<choiceItems.length; i++ )
		{
			ItemStack item = choiceItems[i];
			if ( selectedIndex == i )
				item = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(item);				
			menu.setItem(i+2, item);
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
				menu.setItem(SETUP_PLAYERS_LIMIT_DOWN, playerLimitDown);
			}
			else
				menu.setItem(SETUP_PLAYERS_LIMIT_DOWN, null);
			
			ItemStack playerLimitUp = new ItemStack(Material.MILK_BUCKET);
			setNameAndLore(playerLimitUp, "Increase player limit", ChatColor.YELLOW + "Current limit: " + game.getPlayerLimit(), "Increase limit to " + (game.getPlayerLimit() + 1));
			menu.setItem(SETUP_PLAYERS_LIMIT_UP, playerLimitUp);
		}
		else
		{
			menu.setItem(SETUP_PLAYERS_LIMIT_DOWN, null);
			menu.setItem(SETUP_PLAYERS_LIMIT_UP, null);
		}

		menu.setItem(SETUP_PLAYERS_USE_PLAYER_LIMIT, usePlayerLimit);
		
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
		menu.setItem(SETUP_PLAYERS_LOCK_GAME, lockGame);
		
		// if game mode allows team selection, a toggle to allow manual team selection (and show the team selection scoreboard) ...
		// if disabled, players shouldn't see the scoreboard thing, teams will be auto-assigned
		if ( game.getGameMode().allowTeamSelection() )
		{
			ItemStack teamSelection = new ItemStack(Material.DIODE);
			setNameAndLore(teamSelection, "Allow team selection", "When enabled, players will be", "able to choose their own teams.", "When disabled, teams will be", "allocated randomly.");
			
			if ( teamSelectionEnabled )
				teamSelection = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(teamSelection);
			
			menu.setItem(SETUP_PLAYERS_TEAM_SELECTION, teamSelection);
		}
		else
			menu.setItem(SETUP_PLAYERS_TEAM_SELECTION, null);
	}
	
	private void setupPlayersMenuClicked(Player player, int slot)
	{
		if ( slot == SETUP_PLAYERS_BACK_POS )
		{
			show(player); // go back to lobby or setup, depending on state
			return;
		}
				
		if ( slot == SETUP_PLAYERS_USE_PLAYER_LIMIT )
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
		}
		else if ( slot == SETUP_PLAYERS_LIMIT_DOWN )
		{
			int limit = Math.max(1, game.getPlayerLimit()-1);
			game.setPlayerLimit(limit);
			settingsChanged(" decreased the player limit to " + limit);
		}
		else if ( slot == SETUP_PLAYERS_LIMIT_UP )
		{
			int limit = game.getPlayerLimit()+1;
			game.setPlayerLimit(limit);
			settingsChanged(" increased the player limit to " + limit);
		}
		else if ( slot == SETUP_PLAYERS_LOCK_GAME )
		{
			boolean locked = !game.isLocked();
			game.setLocked(locked);
			settingsChanged(locked ? " locked the game" : " unlocked the game");
		}
		else if ( slot == SETUP_PLAYERS_TEAM_SELECTION )
		{
			teamSelectionEnabled = !teamSelectionEnabled;
			settingsChanged(teamSelectionEnabled ? " enabled team selection" : " disabled team selection");
		}
		
		repopulatePlayersMenu();
		updateMenus();
	}
	
	void monstersMenuClicked(Player player, int slot)
	{
		if ( slot == QUANTITY_MENU_BACK_POS )
		{
			show(player, GameMenu.SETUP_ROOT);
			return;
		}
		
		int clickedQuantity = slot - QUANTITY_MENU_NONE_POS;
		
		if ( game.monsterNumbers == clickedQuantity )
			return;
		
		int prev = game.monsterNumbers;
		game.monsterNumbers = clickedQuantity;
		
		ItemStack item = monsterItems[clickedQuantity] = createQuantityItem(clickedQuantity, true, monsterDescriptions[clickedQuantity]);
		inventories.get(GameMenu.SETUP_MONSTERS).setItem(slot, item);
		item = monsterItems[prev] = createQuantityItem(prev, false, monsterDescriptions[prev]);
		inventories.get(GameMenu.SETUP_MONSTERS).setItem(prev + QUANTITY_MENU_NONE_POS, item);
		
		settingsChanged(" changed the monster numbers to '" + getQuantityText(clickedQuantity) + "'");
	}
	
	void animalsMenuClicked(Player player, int slot)
	{
		if ( slot == QUANTITY_MENU_BACK_POS )
		{
			show(player, GameMenu.SETUP_ROOT);
			return;
		}
		
		int clickedQuantity = slot - QUANTITY_MENU_NONE_POS;
		
		if ( game.animalNumbers == clickedQuantity )
			return;
		
		int prev = game.animalNumbers;
		game.animalNumbers = clickedQuantity;
		
		ItemStack item = animalItems[clickedQuantity] = createQuantityItem(clickedQuantity, true, animalDescriptions[clickedQuantity]);
		inventories.get(GameMenu.SETUP_ANIMALS).setItem(slot, item);
		item = monsterItems[prev] = createQuantityItem(prev, false, animalDescriptions[prev]);
		inventories.get(GameMenu.SETUP_ANIMALS).setItem(prev + QUANTITY_MENU_NONE_POS, item);
		
		settingsChanged(" changed the animal numbers to '" + getQuantityText(clickedQuantity) + "'");
	}
	
	private void lobbyMenuClicked(Player player, int slot)
	{
		switch (slot)
		{
		case LOBBY_MENU_SETTINGS_POS:
			if ( player.getName() == game.hostPlayer && game.getGameState() == GameState.LOBBY )
				show(player, GameMenu.SETUP_ROOT);
			else
				player.sendMessage(listAllSettings(game));
			break;
		case LOBBY_MENU_PLAYER_LIST_POS:
			player.sendMessage(listPlayers(game));
			break;
		case LOBBY_MENU_SPECTATE_POS:
			PlayerInfo info = game.getPlayerInfo(player);
			info.setSpectator(true);
			game.getGameMode().setTeam(player, null);
			updateMenus();
			show(player);
			break;
		case LOBBY_MENU_TEAM_POS:
			show(player, GameMenu.TEAM_SELECTION);
			break;
		case LOBBY_MENU_QUIT_POS:
			game.removePlayerFromGame(player);
			player.closeInventory();
			break;
		case LOBBY_MENU_START_POS:
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
			break;
		case LOBBY_MENU_HELP_POS:
			helpClicked(player);
			break;
		}
	}
	
	private void spectatorLobbyMenuClicked(Player player, int slot)
	{
		if (slot == LOBBY_MENU_SPECTATE_POS)
		{
			if ( game.canJoin() )
			{
				PlayerInfo info = game.getPlayerInfo(player);
				info.setSpectator(false);
				updateMenus();
			}
			show(player);
		}
		else
			lobbyMenuClicked(player, slot);
	}
	
	private void teamMenuClicked(Player player, int slot)
	{
		if ( slot == 0 )
		{
			game.getGameMode().setTeam(player, null);
			game.broadcastMessage(player.getName() + " set their team to auto-assign");
		}
			
		slot--;
		TeamInfo[] teams = game.getGameMode().getTeams();
		if ( slot < teams.length )
		{
			TeamInfo team = teams[slot];
			game.getGameMode().setTeam(player, team);
			game.broadcastMessage(player.getName() + " joined the " + team.getChatColor() + team.getName());
		}
		
		show(player);
	}
	
	private void activeMenuClicked(Player player, int slot)
	{
		switch(slot)
		{
		case ACTIVE_MENU_QUIT_POS:
			game.removePlayerFromGame(player);
			player.closeInventory();
			break;
		}
	}
	
	private static void helpClicked(Player player)
	{
		player.setItemOnCursor(helpBook);
	}
	
	private void settingsChanged(String message)
	{
		// only broadcast when in the lobby
		if ( game.getGameState() == GameState.LOBBY )
			game.broadcastMessage(game.hostPlayer + message);
	}
}

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
	
	EnumMap<GameMenu, Inventory> inventories = new EnumMap<GameMenu, Inventory>(GameMenu.class);
	HashMap<Inventory, MenuItem[]> menuItems = new HashMap<Inventory, MenuItem[]>();
	
	public static Inventory rootMenu;
	private static MenuItem[] rootMenuItems = new MenuItem[9]; 
	
	Game game;
	MenuItem gameRootMenuItem;
	private static ItemStack helpItem, helpBook, backItem, quitItem;

	public MenuManager(Game game)
	{
		this.game = game;
		
		gameRootMenuItem = rootMenuItems[game.getNumber() - 1];
		
		inventories.put(GameMenu.SETUP_ROOT, createSetupMenu());
		inventories.put(GameMenu.LOBBY, createLobbyMenu());
		inventories.put(GameMenu.SPECTATOR_LOBBY, createSpectatorLobbyMenu());
		inventories.put(GameMenu.TEAM_SELECTION, createTeamMenu());
		inventories.put(GameMenu.ACTIVE, createActiveMenu());
		
		inventories.put(GameMenu.SETUP_GAME_MODE, createGameModeMenu());
		inventories.put(GameMenu.SETUP_GAME_MODE_CONFIG, createGameModeConfigMenu());
		inventories.put(GameMenu.SETUP_WORLD_GEN, createWorldGenMenu());
		inventories.put(GameMenu.SETUP_WORLD_GEN_CONFIG, createWorldGenConfigMenu());
		inventories.put(GameMenu.SETUP_PLAYERS, createPlayersMenu());
		inventories.put(GameMenu.SETUP_MONSTERS, createMonstersMenu());
		inventories.put(GameMenu.SETUP_ANIMALS, createAnimalsMenu());
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
			instance.menuItems.put(menu,  items);
		}
		
		items[item.getSlot()] = item;
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
		
		addItemToMenu(null, new MenuItem(rootMenu, 8, helpItem) {
			@Override
			public void runWhenClicked(Player player) {
				player.setItemOnCursor(helpBook);
			}
		});
		
		for (final Game game : KillerMinecraft.instance.games)
		{
			int slot = game.getNumber() - 1;
			
			ItemStack stack = new ItemStack(Material.BUCKET, 1);
			MenuItem item = new MenuItem(rootMenu, slot, stack) {
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
			
			addItemToMenu(null, item);
		}
		
		
		
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
	
	private Inventory createSetupMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer Minecraft: Game setup");
		
		MenuItem gameModeItem = new MenuItem(menu, 0, null) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_GAME_MODE);
			}
			
			@Override
			public void recalculateStack() {
				ItemStack stack = new ItemStack(Material.CAKE);
				setNameAndLore(stack, "Change Game Mode", highlightStyle + "Current mode: " + game.getGameMode().getName(), "The game mode is the main set of rules,", "and controls every aspect of a game.");
				setStack(stack);
			}
		};
		addItemToMenu(this, gameModeItem);
		
		ItemStack worldGenStack = new ItemStack(Material.GRASS);
		setNameAndLore(worldGenStack, "Change World Generator", "");
		MenuItem worldGenItem = new MenuItem(menu, 1, worldGenStack) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_WORLD_GEN);
			}
			
			@Override
			public void recalculateStack() {
				if ( game.getGameMode().allowWorldGeneratorSelection() )
				{
					ItemStack stack = new ItemStack(Material.GRASS);
					setNameAndLore(stack, "Change World Generator", highlightStyle + "Current generator: "+ game.getWorldGenerator().getName(), "The world generator controls", "the terrain in the game's world(s)");
					setStack(stack);
				}
				else
					setStack(null);
			}
		};
		addItemToMenu(this, worldGenItem);				
		/*
		final ItemStack rootMutators = new ItemStack(Material.EXP_BOTTLE);
		setNameAndLore(rootMutators, "Select Mutators", "Mutators change specific aspects", "of a game, but aren't specific", "to any particular game mode");
		addItemToMenu(this, new MenuItem(menu, 2, rootMutators) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_MUTATORS);
			}
		});
		*/
		final ItemStack rootPlayerNumbers = new ItemStack(Material.SKULL_ITEM, 1, (short)3); // steve head
		setNameAndLore(rootPlayerNumbers, "Player limits", "Specify the maximum number of", "players allowed into the game");
		addItemToMenu(this, new MenuItem(menu, 3, rootPlayerNumbers) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_PLAYERS);
			}
		});
		
		final ItemStack rootMonsters = new ItemStack(Material.SKULL_ITEM, 1, (short)4); // creeper head
		setNameAndLore(rootMonsters, "Monster Numbers", "Control the number of", "monsters that spawn");
		addItemToMenu(this, new MenuItem(menu, 4, rootMonsters) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_MONSTERS);
			}
		});
		
		final ItemStack rootAnimals = new ItemStack(Material.EGG);
		setNameAndLore(rootAnimals, "Animal Numbers", "Control the number of", "animals that spawn");
		addItemToMenu(this, new MenuItem(menu, 5, rootAnimals) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_ANIMALS);
			}
		});
		
		final ItemStack rootOpen = new ItemStack(Material.IRON_DOOR);
		setNameAndLore(rootOpen, "Open game lobby", "Open this game, so", "that players can join");
		addItemToMenu(this, new MenuItem(menu, 7, rootOpen) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.LOBBY);
			}
			
			@Override
			public void recalculateStack() {
				setStack(game.getGameState() == GameState.SETUP ? rootOpen : null);
			}
		});

		addItemToMenu(this, new MenuItem(menu, 8, quitItem) {
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
			
			@Override
			public void recalculateStack() {
				setStack(game.getGameState() == GameState.SETUP ? quitItem : backItem);
			}
		});
		
		return menu;
	}

	private Inventory createGameModeMenu()
	{
		Inventory menu = Bukkit.createInventory(null, nearestNine(GameMode.gameModes.size() + 2), "Game mode selection");
		
		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_ROOT);
			}
		});
		
		for ( int i=0; i<GameMode.gameModes.size(); i++ )
		{
			final GameModePlugin mode = GameMode.get(i);
			ItemStack stack = setupGameModeItem(mode, false);
			
			addItemToMenu(this, new MenuItem(menu, i + 2, stack) {
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
	
	private Inventory createGameModeConfigMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Game mode options");

		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_GAME_MODE);
			}
		});
		
		for (int i=1; i<9; i++)
		{
			final int optionNum = i - 1;
			
			addItemToMenu(this, new MenuItem(menu, i, null) {
				@Override
				public void runWhenClicked(Player player) {
					Option[] options = game.getGameMode().options;
					if (optionNum >= options.length)
						return;
					
					Option option = options[optionNum];
					
					ItemStack[] choiceItems = option.optionClicked();
					if ( choiceItems != null )
						showChoiceOptionMenu(player, option, choiceItems, GameMenu.SETUP_GAME_MODE_CONFIG);
					else
					{
						repopulateMenu(GameMenu.SETUP_GAME_MODE_CONFIG);
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
					
					ItemStack item = option.getDisplayStack();
					setNameAndLore(item, option.getName(), option.getDescription());
					setStack(stack);
				}
			});
		}
		return menu;
	}
	
	private Inventory createWorldGenMenu()
	{
		Inventory menu = Bukkit.createInventory(null, nearestNine(WorldGenerator.worldGenerators.size() + 2), "World generator selection");
		
		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_ROOT);
			}
		});
		
		for ( int i=0; i<WorldGenerator.worldGenerators.size(); i++ )
		{
			final WorldGeneratorPlugin world = WorldGenerator.get(i);
			ItemStack stack = setupWorldGenItem(world, false);
			
			addItemToMenu(this, new MenuItem(menu, i + 2, stack) {
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
	
	private Inventory createWorldGenConfigMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "World generator options");

		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_WORLD_GEN);
			}
		});
		
		for (int i=1; i<9; i++)
		{
			final int optionNum = i - 1;
			
			addItemToMenu(this, new MenuItem(menu, i, null) {
				@Override
				public void runWhenClicked(Player player) {
					Option[] options = game.getWorldGenerator().options;
					if (optionNum >= options.length)
						return;
					
					Option option = options[optionNum];
					
					ItemStack[] choiceItems = option.optionClicked();
					if ( choiceItems != null )
						showChoiceOptionMenu(player, option, choiceItems, GameMenu.SETUP_WORLD_GEN_CONFIG);
					else
					{
						repopulateMenu(GameMenu.SETUP_WORLD_GEN_CONFIG);
						settingsChanged(" set the '" + option.getName() + "' setting to " + option.getValueString());
					}
				}
				
				@Override
				public void recalculateStack() {
					Option[] options = game.getWorldGenerator().options;
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
					
					ItemStack item = option.getDisplayStack();
					setNameAndLore(item, option.getName(), option.getDescription());
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
			}
			
			@Override
			public void recalculateStack() {
				ItemStack stack = new ItemStack(Material.MILK_BUCKET);
				setNameAndLore(stack, "Increase player limit", ChatColor.YELLOW + "Current limit: " + game.getPlayerLimit(), "Increase limit to " + (game.getPlayerLimit() + 1));
				setStack(stack);
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
			};
			
			@Override
			public void recalculateStack() {
				ItemStack stack;
				if ( game.getGameState() == GameState.EMPTY || game.getGameState() == GameState.SETUP )
					stack = null;
				else
				{
					stack = new ItemStack(Material.IRON_FENCE);
					if ( game.isLocked() )
					{
						setNameAndLore(stack, "Unlock the game", "This game is locked, so", "no one else can join,", "even if players leave");
						stack = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(stack);
					}
					else
						setNameAndLore(stack, "Lock the game", "Lock this game, so that", "no one else can join,", "even if players leave");
				}
				setStack(stack);
			}
		});
		
		addItemToMenu(this, new MenuItem(menu, 8, null) {
			@Override
			public void runWhenClicked(Player player)
			{
				teamSelectionEnabled = !teamSelectionEnabled;
				settingsChanged(teamSelectionEnabled ? " enabled team selection" : " disabled team selection");
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
	
	private Inventory createMonstersMenu()
	{
		final Inventory menu = Bukkit.createInventory(null, 9, "Monster numbers");
		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_ROOT);
			}
		});
		
		MenuItem[] items = new MenuItem[Game.maxQuantityNum + 1];		
		for (int i=0; i<=items.length; i++)
		{
			ItemStack stack = createQuantityItem(i, game.monsterNumbers == i, monsterDescriptions[i]);
			final int quantity = i;
			
			MenuItem item = new MenuItem(menu, i + 2, stack) {
				@Override
				public void runWhenClicked(Player player) {
					game.monsterNumbers = quantity;						
					settingsChanged(" changed the monster numbers to '" + getQuantityText(quantity) + "'");
				}
			};
			
			addItemToMenu(this, item);
			items[i] = item;
		}
		
		for (MenuItem item : items)
			item.recalculateOnClick(items);
		
		return menu;
	}
	
	private Inventory createAnimalsMenu()
	{
		final Inventory menu = Bukkit.createInventory(null, 9, "Monster numbers");
		addItemToMenu(this, new MenuItem(menu, 0, backItem) {
			@Override
			public void runWhenClicked(Player player) {
				show(player, GameMenu.SETUP_ROOT);
			}
		});
		
		MenuItem[] items = new MenuItem[Game.maxQuantityNum + 1];		
		for (int i=0; i<=items.length; i++)
		{
			ItemStack stack = createQuantityItem(i, game.animalNumbers == i, animalDescriptions[i]);
			final int quantity = i;
			
			MenuItem item = new MenuItem(menu, i + 2, stack) {
				@Override
				public void runWhenClicked(Player player) {
					game.animalNumbers = quantity;
					settingsChanged(" changed the animal numbers to '" + getQuantityText(quantity) + "'");
				}
			};
			
			addItemToMenu(this, item);
			items[i] = item;
		}
		
		for (MenuItem item : items)
			item.recalculateOnClick(items);
				
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
		ItemStack gameSettingsItem = new ItemStack(Material.PAPER, 1);
		setNameAndLore(gameSettingsItem, "View Game Settings", describeSettings(game));	// clicking this should write out more detailed settings to chat
		addItemToMenu(this, new MenuItem(menu, 0, gameSettingsItem) {
			@Override
			public void runWhenClicked(Player player) {
				if ( player.getName() == game.hostPlayer && game.getGameState() == GameState.LOBBY )
					show(player, GameMenu.SETUP_ROOT);
				else
					player.sendMessage(listAllSettings(game));
			}
			@Override
			public void recalculateStack() {
				ItemStack stack = getStack();
				setLore(stack, describeSettings(game));
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
		
		ItemStack autoAssign = new ItemStack(Material.MOB_SPAWNER);
		setNameAndLore(autoAssign, "Auto assign", "Automatically assigns you to", "the team with the fewest", "players, or randomly in the", "event of a tie.");
		addItemToMenu(this, new MenuItem(menu, 0, autoAssign) {
			@Override
			public void runWhenClicked(Player player) {
				game.getGameMode().setTeam(player, null);
				game.broadcastMessage(player.getName() + " set their team to auto-assign");
			}
		});
		
		MenuItem[] teamItems = new MenuItem[8];
		for (int slot = 1; slot < 9; slot++)
		{
			final int teamIndex = slot - 1;
			MenuItem item = new MenuItem(menu, slot, null) {
				@Override
				protected void runWhenClicked(Player player)
				{
					TeamInfo team = game.getGameMode().getTeams()[teamIndex];
					
					game.getGameMode().setTeam(player, team);
					game.broadcastMessage(player.getName() + " joined the " + team.getChatColor() + team.getName());
					show(player);
				}
				
				@Override
				public void recalculateStack()
				{
					TeamInfo team = game.getGameMode().getTeams()[teamIndex];
					
					ItemStack item = new ItemStack(Material.LEATHER_HELMET, 1);
					LeatherArmorMeta meta = (LeatherArmorMeta)item.getItemMeta();
					meta.setColor(team.getArmorColor());
					item.setItemMeta(meta);
					
					setNameAndLore(item, team.getChatColor() + team.getName(), "Join the " + team.getName());
				};
			};
			
			addItemToMenu(this, item);
			teamItems[teamIndex] = item;
		}
		
		return menu;
	}
	
	private Inventory createActiveMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Killer Minecraft: " + game.getName());

		// TODO: call a vote?
		
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
		if ( !game.getGameState().keepLobbyUpToDate || !game.getGameMode().allowTeamSelection() || game.getGameMode().getTeams().length == 0 )
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
					currentOption.setSelectedIndex(slot);
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
		MenuItem[] items = menuItems.get(inventory);
		
		for (MenuItem item : items)
			item.recalculateStack();
	}
	
	public void updateMenus()
	{
		repopulateMenu(GameMenu.LOBBY);
		repopulateMenu(GameMenu.SPECTATOR_LOBBY);
		repopulateMenu(GameMenu.SETUP_ROOT);
		repopulateMenu(GameMenu.SETUP_GAME_MODE);
		repopulateMenu(GameMenu.SETUP_GAME_MODE_CONFIG);
		repopulateMenu(GameMenu.SETUP_WORLD_GEN);
		repopulateMenu(GameMenu.SETUP_WORLD_GEN_CONFIG);
		repopulateMenu(GameMenu.TEAM_SELECTION);
		gameRootMenuItem.recalculateStack();
	}
}

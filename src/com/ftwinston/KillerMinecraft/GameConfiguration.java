package com.ftwinston.KillerMinecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


class GameConfiguration
{
	private Game game;
	EnumMap<Menu, Inventory> inventories = new EnumMap<GameConfiguration.Menu, Inventory>(Menu.class);
	
	ItemStack backItem;
	ItemStack rootGameMode, rootGameModeConfig, rootWorldGen, rootWorldGenConfig, rootPlayerNumbers, rootMonsters, rootAnimals;
	ItemStack[] gameModeItems, worldGenItems;
	ItemStack monstersNone, monstersLow, monstersMed, monstersHigh, monstersTooHigh;
	ItemStack animalsNone, animalsLow, animalsMed, animalsHigh, animalsTooHigh;
	
	public GameConfiguration(Game game)
	{
		this.game = game;
		createMenus();
	}
	
	enum Menu
	{
		ROOT,
		GAME_MODE,
		GAME_MODE_CONFIG,
		WORLD_GEN,
		WORLD_GEN_CONFIG,
		PLAYERS,
		MONSTERS,
		ANIMALS,
	}
	
	private static String /*loreStyle = "" + ChatColor.DARK_PURPLE + ChatColor.ITALIC,*/ highlightStyle = "" + ChatColor.YELLOW + ChatColor.ITALIC;
	Menu currentMenu = Menu.ROOT;
	private final short playerHeadDurability = 3; 
	
	private void createMenus()
	{
		backItem = new ItemStack(Material.IRON_DOOR);
		setNameAndLore(backItem, highlightStyle + "Go back", "Return to the previous menu");
		
		createRootMenu();
		createGameModeMenu();
		createGameModeConfigMenu();
		createWorldGenMenu();
		createWorldGenConfigMenu();
		createPlayersMenu();
		createMonstersMenu();
		createAnimalsMenu();
	}
	
	private void createRootMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, game.getName() + " configuration");
		
		rootGameMode = new ItemStack(Material.CAKE);
		rootGameModeConfig = new ItemStack(Material.DISPENSER);
		rootWorldGen = new ItemStack(Material.GRASS);
		rootWorldGenConfig = new ItemStack(Material.DROPPER);
		//rootMutators = new ItemStack(Material.EXP_BOTTLE);
		rootPlayerNumbers = new ItemStack(Material.SKULL_ITEM, 1, playerHeadDurability); // steve head
		rootMonsters = new ItemStack(Material.SKULL_ITEM, 1, (short)4); // creeper head
		rootAnimals = new ItemStack(Material.EGG);
		
		setNameAndLore(rootGameMode, "Change Game Mode", "");
		setNameAndLore(rootGameModeConfig, "Configure Game Mode", "Any options the current game", "mode has can be configured here");
		setNameAndLore(rootWorldGen, "Change World Generator", "");
		setNameAndLore(rootWorldGenConfig, "Configure World Generator", "Any options the current world", "generator has can be configured here");
		//setNameAndLore(rootMutators, "Select Mutators", "Mutators change specific aspects", "of a game, but aren't specific", "to any particular game mode");
		setNameAndLore(rootPlayerNumbers, "Player limits", "Specify the maximum number of", "players allowed into the game");
		setNameAndLore(rootMonsters, "Monster Numbers", "Control the number of", "monsters that spawn");
		setNameAndLore(rootAnimals, "Animal Numbers", "Control the number of", "animals that spawn");
		
		gameModeChanged(game.getGameMode());
		worldGeneratorChanged(game.getWorldGenerator());
		
		menu.setItem(0, rootGameMode);
		menu.setItem(1, rootGameModeConfig);
		menu.setItem(2, rootWorldGen);
		menu.setItem(3, rootWorldGenConfig);
		//menu.setItem(4, rootMutators);
		menu.setItem(6, rootPlayerNumbers);
		menu.setItem(7, rootMonsters);
		menu.setItem(8, rootAnimals);
		
		inventories.put(Menu.ROOT, menu);
	}
	
	private void createGameModeMenu()
	{
		Inventory menu = Bukkit.createInventory(null, nearestNine(GameMode.gameModes.size() + 1), "Game mode selection");
		menu.setItem(0, backItem);
		
		gameModeItems = new ItemStack[GameMode.gameModes.size()];
		for ( int i=0; i<GameMode.gameModes.size(); i++ )
		{
			GameModePlugin mode = GameMode.get(i);
			ItemStack item = new ItemStack(mode.getMenuIcon());
			
			if ( game.getGameMode().getName().equals(mode.getName()) )
			{
				String[] desc = mode.getDescriptionText();
				ArrayList<String> lore = new ArrayList<String>(desc.length + 1);
				lore.add(highlightStyle + "Current game mode");
				for ( int j=0; j<desc.length; j++)
					lore.add(desc[j]);
				setNameAndLore(item, mode.getName(), lore);
			}
			else
				setNameAndLore(item, mode.getName(), mode.getDescriptionText());

			gameModeItems[i] = item;
			menu.setItem(i+1, item);
		}
		
		inventories.put(Menu.GAME_MODE, menu);
	}
	
	private void createGameModeConfigMenu()
	{
		Inventory menu = null;
		inventories.put(Menu.GAME_MODE_CONFIG, menu);
	}
	
	private void createWorldGenMenu()
	{
		Inventory menu = Bukkit.createInventory(null, nearestNine(WorldGenerator.worldGenerators.size() + 1), "World generator selection");
		menu.setItem(0, backItem);
		
		worldGenItems = new ItemStack[WorldGenerator.worldGenerators.size()];
		for ( int i=0; i<WorldGenerator.worldGenerators.size(); i++ )
		{
			WorldGeneratorPlugin world = WorldGenerator.get(i);
			
			ItemStack item = new ItemStack(world.getMenuIcon());
			if ( game.getWorldGenerator().getName().equals(world.getName()) )
			{
				String[] desc = world.getDescriptionText();
				ArrayList<String> lore = new ArrayList<String>(desc.length + 1);
				lore.add(highlightStyle + "Current world generator");
				for ( int j=0; j<desc.length; j++)
					lore.add(desc[j]);
				setNameAndLore(item, world.getName(), lore);
			}
			else
				setNameAndLore(item, world.getName(), world.getDescriptionText());

			worldGenItems[i] = item;
			menu.setItem(i+1, item);
		}
		
		inventories.put(Menu.WORLD_GEN, menu);
	}

	private void createWorldGenConfigMenu()
	{
		Inventory menu = null;
		inventories.put(Menu.WORLD_GEN_CONFIG, menu);
	}
	
	private void createPlayersMenu()
	{
		Inventory menu = null;
		inventories.put(Menu.PLAYERS, menu);
	}
	
	private void createMonstersMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Monster numbers");
		menu.setItem(0, backItem);
		
		monstersNone = createQuantityItem(0, game.monsterNumbers == 0, "No monsters will spawn");
		monstersLow = createQuantityItem(1, game.monsterNumbers == 1, "Reduced monster spawn rate");
		monstersMed = createQuantityItem(2, game.monsterNumbers == 2, "Normal monster spawn rate");
		monstersHigh = createQuantityItem(3, game.monsterNumbers == 3, "High monster spawn rate");
		monstersTooHigh = createQuantityItem(4, game.monsterNumbers == 4, "Excessive monster spawn rate");
		
		menu.setItem(2, monstersNone);
		menu.setItem(3, monstersLow);
		menu.setItem(4, monstersMed);
		menu.setItem(5, monstersHigh);
		menu.setItem(6, monstersTooHigh);
		
		inventories.put(Menu.MONSTERS, menu);
	}
	
	private void createAnimalsMenu()
	{
		Inventory menu = Bukkit.createInventory(null, 9, "Animal numbers");
		menu.setItem(0, backItem);
		
		animalsNone = createQuantityItem(0, game.animalNumbers == 0, "No animals will spawn");
		animalsLow = createQuantityItem(1, game.animalNumbers == 1, "Reduced animal spawn rate");
		animalsMed = createQuantityItem(2, game.animalNumbers == 2, "Normal animal spawn rate");
		animalsHigh = createQuantityItem(3, game.animalNumbers == 3, "High animal spawn rate");
		animalsTooHigh = createQuantityItem(4, game.animalNumbers == 4, "Excessive animal spawn rate");
		
		menu.setItem(2, animalsNone);
		menu.setItem(3, animalsLow);
		menu.setItem(4, animalsMed);
		menu.setItem(5, animalsHigh);
		menu.setItem(6, animalsTooHigh);
		
		inventories.put(Menu.ANIMALS, menu);
	}

	private ItemStack createQuantityItem(int quantity, boolean selected, String lore)
	{
		ItemStack item = new ItemStack(Material.STICK);
		
		if ( selected )
			setNameAndLore(item, GameInfoRenderer.getQuantityText(quantity), highlightStyle + "Current Setting", lore);
		else
			setNameAndLore(item, GameInfoRenderer.getQuantityText(quantity), lore);
		
		switch ( quantity )
		{
		case 1:
			item.setType(Material.STONE_PICKAXE); break;
		case 2:
			item.setType(Material.IRON_PICKAXE); break;
		case 3:
			item.setType(Material.DIAMOND_PICKAXE); break;
		case 4:
			item.setType(Material.DIAMOND_PICKAXE);
			item = KillerMinecraft.instance.craftBukkit.setEnchantmentGlow(item);
			break;
		}
		
		return item;
	}
	
	private void setNameAndLore(ItemStack item, String name, List<String> lore)
	{
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + name);
		meta.setLore(lore);
		item.setItemMeta(meta);
	}
	
	private void setNameAndLore(ItemStack item, String name, String... lore)
	{
		setNameAndLore(item, name, Arrays.asList(lore));
	}
	
	private void setLore(ItemStack item, String... lore)
	{
		ItemMeta meta = item.getItemMeta();
		meta.setLore(Arrays.asList(lore));
		item.setItemMeta(meta);
	}
	
	private int nearestNine(int num)
	{
		return 9*(int)Math.ceil(num/9.0);
	}

	public void show(Player player)
	{
		String configuring = game.getConfiguringPlayer();
		if ( configuring != null )
		{
			if ( !configuring.equals(player.getName()) )
				player.sendMessage(game.getName() + " is already being configured by " + configuring);
			
			return;
		}
		
		game.setConfiguringPlayer(player.getName());
		showMenu(player, Menu.ROOT);
	}

	private void showMenu(Player player, Menu menu)
	{
		currentMenu = menu;
		player.openInventory(inventories.get(menu));
	}
	
	public void gameModeChanged(GameMode gameMode)
	{
		setLore(rootGameMode, highlightStyle + "Current mode: " + gameMode.getName(), "The game mode is the main set of rules,", "and controls every aspect of a game.");
		//rootMenu.setItem(0, rootGameMode);
	}

	public void worldGeneratorChanged(WorldGenerator world)
	{
		setLore(rootWorldGen, highlightStyle + "Current generator: "+ world.getName(), "The world generator controls", "the terrain in the game's world(s)");
		//rootMenu.setItem(2, rootWorldGen);
	}

	private void rootMenuClicked(Player player, ItemStack item)
	{
		if ( item.getType() == rootGameMode.getType() )
			showMenu(player, Menu.GAME_MODE);
		else if ( item.getType() == rootGameModeConfig.getType() )
			showMenu(player, Menu.GAME_MODE_CONFIG);
		else if ( item.getType() == rootWorldGen.getType() )
			showMenu(player, Menu.WORLD_GEN);
		else if ( item.getType() == rootWorldGenConfig.getType() )
			showMenu(player, Menu.WORLD_GEN_CONFIG);
		else if ( item.getType() == rootPlayerNumbers.getType() && item.getDurability() == playerHeadDurability )
			showMenu(player, Menu.PLAYERS);
		else if ( item.getType() == rootMonsters.getType() )
			showMenu(player, Menu.MONSTERS);
		else if ( item.getType() == rootAnimals.getType() )
			showMenu(player, Menu.ANIMALS);
	}

	private void gameModeMenuClicked(Player player, ItemStack item)
	{
		if ( item.getType() == backItem.getType() )
			showMenu(player, Menu.ROOT);
	}
	
	private void gameModeConfigClicked(Player player, ItemStack item)
	{
		if ( item.getType() == backItem.getType() )
			showMenu(player, Menu.ROOT);
	}
	
	private void worldGenMenuClicked(Player player, ItemStack item)
	{
		if ( item.getType() == backItem.getType() )
			showMenu(player, Menu.ROOT);
	}
	
	private void worldGenConfigClicked(Player player, ItemStack item)
	{
		if ( item.getType() == backItem.getType() )
			showMenu(player, Menu.ROOT);
	}
	
	private void playersMenuClicked(Player player, ItemStack item)
	{
		if ( item.getType() == backItem.getType() )
			showMenu(player, Menu.ROOT);
	}
	
	private void monstersMenuClicked(Player player, ItemStack item)
	{
		if ( item.getType() == backItem.getType() )
			showMenu(player, Menu.ROOT);
	}
	
	private void animalsMenuClicked(Player player, ItemStack item)
	{
		if ( item.getType() == backItem.getType() )
			showMenu(player, Menu.ROOT);
	}
	
	private static Game getGameByConfiguringPlayer(Player player)
	{	
		for ( Game game : KillerMinecraft.instance.games )
		{
			String name = game.getConfiguringPlayer();
			 
			if ( name != null && name.equals(player.getName()) )
				return game;
		}
		return null;
	}
	
	public static void checkEvent(InventoryClickEvent event)
	{
		if ( !(event.getWhoClicked() instanceof Player) || event.getCurrentItem() == null )
			return;
		
		Player player = (Player)event.getWhoClicked();
		Game game = getGameByConfiguringPlayer(player);
		if ( game == null )
			return;
		
		event.setCancelled(true);
		
		if ( event.getRawSlot() >= event.getInventory().getSize() ) // click in player's own inventory
		{
			player.closeInventory();
			return;
		}
		
		switch (game.configuration.currentMenu)
		{
		case ROOT:
			game.configuration.rootMenuClicked(player, event.getCurrentItem()); break;
		case GAME_MODE:
			game.configuration.gameModeMenuClicked(player, event.getCurrentItem()); break;
		case GAME_MODE_CONFIG:
			game.configuration.gameModeConfigClicked(player, event.getCurrentItem()); break;
		case WORLD_GEN:
			game.configuration.worldGenMenuClicked(player, event.getCurrentItem()); break;
		case WORLD_GEN_CONFIG:
			game.configuration.worldGenConfigClicked(player, event.getCurrentItem()); break;
		case PLAYERS:
			game.configuration.playersMenuClicked(player, event.getCurrentItem()); break;
		case MONSTERS:
			game.configuration.monstersMenuClicked(player, event.getCurrentItem()); break;
		case ANIMALS:
			game.configuration.animalsMenuClicked(player, event.getCurrentItem()); break;
		}
	}

	public static void checkEvent(InventoryCloseEvent event)
	{
		if ( !(event.getPlayer() instanceof Player) )
			return;
		
		Player player = (Player)event.getPlayer();
		Game game = getGameByConfiguringPlayer(player);
		if ( game == null )
			return;
		
		// only clear the configuring player if the closed inventory is the "current" one (decided by checking the name)
		if ( event.getInventory().getName().equals(game.configuration.inventories.get(game.configuration.currentMenu).getName()) )
			game.setConfiguringPlayer(null);		
	}
	
	private static void checkClearConfiguringPlayer(Player player)
	{
		Game game = getGameByConfiguringPlayer(player);
		if ( game != null )
		{
			game.setConfiguringPlayer(null);
			player.closeInventory();
		}
	}
	
	public static void checkEvent(PlayerChangedWorldEvent event)
	{
		checkClearConfiguringPlayer(event.getPlayer());
	}
	
	public static void checkEvent(PlayerDeathEvent event)
	{
		checkClearConfiguringPlayer(event.getEntity());
	}
	
	public static void checkEvent(PlayerQuitEvent event)
	{
		checkClearConfiguringPlayer(event.getPlayer());
	}
}

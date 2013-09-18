package com.ftwinston.KillerMinecraft;

import java.util.Arrays;

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

	Inventory rootMenu, gameModeMenu, gameModeConfigMenu, worldGenMenu, worldGenConfigMenu, playersMenu, monstersMenu, animalsMenu;
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
	
	private static String loreStyle = ChatColor.DARK_PURPLE + ChatColor.ITALIC, highlightStyle = ChatColor.YELLOW + ChatColor.ITALIC;
	Menu currentMenu = Menu.ROOT;
	
	private void createMenus()
	{
		backItem = new ItemStack(Material.WOOD_DOOR);
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
		rootMenu = Bukkit.createInventory(null, 9, game.getName() + " configuration");
		
		rootGameMode = new ItemStack(Material.CAKE);
		rootGameModeConfig = new ItemStack(Material.DISPENSER);
		rootWorldGen = new ItemStack(Material.GRASS);
		rootWorldGenConfig = new ItemStack(Material.DROPPER);
		//rootMutators = new ItemStack(Material.EXP_BOTTLE);
		rootPlayerNumbers = new ItemStack(Material.SKULL_ITEM, 1, (short)3); // steve head
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
		
		rootMenu.setItem(0, rootGameMode);
		rootMenu.setItem(1, rootGameModeConfig);
		rootMenu.setItem(2, rootWorldGen);
		rootMenu.setItem(3, rootWorldGenConfig);
		//rootMenu.setItem(4, rootMutators);
		rootMenu.setItem(6, rootPlayerNumbers);
		rootMenu.setItem(7, rootMonsters);
		rootMenu.setItem(8, rootAnimals);
	}
	
	private void createGameModeMenu()
	{
		gameModeMenu = Bukkit.createInventory(null, GameMode.gameModes.size() + 1, "Game mode selection");
		gameModeMenu.setItem(0, backItem);
		
		gameModeItems = new ItemStack[GameMode.gameModes.size()];
		for ( int i=0; i<GameMode.gameModes.size(); i++ )
		{
			GameModePlugin mode = GameMode.get(i);
			
			ItemStack item = new ItemStack(mode.getMenuIcon());
			if ( game.getGameMode().getName().equals(mode.getName()) )
			{
				List<String> lore = Arrays.asList(mode.getDescription());
				lore.set(0, highlightStyle + "(current game mode)");
				setNameAndLore(item, mode.getName(), lore);
			}
			else
				setNameAndLore(item, mode.getName(), mode.getDescription());

			gameModeItems[i] = item;
			gameModeMenu.setItem(i+1, item);
		}
	}
	
	private void createGameModeConfigMenu()
	{
		
	}
	
	private void createWorldGenMenu()
	{
		worldGenMenu = Bukkit.createInventory(null, WorldGenerator.worldGenerators.size() + 1, "World generator selection");
		gameModeMenu.setItem(0, backItem);
		
		worldGenItems = new ItemStack[WorldGenerator.worldGenerators.size()];
		for ( int i=0; i<WorldGenerator.worldGenerators.size(); i++ )
		{
			WorldGeneratorPlugin world = WorldGenerator.get(i);
			
			ItemStack item = new ItemStack(world.getMenuIcon());
			if ( game.getWorldGenerator().getName().equals(world.getName()) )
			{
				List<String> lore = Arrays.asList(world.getDescription());
				lore.set(0, highlightStyle + "(current world generator)");
				setNameAndLore(item, world.getName(), lore);
			}
			else
				setNameAndLore(item, world.getName(), world.getDescription());

			worldGenItems[i] = item;
			worldGenMenu.setItem(i+1, item);
		}
	}
	
	private void createWorldGenConfigMenu()
	{
		
	}
	
	private void createPlayersMenu()
	{
		
	}
	
	private void createMonstersMenu()
	{
		monstersMenu = Bukkit.createInventory(null, 6, "Monster numbers");
		monstersMenu.setItem(0, backItem);
		
		monstersNone = createQuantityItem(0, game.monsterNumbers == 0, "No monsters will spawn");
		monstersLow = createQuantityItem(1, game.monsterNumbers == 1, "A low number of monsters will spawn");
		monstersMed = createQuantityItem(2, game.monsterNumbers == 2, "A normal number of monsters will spawn");
		monstersHigh = createQuantityItem(3, game.monsterNumbers == 3, "A high number of monsters will spawn");
		monstersTooHigh = createQuantityItem(4, game.monsterNumbers == 4, "An excessive number of monsters will spawn");
		
		monstersMenu.setItem(1, monstersNone);
		monstersMenu.setItem(2, monstersLow);
		monstersMenu.setItem(3, monstersMed);
		monstersMenu.setItem(4, monstersHigh);
		monstersMenu.setItem(5, monstersTooHigh);
	}
	
	private void createAnimalsMenu()
	{
		animalsMenu = Bukkit.createInventory(null, 6, "Animal numbers");
		animalsMenu.setItem(0, backItem);
		
		animalsNone = createQuantityItem(0, game.animalNumbers == 0, "No animals will spawn");
		animalsLow = createQuantityItem(1, game.animalNumbers == 1, "A low number of animals will spawn");
		animalsMed = createQuantityItem(2, game.animalNumbers == 2, "A normal number of animals will spawn");
		animalsHigh = createQuantityItem(3, game.animalNumbers == 3, "A high number of animals will spawn");
		animalsTooHigh = createQuantityItem(4, game.animalNumbers == 4, "An excessive number of animals will spawn");
		
		animalsMenu.setItem(1, animalsNone);
		animalsMenu.setItem(2, animalsLow);
		animalsMenu.setItem(3, animalsMed);
		animalsMenu.setItem(4, animalsHigh);
		animalsMenu.setItem(5, animalsTooHigh);
	}

	private ItemStack createQuantityItem(int quantity, boolean selected, String lore)
	{
		ItemStack item;
		switch ( quantity )
		{
		case 0:
			item = new ItemStack(Material.STICK); break;
		case 1:
			item = new ItemStack(Material.STONE_PICKAXE); break;
		case 2:
			item = new ItemStack(Material.IRON_PICKAXE); break;
		case 3:
			item = new ItemStack(Material.DIAMOND_PICKAXE); break;
		case 4:
			item = new ItemStack(Material.DIAMOND_PICKAXE);
			item.addEnchantment(Enchantment.DURABILITY, 1); break;
			break;
		}
		
		if ( selected )
			setNameAndLore(item, GameInfoRenderer.getQuantityText(quantity), highlightStyle + "(currently selected)", lore);
		else
			setNameAndLore(item, GameInfoRenderer.getQuantityText(quantity), lore);
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
		player.openInventory(rootMenu);
	}

	public void gameModeChanged(GameMode gameMode)
	{
		setLore(rootGameMode, "(currently " + highlightStyle + gameMode.getName() + loreStyle + ")", "The game mode is the main set of rules,", "and controls every aspect of a game.");
		rootMenu.setItem(0, rootGameMode);
	}

	public void worldGeneratorChanged(WorldGenerator world)
	{
		setLore(rootWorldGen, "(currently " + highlightStyle + world.getName() + loreStyle + ")", "The world generator controls", "the terrain in the game's world(s)");
		rootMenu.setItem(2, rootWorldGen);
	}

	private void rootMenuClicked(Player player, ItemStack item)
	{
		if ( item.getType() == rootGameMode.getType() )
			player.openInventory(gameModeMenu);
		else if ( item.getType() == rootGameModeConfig.getType() )
			player.openInventory(gameModeConfigMenu);
		else if ( item.getType() == rootWorldGen.getType() )
			player.openInventory(worldGenMenu);
		else if ( item.getType() == rootWorldGenConfig.getType() )
			player.openInventory(worldGenConfigMenu);
		else if ( item.getType() == rootPlayerNumbers.getType() )
			player.openInventory(playersMenu);
		else if ( item.getType() == rootMonsters.getType() )
			player.openInventory(monstersMenu);
		else if ( item.getType() == rootAnimals.getType() )
			player.openInventory(animalsMenu);
	}

	private void gameModeMenuClicked(Player player, ItemStack item)
	{
		
	}
	
	private void gameModeConfigClicked(Player player, ItemStack item)
	{
		
	}
	
	private void worldGenMenuClicked(Player player, ItemStack item)
	{
		
	}
	
	private void worldGenConfigClicked(Player player, ItemStack item)
	{
		
	}
	
	private void playersMenuClicked(Player player, ItemStack item)
	{
		
	}
	
	private void monstersMenuClicked(Player player, ItemStack item)
	{
		
	}
	
	private void animalsMenuClicked(Player player, ItemStack item)
	{
		
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

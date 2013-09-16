package com.ftwinston.KillerMinecraft;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


class GameConfiguration
{
	Inventory rootMenu;
	private Game game;
	
	ItemStack rootGameMode, rootGameModeConfig, rootWorldGen, rootWorldGenConfig, rootPlayerNumbers, rootMonsters, rootAnimals;
	
	public GameConfiguration(Game game)
	{
		this.game = game;
		createRootMenu();
	}
	
	public void createRootMenu()
	{
		rootMenu = Bukkit.createInventory(null, 9, game.getName() + " configuration");
		
		rootGameMode = new ItemStack(Material.CAKE);
		rootGameModeConfig = new ItemStack(Material.DISPENSER);
		rootWorldGen = new ItemStack(Material.GRASS);
		rootWorldGenConfig = new ItemStack(Material.DROPPER);
		//rootMutators = new ItemStack(Material.EXP_BOTTLE);
		rootPlayerNumbers = new ItemStack(Material.SKULL_ITEM, 1, (short)3); // steve head
		rootMonsters = new ItemStack(Material.SKULL_ITEM, 1, (short)5); // creeper head
		rootAnimals = new ItemStack(Material.EGG);
		
		setNameAndLore(rootGameMode, ChatColor.RESET + "Change Game Mode", "");
		setNameAndLore(rootGameModeConfig, ChatColor.RESET + "Configure Game Mode", "Any options the current game", "mode has can be configured here");
		setNameAndLore(rootWorldGen, ChatColor.RESET + "Change World Generator", "");
		setNameAndLore(rootWorldGenConfig, ChatColor.RESET + "Configure World Generator", "Any options the current world", "generator has can be configured here");
		//setNameAndLore(rootMutators, ChatColor.RESET + "Select Mutators", "Mutators change specific aspects", "of a game, but aren't specific", "to any particular game mode");
		setNameAndLore(rootPlayerNumbers, ChatColor.RESET + "Player limits", "Specify the maximum number of", "players allowed into the game");
		setNameAndLore(rootMonsters, ChatColor.RESET + "Monster Numbers", "Control the number of", "monsters that spawn");
		setNameAndLore(rootAnimals, ChatColor.RESET + "Animal Numbers", "Control the number of", "animals that spawn");
		
		rootMenu.setItem(0, rootGameMode);
		rootMenu.setItem(1, rootGameModeConfig);
		rootMenu.setItem(2, rootWorldGen);
		rootMenu.setItem(3, rootWorldGenConfig);
		//rootMenu.setItem(4, rootMutators);
		rootMenu.setItem(6, rootPlayerNumbers);
		rootMenu.setItem(7, rootMonsters);
		rootMenu.setItem(8, rootAnimals);
	}
	
	private void setNameAndLore(ItemStack item, String name, String... lore)
	{
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		meta.setLore(Arrays.asList(lore));
		item.setItemMeta(meta);
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
		setLore(rootGameMode, "(currently " + ChatColor.YELLOW + ChatColor.ITALIC + gameMode.getName() + ChatColor.DARK_PURPLE + ChatColor.ITALIC + ")", "The game mode is the main set of rules,", "and controls every aspect of a game.", "The current mode is ");
		rootMenu.setItem(0, rootGameMode);
	}

	public void worldGeneratorChanged(WorldGenerator world)
	{
		setLore(rootWorldGen, "(currently " + ChatColor.YELLOW + ChatColor.ITALIC + world.getName() + ChatColor.DARK_PURPLE + ChatColor.ITALIC + ")", "The world generator controls", "the terrain in the game's world(s)");
		rootMenu.setItem(2, rootWorldGen);
	}

	public static void checkEvent(InventoryClickEvent event)
	{
		// TODO Auto-generated method stub	
	}

	public static void checkEvent(InventoryCloseEvent event)
	{
		// TODO Auto-generated method stub		
	}
}

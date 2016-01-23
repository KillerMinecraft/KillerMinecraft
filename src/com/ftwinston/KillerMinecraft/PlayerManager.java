package com.ftwinston.KillerMinecraft;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;


class PlayerManager
{
	private KillerMinecraft plugin;

	public PlayerManager(KillerMinecraft plugin)
	{
		this.plugin = plugin;
		
		playerDataFile = new File(plugin.getDataFolder(), "playerData.yml");
		
		if ( playerDataFile.exists() )
			playerData = YamlConfiguration.loadConfiguration(playerDataFile);
		else
			playerData = new YamlConfiguration();
	}
	
	private File playerDataFile;
	private YamlConfiguration playerData = null;
		
	public void savePlayerData(Player player)
	{
		ConfigurationSection section = playerData.getConfigurationSection(player.getName());
		if ( section != null )
			playerData.set(player.getName(), null);
		section = playerData.createSection(player.getName());
		
		//section.set("op", player.isOp());
		section.set("canFly", player.getAllowFlight());
		section.set("exhaustion", (double)player.getExhaustion());
		section.set("exp", player.getTotalExperience());
		section.set("flySpeed", (double)player.getFlySpeed());
		section.set("foodLevel", player.getFoodLevel());
		section.set("gameMode", player.getGameMode().toString());
		//section.set("healthScale", player.getHealthScale());
		//section.set("isHealthScaled", player.isHealthScaled());
		section.set("isFlying", player.isFlying());
		section.set("saturation", player.getSaturation());
		section.set("walkSpeed", (double)player.getWalkSpeed());
		writeLocation(section, "location", "locationWorld", player.getLocation());
		writeLocation(section, "spawn", "spawnWorld", player.getBedSpawnLocation());
		writeLocation(section, "compass", "compassWorld", player.getCompassTarget());
		
		section.set("listName", player.getPlayerListName());
		
		
		PlayerInventory inv = player.getInventory();
		
		section.set("helmet", inv.getHelmet());
		section.set("chest", inv.getChestplate());
		section.set("legs", inv.getLeggings());
		section.set("boots", inv.getBoots());
		
		for ( int i=0; i<inv.getSize(); i++ )
			section.set("i" + i, inv.getItem(i));
		
		player.setGameMode(GameMode.SURVIVAL);
		player.setBedSpawnLocation(null, true);
		clearInventory(player);
		
		resetPlayer(player);
	}
	
	public boolean restorePlayerData(Player player)
	{
		ConfigurationSection section = playerData.getConfigurationSection(player.getName());
		if ( section == null )
		{
			plugin.log.info("Nothing to restore for " + player.getName());
			return false;
		}
		
		resetPlayer(player);
		
		//player.setOp(section.getBoolean("op", false));
		player.setAllowFlight(section.getBoolean("canFly", false));
		player.setExhaustion((float)section.getDouble("exhaustion", 0f));
		player.setTotalExperience(section.getInt("exp", 0));
		player.setFlySpeed((float)section.getDouble("flySpeed", 1f));
		player.setFoodLevel(section.getInt("foodLevel", 20));
		
		GameMode mode;
		try
		{
			mode = GameMode.valueOf(section.getString("gameMode"));
		}
		catch (Exception e)
		{
			mode = GameMode.SURVIVAL;
		}
		player.setGameMode(mode);
		
		//player.setHealthScale(section.getDouble("healthScale", 1.0));
		//player.setHealthScaled(section.getBoolean("isHealthScaled", false));
		player.setFlying(section.getBoolean("isFlying", false));
		player.setSaturation(section.getInt("saturation", 20));
		player.setWalkSpeed((float)section.getDouble("walkSpeed", 1));
		
		Location loc = readLocation(section, "location", "locationWorld");
		if (loc == null)
			loc = Bukkit.getServer().getWorlds().get(0).getSpawnLocation();
		Helper.teleport(player, loc);
		
		player.setBedSpawnLocation(readLocation(section, "spawn", "spawnWorld"));
		player.setCompassTarget(readLocation(section, "compass", "compassWorld"));
		
		String name = section.getString("listName", null);
		if ( name != null )
			player.setPlayerListName(name);
		
		
		clearInventory(player);
		PlayerInventory inv = player.getInventory();
		
		ItemStack stack = section.getItemStack("helmet");
		if ( stack != null )
			inv.setHelmet(stack);
		
		stack = section.getItemStack("chest");
		if ( stack != null )
			inv.setChestplate(stack);
		
		stack = section.getItemStack("legs");
		if ( stack != null )
			inv.setLeggings(stack);
		
		stack = section.getItemStack("boots");
		if ( stack != null )
			inv.setBoots(stack);
				
		for ( int i=0; i<inv.getSize(); i++ )
		{
			stack = section.getItemStack("i" + i);
			if ( stack != null )
				inv.setItem(i, stack);
		}
		
		playerData.set(player.getName(), null);
		return true;
	}
	
	public boolean hasDataToRestore(Player player)
	{		
		ConfigurationSection section = playerData.getConfigurationSection(player.getName());
		return section != null;
	}
	
	void playerDataChanged()
	{
		try
		{
			playerData.save(playerDataFile);
		}
		catch ( IOException ex )
		{
			plugin.log.warning("Unable to save playerData.yml file: " + ex.getMessage());
		}
	}
	
	private void writeLocation(ConfigurationSection section, String locKey, String worldKey, Location loc)
	{
		String worldName = loc == null ? null : loc.getWorld().getName();
		Vector vec = loc == null ? null : loc.toVector();
		
		section.set(locKey, vec);
		section.set(worldKey, worldName);
	}
	
	private Location readLocation(ConfigurationSection section, String locKey, String worldKey)
	{
		Vector pos = section.getVector(locKey);
		String worldName = section.getString(worldKey);
		if (pos == null || worldName == null)
			return null;
		
		World world = Bukkit.getServer().getWorld(worldName);
		if ( world == null )
			return null;
		
		return pos.toLocation(world);
	}
	
	public void clearInventory(Player player) 
	{
		PlayerInventory inv = player.getInventory();
		inv.clear();
		inv.setHelmet(null);
		inv.setChestplate(null);
		inv.setLeggings(null);
		inv.setBoots(null);
	}

	public void resetPlayer(Player player)
	{
		if ( player.isDead() )
			return;
		
		player.setTotalExperience(0);
		
		player.setHealth(player.getMaxHealth());
		player.setFoodLevel(20);
		player.setSaturation(20);
		player.setExhaustion(0);
		player.setFireTicks(0);
		player.setSleepingIgnored(false);
		
		clearInventory(player);
		
		for (PotionEffectType p : PotionEffectType.values())
		     if (p != null && player.hasPotionEffect(p))
		          player.removePotionEffect(p);
		
		player.closeInventory(); // this stops them from keeping items they had in (e.g.) a crafting table
	}
}

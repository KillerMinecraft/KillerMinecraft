package com.ftwinston.KillerMinecraft.CraftBukkit;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.ftwinston.KillerMinecraft.KillerMinecraft;

// a holder for everything that dives into the versioned CraftBukkit code that will break with every minecraft update

public abstract class CraftBukkitAccess
{
	public static CraftBukkitAccess createCorrectVersion(Plugin plugin)
	{
        // Get full package string of CraftServer class, then extract the version name from that 
        // org.bukkit.craftbukkit.versionstring (or for pre-refactor, just org.bukkit.craftbukkit)
		String packageName = plugin.getServer().getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);

        if ( version.equals("v1_8_R3"))
        	return new v1_8_8(plugin);
        else
        	plugin.getLogger().warning("This version of Killer minecraft is not compatible with your server's version of CraftBukkit! (" + version + ") Please download a newer version of Killer minecraft.");
        return null;
	}
	
	protected CraftBukkitAccess(Plugin plugin) { this.plugin = plugin; }
	
	protected Plugin plugin;
	
	@SuppressWarnings("unchecked")
	protected <T> T getField(Class<?> declaringClass, Object o, String fieldName)
	{
		T result;
		try
		{
			Field field = declaringClass.getDeclaredField(fieldName);
			field.setAccessible(true);
			result = (T)field.get(o);
		}
		catch (Throwable t)
		{
			KillerMinecraft.instance.log.info("KillerMinecraft private field access error:");
			t.printStackTrace();
			return null;
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	protected HashMap regionfiles;
	protected Field rafField;
	
	public abstract String getDefaultLevelName();

	public abstract YamlConfiguration getBukkitConfiguration();
	public abstract void saveBukkitConfiguration(YamlConfiguration configuration);
	public abstract String getServerProperty(String name, String defaultVal);
	public abstract void setServerProperty(String name, String value);	
	public abstract void saveServerPropertiesFile();
		
	//public abstract void sendForScoreboard(Player viewer, String name, boolean show);
	public abstract void sendForScoreboard(Player viewer, Player other, boolean show);
	public abstract void forceRespawn(final Player player);
	
	public abstract void bindRegionFiles();
	public void unbindRegionFiles()
	{
		regionfiles = null;
		rafField = null;
	}
	
	public abstract boolean clearWorldReference(String worldName);
	
	public abstract void accountForDefaultWorldDeletion(World newDefault);
	
	public abstract World createWorld(org.bukkit.WorldType type, Environment env, String name, long seed, ChunkGenerator generator, String generatorSettings, boolean generateStructures);
	public abstract void finishCreateWorld(org.bukkit.World world, org.bukkit.WorldType type, Environment env, String name, long seed, ChunkGenerator generator, String generatorSettings, boolean generateStructures);
	
	public abstract boolean isChunkGenerated(Chunk chunk);
	
	public abstract Location findNearestNetherFortress(Location loc);
	public abstract boolean createFlyingEnderEye(Player player, Location target);
	
	public abstract void pushButton(Block b);
	public abstract void setCommandBlockCommand(Block b, String command);

	public abstract ItemStack setEnchantmentGlow(ItemStack item);
	
	public abstract void generateVillage(Location loc, Random random, int radius);
}

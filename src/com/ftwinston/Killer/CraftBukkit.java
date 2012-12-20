package com.ftwinston.Killer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import net.minecraft.server.v1_4_5.MinecraftServer;
import net.minecraft.server.v1_4_5.Packet201PlayerInfo;
import net.minecraft.server.v1_4_5.Packet205ClientCommand;
import net.minecraft.server.v1_4_5.ServerConfigurationManagerAbstract;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_4_5.CraftServer;
import org.bukkit.craftbukkit.v1_4_5.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// a holder for everything that dives into the versioned CraftBukkit code that will break with every minecraft update

class CraftBukkit
{	
	static MinecraftServer getMinecraftServer(Plugin plugin)
	{
		try
		{
			CraftServer server = (CraftServer)plugin.getServer();
			Field f = server.getClass().getDeclaredField("console");
			f.setAccessible(true);
			MinecraftServer console = (MinecraftServer)f.get(server);
			f.setAccessible(false);
			return console;
		}
		catch ( IllegalAccessException ex )
		{
		}
		catch  ( NoSuchFieldException ex )
		{
		}
		
		return null;
	}

	public static ServerConfigurationManagerAbstract getServerConfigurationManager(Killer plugin)
	{
		return getMinecraftServer(plugin).getServerConfigurationManager();
	}
	
	public static String getDefaultLevelName(Plugin plugin)
	{
		return getMinecraftServer(plugin).getPropertyManager().getString("level-name", "world");
	}

	public static YamlConfiguration getBukkitConfiguration(Plugin plugin)
	{
		YamlConfiguration config = null;
		try
		{
			Field configField = CraftServer.class.getDeclaredField("configuration");
        	configField.setAccessible(true);
        	config = (YamlConfiguration)configField.get((CraftServer)plugin.getServer());
			configField.setAccessible(false);
		}
		catch ( IllegalAccessException ex )
		{
		}
		catch  ( NoSuchFieldException ex )
		{
		}
		return config;
	}
	
	public static void saveBukkitConfiguration(Plugin plugin, YamlConfiguration configuration)
	{
		try
		{
			configuration.save((File)CraftBukkit.getMinecraftServer(plugin).options.valueOf("bukkit-settings"));
		}
		catch ( IOException ex )
		{
		}
	}
	
	public static String getServerProperty(Plugin plugin, String name, String defaultVal)
	{
		return getMinecraftServer(plugin).getPropertyManager().properties.getProperty(name, defaultVal);
	}
	
	public static void setServerProperty(Plugin plugin, String name, String value)
	{
		getMinecraftServer(plugin).getPropertyManager().properties.put(name, value);
	}
	
	public static void saveServerPropertiesFile(Plugin plugin)
	{
		getMinecraftServer(plugin).getPropertyManager().savePropertiesFile();
	}
	
	
	public static void sendForScoreboard(Player viewer, String name, boolean show)
	{
		((CraftPlayer)viewer).getHandle().netServerHandler.sendPacket(new Packet201PlayerInfo(name, show, 9999));
	}
	
	public static void sendForScoreboard(Player viewer, Player other, boolean show)
	{
		((CraftPlayer)viewer).getHandle().netServerHandler.sendPacket(new Packet201PlayerInfo(other.getPlayerListName(), show, show ? ((CraftPlayer)other).getHandle().ping : 9999));
	}
	
	public static void forceRespawn(Plugin plugin, final Player player)
    {
    	plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                Packet205ClientCommand packet = new Packet205ClientCommand();
                packet.a = 1;
                ((CraftPlayer) player).getHandle().netServerHandler.a(packet);
            }
        }, 1);
	}
}

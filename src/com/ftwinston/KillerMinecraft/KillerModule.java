package com.ftwinston.KillerMinecraft;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.Event;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.scheduler.BukkitScheduler;


public abstract class KillerModule implements Listener
{
	KillerMinecraft plugin; KillerModulePlugin modulePlugin;
	public final KillerModulePlugin getPlugin() { return modulePlugin; }
	
	public final BukkitScheduler getScheduler() { return plugin.getServer().getScheduler(); }
	
	Game game;
	protected Game getGame() { return game; }
	
	String name; 
	public String getName() 
	{
		return name;
	}

	final void initialize(Game game, KillerModulePlugin myPlugin)
	{
		this.game = game;
		plugin = game.plugin;
		modulePlugin = myPlugin;
		name = modulePlugin.getDisplayName();
		options = setupOptions();
	}
	
	Map<Class<? extends Event>, Set<RegisteredListener>> eventHandlers = new HashMap<Class<? extends Event>, Set<RegisteredListener>>();
	
	Option[] options;
	protected abstract Option[] setupOptions();
	
	public final Option findOption(String name)
	{
		name = name.replace('-', ' ');
		for ( Option option : options )
			if ( option.getName().equalsIgnoreCase(name) )
				return option;
		return null;
	}
	
	public final LinkedList<Player> getOnlinePlayers()
	{		
		return game.getOnlinePlayers(new PlayerFilter());
	}
	
	public final LinkedList<Player> getOnlinePlayers(PlayerFilter filter)
	{
		return game.getOnlinePlayers(filter);
	}
	
	public final LinkedList<OfflinePlayer> getOfflinePlayers(PlayerFilter filter)
	{		
		return game.getOfflinePlayers(filter);
	}
	
	public final LinkedList<OfflinePlayer> getPlayers(PlayerFilter filter)
	{		
		return game.getPlayers(filter);
	}
	
	public final void broadcastMessage(String message)
	{
		game.broadcastMessage(message);
	}
	
	public final void broadcastMessage(PlayerFilter recipients, String message)
	{
		game.broadcastMessage(recipients, message);
	}
}

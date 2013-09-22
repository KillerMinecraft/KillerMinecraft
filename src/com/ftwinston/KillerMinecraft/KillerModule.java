package com.ftwinston.KillerMinecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.event.Listener;
import org.bukkit.event.Event;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.scheduler.BukkitScheduler;


public abstract class KillerModule implements Listener
{
	KillerMinecraft plugin; KillerModulePlugin modulePlugin;
	protected final KillerModulePlugin getPlugin() { return modulePlugin; }
	
	protected final BukkitScheduler getScheduler() { return plugin.getServer().getScheduler(); }
	
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
		name = myPlugin.getName();
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
}

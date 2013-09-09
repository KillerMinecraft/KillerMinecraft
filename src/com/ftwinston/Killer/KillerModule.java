package com.ftwinston.Killer;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public abstract class KillerModule implements Listener
{
	Killer plugin; JavaPlugin modulePlugin;
	protected final JavaPlugin getPlugin() { return plugin; }
	
	protected final BukkitScheduler getScheduler() { return plugin.getServer().getScheduler(); }
	
	Game game;
	protected Game getGame() { return game; }
	
	String name; 
	public String getName() 
	{
		return name;
	}
	
	public abstract String describe();

	final void initialize(Game game, JavaPlugin myPlugin)
	{
		this.game = game;
		plugin = game.plugin;
		modulePlugin = myPlugin;
		name = myPlugin.getName();
		options = setupOptions();
	}
		
	private Option[] options;
	protected abstract Option[] setupOptions();
	public final Option[] getOptions() { return options; }
	public final Option getOption(int num) { return options[num]; }
	public final int getNumOptions() { return options.length; }
		
	public void toggleOption(int num)
	{
		Option option = options[num];
		option.setEnabled(!option.isEnabled());
	}
	
	public final Option findOption(String name)
	{
		name = name.replace('-', ' ');
		for ( Option option : options )
			if ( option.getName().equalsIgnoreCase(name) )
				return option;
		return null;
	}
}

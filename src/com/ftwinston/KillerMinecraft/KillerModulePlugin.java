package com.ftwinston.KillerMinecraft;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class KillerModulePlugin extends JavaPlugin
{
	public abstract String describe();
	
	public abstract Material getMenuIcon();
}

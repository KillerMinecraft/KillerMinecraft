package com.ftwinston.KillerMinecraft;

import java.util.List;

import org.bukkit.World.Environment;

public abstract class WorldGeneratorPlugin extends KillerModulePlugin
{
	public void onEnable()
	{
		KillerMinecraft.registerPlugin(this);
	}
	
	public abstract Environment getWorldType();
	
	public abstract WorldGenerator createInstance();
	
	@Override
	final void initialize(KillerMinecraft plugin)
	{
		// keep the world options in alphabetic order
		List<WorldGeneratorPlugin> pluginList;
		switch (getWorldType())
		{
		case NORMAL:
			pluginList = WorldGenerator.overworldGenerators; break;
		case NETHER:
			pluginList = WorldGenerator.netherGenerators; break;
		case THE_END:
			pluginList = WorldGenerator.endGenerators; break;
		default:
			return;
		}
		
		String name = getName();
		for ( int i=0; i<pluginList.size(); i++ )
			if ( name.compareToIgnoreCase(pluginList.get(i).getName()) < 0 )
			{
				pluginList.add(i, this);
				return;
			}
		pluginList.add(this);
	}
}
package com.ftwinston.KillerMinecraft;

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
		String name = getName();
		for ( int i=0; i<WorldGenerator.worldGenerators.size(); i++ )
			if ( name.compareToIgnoreCase(WorldGenerator.worldGenerators.get(i).getName()) < 0 )
			{
				WorldGenerator.worldGenerators.add(i, this);
				return;
			}
		WorldGenerator.worldGenerators.add(this);
	}
}
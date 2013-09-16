package com.ftwinston.KillerMinecraft;

public abstract class WorldGeneratorPlugin extends KillerModulePlugin
{
	public void onEnable()
	{
		KillerMinecraft.registerWorldGenerator(this);
	}
	
	public abstract WorldGenerator createInstance();
	
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
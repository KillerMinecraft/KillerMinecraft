package com.ftwinston.Killer.WorldOptions;

import org.bukkit.WorldType;

import com.ftwinston.Killer.Option;
import com.ftwinston.Killer.WorldConfig;

public class DefaultWorld extends com.ftwinston.Killer.WorldOption
{
	public static final int largeBiomes = 0;

	@Override
	public Option[] setupOptions()
	{
		Option[] options = {
			new Option("Use large biomes", false)
		};
		
		return options;
	}
	
	@Override
	public void setupWorld(WorldConfig world, Runnable runWhenDone)
	{
		if ( getOption(largeBiomes).isEnabled() )
			world.setWorldType(WorldType.LARGE_BIOMES);
		createWorld(world, runWhenDone);
	}
}

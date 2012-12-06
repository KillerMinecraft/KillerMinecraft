package com.ftwinston.Killer.WorldOptions;

import org.bukkit.World.Environment;
import org.bukkit.WorldType;

import com.ftwinston.Killer.Option;
import com.ftwinston.Killer.WorldConfig;

public class Superflat extends com.ftwinston.Killer.WorldOption
{
	public static final int classicFlat = 0, tunnelersDream = 1, waterWorld = 2, overworld = 3, snowyKingdom = 4;
	
	@Override
	public Option[] setupOptions()
	{
		Option[] options = {
			new Option("Classic Flat", true),
			new Option("Tunnelers' Dream", false),
			new Option("Water World", false),
			new Option("Overworld", false),
			new Option("Snowy Kingdom", false)
		};
		
		return options;
	}
	
	@Override
	public void toggleOption(int num)
	{
		super.toggleOption(num);
		Option.ensureOnlyOneEnabled(getOptions(), num, classicFlat, tunnelersDream, waterWorld, overworld, snowyKingdom);
	}
	
	@Override
	public void setupWorld(WorldConfig world, Runnable runWhenDone)
	{
		if ( world.getEnvironment() == Environment.NORMAL )
		{
			world.setWorldType(WorldType.FLAT);
			
			if ( getOption(tunnelersDream).isEnabled() )
				;
			else if ( getOption(waterWorld).isEnabled() )
				;
			else if ( getOption(overworld).isEnabled() )
				;
			else if ( getOption(snowyKingdom).isEnabled() )
				;
		}
		
		createWorld(world, runWhenDone);
	}
}

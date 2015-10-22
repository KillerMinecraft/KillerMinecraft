package com.ftwinston.KillerMinecraft;

import java.util.ArrayList;

import org.bukkit.inventory.Recipe;

public abstract class GameModePlugin extends KillerModulePlugin
{
	public void onEnable()
	{
		KillerMinecraft.registerPlugin(this);
	}
	
	public abstract GameMode createInstance();
	
	@Override
	final void initialize(KillerMinecraft plugin)
	{	
		plugin.recipeManager.registerCustomRecipes(createCustomRecipes(), this);
		
		// keep the game modes in alphabetic order
		String name = getName();
		for ( int i=0; i<GameMode.gameModes.size(); i++ )
			if ( name.compareToIgnoreCase(GameMode.gameModes.get(i).getName()) < 0 )
			{
				GameMode.gameModes.add(i, this);
				return;
			}
		GameMode.gameModes.add(this);
	}
	
	protected ArrayList<Recipe> createCustomRecipes() { return null; }
}
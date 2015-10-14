package com.ftwinston.KillerMinecraft;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class KillerModulePlugin extends JavaPlugin
{
	public abstract String[] getDescriptionText();
	
	public abstract Material getMenuIcon();
	
	String concatenateDescription(boolean includeName)
	{
		StringBuilder sb = new StringBuilder();
		if ( includeName )
		{
			sb.append(getName());
			sb.append('\n');
		}
		String[] desc = getDescriptionText();
		
		if ( desc != null )
		{
			if ( desc.length > 0 )
				sb.append(desc[0]);
			for ( int i=1; i<desc.length; i++)
			{
				sb.append(' ');
				sb.append(desc[i]);
			}
		}
		return sb.toString();
	}
	
	public String getDisplayName() { return getName().replace('_', ' '); }
}

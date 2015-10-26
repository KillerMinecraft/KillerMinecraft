package com.ftwinston.KillerMinecraft.Configuration;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.ftwinston.KillerMinecraft.Option;

public class ToggleOption extends Option
{
	public ToggleOption(String name, boolean enabledByDefault, String... description)
	{
		super(name);
		setEnabled(enabledByDefault);
		
		this.description = new String[description.length+1];
		for ( int i=0; i<description.length; i++ )
			this.description[i+1] = description[i];
	}

	private String[] description;
	
	@Override
	protected ItemStack getDisplayStack() { return new ItemStack(enabled ? Material.LAVA_BUCKET : Material.BUCKET); }
	
	@Override
	protected String[] getDescription()
	{
		description[0] = isEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled";
		return description;
	}
	
	@Override
	protected boolean trySetValue(String value)
	{
		setEnabled(Boolean.parseBoolean(value));
		return true;
	}
	
	@Override
	protected String getValueString()
	{
		return Boolean.toString(enabled);
	}
	
	@Override
	protected ItemStack[] optionClicked() { toggle(); return null; }
	
	private boolean enabled;
	public boolean isEnabled() { return enabled; }
	protected void setEnabled(boolean enabled) { this.enabled = enabled; setSelectedIndex(enabled ? 1 : 0); }
	
	public void toggle()
	{
		if ( enableOneIfAllDisabled != null && isEnabled() )
		{
			boolean anyEnabled = false;
			for ( ToggleOption option : enableOneIfAllDisabled )
				if ( option.isEnabled() )
				{
					anyEnabled = true;
					break;
				}
			
			if (!anyEnabled && enableOneIfAllDisabled.size() > 0 )
				enableOneIfAllDisabled.get(0).toggle();
		}
		
		setEnabled(!enabled);
		
		if (disableWhenThisEnabled != null && isEnabled())
			for ( ToggleOption option : disableWhenThisEnabled )
				option.setEnabled(false);
	}
		
	private ArrayList<ToggleOption> disableWhenThisEnabled;
	private ArrayList<ToggleOption> enableOneIfAllDisabled;
					
	public static void ensureOnlyOneEnabled(ToggleOption... options)
	{
		for ( ToggleOption option : options ) 
		{
			option.disableWhenThisEnabled = new ArrayList<ToggleOption>();
			
			for ( ToggleOption other : options )
				if ( other != option )
					option.disableWhenThisEnabled.add(other);
		}
		
		ensureAtLeastOneEnabled(options);
	}
	
	public static void ensureAtLeastOneEnabled(ToggleOption... options)
	{
		for ( ToggleOption option : options ) 
		{
			option.enableOneIfAllDisabled = new ArrayList<ToggleOption>();
			
			for ( ToggleOption other : options )
				if ( other != option )
					option.enableOneIfAllDisabled.add(other);
		}		
	}
}

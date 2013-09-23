package com.ftwinston.KillerMinecraft.Configuration;

import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.ftwinston.KillerMinecraft.KillerMinecraft;
import com.ftwinston.KillerMinecraft.Option;

public class ChoiceOption extends Option
{
	public ChoiceOption(String name, String[] values, Material[] icons, int selectedIndex)
	{
		super(name);
		
		if ( values.length != icons.length )
		{
			KillerMinecraft.instance.log.warning("Value and Icon array parameters don't have the same length for choice option '" + name + "'");
		}
		
		if ( selectedIndex < 0 )
			selectedIndex = 0;
		else if ( selectedIndex >= values.length )
			selectedIndex = values.length - 1;
		setSelectedIndex(selectedIndex);
		
		this.values = values;
		this.icons = icons;
	}
	
	private String[] values;
	private Material[] icons;
	
	@Override
	protected Material getDisplayMaterial() { return icons[getSelectedIndex()]; }

	@Override
	protected String[] getDescription() {
		return new String[] { ChatColor.YELLOW + "Current value: " + getValue(), "<no description available>" };
	}

	@Override
	protected boolean trySetValue(String value)
	{
		for ( int i=0; i<values.length; i++ )
			if ( value.equalsIgnoreCase(values[i]))
			{
				setSelectedIndex(i);
				return true;
			}
		
		return false;
	}
	
	@Override
	protected String getValueString()
	{
		return getValue();
	}
	
	public String getValue()
	{
		return values[getSelectedIndex()];
	}
	
	final int maxNumItems = 34; 
	
    @Override
    public ItemStack[] optionClicked()
    {
    	int numItems = Math.min(values.length, maxNumItems);
    	ItemStack[] items = new ItemStack[numItems];
    	
    	for ( int i=0; i<numItems; i++ )
    	{
    		ItemStack item = new ItemStack(icons[i]);
    		
    		ItemMeta meta = item.getItemMeta();
    		
    		meta.setDisplayName(ChatColor.RESET + values[i]);
    		
    		if ( i == getSelectedIndex() )
    			meta.setLore(Arrays.asList("" + ChatColor.YELLOW + ChatColor.ITALIC + "Current value", "<no description available>"));
    		else
    			meta.setLore(Arrays.asList("<no description available>"));
    		
    		item.setItemMeta(meta);
    		
    		items[i] = item;
    	}
    	
    	return items;
    }
}

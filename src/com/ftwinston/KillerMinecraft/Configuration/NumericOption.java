package com.ftwinston.KillerMinecraft.Configuration;

import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.ftwinston.KillerMinecraft.Option;

public class NumericOption extends Option
{
	public NumericOption(String name, int min, int max, Material icon, int defaultVal, String... description)
	{
		super(name);
		
		if ( max < min )
		{
			this.min = max;
			this.max = min;
		}
		else
		{
			this.min = min;
			this.max = max;
		}

		int val = Math.min(max, Math.max(min, defaultVal));
		setSelectedIndex(val-min);
			
		this.icon = icon;
		
		this.description = new String[description.length+1];
		for ( int i=0; i<description.length; i++ )
			this.description[i+1] = description[i];
	}
	
	private int min, max, value;
	private Material icon;
	private String[] description;
	
	protected void setValue(int newVal)
	{
		setSelectedIndex(value-min);
	}
	
	@Override
	protected void setSelectedIndex(int index)
	{
		super.setSelectedIndex(index);
		value = index + min;
	}
	
	@Override
	protected Material getDisplayMaterial() { return icon; }

	@Override
	protected String[] getDescription() {
		description[0] = ChatColor.YELLOW + "Current value: " + getValue();
		return description;
	}

	@Override
	protected boolean trySetValue(String value)
	{
		int val = Integer.parseInt(value);
		
		if ( val >= min && val <= max )
		{
			setValue(val);
			return true;
		}
		
		return false;
	}
	
	@Override
	protected String getValueString()
	{
		return Integer.toString(getValue());
	}
	
	public int getValue()
	{
		return value;
	}
	
	final int maxNumItems = 34; 
	
    @Override
    public ItemStack[] optionClicked()
    {
    	int numItems = Math.min(max - min + 1, maxNumItems);
    	ItemStack[] items = new ItemStack[numItems];
    	
    	for ( int i=0; i<numItems; i++ )
    	{
			boolean selected = i == getSelectedIndex();
    		ItemStack item = new ItemStack(selected ? Material.REDSTONE_TORCH_ON : Material.REDSTONE_TORCH_OFF);
    		
    		ItemMeta meta = item.getItemMeta();
    		
    		meta.setDisplayName(ChatColor.RESET + "" + (i+min));
    		
    		if ( selected )
    			meta.setLore(Arrays.asList("" + ChatColor.YELLOW + ChatColor.ITALIC + "Current value"));
    		else
    			meta.setLore(Arrays.asList("Click to change the", "value to this number"));
    		
    		item.setItemMeta(meta);
    		
    		items[i] = item;
    	}
    	
    	return items;
    }
}

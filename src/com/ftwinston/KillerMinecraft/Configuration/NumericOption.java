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
		this(name, min, max, icon, defaultVal, 1, description);
	}
	
	public NumericOption(String name, int min, int max, Material icon, int defaultVal, int interval, String... description)
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

		this.interval = interval; 
		
		int val = Math.min(max, Math.max(min, defaultVal));
		setSelectedIndex(val-min);
			
		this.icon = icon;
		
		this.description = new String[description.length+1];
		for ( int i=0; i<description.length; i++ )
			this.description[i+1] = description[i];
	}
	
	private int min, max, value, interval;
	private Material icon;
	private String[] description;
	
	public void setValue(int newVal)
	{
		setSelectedIndex((value * interval) - min);
	}
	
	@Override
	protected void setSelectedIndex(int index)
	{
		value = (index * interval) + min;
		super.setSelectedIndex(index);
	}
	
	@Override
	protected ItemStack getDisplayStack() { return new ItemStack(icon); }

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
    	int numItems = Math.min((max - min)/interval + 1, maxNumItems);
    	ItemStack[] items = new ItemStack[numItems];
    	
    	for ( int i=0; i<numItems; i++ )
    	{
			boolean selected = i == getSelectedIndex();
			int num = (i * interval) + min;
    		ItemStack item = new ItemStack(selected ? Material.REDSTONE_BLOCK : Material.COAL_BLOCK, num > 1 && num <= 64 ? num : 1);
    		
    		ItemMeta meta = item.getItemMeta();
    		
    		meta.setDisplayName(ChatColor.RESET + "" + num);
    		
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

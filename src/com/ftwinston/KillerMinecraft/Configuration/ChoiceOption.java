package com.ftwinston.KillerMinecraft.Configuration;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.ftwinston.KillerMinecraft.Option;

public class ChoiceOption<T extends Enum<T>> extends Option
{
	public ChoiceOption(String name)
	{
		super(name);
	}
	
	private ArrayList<Choice> choices = new ArrayList<Choice>();
	
	@Override
	protected Material getDisplayMaterial() { return choices.get(getSelectedIndex()).icon; }

	@Override
	protected String[] getDescription() {
		Choice c = choices.get(getSelectedIndex());
		
		String[] desc = new String[c.description.length+1];
		desc[0] = ChatColor.YELLOW + "Current value: " + c.name;
		
		for ( int i=0; i<c.description.length; i++ )
			desc[i+1] = c.description[i];
		
		return desc;
	}

	@Override
	protected boolean trySetValue(String value)
	{
		for ( int i=0; i<choices.size(); i++ )
			if ( value.equalsIgnoreCase(choices.get(i).name))
			{
				setSelectedIndex(i);
				return true;
			}
		
		return false;
	}
	
	@Override
	protected String getValueString()
	{
		return choices.get(getSelectedIndex()).name;
	}
	
	public T getValue()
	{
		return choices.get(getSelectedIndex()).value;
	}

	public void setValue(T value)
	{
		for ( int i=0; i<choices.size(); i++ )
			if ( choices.get(i).value == value )
			{
				setSelectedIndex(i);
				break;
			}
	}
	
	public void addChoice(String name, T value, Material icon, String... description)
	{
		choices.add(new Choice(name, value, icon, description));
	}
	
	final int maxNumItems = 34; 
	
    @Override
    public ItemStack[] optionClicked()
    {
    	int numItems = Math.min(choices.size(), maxNumItems);
    	ItemStack[] items = new ItemStack[numItems];
    	
    	for ( int i=0; i<numItems; i++ )
    	{
    		Choice c = choices.get(i);
    		ItemStack item = new ItemStack(c.icon);
    		
    		ItemMeta meta = item.getItemMeta();
    		
    		meta.setDisplayName(ChatColor.RESET + c.name);
    		
    		ArrayList<String> desc = new ArrayList<String>(c.description.length+1);
    		for ( int j=0; j<c.description.length; j++ )
    			desc.add(c.description[j]);
    		if ( i == getSelectedIndex() )
    			desc.add(0, "" + ChatColor.YELLOW + ChatColor.ITALIC + "Current value");
    		meta.setLore(desc);
    		
    		item.setItemMeta(meta);
    		
    		items[i] = item;
    	}
    	
    	return items;
    }
    
    class Choice
    {
    	Choice(String name, T value, Material icon, String[] description)
    	{
    		this.name = name;
    		this.value = value;
    		this.icon = icon;
    		this.description = description;
    	}
    	
    	public String name;
    	public T value;
    	public Material icon;
    	public String[] description;
    }
}

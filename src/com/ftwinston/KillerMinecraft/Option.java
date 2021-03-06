package com.ftwinston.KillerMinecraft;

import org.bukkit.inventory.ItemStack;

public abstract class Option
{
	protected Option(String name)
	{
		if ( name.length() > 32 )
			this.name = name.substring(0, 32);
		else
			this.name = name;
	}
	
	private String name;
	public String getName() { return name; }
	
	protected abstract boolean trySetValue(String value);
	protected abstract String getValueString();
	
	protected abstract ItemStack getDisplayStack();
	protected abstract String[] getDescription(); 

	protected abstract ItemStack[] optionClicked();
	
	private int selectedIndex = 0;
	protected int getSelectedIndex() { return selectedIndex; }
	protected void setSelectedIndex(int index) { selectedIndex = index; changed(); }
	
	protected void changed() { }
	
	private boolean hidden = false;
	public boolean isHidden() { return hidden; }
	public void setHidden(boolean val) { hidden = val; }
}
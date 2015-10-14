package com.ftwinston.KillerMinecraft.Configuration;

import java.util.LinkedList;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class MenuItem
{
	public MenuItem(Inventory menu, int slot, ItemStack stack)
	{
		this.menu = menu;
		this.slot = slot;
		this.stack = stack;
		
		bind();
	}
	
	protected Inventory menu;
	protected int slot;
	protected ItemStack stack;
	
	public Inventory getMenu() { return menu; }
	public int getSlot() { return slot; }
	
	public ItemStack getStack() { return stack; }
	public void setStack(ItemStack stack) { this.stack = stack; bind(); }

	protected void bind() { menu.setItem(slot, stack); }
	public void recalculateStack() { }
	
	private LinkedList<MenuItem> itemsToRecalculate = new LinkedList<MenuItem>();
	public void recalculateOnClick(MenuItem... items)
	{
		for (MenuItem item : items)
			itemsToRecalculate.add(item);
	}
	
	public final void clicked(Player player)
	{
		runWhenClicked(player);
		
		// recalculate all the menu items this has linked to recalculate on click
		for (MenuItem item : itemsToRecalculate)
			item.recalculateStack();
	}
	protected abstract void runWhenClicked(Player player);
}
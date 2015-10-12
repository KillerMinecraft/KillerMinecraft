package com.ftwinston.KillerMinecraft.Configuration;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class MenuItem
{
	public MenuItem(ItemStack item) { this.item = item; }
	protected ItemStack item;
	
	public ItemStack getStack() { return item; }
	public void setStack(ItemStack item) { this.item = item; }
	
	public abstract void runWhenClicked(Player player);
}
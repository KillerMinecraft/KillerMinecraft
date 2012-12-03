package com.ftwinston.Killer;

import net.minecraft.server.NBTTagCompound;

import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class NamedItemStack {

	private final ItemStack itemStack;
	
	public NamedItemStack(ItemStack itemStack) {
		this.itemStack = itemStack;
		CraftItemStack cis = ((CraftItemStack)this.itemStack);
		NBTTagCompound tag = cis.getHandle().getTag();
		if (tag == null) {
			cis.getHandle().setTag(new NBTTagCompound());
		}
	}
	
	public ItemStack getItem() { return itemStack; }
	
	private boolean hasDisplay() {
		return ((CraftItemStack)this.itemStack).getHandle().getTag().hasKey("display");
	}
	
	private NBTTagCompound getDisplay() {
		return ((CraftItemStack)this.itemStack).getHandle().getTag().getCompound("display");
	}
	
	private void addDisplay() {
		((CraftItemStack)this.itemStack).getHandle().getTag().setCompound("display", new NBTTagCompound());
	}
	
	public String getName() {
		if (hasDisplay() == false) {
			return null;
		}
		String name = getDisplay().getString("Name");
		if (name.equals("")) {
			return null;
		}
		return name;
	}
	
	public void setName(String name) {
		if (hasDisplay() == false) {
			this.addDisplay();
		}
		NBTTagCompound display = this.getDisplay();
		if (name == null) {
			display.remove("Name");
		}
		display.setString("Name", name);
	}
}
package com.ftwinston.KillerMinecraft.Configuration;

import org.bukkit.ChatColor;
import org.bukkit.Color;

public abstract class TeamInfo
{
	public abstract String getName();
	public boolean allowTeamChat() { return true; }
	public ChatColor getChatColor() { return ChatColor.RESET; }
	public Color getArmorColor() { return Color.WHITE; }
	public byte getWoolColor() { return (byte)0x0; }
}

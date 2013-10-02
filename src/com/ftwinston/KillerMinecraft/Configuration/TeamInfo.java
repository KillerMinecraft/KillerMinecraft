package com.ftwinston.KillerMinecraft.Configuration;

import org.bukkit.ChatColor;
import org.bukkit.Color;

public class TeamInfo
{
	public TeamInfo(String name, ChatColor chatColor, Color armorColor, byte woolColor)
	{
		this.name = name;
		this.chatColor = chatColor;
		this.armorColor = armorColor;
		this.woolColor = woolColor;
	}
	
	private String name;
	private ChatColor chatColor;
	private Color armorColor;
	private byte woolColor;
	
	public String getName() { return name; }
	public ChatColor getChatColor() { return chatColor; }
	public Color getArmorColor() { return armorColor; }
	public byte getWoolColor() { return woolColor; }
	
	public void setName(String name) { this.name = name; }
	public void setChatColor(ChatColor color) { chatColor = color; }
	public void setArmorColor(Color color) { armorColor = color; }
	public void setWoolColor(byte color) { woolColor = color; }

	public final static TeamInfo Red = new TeamInfo("red team", ChatColor.RED, Color.RED, (byte)0xE);
	public final static TeamInfo Blue = new TeamInfo("blue team", ChatColor.BLUE, Color.fromRGB(0x0066FF), (byte)0xB);
	public final static TeamInfo Yellow = new TeamInfo("yellow team", ChatColor.YELLOW, Color.YELLOW, (byte)0x4);
	public final static TeamInfo Green = new TeamInfo("green team", ChatColor.GREEN, Color.GREEN, (byte)0x5);
	public final static TeamInfo Purple = new TeamInfo("purple team", ChatColor.DARK_PURPLE, Color.PURPLE, (byte)0xA);
	public final static TeamInfo Aqua = new TeamInfo("aqua team", ChatColor.AQUA, Color.AQUA, (byte)0x3);
	public final static TeamInfo White = new TeamInfo("white team", ChatColor.WHITE, Color.WHITE, (byte)0x0);
	public final static TeamInfo DarkGrey = new TeamInfo("dark grey team", ChatColor.DARK_GRAY, Color.fromRGB(0x3F3F3F), (byte)0x7);
	public final static TeamInfo LightGrey = new TeamInfo("light grey team", ChatColor.GRAY, Color.fromRGB(0xBEBEBE), (byte)0x8);
	public final static TeamInfo Pink = new TeamInfo("pink team", ChatColor.LIGHT_PURPLE, Color.fromRGB(0xFE3FFE), (byte)0x6);
}

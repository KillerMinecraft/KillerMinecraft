package com.ftwinston.KillerMinecraft.Configuration;

import org.bukkit.ChatColor;
import org.bukkit.Color;

public class Team
{
	public Team(String name, ChatColor chatColor, Color armorColor, byte woolColor)
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
	
	public final static Team Blue = new Team("blue team", ChatColor.BLUE, Color.fromRGB(0x0066FF), (byte)0xB);
	public final static Team Red = new Team("red team", ChatColor.RED, Color.RED, (byte)0xE);
	

	public final static Team Yellow = new Team("yellow team", ChatColor.YELLOW, Color.YELLOW, (byte)0x4);
	public final static Team Green = new Team("green team", ChatColor.GREEN, Color.GREEN, (byte)0x5);
	public final static Team Purple = new Team("purple team", ChatColor.DARK_PURPLE, Color.PURPLE, (byte)0xA);
	public final static Team Aqua = new Team("aqua team", ChatColor.AQUA, Color.AQUA, (byte)0x3);
	public final static Team White = new Team("white team", ChatColor.WHITE, Color.WHITE, (byte)0x0);
	public final static Team DarkGrey = new Team("dark grey team", ChatColor.DARK_GRAY, Color.fromRGB(0x3F3F3F), (byte)0x7);
	public final static Team LightGrey = new Team("light grey team", ChatColor.GRAY, Color.fromRGB(0xBEBEBE), (byte)0x8);
	public final static Team Pink = new Team("pink team", ChatColor.LIGHT_PURPLE, Color.fromRGB(0xFE3FFE), (byte)0x6);
}

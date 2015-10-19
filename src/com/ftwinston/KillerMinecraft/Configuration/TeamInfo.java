package com.ftwinston.KillerMinecraft.Configuration;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;

public abstract class TeamInfo
{
	public abstract String getName();
	public boolean allowTeamChat() { return true; }
	public ChatColor getChatColor() { return ChatColor.RESET; }
	public Color getArmorColor() { return Color.WHITE; }
	public byte getWoolColor() { return (byte)0x0; }
	
	public Score getScoreboardScore() { return scoreboardScore; }
	public void setScoreboardScore(Score score) { scoreboardScore = score; } 
	private Score scoreboardScore = null;
	
	public Team getScoreboardTeam() { return scoreboardTeam; }
	public void setScoreboardTeam(Team team) { scoreboardTeam = team; } 
	private Team scoreboardTeam = null;
}

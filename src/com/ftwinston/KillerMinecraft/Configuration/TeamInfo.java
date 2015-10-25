package com.ftwinston.KillerMinecraft.Configuration;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;

public abstract class TeamInfo
{
	public abstract String getName();
	public boolean allowTeamChat() { return true; }
	public ChatColor getChatColor() { return ChatColor.RESET; }
	public DyeColor getDyeColor() { return DyeColor.WHITE; }
	
	public Score getScoreboardScore() { return scoreboardScore; }
	public void setScoreboardScore(Score score) { scoreboardScore = score; } 
	private Score scoreboardScore = null;
	
	public Team getScoreboardTeam() { return scoreboardTeam; }
	public void setScoreboardTeam(Team team) { scoreboardTeam = team; } 
	private Team scoreboardTeam = null;
}

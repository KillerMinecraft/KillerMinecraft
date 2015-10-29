package com.ftwinston.KillerMinecraft;

import java.util.LinkedList;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public abstract class Vote
{
	static final long voteDuration = 400; // 20 secs
	
	public Vote(Game game, String question, Player initiatedBy)
	{
		this.game = game;
		this.question = question;
		this.initiatedBy = initiatedBy == null ? null : initiatedBy.getName();
		numYesVotes = numNoVotes = 0;
		processID = -1;
	}
	
	
	Game game;
	String question, initiatedBy;
	LinkedList<String> playersWhoCanVote;
	int processID, numYesVotes, numNoVotes;
	
	protected abstract void runOnYes();
	protected abstract void runOnNo();
	protected abstract void runOnDraw();
	
	void start()
	{
		playersWhoCanVote = new LinkedList<String>();
		for (Player player : game.getOnlinePlayers())
			playersWhoCanVote.add(player.getName());
		numYesVotes = numNoVotes = 0;
		
		game.broadcastMessage(ChatColor.YELLOW + (initiatedBy == null ? "Vote started: " : initiatedBy + " started a vote: ") + ChatColor.RESET + question + ChatColor.YELLOW + "\nSay " + ChatColor.GREEN + "Y" + ChatColor.YELLOW + " to vote yes, or " + ChatColor.RED + "N" + ChatColor.YELLOW + " to vote no.");
		processID = game.plugin.getServer().getScheduler().scheduleSyncDelayedTask(game.plugin, new Runnable() { 
			public void run() {
				runResult();
			}
		}, Vote.voteDuration);
	}
	
	void runResult()
	{
		if (game.currentVote == this)
			game.currentVote = null;
		
		if ( numYesVotes > numNoVotes )
		{
			game.broadcastMessage(ChatColor.YELLOW +"Vote succeeded (" + ChatColor.GREEN + numYesVotes + ChatColor.YELLOW + " for, " + ChatColor.RED + numNoVotes + ChatColor.YELLOW + " against)");
			runOnYes();
		}
		else if ( numNoVotes > numYesVotes )
		{
			game.broadcastMessage(ChatColor.YELLOW + "Vote failed (" + ChatColor.GREEN + numYesVotes + ChatColor.YELLOW + " for, " + ChatColor.RED + numNoVotes + ChatColor.YELLOW + " against)");
			runOnNo();
		}
		else
		{
			game.broadcastMessage(ChatColor.YELLOW + "Vote tied (" + ChatColor.GREEN + numYesVotes + ChatColor.YELLOW + " for, " + ChatColor.RED + numNoVotes + ChatColor.YELLOW + " against)");
			runOnDraw();
		}
	}

	boolean placeVote(Player player, boolean choice)
	{
		if ( !playersWhoCanVote.contains(player.getName()) )
			return false; // this player already voted
		
		if ( choice )
			numYesVotes++;
		else
			numNoVotes++;
		
		playersWhoCanVote.remove(player.getName());
		
		// check for early finish
		if (playersWhoCanVote.size() != 0)
			return true;
		
		// delay this by one tick so that the vote-placement message will show up BEFORE the vote result message
		game.plugin.getServer().getScheduler().scheduleSyncDelayedTask(game.plugin, new Runnable() {
			public void run()
			{
				// stop the scheduled task
				cancelTask();
				runResult();
			}
		});
		
		return true;
	}
	
	void cancelTask()
	{
		game.plugin.getServer().getScheduler().cancelTask(processID);
		processID = -1;
	}
}

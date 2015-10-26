package com.ftwinston.KillerMinecraft;

import java.util.LinkedList;

import org.bukkit.ChatColor;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.InactivityConversationCanceller;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
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
	
	static void showVoteMenu(Player player)
	{
		Conversation convo = voteConvFactory.buildConversation(player);
		convo.begin();
	}
	
	private static ConversationFactory voteConvFactory;
	
	static void setupConversation()
	{
		final MessagePrompt cantVotePrompt = new MessagePrompt() {
			@Override
			public String getPromptText(ConversationContext context) {
				return "You cannot start a vote right now, as another vote is in progress.";
			}
			
			@Override
			protected Prompt getNextPrompt(ConversationContext context)
			{
				return Prompt.END_OF_CONVERSATION;
			}
		};
		
        final NumericPrompt initialPrompt = new NumericPrompt() {
        	
			public String getPromptText(ConversationContext context)
			{
				return "What do you want to start a vote on? Say the number to choose:\n" + ChatColor.GOLD + "1." + ChatColor.RESET + " End game\n" + ChatColor.GOLD + "0." + ChatColor.RESET + " Cancel";
			}
			
			protected Prompt acceptValidatedInput(ConversationContext context, Number val)
			{
				Player player = context.getForWhom() instanceof Player ? (Player)context.getForWhom() : null;
				final Game game = KillerMinecraft.instance.getGameForPlayer(player);
				if (game == null)
					return Prompt.END_OF_CONVERSATION;
				
				if (game.currentVote != null)
					return cantVotePrompt;
				
				int choice = val.intValue();
				
				Vote vote = null;
				if ( choice == 1 )
					vote = new Vote(game, "End the current game?", player) {
						protected void runOnYes() { game.finishGame(); }
						protected void runOnNo() {}
						protected void runOnDraw() {}
					};
				
				
				if (vote != null)
					game.startVote(vote);
				
				return Prompt.END_OF_CONVERSATION;
			}
		};
        
        voteConvFactory = new ConversationFactory(KillerMinecraft.instance);
        voteConvFactory.withFirstPrompt(initialPrompt);
        voteConvFactory.withLocalEcho(false);
        voteConvFactory.withModality(false);
        voteConvFactory.withConversationCanceller(new InactivityConversationCanceller(KillerMinecraft.instance, 30) {
        	@Override
        	protected void cancelling(Conversation conversation) {
        		Player player = conversation.getForWhom() instanceof Player ? (Player)conversation.getForWhom() : null;
    			if ( player != null )
    				player.sendMessage("Vote setup cancelled");
        	};
        });
	}
}

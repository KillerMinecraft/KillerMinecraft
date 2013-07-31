package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.InactivityConversationCanceller;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class VoteManager
{
	static VoteManager instance;
	public static VoteManager getInstance() { return instance; }
	private Killer plugin;
	
	private boolean inVote; 
	long voteDuration = 400; // 20 secs

	private List<String> playersWhoCanVote = new ArrayList<String>();
	private int numYesVotes, numNoVotes;
	
	private int voteResultProcessID;
	private VoteResult voteResult;

	private ConversationFactory voteConvFactory;
	
	VoteManager(Killer _plugin)
	{
		this.plugin = _plugin;
		instance = this;
		inVote = false;

        setupConversation();
	}
	
	public boolean isInVote() { return instance.inVote; }
	
	public void startVote(Game game, String question, Player initiatedBy, Runnable runOnYes, Runnable runOnNo, Runnable runOnDraw)
	{
		if ( isInVote() )
			return;
	
		playersWhoCanVote.clear();
		for ( Player player : game.getOnlinePlayers())
			playersWhoCanVote.add(player.getName());
		numYesVotes = numNoVotes = 0;
		inVote = true;
		
		game.broadcastMessage(ChatColor.YELLOW + (initiatedBy == null ? "Vote started: " : initiatedBy.getName() + " started a vote: ") + ChatColor.RESET + question + ChatColor.YELLOW + "\nSay " + ChatColor.GREEN + "Y" + ChatColor.YELLOW + " to vote yes, or " + ChatColor.RED + "N" + ChatColor.YELLOW + " to vote no.");
		
		voteResult = new VoteResult(game, runOnYes, runOnNo, runOnDraw);
		voteResultProcessID = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, voteResult, voteDuration);
	}
	
	class VoteResult implements Runnable
	{
		public VoteResult(Game game, Runnable onYes, Runnable onNo, Runnable onDraw)
		{
			this.game = game;
			runOnYes = onYes;
			runOnNo = onNo;
			runOnDraw = onDraw;
		}
		
		private Game game;
		private Runnable runOnYes, runOnNo, runOnDraw;
		
		public void run()
		{
			if ( numYesVotes > numNoVotes )
			{
				game.broadcastMessage(ChatColor.YELLOW +"Vote succeeded (" + ChatColor.GREEN + numYesVotes + ChatColor.YELLOW + " for, " + ChatColor.RED + numNoVotes + ChatColor.YELLOW + " against)");
				if ( runOnYes != null )
					runOnYes.run();
			}
			else if ( numNoVotes > numYesVotes )
			{
				game.broadcastMessage(ChatColor.YELLOW + "Vote failed (" + ChatColor.GREEN + numYesVotes + ChatColor.YELLOW + " for, " + ChatColor.RED + numNoVotes + ChatColor.YELLOW + " against)");
				if ( runOnNo != null )
					runOnNo.run();
			}
			else
			{
				game.broadcastMessage(ChatColor.YELLOW + "Vote tied (" + ChatColor.GREEN + numYesVotes + ChatColor.YELLOW + " for, " + ChatColor.RED + numNoVotes + ChatColor.YELLOW + " against)");
				if ( runOnDraw != null )
					runOnDraw.run();
			}
			
			inVote = false;
			voteResultProcessID = -1;
		}
	}
	
	boolean doVote(Player player, boolean choice)
	{
		if ( !isInVote() )
			return false;
		
		if ( !playersWhoCanVote.contains(player.getName()) )
			return false; // this player already voted
		
		if ( choice )
			numYesVotes ++;
		else
			numNoVotes ++;
		
		playersWhoCanVote.remove(player.getName());
		
		// check for early finish, but delay this by one tick so that the triggering vote message will show up BEFORE the vote result message
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run()
			{
				if ( !isInVote() )
					return;
				
				// only finish early if everyone's voted (we could finish once we have a guaranteed result, but that feels weird if you're one of those that are too slow to vote)
				if ( playersWhoCanVote.size() > 0 )
					return;
				
				// stop the scheduled task
				if ( voteResultProcessID != -1 )
					plugin.getServer().getScheduler().cancelTask(voteResultProcessID);
				
				// run the result
				voteResult.run();				
			}
		});
		
		return true;
	}

	private void setupConversation()
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
				return "What do you want to start a vote on? Say the number to choose:\n" + ChatColor.GOLD + "1." + ChatColor.RESET + " Restart game\n" + ChatColor.GOLD + "2." + ChatColor.RESET + " End game\n" + ChatColor.GOLD + "0." + ChatColor.RESET + " Cancel";
			}
			
			protected Prompt acceptValidatedInput(ConversationContext context, Number val)
			{
				Player player = context.getForWhom() instanceof Player ? (Player)context.getForWhom() : null;
				final Game game = plugin.getGameForPlayer(player);
				
				if ( isInVote() )
					return cantVotePrompt;
				
				int choice = val.intValue();
				
				if ( choice == 1 )
					startVote(game, "Restart the current game?", player, new Runnable() {
						public void run()
						{
							game.forcedGameEnd = true;
							game.getGameMode().finishGame();
							game.restartGame(null);
						}
					}, null, null);
				
				else if ( choice == 2 )
					startVote(game, "End the current game?", player, new Runnable() {
						public void run()
						{
							game.forcedGameEnd = true;
							game.getGameMode().finishGame();
							game.endGame(null);
						}
					}, null, null);
				
				return Prompt.END_OF_CONVERSATION;
			}
		};
        
        voteConvFactory = new ConversationFactory(plugin);
        voteConvFactory.withFirstPrompt(initialPrompt);
        voteConvFactory.withLocalEcho(false);
        voteConvFactory.withModality(false);
        voteConvFactory.withConversationCanceller(new InactivityCanceller(plugin, 30));
	}

	protected class InactivityCanceller extends InactivityConversationCanceller
	{
		public InactivityCanceller(Plugin plugin, int timeoutSeconds)
		{
			super(plugin, timeoutSeconds);
		}
	
		@Override
		protected void cancelling(Conversation conversation)
		{
			Player player = conversation.getForWhom() instanceof Player ? (Player)conversation.getForWhom() : null;
			if ( player != null )
				player.sendMessage("Vote setup cancelled");
		}
	}
	
	void showVoteMenu(Player player)
	{
		Conversation convo = voteConvFactory.buildConversation(player);
		convo.begin();
	}
}

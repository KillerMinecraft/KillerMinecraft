package com.ftwinston.Killer;

import org.bukkit.ChatColor;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.InactivityConversationCanceller;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;


public class GameConfiguration
{
	public static GameConfiguration instance;
	private static ConversationFactory configConvFactory;
	private static final ChatColor numberColor = ChatColor.GOLD;
	
	public GameConfiguration()
	{
		configConvFactory = new ConversationFactory(Killer.instance);
        configConvFactory.withLocalEcho(false);
        configConvFactory.withModality(true);
	}
	
	public void showConversation(Player player, Game game)
	{
		if ( player.isConversing() )
			return;
		
		String configuring = game.getConfiguringPlayer();
		if ( configuring != null )
		{
			if ( !configuring.equals(player.getName()) )
				player.sendMessage(game.getName() + " is already being configured by " + configuring);
			
			return;
		}
		
		game.setConfiguringPlayer(player.getName());
		
		configConvFactory.withFirstPrompt(new MainPrompt(game));
        configConvFactory.withConversationCanceller(new InactivityCanceller(game, 30));
        configConvFactory.addConversationAbandonedListener(new AbandonedListener(game));
        
		Conversation convo = configConvFactory.buildConversation(player);
		convo.begin();
	}
	
	protected void writeColoredNumber(StringBuilder sb, int number)
	{
		sb.append("\n");
		sb.append(numberColor);
		sb.append(number);
		sb.append(". ");
		sb.append(ChatColor.RESET);
	}

	protected void configurationFinished(Game game)
	{
		game.setConfiguringPlayer(null);
	}

	private class InactivityCanceller extends InactivityConversationCanceller
	{
		private Game game;
		public InactivityCanceller(Game game, int timeoutSeconds)
		{
			super(Killer.instance, timeoutSeconds);
			this.game = game;
		}
	
		@Override
		protected void cancelling(Conversation conversation)
		{
			Player player = conversation.getForWhom() instanceof Player ? (Player)conversation.getForWhom() : null;
			if ( player != null )
			{
				configurationFinished(game);
				player.sendMessage("Configuration cancelled due to inactivity");
			}
		}
	}
	
	private class AbandonedListener implements ConversationAbandonedListener
	{
		private Game game;
		public AbandonedListener(Game game)
		{
			this.game = game;
		}
		
		@Override
		public void conversationAbandoned(ConversationAbandonedEvent arg)
		{
			configurationFinished(game);
		}
	}
	
	private class ClosingPrompt extends MessagePrompt
	{
		@Override
		public String getPromptText(ConversationContext context)
		{
			return "\n\n\n\n\n\n\n\n\n\n\n.";
		}

		@Override
		protected Prompt getNextPrompt(ConversationContext context) {
			return Prompt.END_OF_CONVERSATION;
		}
	}
	
	private class MainPrompt extends NumericPrompt
	{
		private Game game;
		public MainPrompt(Game game)
		{
			this.game = game;
		}
		
		public String getPromptText(ConversationContext context)
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("\n\n\n\n\n\n\n\n\n\n\nConfiguring " + game.getName());
			sb.append("\n\nGame mode: ");
			sb.append(game.getGameMode().getName());
			
			if ( game.getGameMode().allowWorldOptionSelection() )
			{
				sb.append("\nWorld: ");
				sb.append(game.getWorldOption().getName());
			}
			
			writeColoredNumber(sb, 1);
			sb.append("change game mode");
			writeColoredNumber(sb, 2);
			sb.append("configure game mode");
			
			if ( game.getGameMode().allowWorldOptionSelection() )
			{
				writeColoredNumber(sb, 3);
				sb.append("change world");
				writeColoredNumber(sb, 4);
				sb.append("configure world");
			}
			else
				sb.append("\nThis game mode configures the world itself");
			
			writeColoredNumber(sb, 0);
			sb.append("close");
			
			return sb.toString();
		}
		
		protected Prompt acceptValidatedInput(ConversationContext context, Number val)
		{
			switch ( val.intValue() )
			{
			case 1:
				return new ModePrompt(game, this);
			case 2:
				return new ModeConfigPrompt(game, this);
			case 3:
				if ( game.getGameMode().allowWorldOptionSelection() )
					return new WorldPrompt(game, this);
				configurationFinished(game);
				return new ClosingPrompt();
			case 4:
				if ( game.getGameMode().allowWorldOptionSelection() )
					return new WorldConfigPrompt(game, this);
				configurationFinished(game);
				return new ClosingPrompt();
			default:
				configurationFinished(game);
				return new ClosingPrompt();
			}
		}
	}
	
	private class ModePrompt extends NumericPrompt
	{
		private Game game;
		private Prompt back;
		public ModePrompt(Game game, MainPrompt back)
		{
			this.game = game;
			this.back = back;
		}
		
		public String getPromptText(ConversationContext context)
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("\n\n\n\n\n\n\n\n\n\n\nCurrent game mode: ");
			sb.append(game.getGameMode().getName());
			
			for ( int i=0; i<GameMode.gameModes.size(); i++ )
			{
				writeColoredNumber(sb, i+1);
				sb.append(GameMode.get(i).getName());
			}

			writeColoredNumber(sb, 0);
			sb.append("go back");
			
			return sb.toString();
		}
		
		protected Prompt acceptValidatedInput(ConversationContext context, Number val)
		{
			if ( !game.getGameState().canChangeGameSetup )
				return new ClosingPrompt();
			
			int choice = val.intValue();
			if ( choice > 0 && choice <= GameMode.gameModes.size() )
			{
				// change the game mode
				game.setGameMode(GameMode.get(choice-1));
			}
			
			return back;
		}
	}
	
	private class ModeConfigPrompt extends NumericPrompt
	{
		private Game game;
		private Prompt back;
		public ModeConfigPrompt(Game game, MainPrompt back)
		{
			this.game = game;
			this.back = back;
		}
		
		public String getPromptText(ConversationContext context)
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("\n\n\n\n\n\n\n\n\n\n\nGame mode: ");
			sb.append(game.getGameMode().getName());
			
			Option[] options = game.getGameMode().getOptions();
			
			for ( int i=0; i<options.length; i++ )
			{
				writeColoredNumber(sb, i+1);
				sb.append(options[i].getName());
				sb.append(": ");
				
				if ( options[i].isEnabled() )
				{
					sb.append(ChatColor.GREEN);
					sb.append("ON");
				}
				else
				{
					sb.append(ChatColor.RED);
					sb.append("OFF");
				}
				sb.append(ChatColor.RESET);
			}
			
			writeColoredNumber(sb, 0);
			sb.append("go back");
			
			return sb.toString();
		}
		
		protected Prompt acceptValidatedInput(ConversationContext context, Number val)
		{
			if ( !game.getGameState().canChangeGameSetup )
				return new ClosingPrompt();
			
			int choice = val.intValue();
			if ( choice > 0 && choice <= game.getGameMode().getNumOptions() )
			{
				game.getGameMode().toggleOption(choice-1);
				return this;
			}
			
			return back;
		}
	}
	
	private class WorldPrompt extends NumericPrompt
	{
		private Game game;
		private Prompt back;
		public WorldPrompt(Game game, MainPrompt back)
		{
			this.game = game;
			this.back = back;
		}
		
		public String getPromptText(ConversationContext context)
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("\n\n\n\n\n\n\n\n\n\n\nCurrent world option: ");
			sb.append(game.getWorldOption().getName());
			
			for ( int i=0; i<WorldOption.worldOptions.size(); i++ )
			{
				writeColoredNumber(sb, i+1);
				sb.append(WorldOption.get(i).getName());
			}
			
			writeColoredNumber(sb, 0);
			sb.append("go back");
			
			return sb.toString();
		}
		
		protected Prompt acceptValidatedInput(ConversationContext context, Number val)
		{
			if ( !game.getGameState().canChangeGameSetup )
				return new ClosingPrompt();
			
			int choice = val.intValue();
			if ( choice > 0 && choice <= WorldOption.worldOptions.size() && game.getGameMode().allowWorldOptionSelection() )
			{
				// change the world option
				game.setWorldOption(WorldOption.get(choice-1));
			}
			
			return back;
		}
	}
	
	private class WorldConfigPrompt extends NumericPrompt
	{
		private Game game;
		private Prompt back;
		public WorldConfigPrompt(Game game, MainPrompt back)
		{
			this.game = game;
			this.back = back;
		}
		
		public String getPromptText(ConversationContext context)
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("\n\n\n\n\n\n\n\n\n\n\nWorld option: ");
			sb.append(game.getGameMode().getName());
			
			Option[] options = game.getWorldOption().getOptions();
			
			for ( int i=0; i<options.length; i++ )
			{
				writeColoredNumber(sb, i+1);
				sb.append(options[i].getName());
				sb.append(": ");
				
				if ( options[i].isEnabled() )
				{
					sb.append(ChatColor.GREEN);
					sb.append("ON");
				}
				else
				{
					sb.append(ChatColor.RED);
					sb.append("OFF");
				}
				sb.append(ChatColor.RESET);
			}
			
			writeColoredNumber(sb, 0);
			sb.append("go back");
			
			return sb.toString();
		}
		
		protected Prompt acceptValidatedInput(ConversationContext context, Number val)
		{
			if ( !game.getGameState().canChangeGameSetup )
				return new ClosingPrompt();
			
			int choice = val.intValue();
			if ( choice > 0 && choice <= game.getWorldOption().getNumOptions() )
			{
				game.getWorldOption().toggleOption(choice-1);
				return this;
			}
			
			return back;
		}
	}
}

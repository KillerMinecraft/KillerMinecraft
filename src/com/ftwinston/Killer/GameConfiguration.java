package com.ftwinston.Killer;

import org.bukkit.ChatColor;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.InactivityConversationCanceller;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

public class GameConfiguration
{
	public static GameConfiguration instance;
	private static ConversationFactory configConvFactory;
	private static final ChatColor numberColor = ChatColor.GOLD;
	public static void Initialize()
	{
		if ( instance == null )
			return;
		
		instance = new GameConfiguration();
		instance.setupConversation();
	}
	
	private void setupConversation()
	{
		configConvFactory = new ConversationFactory(Killer.instance);
        configConvFactory.withLocalEcho(false);
        configConvFactory.withModality(true);
	}
	
	public void ShowConversation(Player player, Game game)
	{
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
			
			sb.append("Configuring Game");
			if ( Settings.maxSimultaneousGames > 1 )
			{
				sb.append(" #");
				sb.append(game.getNumber());
			}
			sb.append("\n\nGame mode: ");
			sb.append(game.getGameMode().getName());
			writeColoredNumber(sb, 1);
			sb.append("change");
			writeColoredNumber(sb, 2);
			sb.append("configure\n");
			
			if ( game.getGameMode().allowWorldOptionSelection() )
			{
				sb.append("World: ");
				sb.append(game.getWorldOption().getName());
				writeColoredNumber(sb, 3);
				sb.append("change");
				writeColoredNumber(sb, 4);
				sb.append("configure");
			}
			else
				sb.append("This game mode configures the world itself");
			
			sb.append("\n[type anything else to close]");
			
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
				return Prompt.END_OF_CONVERSATION;
			case 4:
				if ( game.getGameMode().allowWorldOptionSelection() )
					return new WorldConfigPrompt(game, this);
				configurationFinished(game);
				return Prompt.END_OF_CONVERSATION;
			default:
				configurationFinished(game);
				return Prompt.END_OF_CONVERSATION;
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
			
			sb.append("Current game mode: ");
			sb.append(game.getGameMode().getName());
			
			for ( int i=0; i<GameMode.gameModes.size(); i++ )
			{
				writeColoredNumber(sb, i+1);
				sb.append(GameMode.get(i).getName());
			}
			
			sb.append("\n[type anything else to go back]");
			
			return sb.toString();
		}
		
		protected Prompt acceptValidatedInput(ConversationContext context, Number val)
		{
			if ( !game.getGameState().canChangeGameSetup )
				return Prompt.END_OF_CONVERSATION;
			
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
			
			sb.append("Game mode: ");
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
			
			sb.append("\n[type anything else to go back]");
			
			return sb.toString();
		}
		
		protected Prompt acceptValidatedInput(ConversationContext context, Number val)
		{
			if ( !game.getGameState().canChangeGameSetup )
				return Prompt.END_OF_CONVERSATION;
			
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
			
			sb.append("Current world option: ");
			sb.append(game.getWorldOption().getName());
			
			for ( int i=0; i<GameMode.gameModes.size(); i++ )
			{
				writeColoredNumber(sb, i+1);
				sb.append(GameMode.get(i).getName());
			}
			
			sb.append("\n[type anything else to go back]");
			
			return sb.toString();
		}
		
		protected Prompt acceptValidatedInput(ConversationContext context, Number val)
		{
			if ( !game.getGameState().canChangeGameSetup )
				return Prompt.END_OF_CONVERSATION;
			
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
			
			sb.append("World option: ");
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
			
			sb.append("\n[type anything else to go back]");
			
			return sb.toString();
		}
		
		protected Prompt acceptValidatedInput(ConversationContext context, Number val)
		{
			if ( !game.getGameState().canChangeGameSetup )
				return Prompt.END_OF_CONVERSATION;
			
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

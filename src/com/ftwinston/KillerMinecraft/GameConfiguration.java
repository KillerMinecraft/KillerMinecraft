package com.ftwinston.KillerMinecraft;

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
		configConvFactory = new ConversationFactory(KillerMinecraft.instance);
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
		
		configConvFactory.withFirstPrompt(new MainPrompt(game));
        configConvFactory.withConversationCanceller(new InactivityCanceller(game, 30));
        configConvFactory.addConversationAbandonedListener(new AbandonedListener(game));
        
		Conversation convo = configConvFactory.buildConversation(player);
		convo.begin();
		
		game.setConfiguringPlayer(player.getName(), convo);
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
		game.setConfiguringPlayer(null, null);
	}

	private class InactivityCanceller extends InactivityConversationCanceller
	{
		private Game game;
		public InactivityCanceller(Game game, int timeoutSeconds)
		{
			super(KillerMinecraft.instance, timeoutSeconds);
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
			return "\n\n\n\n\n\n\n\n\n\n\n ";
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
			
			if ( game.getGameMode().allowWorldGeneratorSelection() )
			{
				sb.append("\nWorld: ");
				sb.append(game.getWorldGenerator().getName());
			}
			
			writeColoredNumber(sb, 1);
			sb.append("change game mode");
			writeColoredNumber(sb, 2);
			sb.append("configure game mode");
			
			if ( game.getGameMode().allowWorldGeneratorSelection() )
			{
				writeColoredNumber(sb, 3);
				sb.append("change world generator");
				writeColoredNumber(sb, 4);
				sb.append("configure world generator");
			}
			else
				sb.append("\nThis game mode configures the world itself");
			
			writeColoredNumber(sb, 5);
			sb.append("change player limits");
			
			writeColoredNumber(sb, 6);
			sb.append("change monster numbers");
			
			writeColoredNumber(sb, 7);
			sb.append("change animal numbers");
			
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
				if ( game.getGameMode().allowWorldGeneratorSelection() )
					return new WorldPrompt(game, this);
				configurationFinished(game);
				return new ClosingPrompt();
			case 4:
				if ( game.getGameMode().allowWorldGeneratorSelection() )
					return new WorldConfigPrompt(game, this);
				configurationFinished(game);
				return new ClosingPrompt();
			case 5:
				return new PlayerLimitsPrompt(game, this);
			case 6:
				return new MonstersPrompt(game, this);
			case 7:
				return new AnimalsPrompt(game, this);
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
				game.modeRenderer.allowForChanges();
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
				game.modeRenderer.allowForChanges();
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
			sb.append(game.getWorldGenerator().getName());
			
			for ( int i=0; i<WorldGenerator.worldGenerators.size(); i++ )
			{
				writeColoredNumber(sb, i+1);
				sb.append(WorldGenerator.get(i).getName());
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
			if ( choice > 0 && choice <= WorldGenerator.worldGenerators.size() && game.getGameMode().allowWorldGeneratorSelection() )
			{
				// change the world option
				game.setWorldGenerator(WorldGenerator.get(choice-1));
				game.miscRenderer.allowForChanges();
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
			
			Option[] options = game.getWorldGenerator().getOptions();
			
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
			if ( choice > 0 && choice <= game.getWorldGenerator().getNumOptions() )
			{
				game.getWorldGenerator().toggleOption(choice-1);
				game.miscRenderer.allowForChanges();
				return this;
			}
			
			return back;
		}
	}
	
	private class PlayerLimitsPrompt extends NumericPrompt
	{
		private Game game;
		private Prompt back;
		public PlayerLimitsPrompt(Game game, MainPrompt back)
		{
			this.game = game;
			this.back = back;
		}
		
		public String getPromptText(ConversationContext context)
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("\n\n\n\n\n\n\n\n\n\n\n");
			if ( game.usesPlayerLimit() )
			{
				sb.append("Player limit: " );
				sb.append(game.getPlayerLimit());
				sb.append("\nEnter the player limit you want, or 0 to remove the limit.");
			}
			else
				sb.append("No player limit.\nEnter the player limit you want, or 0 to leave with no limit.");

			return sb.toString();
		}
		
		protected Prompt acceptValidatedInput(ConversationContext context, Number val)
		{
			if ( !game.getGameState().canChangeGameSetup )
				return new ClosingPrompt();
			
			int limit = val.intValue();
			if ( limit > 0 )
			{
				game.setUsesPlayerLimit(true);
				game.setPlayerLimit(limit);
			}
			else
				game.setUsesPlayerLimit(false);

			game.miscRenderer.allowForChanges();
			return back;
		}
	}
	

	
	private class AnimalsPrompt extends NumericPrompt
	{
		private Game game;
		private Prompt back;
		public AnimalsPrompt(Game game, MainPrompt back)
		{
			this.game = game;
			this.back = back;
		}
		
		public String getPromptText(ConversationContext context)
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("\n\n\n\n\n\n\n\n\n\n\n");
			sb.append("Animal numbers: " );
			sb.append(GameInfoRenderer.getQuantityText(game.animalNumbers));
			sb.append("\nEnter a number from ");
			sb.append(Game.minQuantityNum);
			sb.append(" to ");
			sb.append(Game.maxQuantityNum);
			sb.append(", representing the following animal numbers:\n");
			for ( int i=Game.minQuantityNum; i<=Game.maxQuantityNum; i++)
			{
				sb.append(i);
				sb.append(" - ");
				sb.append(GameInfoRenderer.getQuantityText(i));
				if ( i < Game.maxQuantityNum )
					sb.append(", ");
			}
			
			return sb.toString();
		}
		
		protected Prompt acceptValidatedInput(ConversationContext context, Number val)
		{
			if ( !game.getGameState().canChangeGameSetup )
				return new ClosingPrompt();
			
			int number = val.intValue();
			if ( number >= Game.minQuantityNum && number <= Game.maxQuantityNum )
			{
				game.animalNumbers = number;
				game.miscRenderer.allowForChanges();
			}
			
			return back;
		}
	}
	

	
	private class MonstersPrompt extends NumericPrompt
	{
		private Game game;
		private Prompt back;
		public MonstersPrompt(Game game, MainPrompt back)
		{
			this.game = game;
			this.back = back;
		}
		
		public String getPromptText(ConversationContext context)
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("\n\n\n\n\n\n\n\n\n\n\n");
			sb.append("Monster numbers: " );
			sb.append(GameInfoRenderer.getQuantityText(game.monsterNumbers));
			sb.append("\nEnter a number from ");
			sb.append(Game.minQuantityNum);
			sb.append(" to ");
			sb.append(Game.maxQuantityNum);
			sb.append(", representing the following monster numbers:\n");
			for ( int i=Game.minQuantityNum; i<=Game.maxQuantityNum; i++)
			{
				sb.append(i);
				sb.append(" - ");
				sb.append(GameInfoRenderer.getQuantityText(i));
				if ( i < Game.maxQuantityNum )
					sb.append(", ");
			}
			
			return sb.toString();
		}
		
		protected Prompt acceptValidatedInput(ConversationContext context, Number val)
		{
			if ( !game.getGameState().canChangeGameSetup )
				return new ClosingPrompt();
			
			int number = val.intValue();
			if ( number >= Game.minQuantityNum && number <= Game.maxQuantityNum )
			{
				game.monsterNumbers = number;
				game.miscRenderer.allowForChanges();
			}
			
			return back;
		}
	}
}

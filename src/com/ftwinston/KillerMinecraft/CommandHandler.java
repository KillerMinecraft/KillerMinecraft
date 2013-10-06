package com.ftwinston.KillerMinecraft;

import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.ftwinston.KillerMinecraft.Configuration.TeamInfo;
import com.ftwinston.KillerMinecraft.GameConfiguration.Menu;

public class CommandHandler
{
	public static boolean onCommand(KillerMinecraft plugin, CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("spec"))
			return CommandHandler.spectatorCommand(plugin, sender, cmd, label, args);
		else if (cmd.getName().equalsIgnoreCase("vote"))
		{
			if ( sender instanceof Player )
				plugin.voteManager.showVoteMenu((Player)sender);
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("team"))
			return CommandHandler.teamCommand(plugin, sender, cmd, label, args);
		else if (cmd.getName().equalsIgnoreCase("help"))
		{
			if ( !(sender instanceof Player) )
				return true;
			
			// if they've already reached the end of the messages, start again from the beginning
			Player player = (Player)sender;
			Game game = plugin.getGameForPlayer(player);
			if ( game == null )
				return true;
			
			if ( !game.getGameMode().sendGameModeHelpMessage(player) )
			{// if there was no message to send, restart from the beginning
				game.getPlayerInfo().get(player.getName()).nextHelpMessage = 0;
				game.getGameMode().sendGameModeHelpMessage(player);
			}
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("killer"))
			return CommandHandler.killerCommand(plugin, sender, cmd, label, args);
		
		return false;
	}
	
	public static boolean spectatorCommand(KillerMinecraft plugin, CommandSender sender, Command cmd, String label, String[] args)
	{
		if ( !(sender instanceof Player) )
			return false;
		
		if ( args.length == 0 )
		{
			sender.sendMessage("Usage: /spec main, /spec nether, /spec <player name>, or /spec follow");
			return true;
		}
		Player player = (Player)sender;
		
		Game game = plugin.getGameForWorld(player.getWorld());
		if ( game == null || Helper.isAlive(game, player) )
		{
			sender.sendMessage("Only spectators can use this command");
			return true;
		}
		
		if ( args[0].equalsIgnoreCase("main") )
		{
			plugin.playerManager.teleport(player, game.getGameMode().getSpawnLocation(player));
		}
		else if ( args[0].equalsIgnoreCase("nether") )
		{
			World nether = null;
			for ( World world : game.getWorlds() )
				if ( world.getEnvironment() == Environment.NETHER )
				{
					nether = world;
					break;
				}
			
			if ( nether != null )
				plugin.playerManager.teleport(player, nether.getSpawnLocation());
			else
				sender.sendMessage("Nether world not found, please try again");
		}
		else
		{
			Player other = plugin.getServer().getPlayer(args[0]);
			if ( other == null || !other.isOnline() || game != plugin.getGameForPlayer(other) || !Helper.isAlive(game, other))
				sender.sendMessage("Player not found: " + args[0]);
			else if ( Helper.getTargetName(game, player) != null )
				Helper.setTargetOf(game,  player, other);
			
			plugin.playerManager.moveToSee(player, other);
		}
		
		return true;
	}

	
	static boolean teamCommand(KillerMinecraft plugin, CommandSender sender, Command cmd, String label, String[] args)
	{
		if ( !(sender instanceof Player) )
			return true;
		
		Player player = (Player)sender;
		Game game = plugin.getGameForPlayer(player);
		
		if ( game == null )
			return true;
		
		if ( args.length == 0 && player.getWorld() == plugin.stagingWorld )
		{
			if ( game.allowTeamSelection() && game.getGameState().canChangeGameSetup )
				game.configuration.showMenu(player, Menu.TEAM_SELECTION);
			return true;
		}
		else
			return teamChat(plugin, game, player, cmd, label, args);
	}
	
	static boolean teamChat(KillerMinecraft plugin, Game game, Player player, Command cmd, String label, String[] args)
	{		
		if ( args.length == 0 )
		{
			player.sendMessage("Usage: /team <message>");
			return true;
		}
				
		if ( game == null || !game.getGameState().usesGameWorlds )
			return true;
		
		TeamInfo team = game.getTeamForPlayer(player);
		if ( team == null || !team.allowTeamChat() )
		{
			player.sendMessage("Team chat is not available in this game mode");
			return true;
		}
		
		String message = "[Team] " + ChatColor.RESET + args[0];
		for ( int i=1; i<args.length; i++ )
			message += " " + args[i];
		
		PlayerManager.Info info = game.getPlayerInfo().get(player.getName());
		List<Player> recipients = game.getOnlinePlayers(new PlayerFilter().team(info.getTeam()));
		
		// most of this code is a clone of the actual chat code in NetServerHandler.chat
		AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(false, player, "ignored", new HashSet<Player>(recipients));
		plugin.getServer().getPluginManager().callEvent(event);

		if (event.isCancelled())
			return true;
	
		message = String.format(event.getFormat(), player.getDisplayName(), message);
		plugin.getServer().getConsoleSender().sendMessage(message);
		
		for (Player recipient : event.getRecipients())
			recipient.sendMessage(message);
		
		return true;
	}

	public static boolean killerCommand(KillerMinecraft plugin, CommandSender sender, Command cmd, String label, String[] args)
	{
		Player player;
		if ( sender instanceof Player )
			player = (Player)sender;
		else
			player = null;
		
		// players, op or otherwise, can use /killer join and /killer quit,
		// ONLY IF the staging world isn't the server default.
		if ( !plugin.stagingWorldIsServerDefault )
		{
			if ( args.length > 0 )
			{
				String firstParam = args[0].toLowerCase();
				if ( firstParam.equals("join") )
				{
					if ( player == null )
					{
						sender.sendMessage("Only players can run this command");
						return true;
					}
					Game game = plugin.getGameForWorld(player.getWorld());
					if ( player.getWorld() == plugin.stagingWorld || game != null )
					{
						sender.sendMessage("You are already in Killer Minecraft, so you can't join again!");
						return true;
					}
					
					plugin.playerManager.movePlayerIntoKillerGame(player);
					return true;
				}
				else if ( firstParam.equals("quit") || firstParam.equals("exit"))
				{
					if ( player == null )
					{
						sender.sendMessage("Only players can run this command");
						return true;
					}
					Game game = plugin.getGameForWorld(player.getWorld());
					if ( player.getWorld() != plugin.stagingWorld && game == null )
					{
						sender.sendMessage("You are not in Killer Minecraft, so you can't quit!");
						return true;
					}
					
					plugin.playerManager.movePlayerOutOfKillerGame(player);
					return true;
				}
				else if ( firstParam.equals("game") )
				{
					if ( player == null )
					{
						sender.sendMessage("Only players can run this command");
						return true;
					}
					
					Game game = plugin.getGameForPlayer(player);
					if ( game == null )
					{
						if ( player.getWorld() == plugin.stagingWorld )
							sender.sendMessage("You're in the Killer staging world, but you're not in a game");
						else
							sender.sendMessage("You're not in a Killer game");
					}	
					else if ( player.getWorld() == plugin.stagingWorld )
						sender.sendMessage("You're in the the setup room for " + game.getName());
					else
						sender.sendMessage("You're in " + game.getName()+1);
					
					return true;
				}
			}
			if ( player != null && !player.isOp() )
			{
				sender.sendMessage("Invalid command: Use /killer join to enter the game, and /killer quit to leave it");
				return true;
			}
		}
	
		// op players and non-players can use the others
	
		if ( player != null && !player.isOp() )
		{
			sender.sendMessage("You must be a server op to run this command");
			return true;
		}
		
		if ( args.length == 0 )
		{
			if ( !plugin.stagingWorldIsServerDefault && player != null )
				sender.sendMessage("Usage: /killer join, /killer quit, /killer game, /killer end");
			else
				sender.sendMessage("Usage: /killer end");
			return true;
		}
		
		String firstParam = args[0].toLowerCase();
		if ( firstParam.equals("end") )
		{
			Game game = plugin.getGameForPlayer(player);
			if ( game != null && game.getGameState().usesGameWorlds )
			{
				game.forcedGameEnd = true;
				game.getGameMode().gameFinished();
				game.endGame(sender);
			}
		}
		else
			sender.sendMessage("Invalid parameter: " + args[0] + " - type /killer to list allowed parameters");
		
		return true;
	}
	
	public static boolean stagingWorldCommand(KillerMinecraft plugin, CommandSender sender, Command cmd, String label, String[] args)
	{
/*
Command block commands will be as follows:

|setup game X start
|setup game X confirm
|setup game X cancel
|setup game X difficulty up
|setup game X difficulty down
|setup game X monsters up
|setup game X monsters down
|setup game X animals up
|setup game X animals down
|setup game X show modes
|setup game X show worlds
|setup game X show modeconfig
|setup game X show worldconfig
|setup game X show misc
setup game X option Y
setup game X playerlimit on/off/up/down

setup game X include @p
setup game X exclude @p

|setup arena X spleef
|setup arena X survival
setup arena X reset
|setup arena X equip @p

setup tp @p pos CX CY CZ
setup tp @p game X
setup tp @p game X else CX CY CZ
setup tp @p exit

setup block TYPE DATA CX CY CZ
setup block TYPE DATA CX1 CY1 CZ1 CX2 CY2 CZ2
*/
		if ( args.length < 3 )
		{
			plugin.log.warning("Invalid command: too few arguments");
			return true;
		}
		
		if ( args[0] == "game" )
		{
			int num;
			try
			{
				num = Integer.parseInt(args[1]);
			}
			catch ( NumberFormatException ex )
			{
				plugin.log.warning("Command error: invalid game #: " + args[1]);
				return true;
			}
			
			if ( num > 0 && num <= plugin.games.length )
				gameCommand(plugin, plugin.games[num-1], args);
			else
				plugin.log.warning("Command error: no game #" + num);
		}
		else if ( args[0] == "tp" )
		{
			teleportCommand(plugin, args);
		}
		else if ( args[0] == "block" )
		{
			
		}
		return true;
	}
	
	private static void gameCommand(KillerMinecraft plugin, Game game, String[] args)
	{
		if ( game == null || !game.getGameState().canChangeGameSetup )
			return;
		
		String cmd = args[2];
		if ( cmd == "difficulty" )
			changeDifficulty(plugin, game, args.length > 3 ? args[3] : "");
		else if ( cmd == "monsters" )
			changeMonsterNumbers(plugin, game, args.length > 3 ? args[3] : "");
		else if ( cmd == "animals" )
			changeAnimalNumbers(plugin, game, args.length > 3 ? args[3] : "");
		else if ( cmd == "include" ) 
		{
			if ( args.length <= 3 )
			{
				plugin.log.warning("Player name not specified");
				return;
			}
			
			Player player = plugin.getServer().getPlayer(args[3]);
			if ( player == null || !player.isOnline() )
			{
				plugin.log.warning("Player not found: " + args[3]);
				return;									
			}
			
			if ( game.getGameState().usesGameWorlds )
			{
				if ( !Settings.allowLateJoiners )
				{
					plugin.log.warning("Cannot join: game is in progress");
					return;
				}
				if ( game.usesPlayerLimit() && game.getPlayers().size() >= game.getPlayerLimit() )
				{
					plugin.log.warning("Cannot join: game is full");
					return;
				}
				
				plugin.playerManager.teleport(player, game.getGameMode().getSpawnLocation(player));
			}
			game.addPlayerToGame(player);
		}
		else if ( cmd == "exclude" ) 
		{
			if ( args.length <= 3 )
			{
				plugin.log.warning("Player name not specified");
				return;
			}
			
			Player player = plugin.getServer().getPlayer(args[3]);
			if ( player == null || !player.isOnline() )
			{
				plugin.log.warning("Player not found: " + args[3]);
				return;									
			}
			
			game.removePlayerFromGame(player);
		}
		else
			plugin.log.warning("Invalid game command: " + cmd);
	}

	private static void changeDifficulty(KillerMinecraft plugin, Game game, String param)
	{
		if ( param == "up ")
		{
			if ( game.getDifficulty().getValue() < Difficulty.HARD.getValue() )
			{
				game.setDifficulty(Difficulty.getByValue(game.getDifficulty().getValue()+1));
			}
		}
		else if ( param == "down" )
		{
			if ( game.getDifficulty().getValue() > Difficulty.PEACEFUL.getValue() )
			{
				game.setDifficulty(Difficulty.getByValue(game.getDifficulty().getValue()-1));
			}
		}
		else
			plugin.log.warning("Invalid difficulty parameter, expected up/down, got: " + param);
	}

	private static void changeMonsterNumbers(KillerMinecraft plugin, Game game, String param)
	{
		if ( param == "up ")
		{
			if ( game.monsterNumbers < Game.maxQuantityNum )
			{
				game.monsterNumbers++;
			}
		}
		else if ( param == "down" )
		{
			if ( game.monsterNumbers > Game.minQuantityNum )
			{
				game.monsterNumbers--;
			}
		}
		else
			plugin.log.warning("Invalid monster number parameter, expected up/down, got: " + param);
	}
	
	private static void changeAnimalNumbers(KillerMinecraft plugin, Game game, String param)
	{
		if ( param == "up ")
		{
			if ( game.animalNumbers < Game.maxQuantityNum )
			{
				game.animalNumbers++;
			}
		}
		else if ( param == "down" )
		{
			if ( game.animalNumbers > Game.minQuantityNum )
			{
				game.animalNumbers--;
			}
		}
		else
			plugin.log.warning("Invalid animal number parameter, expected up/down, got: " + param);
	}
	
	private static void teleportCommand(KillerMinecraft plugin, String[] args)
	{
		//String playerName = args[1];
		String cmd = args[2];
		
		if ( cmd == "exit" )
		{
			// teleport out of killer
		}
		else if ( cmd == "pos" )
		{
			if ( args.length < 6 )
			{
				plugin.log.warning("Teleport command error: incomplete coordinates provided");
				return;
			}
			/*
			int cx, cy, cz;
			
			try
			{
				cx = Integer.parseInt(args[3]);
				cy = Integer.parseInt(args[4]);
				cz = Integer.parseInt(args[5]);
			}
			catch ( NumberFormatException ex )
			{
				plugin.log.warning("Teleport command error: invalid coordinates #: " + args[3] + " " + args[4] + " " + args[5]);
				return;
			}
			*/
			// teleport to the provided coordinates
		}
		else if ( cmd == "game" )
		{
			int num;
			try
			{
				num = Integer.parseInt(args[3]);
				if ( num < 0 || num > plugin.games.length)
				{
					plugin.log.warning("Teleport command error: no game #" + num);
					return;
				}
			}
			catch ( NumberFormatException ex )
			{
				plugin.log.warning("Teleport command error: invalid game #: " + args[3]);
				return;
			}
			
			Game game = plugin.games[num-1];
			
			if ( game.getGameState().usesGameWorlds )
			{
				; // teleport into this game
				return;
			}
			
			if ( args.length > 7 && args[4] == "else" )
			{
				/*
				int cx, cy, cz;
				
				try
				{
					cx = Integer.parseInt(args[5]);
					cy = Integer.parseInt(args[6]);
					cz = Integer.parseInt(args[7]);
				}
				catch ( NumberFormatException ex )
				{
					plugin.log.warning("Teleport command error: invalid coordinates #: " + args[5] + " " + args[6] + " " + args[7]);
					return;
				}
				*/
				// teleport to the provided coordinates
			}
		}
		else
			plugin.log.warning("Invalid teleport command: " + cmd);
	}
}
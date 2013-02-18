package com.ftwinston.Killer;

import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class CommandHandler
{

	public static boolean spectatorCommand(Killer plugin, CommandSender sender, Command cmd, String label, String[] args)
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

	public static boolean teamChat(Killer plugin, CommandSender sender, Command cmd, String label, String[] args)
	{
		if ( !(sender instanceof Player) )
			return true;
		
		if ( args.length == 0 )
		{
			sender.sendMessage("Usage: /team <message>");
			return true;
		}
		
		Player player = (Player)sender;
		Game game = plugin.getGameForPlayer(player);
		
		if ( game == null || !game.getGameState().usesGameWorlds )
			return true;
		
		if ( game.getGameMode().teamAllocationIsSecret() )
		{
			sender.sendMessage("Team chat is not available in " + game.getGameMode().getName() + " mode");
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

	public static boolean killerCommand(Killer plugin, CommandSender sender, Command cmd, String label, String[] args)
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
					
					if ( !plugin.playerManager.isInventoryEmpty(player.getInventory()) )
					{
						sender.sendMessage("You must have a completely empty inventory to join Killer Minecraft!");
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
						sender.sendMessage("You're in the the setup room for Killer game #" + (game.getNumber()+1));
					else
						sender.sendMessage("You're in Killer game #" + (game.getNumber()+1));
					
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
				sender.sendMessage("Usage: /killer join, /killer quit, /killer game, /killer restart, /killer end");
			else
				sender.sendMessage("Usage: /killer restart, /killer end");
			return true;
		}
		
		String firstParam = args[0].toLowerCase();
		if ( firstParam.equals("restart") )
		{
			Game game = plugin.getGameForPlayer(player);
			if ( game != null && game.getGameState().usesGameWorlds )
			{
				game.forcedGameEnd = true;
				game.getGameMode().gameFinished();
				game.restartGame(sender);
			}
		}
		else if ( firstParam.equals("end") )
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
}

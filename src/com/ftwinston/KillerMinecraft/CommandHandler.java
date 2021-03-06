package com.ftwinston.KillerMinecraft;

import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.ftwinston.KillerMinecraft.Game.PlayerInfo;
import com.ftwinston.KillerMinecraft.Configuration.TeamInfo;

public class CommandHandler
{

	public static boolean onCommand(KillerMinecraft plugin, CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("killer"))
		{
			if ( args.length == 0 && sender instanceof Player )
				MenuManager.show((Player)sender);
			else
				killerCommand(plugin, sender, cmd, label, args);
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("team"))
			return teamCommand(plugin, sender, cmd, label, args);
		else if (cmd.getName().equalsIgnoreCase("help"))
			return helpCommand(plugin, sender, cmd, label, args);
		
		return false;
	}

	private static boolean teamCommand(KillerMinecraft plugin, CommandSender sender, Command cmd, String label, String[] args)
	{
		if ( !(sender instanceof Player) )
			return true;
		
		Player player = (Player)sender;
		Game game = plugin.getGameForPlayer(player);
		
		if ( game == null )
			return true;
		
		if ( args.length == 0 )
		{
			player.sendMessage("Usage: /team <message>");
			return true;
		}
				
		if ( game == null || !game.getGameState().playersInWorld )
			return true;
		
		TeamInfo team = game.getPlayerInfo(player).getTeam();
		if ( team == null || !team.allowTeamChat() )
		{
			player.sendMessage("Team chat is not available in this game mode");
			return true;
		}
		
		String message = "[Team] " + ChatColor.RESET + args[0];
		for ( int i=1; i<args.length; i++ )
			message += " " + args[i];
		
		PlayerInfo info = game.getPlayerInfo().get(player.getName());
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
	
	private static boolean helpCommand(KillerMinecraft plugin, CommandSender sender, Command cmd, String label, String[] args) 
	{
		if ( !(sender instanceof Player) )
			return true;
		
		// if they've already reached the end of the messages, start again from the beginning
		Player player = (Player)sender;
		Game game = plugin.getGameForWorld(player.getWorld());
		if ( game == null )
			return true;
		
		if (!game.sendHelpMessage(player))
		{// if there was no message to send, restart from the beginning
			game.setupHelpMessages(player);
			game.sendHelpMessage(player);
		}
		return true;
	}

	private static boolean killerCommand(KillerMinecraft plugin, CommandSender sender, Command cmd, String label, String[] args)
	{
		Player player = null;
		Game game = null;
		if ( sender instanceof Player)
		{
			player = (Player)sender;
			game = plugin.getGameForPlayer(player);
			
			// players in a game can use /killer quit to get out of it
			if ( args[0].toLowerCase() == "quit" )
			{
				if ( game != null )
					game.removePlayerFromGame(player);
				else
					player.sendMessage("You aren't in a game, so cannot quit");
				return true;
			}
			
			if ( !player.isOp() )
				return true;
		}
		
		// by this point, you're either an Op or not a player.
		// but we don't have any other commands yet. Was thinking of something like: /killer install http://ftwinston.com/killer/plugin.jar 
		return true;
	}
}
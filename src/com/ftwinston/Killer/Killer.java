package com.ftwinston.Killer;

/*
 * Killer Minecraft
 * a Minecraft Bukkit Mod by 
 * David "Canazza" McQuillan and
 * Andrew Winston "FTWinston" Winston Watkins AKA Winston
 * Created 18/06/2012
 */
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Killer extends JavaPlugin
{
	public void onEnable()
	{
		getConfig().addDefault("autoAssign", false);
		getConfig().addDefault("restartDay", true);
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		autoAssignKiller = getConfig().getBoolean("autoAssign");
		restartDayWhenFirstPlayerJoins = getConfig().getBoolean("restartDay");
		
		
        getServer().getPluginManager().registerEvents(eventListener, this);
	}

	public void onDisable()
	{
		//saveConfig();
		//reloadConfig();
	}
	
	private final int absMinPlayers = 2;
	public boolean autoAssignKiller, restartDayWhenFirstPlayerJoins;
	private EventListener eventListener = new EventListener(this);
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("killer"))
		{
			if ( sender instanceof Player )
			{
				Player player = (Player)sender;
				if ( !player.isOp() )
				{
					sender.sendMessage("Sorry, you must be an op to use this command.");
					return true;
				}
			}
			
			if ( args.length > 0 )
			{
				if ( args[0].equalsIgnoreCase("assign") )
				{
					if  ( !assignKiller(sender) )
						return true;
				}
				else if ( args[0].equalsIgnoreCase("reveal") )
				{
					revealKiller(sender);
					return true;
				}
				else if ( args[0].equalsIgnoreCase("clear") )
				{
					clearKiller(sender);					
					return true;
				}
			}

			sender.sendMessage("Invalid command, available parameters are: assign, reveal, clear");
			return true;
		}
		return false;
	}
	
	public boolean assignKiller(CommandSender sender)
	{
		Player[] players = getServer().getOnlinePlayers();
		if ( players.length < absMinPlayers )
		{
			if ( sender != null )
				sender.sendMessage("This game mode really doesn't work with fewer than " + absMinPlayers + " players. Seriously.");
			return false;
		}
		
		Random random = new Random();
		int randomIndex = random.nextInt(players.length);
		
		for ( int i=0; i<players.length; i++ )
		{
			Player player = players[i];
			if ( i == randomIndex )
			{
				killerName = player.getName();
				player.sendMessage(ChatColor.RED + "You are the killer!");
			}
			else
				player.sendMessage(ChatColor.YELLOW + "You are not the killer.");
		}
		
		String senderName = sender == null ? "" : " by " + sender.getName();
		getServer().broadcastMessage("A killer has been randomly assigned" + senderName + " - nobody but the killer knows who it is.");
		return true;
	}
	
	private void revealKiller(CommandSender sender)
	{
		if ( hasKillerAssigned() )
		{
			String senderName = sender == null ? "automatically" : "by " + sender.getName();
			getServer().broadcastMessage(ChatColor.RED + "Revealed: " + killerName + " was the killer! " + ChatColor.WHITE + "(revealed " + senderName + ")");
			
			killerName = null;
		}
		else if ( sender != null )
			sender.sendMessage("No killer has been assigned, nothing to reveal!");
	}
	
	private void clearKiller(CommandSender sender)
	{
		if ( hasKillerAssigned() )
		{
			String senderName = sender == null ? "automatically" : "by " + sender.getName();
			getServer().broadcastMessage(ChatColor.RED + "The killer has been cleared: there is no longer a killer! " + ChatColor.WHITE + "(cleared " + senderName + ")");
			
			Player killerPlayer = Bukkit.getServer().getPlayerExact(killerName);
			if ( killerPlayer != null )
				killerPlayer.sendMessage(ChatColor.YELLOW + "You are no longer the killer.");
				
			killerName = null;
		}
		else if ( sender != null )
			sender.sendMessage("No killer has been assigned, nothing to clear!");
	}

	private String killerName = null;

	public boolean hasKillerAssigned()
	{
		return killerName != null;
	}

}
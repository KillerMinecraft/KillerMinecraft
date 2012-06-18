package com.ftwinston.Killer;

/*
 * Killer Minecraft
 * a Minecraft Bukkit Mod by 
 * David "Canazza" McQuillan and
 * Andrew Winston "FTWinston" Winston Watkins AKA Winston
 * Created 18/06/2012
 */
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Killer extends JavaPlugin
{
	Logger log = Logger.getLogger("Minecraft");
	
	public void onEnable()
	{
		log.info("Killer mode has been enabled");
		PluginManager pm = getServer().getPluginManager();
		getConfig().getDefaults();
	}

	public void onDisable()
	{
		log.info("Killer mode has been disabled");
		saveConfig();
		reloadConfig();
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("killer"))
		{
			String senderName;
			if ( sender instanceof Player )
			{
				Player player = (Player)sender;
				if ( !player.isOp() )
				{
					sender.sendMessage("Sorry, you must be an op to use this command.");
					return true;
				}
				senderName = player.getName();
			}
			else
				senderName = "the server";
		
			if ( args.length > 0 )
			{
				if ( args[0].equalsIgnoreCase("assign") )
				{
					Player[] players = getServer().getOnlinePlayers();
					if ( players.length < 3 )
					{
						sender.sendMessage("This game mode really doesn't work with fewer than 3 players. Seriously.");
						return true;
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
							player.sendMessage("You are not the killer.");
					}
					
					getServer().broadcastMessage("A killer has been randomly assigned by " + senderName + " - nobody but the killer knows who it is.");
				}
				else if ( args[0].equalsIgnoreCase("reveal") )
				{
					if ( killerName == null )
						sender.sendMessage("No killer has been assigned, nothing to reveal!");
					else
						getServer().broadcastMessage(ChatColor.RED + "Revealed: " + killerName + " was the killer! (revealed by " + senderName + ")");
						
					killerName = null;
				}
				else if ( args[0].equalsIgnoreCase("clear") )
				{
					getServer().broadcastMessage(ChatColor.RED + "The killer has been cleared: there is no longer a killer! (cleared by " + senderName + ")");
					
					if ( killerName != null )
					{
						Player killerPlayer = (Bukkit.getServer().getPlayer(killerName));
						if ( killerPlayer != null )
							killerPlayer.sendMessage("You are no longer the killer.");
							
						killerName = null;
					}
				}
			}
			
			return true;
		}
		return false;
	}
	
	private String killerName = null;

}
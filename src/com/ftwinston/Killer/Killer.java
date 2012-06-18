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
import org.bukkit.plugin.java.JavaPlugin;

public class Killer extends JavaPlugin
{
	Logger log = Logger.getLogger("Minecraft");
	
	public void onEnable()
	{
		//log.info("Killer mode has been enabled");
		getConfig().getDefaults();
		
        getServer().getPluginManager().registerEvents(deathListener, this);
	}

	public void onDisable()
	{
		//log.info("Killer mode has been disabled");
		saveConfig();
		reloadConfig();
	}
	
	private final int minPlayers = 2;
	private DeathBanListener deathListener = new DeathBanListener(this);
	
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
					if ( players.length < minPlayers )
					{
						sender.sendMessage("This game mode really doesn't work with fewer than " + minPlayers + " players. Seriously.");
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
							player.sendMessage(ChatColor.YELLOW + "You are not the killer.");
					}
					
					getServer().broadcastMessage("A killer has been randomly assigned by " + senderName + " - nobody but the killer knows who it is.");
					return true;
				}
				else if ( args[0].equalsIgnoreCase("reveal") )
				{
					if ( killerName == null )
						sender.sendMessage("No killer has been assigned, nothing to reveal!");
					else
						getServer().broadcastMessage(ChatColor.RED + "Revealed: " + killerName + " was the killer! " + ChatColor.WHITE + "(revealed by " + senderName + ")");
						
					killerName = null;
					return true;
				}
				else if ( args[0].equalsIgnoreCase("clear") )
				{
					if ( killerName != null )
					{
						getServer().broadcastMessage(ChatColor.RED + "The killer has been cleared: there is no longer a killer! " + ChatColor.WHITE + "(cleared by " + senderName + ")");
						
						Player killerPlayer = (Bukkit.getServer().getPlayer(killerName));
						if ( killerPlayer != null )
							killerPlayer.sendMessage(ChatColor.YELLOW + "You are no longer the killer.");
							
						killerName = null;
					}
					else
						sender.sendMessage("No killer has been assigned, nothing to clear!");
					
					return true;
				}
			}

			sender.sendMessage("Invalid command, available parameters are: assign, reveal, clear");
			return true;
		}
		return false;
	}
	
	private String killerName = null;

}
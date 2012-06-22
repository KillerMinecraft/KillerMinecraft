package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SpectatorManager
{
	private static SpectatorManager instance;
	public static SpectatorManager get()
	{
		return instance;
	}
	private JavaPlugin plugin;
	public SpectatorManager(JavaPlugin plugin)
	{
		this.plugin = plugin;
		instance = this;
	}
	private List<String> Spectators = new ArrayList<String>();
	
	public void playerJoined(Player player)
	{
		for(String spec:Spectators)
			if(spec != player.getName())
			{
				Player other = plugin.getServer().getPlayerExact(spec);
				if ( other != null )
					player.hidePlayer(other);
			}
	}
	public boolean isSpectator(Player player)
	{
		return Spectators.contains(player.getName());
	}
	
	public void addSpectator(Player player)
	{
		player.setAllowFlight(true);
		player.getInventory().clear();
		makePlayerInvisibleToAll(player);
		
		if(!Spectators.contains(player.getName()))
			Spectators.add(player.getName());
	}
	
	public void removeSpectator(Player player)
	{
		player.setFlying(false);
		player.setAllowFlight(false);
		player.getInventory().clear();
		makePlayerVisibleToAll(player);
		
		if(Spectators.contains(player.getName()))
			Spectators.remove(player.getName());
	}
	
	private void makePlayerInvisibleToAll(Player player)
	{
		for(Player p : plugin.getServer().getOnlinePlayers())
			p.hidePlayer(player);
	}
	
	private void makePlayerVisibleToAll(Player player)
	{
		for(Player p :  plugin.getServer().getOnlinePlayers())
			p.showPlayer(player);
	}
	
	public String handleSpectatorCommand(String command, String param)
	{
		if ( command.equals("add") )
		{
				Player addPlayer = plugin.getServer().getPlayer(param);
				if(addPlayer == null)
					return String.format("Player '%s' not found",param);
				
				addSpectator(addPlayer);
				return "Player Added to Spectators";
		}
		else if ( command.equals("remove") )
		{
				Player removePlayer = plugin.getServer().getPlayer(param);
				if(removePlayer == null)
					return String.format("Player '%s' not found",param);
				
				removeSpectator(removePlayer);
				return "Player removed from Spectators";
		}
		else if ( command.equals("list") )
		{
				StringBuilder list = new StringBuilder();
				list.append(this.Spectators.size() +" spectator(s): ");
				if(this.Spectators.size() > 0)
					for(String spec: this.Spectators)
						list.append(spec + ", ");
				
				return list.toString().substring(0,list.length()-2);
		}
		return "No command " + command + " found. Valid commands are add {player} and remove {player}";
	}
}

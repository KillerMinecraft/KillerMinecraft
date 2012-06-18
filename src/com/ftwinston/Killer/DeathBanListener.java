package com.ftwinston.Killer;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathBanListener implements Listener
{
    public static Killer plugin;
    
    public DeathBanListener(Killer instance)
	{
		plugin = instance;
    }
    

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event)
	{
    	if (!(event instanceof PlayerDeathEvent))
    		return;
		
		Player player = (Player) event.getEntity();
		if ( player == null )
			return;
		
		String playerName = player.getName();
		plugin.getServer().broadcastMessage(ChatColor.RED + playerName + " has now been banned.");
		plugin.getConfig().set("banned." + playerName, playerName);
		plugin.saveConfig();
		player.setBanned(true);
		player.kickPlayer(playerName);
	}
}
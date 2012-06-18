package com.ftwinston.Killer;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;

public class DeathBanListener extends EntityListener
{
    public static Killer plugin;
    
    public DeathBanListener(Killer instance)
	{
		plugin = instance;
    }
    
	public void onEntityDeath(EntityDeathEvent event)
	{
		if(!(event.getEntity() instanceof Player))
			return;
		
		Player player = (Player) event.getEntity();
		
		String playerName = player.getName();
		plugin.getServer().broadcastMessage(ChatColor.RED + playerName + " has now been banned.");
		plugin.getConfig().set("banned." + playerName, playerName);
		plugin.saveConfig();
		player.setBanned(true);
		player.kickPlayer(playerName);
	}
}
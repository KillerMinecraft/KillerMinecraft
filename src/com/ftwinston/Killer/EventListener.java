package com.ftwinston.Killer;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventListener implements Listener
{
    public static Killer plugin;
    
    public EventListener(Killer instance)
	{
		plugin = instance;
    }
    
    
    private int autoStartProcessID = -1;
    
    @EventHandler
	public void onPlayerJoin(PlayerJoinEvent p)
    {
    	Player[] players = null;
    	
    	if ( plugin.restartDayWhenFirstPlayerJoins )
    	{
    		players = plugin.getServer().getOnlinePlayers();
    		if ( players.length == 1 )
    			plugin.getServer().getWorlds().get(0).setTime(0);
    	}

    	if ( plugin.autoAssignKiller )
    	{
    		if ( players == null )
    			players = plugin.getServer().getOnlinePlayers();
    		
    		if ( players.length != 1 )
    			return; // only do this when the first player joins
    		
    		autoStartProcessID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
    			long lastRun = 0;
    			public void run()
    			{
    				long time = plugin.getServer().getWorlds().get(0).getTime();    				
    				
    				if ( time < lastRun ) // time of day has gone backwards! Must be a new day!
    				{	
    					// only if we have enough players to assign a killer should we cancel this loop process. Otherwise, try again tomorrow.
    					if ( plugin.hasKillerAssigned() || plugin.assignKiller(null) )
    					{
    						plugin.getServer().getScheduler().cancelTask(autoStartProcessID);
        					autoStartProcessID = -1;
    					}
    					else
    						lastRun = time;
    				}
    				else
    					lastRun = time;
    			}
    		}, 600L, 100L); // initial wait: 30s, then check every 5s
    	}
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent p)
    {
    	if ( plugin.autoAssignKiller && autoStartProcessID != -1 )
    	{
    		Player[] players = plugin.getServer().getOnlinePlayers();
    		if ( players.length > 1 )
    			return; // only do this when the server is empty
    		
    		plugin.getServer().getScheduler().cancelTask(autoStartProcessID);
			autoStartProcessID = -1;
    	}
    }
    
    @EventHandler
    public void onPlayerKick(PlayerKickEvent p)
    {
    	onPlayerQuit(null);
    }
    
    
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event)
    {
    	if (!(event instanceof PlayerDeathEvent))
    		return;
		
		Player player = (Player) event.getEntity();
		if ( player == null )
			return;
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new DelayedBan(player.getName()), 30);		
	}
    
    class DelayedBan implements Runnable
    {
    	String name;
    	public DelayedBan(String playerName) { name = playerName; }
    	
    	public void run()
    	{
			plugin.getServer().broadcastMessage(ChatColor.RED + name + " has now been banned.");
			
			Player player = Bukkit.getServer().getPlayerExact(name);
			if (player != null)
			{
				player.setBanned(true);
				player.kickPlayer(name);
			}
    	}
    }
}
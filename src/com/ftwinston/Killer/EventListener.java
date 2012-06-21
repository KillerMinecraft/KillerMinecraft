package com.ftwinston.Killer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventListener implements Listener
{
    public static Killer plugin;
    
    public EventListener(Killer instance)
	{
		plugin = instance;
    }
    
    // prevent spectators breaking anything, prevent anyone breaking the plinth
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event)
    {
    	if(SpectatorManager.get().isSpectator(event.getPlayer()))
    	{
    		event.setCancelled(true);
    	}
    	else
    	{
    		Location loc = event.getBlock().getLocation();
    		if ( loc.getWorld() == plugin.plinthPressurePlateLocation.getWorld()
    	            && loc.getX() >= plugin.plinthPressurePlateLocation.getBlockX() - 1
    	            && loc.getX() <= plugin.plinthPressurePlateLocation.getBlockX() + 1
    	            && loc.getZ() >= plugin.plinthPressurePlateLocation.getBlockZ() - 1
    	            && loc.getZ() <= plugin.plinthPressurePlateLocation.getBlockZ() + 1
    	        )
    	            event.setCancelled(true);
    	}
    }
    
    // prevent anyone placing blocks over the plinth
    public void onBlockCanBuild(BlockCanBuildEvent event)
    {
    	Location loc = event.getBlock().getLocation();
        if ( loc.getWorld() == plugin.plinthPressurePlateLocation.getWorld()
            && loc.getX() >= plugin.plinthPressurePlateLocation.getBlockX() - 1
            && loc.getX() <= plugin.plinthPressurePlateLocation.getBlockX() + 1
            && loc.getZ() >= plugin.plinthPressurePlateLocation.getBlockZ() - 1
            && loc.getZ() <= plugin.plinthPressurePlateLocation.getBlockZ() + 1
        )
            event.setBuildable(false);
    }
    
    // prevent lava/water from flowing onto the plinth
    @EventHandler
    public void BlockFromTo(BlockFromToEvent event)
    {
        Location loc = event.getToBlock().getLocation();
        if ( loc.getWorld() == plugin.plinthPressurePlateLocation.getWorld()
            && loc.getX() >= plugin.plinthPressurePlateLocation.getBlockX() - 1
            && loc.getX() <= plugin.plinthPressurePlateLocation.getBlockX() + 1
            && loc.getZ() >= plugin.plinthPressurePlateLocation.getBlockZ() - 1
            && loc.getZ() <= plugin.plinthPressurePlateLocation.getBlockZ() + 1
        )
            event.setCancelled(true);
    }

    private int autoStartProcessID = -1;
    
    @EventHandler
	public void onPlayerJoin(PlayerJoinEvent p)
    {
    	/*final Player player = p.getPlayer();
		if ( true ) // this should actually decide if the player is already part of the current game or not (e.g. were they just disconnected)
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
	            public void run() {
	                plugin.moveToMainWorld(player);
	                player.getInventory().clear();
	                player.setTotalExperience(0);
	            }
	        }, 1);*/

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
    		
    		plugin.autoStartProcessID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
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
        					plugin.autoStartProcessID = -1;
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
		plugin.cancelAutoStart();
		
		// if the game is "active" then give them 30s to rejoin, otherwise consider them to be "killed" almost right away.
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new ForgetDisconnectedPlayer(p.getPlayer().getName()), plugin.hasKillerAssigned() ? 600 : 60);
    }
    
    class ForgetDisconnectedPlayer implements Runnable
    {
    	String name;
    	public ForgetDisconnectedPlayer(String playerName) { name = playerName; }
    	
    	public void run()
    	{
			Player player = Bukkit.getServer().getPlayerExact(name);
			if ( player == null || !player.isOnline() )
			{
				plugin.playerKilled(name);
			}
    	}
    }
    
    
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event)
    {
    	if (!(event instanceof PlayerDeathEvent))
    		return;
		
		Player player = (Player) event.getEntity();
		if ( player == null )
			return;
		
		// the only reason this is delayed is to avoid banning the player before they properly die.
		// once observer mode is in place instead of banning, this can be a direct function call! 
		
		// plugin.playerKilled(player.getName());
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new DelayedDeathEffect(player.getName()), 30);		
	}
    
    class DelayedDeathEffect implements Runnable
    {
    	String name;
    	public DelayedDeathEffect(String playerName) { name = playerName; }
    	
    	public void run()
    	{
    		plugin.playerKilled(name);
    	}
    }
}
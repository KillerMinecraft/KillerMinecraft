package com.ftwinston.Killer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class EventListener implements Listener
{
    public static Killer plugin;
    
    public EventListener(Killer instance)
	{
		plugin = instance;
    }
    
    // when you die a spectator, be made able to fly again when you respawn
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
    	if(PlayerManager.get().isSpectator(event.getPlayer()))
    		PlayerManager.get().addSpectator(event.getPlayer());
    }
    
    // prevent spectators picking up anything
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event)
    {
    	if(PlayerManager.get().isSpectator(event.getPlayer()))
    		event.setCancelled(true);
    }
    
    // prevent spectators breaking anything, prevent anyone breaking the plinth
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event)
    {
    	if(PlayerManager.get().isSpectator(event.getPlayer()))
    		event.setCancelled(true);
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
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event)
    {
    	if(PlayerManager.get().isSpectator(event.getPlayer()))
    		event.setCancelled(true);
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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
    	if(event.isCancelled())
    		return;
   
	  	if(event.getClickedBlock().getType() == Material.STONE_PLATE)
	  	{
	  		Location loc = event.getClickedBlock().getLocation();
	        if ( loc.getWorld() == plugin.plinthPressurePlateLocation.getWorld()
	            && loc.getX() >= plugin.plinthPressurePlateLocation.getBlockX() - 1
	            && loc.getX() <= plugin.plinthPressurePlateLocation.getBlockX() + 1
	            && loc.getZ() >= plugin.plinthPressurePlateLocation.getBlockZ() - 1
	            && loc.getZ() <= plugin.plinthPressurePlateLocation.getBlockZ() + 1
	        )	
	        {
	        	// does the player have a blaze rod in their inventory?
	        	if ( event.getPlayer().getInventory().contains(Material.BLAZE_ROD) )
	        		plugin.doItemVictory(event.getPlayer());
	        }
	  	}
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event)
    {
        if( event.getEntity() instanceof Player )
        {
        	Player player = (Player)event.getEntity();
        	if(PlayerManager.get().isSpectator(player))
        		event.setCancelled(true);
        }
        else if (event instanceof EntityDamageByEntityEvent )
        {
        	Entity damager = ((EntityDamageByEntityEvent)event).getDamager();
        	if ( damager != null && damager instanceof Player )
        	{
        		if(PlayerManager.get().isSpectator((Player)damager))
        			event.setCancelled(true);
        	}
        }
	}
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event)
    {
        if( event.getEntity() instanceof Player )
        {
        	Player player = (Player)event.getEntity();
        	if(PlayerManager.get().isSpectator(player))
        		event.setCancelled(true);
        }
        else if (event instanceof EntityDamageByEntityEvent )
        {
        	Entity damager = ((EntityDamageByEntityEvent)event).getDamager();
        	if ( damager != null && damager instanceof Player )
        	{
        		if(PlayerManager.get().isSpectator((Player)damager))
        			event.setCancelled(true);
        	}
        }
	}
    
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event)
    {
    	if( event.getTarget() != null && event.getTarget() instanceof Player && PlayerManager.get().isSpectator((Player)event.getTarget()))
    		event.setCancelled(true);
    }
   
    private int autoStartProcessID = -1;
    
    @EventHandler
	public void onPlayerJoin(PlayerJoinEvent p)
    {
    	PlayerManager.get().playerJoined(p.getPlayer());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent p)
    {
		plugin.cancelAutoStart();
		
		// if the game is "active" then give them 30s to rejoin, otherwise consider them to be "killed" almost right away.
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new ForgetDisconnectedPlayer(p.getPlayer().getName()), plugin.hasKillerAssigned() ? 600 : 20);
    }
    
    class ForgetDisconnectedPlayer implements Runnable
    {
    	String name;
    	public ForgetDisconnectedPlayer(String playerName) { name = playerName; }
    	
    	public void run()
    	{
			Player player = Bukkit.getServer().getPlayerExact(name);
			if ( player == null || !player.isOnline() )
				PlayerManager.playerKilled(name);
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
		
		// the only reason this is delayed is to avoid banning the player before they properly die, if we're banning players on death
		
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
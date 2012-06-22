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
    	if(PlayerManager.instance.isSpectator(event.getPlayer().getName()))
    		PlayerManager.instance.addSpectator(event.getPlayer());
    }
    
    // prevent spectators picking up anything
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event)
    {
    	if(PlayerManager.instance.isSpectator(event.getPlayer().getName()))
    		event.setCancelled(true);
    }
    
    // prevent spectators breaking anything, prevent anyone breaking the plinth
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event)
    {
    	if(PlayerManager.instance.isSpectator(event.getPlayer().getName()))
    		event.setCancelled(true);
    	else if ( isOnPlinth(event.getBlock().getLocation()) )
			event.setCancelled(true);
    }
    
    // prevent anyone placing blocks over the plinth
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event)
    {
    	if(PlayerManager.instance.isSpectator(event.getPlayer().getName()))
    		event.setCancelled(true);
    	else if ( isOnPlinth(event.getBlock().getLocation()) )
			event.setCancelled(true);
    }
    
    // prevent lava/water from flowing onto the plinth
    @EventHandler
    public void BlockFromTo(BlockFromToEvent event)
    {
        if ( isOnPlinth(event.getToBlock().getLocation()) )
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
    	if(event.isCancelled())
    		return;
   
	  	if(event.getClickedBlock().getType() == Material.STONE_PLATE)
	  	{
	        if ( isOnPlinth(event.getClickedBlock().getLocation()) )
	        {
				// spectators can no longer win the game
				if(PlayerManager.instance.isSpectator(event.getPlayer().getName()))
					return;
			
	        	// does the player have a blaze rod in their inventory?
	        	if ( event.getPlayer().getInventory().contains(Material.BLAZE_ROD) )
	        		PlayerManager.instance.gameFinished(false, true, event.getPlayer().getName());
	        }
	  	}
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event)
    {
        if( event.getEntity() instanceof Player )
        {
        	Player player = (Player)event.getEntity();
        	if(PlayerManager.instance.isSpectator(player.getName()))
        		event.setCancelled(true);
        }
        else if (event instanceof EntityDamageByEntityEvent )
        {
        	Entity damager = ((EntityDamageByEntityEvent)event).getDamager();
        	if ( damager != null && damager instanceof Player && PlayerManager.instance.isSpectator(((Player)damager).getName()))
				event.setCancelled(true);
        }
	}
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event)
    {
        if( event.getEntity() instanceof Player )
        {
        	Player player = (Player)event.getEntity();
        	if(PlayerManager.instance.isSpectator(player.getName()))
        		event.setCancelled(true);
        }
        else if (event instanceof EntityDamageByEntityEvent )
        {
        	Entity damager = ((EntityDamageByEntityEvent)event).getDamager();
        	if ( damager != null && damager instanceof Player && PlayerManager.instance.isSpectator(((Player)damager).getName()))
				event.setCancelled(true);
        }
	}
    
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event)
    {
    	if( event.getTarget() != null && event.getTarget() instanceof Player && PlayerManager.instance.isSpectator(((Player)event.getTarget()).getName()))
    		event.setCancelled(true);
    }
    
    @EventHandler
	public void onPlayerJoin(PlayerJoinEvent p)
    {
		// if I log into the holding world (cos I logged out there), move me back to the main world's spawn and clear me out
		if ( p.getPlayer().getLocation().getWorld() == WorldManager.instance.holdingWorld )
		{
			Player player = p.getPlayer();
			player.getInventory().clear();
			player.setTotalExperience(0);
			player.teleport(plugin.getServer.getWorlds().get(0).getSpawnLocation());
		}
		
    	PlayerManager.instance.playerJoined(p.getPlayer());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent p)
    {
		plugin.cancelAutoAssign();
		
		// if the game is "active" then give them 30s to rejoin, otherwise consider them to be "killed" almost right away.
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new DelayedDeathEffect(p.getPlayer().getName(), true), plugin.hasKillerAssigned() ? 600 : 20);
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
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new DelayedDeathEffect(player.getName(), false), 10);
	}
    
    class DelayedDeathEffect implements Runnable
    {
    	String name;
		boolean checkDisconnected;
    	public DelayedDeathEffect(String playerName, bool disconnect)
		{
			name = playerName;
			checkDisconnected = disconnect;
		}
    	
    	public void run()
    	{
			if ( checkDisconnected )
			{
				Player player = Bukkit.getServer().getPlayerExact(name);
				if ( player != null && player.isOnline() )
					return; // player has reconnected, so don't kill them
			}
    		plugin.playerKilled(name);
    	}
    }
	
	private boolean isOnPlinth(Location loc)
	{
		Location plinthLoc = plugin.plinthPressurePlateLocation;
		return  plinthLoc != null && loc.getWorld() == plinthLoc.getWorld()
	            && loc.getX() >= plinthLoc.getBlockX() - 1
	            && loc.getX() <= plinthLoc.getBlockX() + 1
	            && loc.getZ() >= plinthLoc.getBlockZ() - 1
	            && loc.getZ() <= plinthLoc.getBlockZ() + 1
	}
}
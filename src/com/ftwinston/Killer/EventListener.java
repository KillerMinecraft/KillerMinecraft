package com.ftwinston.Killer;

import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.ItemStack;


class EventListener implements Listener
{
    public static Killer plugin;
    
    public EventListener(Killer instance)
	{
		plugin = instance;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST) // run last, because it deletes the intitial world
    public void onWorldInit(final WorldInitEvent event)
    {
    	if ( plugin.stagingWorldIsServerDefault && plugin.worldManager.stagingWorld == null )
    	{
    		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
    			public void run()
    			{
    				if ( GameMode.gameModes.size() == 0 )
					{
						plugin.warnNoGameModes();
						return;
					}
    				if ( WorldOption.worldOptions.size() == 0 )
					{
						plugin.warnNoWorldOptions();
						return;
					}
    				plugin.worldManager.createStagingWorld(Settings.stagingWorldName);
					plugin.worldManager.deleteWorlds(null, event.getWorld());
					plugin.craftBukkit.accountForDefaultWorldDeletion(plugin.worldManager.stagingWorld);
    			}
    		}, 1);
    	}
    }

    // when you die a spectator, be made able to fly again when you respawn
    @EventHandler(priority = EventPriority.HIGHEST) // run last, so we can absolutely say where you should respawn, in a Killer game
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
		if ( !plugin.isGameWorld(event.getPlayer().getWorld()) )
			return;
		
    	final String playerName = event.getPlayer().getName();
    	
		if ( plugin.getGameState().usesGameWorlds && plugin.worldManager.worlds.size() > 0 )
			event.setRespawnLocation(plugin.getGameMode().getSpawnLocation(event.getPlayer()));
		else
		{
			event.setRespawnLocation(plugin.stagingWorldManager.getStagingWorldSpawnPoint());
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
    			public void run()
    			{
    				Player player = plugin.getServer().getPlayerExact(playerName);
    				if ( player != null )
    					plugin.playerManager.giveStagingWorldInstructionBook(player);
    			}
    		});
		}
	
    	if(PlayerManager.instance.isSpectator(event.getPlayer().getName()))
    	{
    		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
    			public void run()
    			{
    				Player player = plugin.getServer().getPlayerExact(playerName);
    				if ( player != null )
    				{
    					boolean alive = plugin.getGameMode().isAllowedToRespawn(player);
    					plugin.playerManager.setAlive(player, alive);
    					if ( alive )
    						player.setCompassTarget(plugin.playerManager.getCompassTarget(player));
    				}
    			}
    		});
    	}
    }
    
    // spectators moving between worlds
    @EventHandler(priority = EventPriority.HIGHEST)
    public void OnPlayerChangedWorld(PlayerChangedWorldEvent event)
    {
		boolean wasInKiller = plugin.isGameWorld(event.getFrom());
		boolean nowInKiller = plugin.isGameWorld(event.getPlayer().getWorld());
		
		if ( wasInKiller )
		{
			if ( nowInKiller )
			{
				Player player = event.getPlayer();
				if(PlayerManager.instance.isSpectator(player.getName()))
					PlayerManager.instance.setAlive(player, false);
				else
					player.setCompassTarget(plugin.playerManager.getCompassTarget(player));
			}
			else
			{
				playerQuit(event.getPlayer(), false);
				plugin.playerManager.previousLocations.remove(event.getPlayer().getName()); // they left Killer, so forget where they should be put on leaving
			}
		}
		else if ( nowInKiller )
			playerJoined(event.getPlayer());
		
		if ( event.getPlayer().getWorld() == plugin.worldManager.stagingWorld )
			plugin.playerManager.putPlayerInStagingWorld(event.getPlayer());
    }
    
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerPortal(PlayerPortalEvent event)
	{
		if ( !plugin.isGameWorld(event.getFrom().getWorld()) )
			return;
		
		PortalHelper helper = new PortalHelper(event.getPortalTravelAgent());
		event.setCancelled(true); // we're going to handle implementing the portalling ourselves
		
		plugin.getGameMode().handlePortal(event.getCause(), event.getFrom(), helper); // see? I told you
		helper.performTeleport(event.getCause(), event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityPortal(EntityPortalEvent event)
	{
		if ( !plugin.isGameWorld(event.getFrom().getWorld()) )
			return;
		
		PortalHelper helper = new PortalHelper(event.getPortalTravelAgent());
		event.setCancelled(true); // we're going to handle implementing the portalling ourselves
		
		plugin.getGameMode().handlePortal(TeleportCause.NETHER_PORTAL, event.getFrom(), helper); // see? I told you
		helper.performTeleport(TeleportCause.NETHER_PORTAL, event.getEntity());
	}
	
    // prevent spectators picking up anything
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event)
    {
		if ( !plugin.isGameWorld(event.getPlayer().getWorld()) )
			return;

    	if(PlayerManager.instance.isSpectator(event.getPlayer().getName()))
    		event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event)
    {
    	if ( event.getLocation().getWorld() == plugin.worldManager.stagingWorld )
    		event.setCancelled(true);
    }
    
    // prevent spectators breaking anything, prevent anyone breaking protected locations
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event)
    {
		if ( !plugin.isGameWorld(event.getPlayer().getWorld()) )
			return;

    	event.setCancelled(
			PlayerManager.instance.isSpectator(event.getPlayer().getName()) ||
			plugin.worldManager.isProtectedLocation(event.getBlock().getLocation())
		);
    }
    
    // prevent anyone placing blocks on protected locations
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event)
    {
		if ( !plugin.isGameWorld(event.getPlayer().getWorld()) )
			return;

    	event.setCancelled(
			PlayerManager.instance.isSpectator(event.getPlayer().getName()) ||
			plugin.worldManager.isProtectedLocation(event.getBlock().getLocation()) ||
			event.getBlock().getLocation().getWorld() == plugin.worldManager.stagingWorld
		);
    }
    
    // prevent lava/water from flowing onto protected locations
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void BlockFromTo(BlockFromToEvent event)
    {
		if ( !plugin.isGameWorld(event.getToBlock().getLocation().getWorld()) )
			return;
		
        event.setCancelled(plugin.worldManager.isProtectedLocation(event.getBlock().getLocation()));
    }
    
	// prevent pistons pushing things into/out of protected locations
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event)
    {
		if ( !plugin.isGameWorld(event.getBlock().getLocation().getWorld()) )
			return;
		
    	event.setCancelled(plugin.worldManager.isProtectedLocation(event.getBlock().getLocation()));
    }
    
	// prevent explosions from damaging protected locations
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event)
    {
		if ( !plugin.isGameWorld(event.getEntity().getWorld()) )
			return;
		
    	List<Block> blocks = event.blockList();
    	for ( int i=0; i<blocks.size(); i++ )
    		if ( plugin.worldManager.isProtectedLocation(blocks.get(i).getLocation()) )
    		{
    			blocks.remove(i);
    			i--;
    		}
    	
    	if ( event.getEntity().getWorld() == plugin.worldManager.stagingWorld )
    	{
    		plugin.arenaManager.monsterKilled();
    		event.setYield(0);
    	}
    }
    
	// switching between spectator items
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerItemSwitch(PlayerItemHeldEvent event)
    {
		if ( !plugin.isGameWorld(event.getPlayer().getWorld()) )
			return;
		
    	if ( plugin.playerManager.isSpectator(event.getPlayer().getName()) )
    	{
    		ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
    		
    		if ( item == null )
    			plugin.playerManager.setFollowTarget(event.getPlayer(), null);
    		else if ( item.getType() == Settings.teleportModeItem )
    		{
    			event.getPlayer().sendMessage("Free look mode: left click to teleport " + ChatColor.YELLOW + "to" + ChatColor.RESET + " where you're looking, right click to teleport " + ChatColor.YELLOW + "through" + ChatColor.RESET + " through what you're looking");
    			plugin.playerManager.setFollowTarget(event.getPlayer(), null);
    		}
    		else if ( item.getType() == Settings.followModeItem )
    		{
    			event.getPlayer().sendMessage("Follow mode: click to cycle target");
    			String target = plugin.playerManager.getNearestFollowTarget(event.getPlayer());
    			plugin.playerManager.setFollowTarget(event.getPlayer(), target);
				plugin.playerManager.checkFollowTarget(event.getPlayer(), target);
    		}
    		else
    			plugin.playerManager.setFollowTarget(event.getPlayer(), null);
    	}
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
		if ( !plugin.isGameWorld(event.getPlayer().getWorld()) )
			return;
		
		if ( plugin.getGameState().canChangeGameSetup && event.getPlayer().getWorld() == plugin.worldManager.stagingWorld
		  && event.getClickedBlock() != null && (event.getClickedBlock().getType() == Material.STONE_BUTTON || event.getClickedBlock().getType() == Material.STONE_PLATE) )
		{
			plugin.stagingWorldManager.setupButtonClicked(event.getClickedBlock().getLocation().getBlockX(), event.getClickedBlock().getLocation().getBlockZ(), event.getPlayer());
			return;
		}
		
    	// spectators can't interact with anything, but they do use clicking to handle their spectator stuff
    	String playerName = event.getPlayer().getName();
    	if ( plugin.playerManager.isSpectator(playerName) )
    	{
    		event.setCancelled(true);
    		Material held = event.getPlayer().getItemInHand().getType();
    		
    		if ( held == Settings.teleportModeItem )
    		{
				if ( event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK )
    				plugin.playerManager.doSpectatorTeleport(event.getPlayer(), false);
    			else if ( event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK )
    				plugin.playerManager.doSpectatorTeleport(event.getPlayer(), true);
    		}
    		else if ( held == Settings.followModeItem )
    		{
        		PlayerManager.Info info = plugin.playerManager.getInfo(playerName); 
        		
    			if ( event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK )
        		{
    				String target = plugin.playerManager.getNextFollowTarget(event.getPlayer(), info.target, true);
    				plugin.playerManager.setFollowTarget(event.getPlayer(), target);
    				plugin.playerManager.checkFollowTarget(event.getPlayer(), target);
    				event.getPlayer().sendMessage("Following " + info.target);
        		}
    			else if ( event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK )
        		{
    				String target = plugin.playerManager.getNextFollowTarget(event.getPlayer(), info.target, false);
    				plugin.playerManager.setFollowTarget(event.getPlayer(), target);
    				plugin.playerManager.checkFollowTarget(event.getPlayer(), target);
    				event.getPlayer().sendMessage("Following " + info.target);
        		}
    		}
    		
    		return;
    	}

    	// prevent spectators from interfering with other players' block placement
    	if ( !event.isCancelled() && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null )
    	{
    		Block b = event.getClickedBlock().getRelative(event.getBlockFace());
    		double minX = b.getX() - 1, maxX = b.getX() + 2,
    			   minY = b.getY() - 2, maxY = b.getY() + 1,
    			   minZ = b.getZ() - 1, maxZ = b.getZ() + 2;
    		
    		List<Player> spectators = plugin.getGameMode().getOnlinePlayers(new PlayerFilter().notAlive().world(b.getWorld()).exclude(event.getPlayer()));
    		for ( Player spectator : spectators )
    		{
    			Location loc = spectator.getLocation();
    			if ( loc.getX() >= minX && loc.getX() <= maxX
	    				&& loc.getY() >= minY && loc.getY() <= maxY
						&& loc.getZ() >= minZ && loc.getZ() <= maxZ )
    				spectator.teleport(spectator.getLocation().add(0, 3, 0)); // just teleport them upwards, out of the way of this block place
    		}
    	}
    	
		// eyes of ender can be made to seek out nether fortresses
    	if ( plugin.isEnderEyeRecipeEnabled() && event.getPlayer().getWorld().getEnvironment() == Environment.NETHER && event.getPlayer().getItemInHand() != null && event.getPlayer().getItemInHand().getType() == Material.EYE_OF_ENDER && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) )
    	{
			Location target = plugin.craftBukkit.findNearestNetherFortress(event.getPlayer().getLocation());
			if ( target == null )
				event.getPlayer().sendMessage("No nether fortresses nearby");
			else
			{
				plugin.craftBukkit.createFlyingEnderEye(event.getPlayer(), target);
				event.getPlayer().getItemInHand().setAmount(event.getPlayer().getItemInHand().getAmount() - 1);				
			}
    		
    		event.setCancelled(true);
    		return;
    	}
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event)
    {
		if ( !plugin.isGameWorld(event.getPlayer().getWorld()) )
			return;
		
    	// spectators can't drop items
    	if ( event.getPlayer().getWorld() == plugin.worldManager.stagingWorld || plugin.playerManager.isSpectator(event.getPlayer().getName()) )
    		event.setCancelled(true);
    	
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event)
    {
    	if ( !plugin.isGameWorld(event.getWhoClicked().getWorld()) )
			return;
    	
    	Player player = (Player)event.getWhoClicked();
    	if ( player == null )
    		return;
    	
    	// spectators can't rearrange their inventory ... is that a bit mean?
    	if ( plugin.playerManager.isSpectator(player.getName()) )
    		event.setCancelled(true);
    }
    
	// spectators can't deal or receive damage
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event)
    {
		if ( !plugin.isGameWorld(event.getEntity().getWorld()) )
			return;
		
		event.setCancelled(false);
        if ( event instanceof EntityDamageByEntityEvent )
        {
        	Entity damager = ((EntityDamageByEntityEvent)event).getDamager();
        	if ( damager != null && damager instanceof Player )
        	{
        		if ( PlayerManager.instance.isSpectator(((Player)damager).getName()))
        			event.setCancelled(true);
        	}
        }
        if ( event.isCancelled() || event.getEntity() == null || !(event.getEntity() instanceof Player))
        	return;
        
        Player victim = (Player)event.getEntity();
        
		if(PlayerManager.instance.isSpectator(victim.getName()))
    		event.setCancelled(true);
	}
    
	// can't empty buckets onto protected locations
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event)
    {
		if ( !plugin.isGameWorld(event.getPlayer().getWorld()) )
			return;
		
		Block affected = event.getBlockClicked().getRelative(event.getBlockFace());
		event.setCancelled(plugin.worldManager.isProtectedLocation(affected.getLocation()));
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onCraftItem(CraftItemEvent event)
    {
    	// killer recipes can only be crafter in killer worlds, or we could screw up the rest of the server
    	if ( !plugin.isGameWorld(event.getWhoClicked().getWorld()) )
    	{
    		if ( 	(plugin.isDispenserRecipeEnabled() && plugin.isDispenserRecipe(event.getRecipe()))
    			 || (plugin.isEnderEyeRecipeEnabled() && plugin.isEnderEyeRecipe(event.getRecipe()))
    			 || (plugin.isMonsterEggRecipeEnabled() && plugin.isMonsterEggRecipe(event.getRecipe()))
    			)
    		{
    			event.setCancelled(true);
    		}
    	}
		else
			event.setCancelled(false); // otherwise, allow all crafting
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event)
    {
    	if ( event.getLocation().getWorld() == plugin.worldManager.stagingWorld && event.getSpawnReason() == SpawnReason.NATURAL )
    		event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event)
    {
		if ( !plugin.isGameWorld(event.getEntity().getWorld()) )
			return;
		
		// monsters shouldn't target spectators
    	if( event.getTarget() != null && event.getTarget() instanceof Player && PlayerManager.instance.isSpectator(((Player)event.getTarget()).getName()))
    		event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event)
    {
		if ( !plugin.isGameWorld(event.getPlayer().getWorld()) )
			return;
		
    	if ( plugin.voteManager.isInVote() )
    	{
    		if ( event.getMessage().equalsIgnoreCase("Y") && plugin.voteManager.doVote(event.getPlayer(), true) )
    		{
    			event.setMessage(ChatColor.GREEN + "Y");
    			return;
    		}
    		else if ( event.getMessage().equalsIgnoreCase("N") && plugin.voteManager.doVote(event.getPlayer(), false) )
    		{
    			event.setMessage(ChatColor.RED + "N");
    			return;
    		}
    	}
    	
    	// don't mess with spectator chat if they're in the vote setup conversation
    	if ( event.getPlayer().isConversing() )
    		return;
    	
    	if ( plugin.getGameState() == Killer.GameState.finished || !PlayerManager.instance.isSpectator(event.getPlayer().getName()))
		{// colored player names shouldn't produce colored messages ... spectator chat isn't special when the game is in the "finished" state.
			event.setMessage(ChatColor.RESET + event.getMessage());
    		return;
		}

    	// mark spectator chat, and hide it from non-spectators
    	event.setMessage(ChatColor.YELLOW + "[Spec] " + ChatColor.RESET + event.getMessage());
    	
    	for (Player recipient : new HashSet<Player>(event.getRecipients()))
    		if ( recipient != null && recipient.isOnline() && !PlayerManager.instance.isSpectator(recipient.getName()))
    			event.getRecipients().remove(recipient);
    }
	
    @EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event)
    {
    	if ( event.getPlayer().getWorld() == plugin.worldManager.stagingWorld )
    	{
    		final String playerName = event.getPlayer().getName();
    		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
    			public void run()
    			{
    				Player player = plugin.getServer().getPlayerExact(playerName);
    				if ( player != null )
    					if ( plugin.getGameState().usesGameWorlds && plugin.worldManager.worlds.size() > 0 )
    						plugin.playerManager.teleport(player, plugin.getGameMode().getSpawnLocation(player));
    					else
    						plugin.playerManager.putPlayerInStagingWorld(player);
    			}
    		});
    	}
    	
		if ( plugin.isGameWorld(event.getPlayer().getWorld()) )
			playerJoined(event.getPlayer());
	}
	
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
    	if ( event.getPlayer().getWorld() == plugin.worldManager.stagingWorld )
    		plugin.arenaManager.playerKilled();
    	else if ( plugin.isGameWorld(event.getPlayer().getWorld()) )
			playerQuit(event.getPlayer(), true);
	}
	
	private void playerJoined(Player player)
	{
		// if I log into the staging world (cos I logged out there), move me back to the main world's spawn and clear me out
		if ( player.getWorld() == plugin.worldManager.stagingWorld && plugin.getGameState().usesGameWorlds && plugin.worldManager.worlds.size() > 0 )
		{
			player.getInventory().clear();
			player.setTotalExperience(0);
			plugin.playerManager.teleport(player, plugin.worldManager.worlds.get(0).getSpawnLocation());
		}
		
    	plugin.playerManager.playerJoined(player);
    }
    
	private void playerQuit(Player player, boolean actuallyLeftServer)
	{
		if ( actuallyLeftServer ) // the quit message should be sent to the scoreboard of anyone who this player was invisible to
			for ( Player online : plugin.getOnlinePlayers() )
				if ( !online.canSee(player) )
					plugin.craftBukkit.sendForScoreboard(online, player, false);
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new DelayedDeathEffect(player.getName(), true), 600);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event)
    {
    	if ( !plugin.isGameWorld(event.getEntity().getWorld()) )
    		return;
    	
    	if ( event.getEntity().getWorld() == plugin.worldManager.stagingWorld )
		{
			event.getDrops().clear();
    		event.setDroppedExp(0);

        	if ( event instanceof PlayerDeathEvent )
        	{
        		plugin.arenaManager.playerKilled();
        		((PlayerDeathEvent) event).setDeathMessage(((PlayerDeathEvent) event).getDeathMessage().replace("hit the ground too hard", "fell out of the world"));
        		
        		final Player player = (Player)event.getEntity();
        		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					@Override
					public void run() {
		        		plugin.craftBukkit.forceRespawn(player);
					}
				}, 30);
        	}
        	else
        		plugin.arenaManager.monsterKilled(); // entity killed ... if its a monster in arena mode in the staging world
        	
    		return;
    	}
    	
    	if ( !(event instanceof PlayerDeathEvent) )
    		return;
    	
    	PlayerDeathEvent pEvent = (PlayerDeathEvent)event;
		
    	Player player = pEvent.getEntity();
		if ( player == null )
			return;
		
		if ( plugin.getGameMode().useDiscreetDeathMessages() )
			pEvent.setDeathMessage(ChatColor.RED + player.getName() + " died");	
		
		// the only reason this is delayed is to avoid banning the player before they properly die, if we're banning players on death
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new DelayedDeathEffect(player.getName(), false), 10);
	}
    
    class DelayedDeathEffect implements Runnable
    {
    	String name;
		boolean checkDisconnected;
    	public DelayedDeathEffect(String playerName, boolean disconnect)
		{
			name = playerName;
			checkDisconnected = disconnect;
		}
    	
    	public void run()
    	{
    		OfflinePlayer player = Bukkit.getServer().getPlayerExact(name);
    		if ( player == null )
    			player = Bukkit.getServer().getOfflinePlayer(name);
    		
			if ( checkDisconnected )
			{
				if ( player != null && player.isOnline() )
					return; // player has reconnected, so don't do anything
				
				if ( plugin.playerManager.isAlive(name) )
					plugin.statsManager.playerQuit();
			}
    		plugin.playerManager.playerKilled(player);
    	}
    }
}
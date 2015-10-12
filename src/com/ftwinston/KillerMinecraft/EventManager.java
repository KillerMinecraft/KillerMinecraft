package com.ftwinston.KillerMinecraft;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredListener;

import com.ftwinston.KillerMinecraft.Game.GameState;
import com.ftwinston.KillerMinecraft.Game.PlayerInfo;

class EventManager implements Listener
{
	KillerMinecraft plugin;

	public EventManager(KillerMinecraft instance)
	{
		plugin = instance;
	}

	public void registerEvents(KillerModule module)
	{
		module.eventHandlers = plugin.getPluginLoader().createRegisteredListeners(module, plugin);
	}

	public void unregisterEvents(KillerModule module)
	{
		module.eventHandlers.clear();
	}

	private void fireGameEvent(Event event, Game game) throws EventException
	{
		fireModuleEvents(event, game.getGameMode());
		fireModuleEvents(event, game.getWorldGenerator());
	}
	
	private void fireModuleEvents(Event event, KillerModule module) throws EventException
	{	
		Set<RegisteredListener> listeners = module.eventHandlers.get(event.getClass());
		if ( listeners != null )
			for (RegisteredListener listener : listeners)
				listener.callEvent(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST) // run last, so we can absolutely say where you should respawn, in a Killer game
	public void onEvent(PlayerRespawnEvent event) throws EventException
	{
		final Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game == null )
			return;
		
		event.setRespawnLocation(game.getGameMode().getSpawnLocation(event.getPlayer()));
		fireGameEvent(event, game);
		
		final String playerName = event.getPlayer().getName();
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run()
			{
				Player player = Helper.getPlayer(playerName);
				if ( player == null )
					return;
				
				// when you die a spectator, be made able to fly again when you respawn
				if ( Helper.isSpectator(game, player) )
					Helper.makeSpectator(game, player);
				else
					player.setCompassTarget(game.getCompassTarget(player));
			}
		});
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEvent(PlayerChangedWorldEvent event) throws EventException
	{
		World toWorld = event.getPlayer().getWorld();
		Game fromGame = plugin.getGameForWorld(event.getFrom());
		Game toGame = plugin.getGameForWorld(toWorld);

		if ( fromGame != toGame )
		{
			Player player = event.getPlayer();
			Game playerGame = plugin.getGameForPlayer(player);

			if ( fromGame != null && playerGame == fromGame )
			{
				// have just left a game. If we were still part of that (i.e. haven't just quit), then remove from that game
				playerGame.removePlayerFromGame(player);
			}
			
			if ( toGame != null && playerGame != toGame )
			{
				// not a part of this game ... chuck them back out
				World destination = Bukkit.getServer().getWorlds().get(0);
				Helper.teleport(player, destination.getSpawnLocation());
			}
		}
		else if ( fromGame != null )
		{
			PlayerInfo info = toGame.getPlayerInfo(event.getPlayer());
			if( info != null && !info.isSpectator() )
				event.getPlayer().setCompassTarget(fromGame.getCompassTarget(event.getPlayer()));
			
			fireGameEvent(event, toGame);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEvent(PlayerPortalEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getFrom().getWorld());
		if ( game == null || !PortalHelper.isAllowedToPortal(event.getPlayer()) )
			return;
		
		PortalHelper helper = new PortalHelper(event.getPortalTravelAgent());
		event.setCancelled(true); // we're going to handle implementing the portalling ourselves
		
		game.getGameMode().handlePortal(event.getCause(), event.getPlayer().getLocation(), helper);
		helper.performTeleport(event.getCause(), event.getPlayer());
		fireGameEvent(event, game);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEvent(EntityPortalEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getFrom().getWorld());
		if ( game == null || !PortalHelper.isAllowedToPortal(event.getEntity()) )
			return;
		
		PortalHelper helper = new PortalHelper(event.getPortalTravelAgent());
		event.setCancelled(true); // we're going to handle implementing the portalling ourselves
		
		game.getGameMode().handlePortal(TeleportCause.NETHER_PORTAL, event.getEntity().getLocation(), helper);
		helper.performTeleport(TeleportCause.NETHER_PORTAL, event.getEntity());
		fireGameEvent(event, game);
	}

	// prevent spectators picking up anything
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEvent(PlayerPickupItemEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game == null )
			return;
		
		if( Helper.isSpectator(game, event.getPlayer()) )
			event.setCancelled(true);
		else
			fireGameEvent(event, game);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEvent(ItemSpawnEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getLocation().getWorld());
		
		if ( game == null )
			return;

		if ( plugin.worldManager.isProtectedLocation(game, event.getLocation(), null) )
			event.setCancelled(true);
		else
			fireGameEvent(event, game);
	}
	
	// prevent spectators breaking anything, prevent anyone breaking protected locations
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(BlockBreakEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		
		if ( plugin.worldManager.isProtectedLocation(game, event.getBlock().getLocation(), event.getPlayer())
			|| Helper.isSpectator(game, event.getPlayer())
			)
			event.setCancelled(true);
		else if ( game != null )
			fireGameEvent(event, game);
	}
	
	// prevent spectators breaking frames, prevent anyone breaking protected frames
	// more complicated than above, because we want to protect against breakings that aren't by entities (I guess)
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(HangingBreakEvent event) throws EventException
	{
		Location loc = event.getEntity().getLocation();
		Game game = plugin.getGameForWorld(loc.getWorld());
		
		Player player = null;
		if ( event instanceof HangingBreakByEntityEvent)
		{
			Entity entity = ((HangingBreakByEntityEvent)event).getRemover();
			if ( entity instanceof Player )
			{
				player = (Player)entity;
				if ( Helper.isSpectator(game, player) )
				{
					event.setCancelled(true);
					return;
				}
			}
		}
		
		if ( plugin.worldManager.isProtectedLocation(game, loc, player) )
			event.setCancelled(true);
		else if ( game != null )
			fireGameEvent(event, game);
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(HangingPlaceEvent event) throws EventException
	{
		Location loc = event.getEntity().getLocation();
		Game game = plugin.getGameForWorld(loc.getWorld());
		
		if (Helper.isSpectator(game, event.getPlayer())
			|| plugin.worldManager.isProtectedLocation(game, loc, event.getPlayer())
			)
			event.setCancelled(true);
		else if ( game != null )
			fireGameEvent(event, game);
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(PlayerInteractEntityEvent event) throws EventException
	{	
		Location loc = event.getRightClicked().getLocation();
		Game game = plugin.getGameForWorld(loc.getWorld());
		
		if ( Helper.isSpectator(game, event.getPlayer())
			|| plugin.worldManager.isProtectedLocation(game, loc, event.getPlayer())
			) 
			event.setCancelled(true);
		else if ( game != null )
			fireGameEvent(event, game);
	}
	
	// prevent anyone placing blocks on protected locations
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(BlockPlaceEvent event) throws EventException
	{
		World world = event.getBlock().getWorld();
		Game game = plugin.getGameForWorld(world);
		
		if ( Helper.isSpectator(game, event.getPlayer())
			|| plugin.worldManager.isProtectedLocation(game, event.getBlock().getLocation(), event.getPlayer())
			)
			event.setCancelled(true);
		else if ( game != null )
			fireGameEvent(event, game);
	}
	
	// prevent lava/water from flowing onto protected locations
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(BlockFromToEvent event) throws EventException
	{
		if ( plugin.worldManager.isProtectedLocation(event.getBlock().getLocation(), null) )
		{
			event.setCancelled(true);
			return;
		}
		
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());		
		if ( game != null )
			fireGameEvent(event, game);
	}
	
	// prevent explosions from damaging protected locations
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEvent(EntityExplodeEvent event) throws EventException
	{
		World world = event.getEntity().getWorld();
		Game game = plugin.getGameForWorld(world);
		if ( game == null && !Settings.nothingButKiller ) 
			return;
		
		List<Block> blocks = event.blockList();
		for ( int i=0; i<blocks.size(); i++ )
			if ( plugin.worldManager.isProtectedLocation(game, blocks.get(i).getLocation(), null) )
				{
					blocks.remove(i);
					i--;
				}
		
		if ( game != null )
			fireGameEvent(event, game);
	}
	
	// switching between spectator items
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEvent(PlayerItemHeldEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game == null ) 
			return;
		
		if ( Helper.isSpectator(game, event.getPlayer()) )
		{
			ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
			
			if ( item == null )
				game.getPlayerInfo(event.getPlayer()).spectatorTarget = null;
			else if ( item.getType() == Settings.teleportModeItem )
			{
				event.getPlayer().sendMessage("Free look mode: left click to teleport " + ChatColor.YELLOW + "to" + ChatColor.RESET + " where you're looking, right click to teleport " + ChatColor.YELLOW + "through" + ChatColor.RESET + " through what you're looking");
				game.getPlayerInfo(event.getPlayer()).spectatorTarget = null;
			}
			else if ( item.getType() == Settings.followModeItem )
			{
				event.getPlayer().sendMessage("Follow mode: click to cycle target");
				Player target = plugin.spectatorManager.getNearestFollowTarget(game, event.getPlayer());
				game.getPlayerInfo(event.getPlayer()).spectatorTarget = target.getName();
				if ( target != null )
					plugin.spectatorManager.checkFollowTarget(game, event.getPlayer(), target.getName());
			}
			else
				game.getPlayerInfo(event.getPlayer()).spectatorTarget = null;
			return;
		}
		
		fireGameEvent(event, game);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEvent(PlayerInteractEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());		
		if ( game == null ) 
			return;

		// spectators can't interact with anything, but they do use clicking to handle their spectator stuff
		if ( Helper.isSpectator(game, event.getPlayer()) )
		{
			event.setCancelled(true);
			Material held = event.getPlayer().getItemInHand().getType();
			
			if ( held == Settings.teleportModeItem )
			{
				if ( event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK )
					plugin.spectatorManager.doSpectatorTeleport(event.getPlayer(), false);
				else if ( event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK )
					plugin.spectatorManager.doSpectatorTeleport(event.getPlayer(), true);
			}
			else if ( held == Settings.followModeItem )
			{
				PlayerInfo info = game.getPlayerInfo(event.getPlayer());
				
				if ( event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK )
				{
					String target = plugin.spectatorManager.getNextFollowTarget(game, event.getPlayer(), info.spectatorTarget, true);
					info.spectatorTarget = target;
					plugin.spectatorManager.checkFollowTarget(game, event.getPlayer(), target);
					event.getPlayer().sendMessage("Following " + target);
				}
				else if ( event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK )
				{
					String target = plugin.spectatorManager.getNextFollowTarget(game, event.getPlayer(), info.spectatorTarget, false);
					info.spectatorTarget = target;
					plugin.spectatorManager.checkFollowTarget(game, event.getPlayer(), target);
					event.getPlayer().sendMessage("Following " + target);
				}
			}
			return;
		}
		// prevent spectators from interfering with other players' block placement
		else if ( !event.isCancelled() && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null )
		{
			Block b = event.getClickedBlock().getRelative(event.getBlockFace());
			double minX = b.getX() - 1, maxX = b.getX() + 2,
				   minY = b.getY() - 2, maxY = b.getY() + 1,
				   minZ = b.getZ() - 1, maxZ = b.getZ() + 2;
			
			List<Player> spectators = game.getGameMode().getOnlinePlayers(new PlayerFilter().onlySpectators().world(b.getWorld()).exclude(event.getPlayer()));
			for ( Player spectator : spectators )
			{
				Location loc = spectator.getLocation();
				if ( loc.getX() >= minX && loc.getX() <= maxX
						&& loc.getY() >= minY && loc.getY() <= maxY
						&& loc.getZ() >= minZ && loc.getZ() <= maxZ )
					spectator.teleport(spectator.getLocation().add(0, 3, 0)); // just teleport them upwards, out of the way of this block place
			}
		}
		
		if ( !event.isCancelled() )
			fireGameEvent(event, game);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEvent(PlayerDropItemEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game == null ) 
			return;
		
		// spectators can't drop items
		if ( Helper.isSpectator(game, event.getPlayer()) )
			event.setCancelled(true);
		else
			fireGameEvent(event, game);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEvent(InventoryClickEvent event) throws EventException
	{
		Player player = (Player)event.getWhoClicked();
		if ( player != null && MenuManager.checkEvent(player, event) )
			return;
		
		World world = event.getWhoClicked().getWorld();
		Game game = plugin.getGameForWorld(world);
		if ( game == null )
			return;
		
		// spectators can't rearrange their inventory ... is that a bit mean?
		if ( Helper.isSpectator(game, player) )
			event.setCancelled(true);
		else
			fireGameEvent(event, game);
	}
	
	// spectators can't deal or receive damage
	@EventHandler(priority = EventPriority.HIGH)
	public void onEvent(EntityDamageEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game == null ) 
			return;
		
		if ( event instanceof EntityDamageByEntityEvent )
		{
			Player attacker = Helper.getAttacker(event);
			if ( attacker != null )
			{
				if ( Helper.isSpectator(game, attacker) )
				{
					event.setCancelled(true);
					return;
				}
			}
		}
		if ( event.getEntity() instanceof Player)
		{
			Player victim = (Player)event.getEntity();
		
			if( Helper.isSpectator(game, victim) )
			{
				event.setCancelled(true);
				return;
			}
		}
		
		fireGameEvent(event, game);
	}
	
	// can't empty buckets onto protected locations
	@EventHandler(priority = EventPriority.HIGH)
	public void onEvent(PlayerBucketEmptyEvent event) throws EventException
	{
		if ( plugin.worldManager.isProtectedLocation(event.getBlockClicked().getRelative(event.getBlockFace()).getLocation(), null) )
		{
			event.setCancelled(true);
			return;
		}
		
		Game game = plugin.getGameForWorld(event.getBlockClicked().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onEvent(PrepareItemCraftEvent event) throws EventException
	{
		plugin.recipeManager.handleCraftEvent(event);

		if ( event.getViewers().size() == 0 )
			return;
		Game game = plugin.getGameForWorld(event.getViewers().get(0).getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(EntityTargetEvent event) throws EventException
	{
		World world = event.getEntity().getWorld();
		Game game = plugin.getGameForWorld(world);
		if ( game == null ) 
			return;
		
		// monsters shouldn't target spectators
		if( event.getTarget() != null && event.getTarget() instanceof Player && Helper.isSpectator(game, (Player)event.getTarget()) )
			event.setCancelled(true);
		else
			fireGameEvent(event, game);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEvent(AsyncPlayerChatEvent event) throws EventException
	{
		// don't mess with chat if they're in a conversation
		if ( event.getPlayer().isConversing() )
			return;
			
		World world = event.getPlayer().getWorld();
		Game game = plugin.getGameForWorld(world);
		if ( game == null )
		{
			if ( Settings.filterChat )
			{// this player isn't in a game world, so players that are in game worlds should not see this message
				for (Player recipient : new HashSet<Player>(event.getRecipients()))
					if ( recipient != null && recipient.isOnline() && plugin.getGameForWorld(recipient.getWorld()) != null )
						event.getRecipients().remove(recipient);
			}
			
			return;
		}
		/*
		boolean isVote = false;
		if ( plugin.voteManager.isInVote() )
		{
			if ( event.getMessage().equalsIgnoreCase("Y") && plugin.voteManager.doVote(event.getPlayer(), true) )
			{
				event.setMessage(ChatColor.GREEN + "Y");
				isVote = true;
			}
			else if ( event.getMessage().equalsIgnoreCase("N") && plugin.voteManager.doVote(event.getPlayer(), false) )
			{
				event.setMessage(ChatColor.RED + "N");
				isVote = true;
			}
		}
		*/
		if ( Settings.filterChat )
		{// players that are not in this game's worlds should not see this message
				for (Player recipient : new HashSet<Player>(event.getRecipients()))
					if ( recipient != null && recipient.isOnline()
						&& recipient.getWorld() != world
						&& plugin.getGameForWorld(recipient.getWorld()) != game )
						event.getRecipients().remove(recipient);
		}
		/*
		if ( isVote )
			return;
		*/
		if ( game.getGameState() != GameState.FINISHED && Helper.isSpectator(game, event.getPlayer()) )
		{// mark spectator chat, and hide it from non-spectators
			event.setMessage(ChatColor.YELLOW + "[Spec] " + ChatColor.RESET + event.getMessage());
			
			for (Player recipient : new HashSet<Player>(event.getRecipients()))
				if ( recipient != null && recipient.isOnline() && Helper.isSpectator(game, recipient.getPlayer()))
					event.getRecipients().remove(recipient);
			return;
		}
		else
		{// colored player names shouldn't produce colored messages ... spectator chat isn't special when the game is in the "finished" state.
			event.setMessage(ChatColor.RESET + event.getMessage());
		}
		
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEvent(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		World world = player.getWorld();
		
		final Game game = plugin.getGameForWorld(world);
		if ( game == null )
		{
			if ( Settings.nothingButKiller )
			{
				ChunkGenerator gen = world.getGenerator();
				if ( gen != null && gen.getClass() == StagingWorldGenerator.class )
					Helper.teleport(event.getPlayer(), world.getSpawnLocation());
			}

			if ( player != null && plugin.playerManager.restorePlayerData(player) )
				plugin.playerManager.playerDataChanged();
	
			return;
		}


		if ( player != null && plugin.getGameForPlayer(player) != game )
		{// if you log in into a game you're not part of, get chucked straight out
			plugin.playerManager.restorePlayerData(player);
			plugin.playerManager.playerDataChanged();
		}
		else
			game.getGameMode().playerReconnected(player);
		
		if ( Settings.filterScoreboard )
		{// hide this person from the scoreboard of any games that they aren't in
			for ( Game otherGame : plugin.games )
				if ( otherGame != game )
					for ( Player other : otherGame.getOnlinePlayers(new PlayerFilter().exclude(player)) )
						plugin.craftBukkit.sendForScoreboard(other, player, false);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEvent(PlayerQuitEvent event)
	{
		MenuManager.inventoryClosed(event.getPlayer());
		
		final Game game = plugin.getGameForPlayer(event.getPlayer());
		if ( game == null )
			return;
		
		if ( !game.getGameState().usesWorlds )
			game.removePlayerFromGame(event.getPlayer(), true); // disconnecting when in a non-active game should just chuck you out
		else
		{
			final String playerName = event.getPlayer().getName();
			
			// if they don't reconnect within a few seconds, remove them from the game
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				public void run()
				{
					OfflinePlayer player = Helper.getOfflinePlayer(playerName);
					if ( player != null )
						return;
					
					game.removePlayerFromGame(player, true);
				}
			}, 100);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onEvent(EntityDeathEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
	 	if ( game == null )
	 		return;
	 	
		if ( !(event instanceof PlayerDeathEvent) )
		{
			fireGameEvent(event, game);
			return;
		}

		PlayerDeathEvent pEvent = (PlayerDeathEvent)event;
		
		Player player = pEvent.getEntity();
		if ( player == null )
			return;
		
		fireGameEvent(pEvent, game);
		game.broadcastMessage(pEvent.getDeathMessage());
		pEvent.setDeathMessage("");
		
		MenuManager.inventoryClosed(player);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockBurnEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockCanBuildEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockDamageEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockDispenseEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockExpEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockFadeEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockFormEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockGrowEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockIgniteEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockPhysicsEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockPistonExtendEvent event) throws EventException
	{
		// prevent pistons pushing things into/out of protected locations
		if ( plugin.worldManager.isProtectedLocation(event.getBlock().getLocation(), null) )
		{
			event.setCancelled(true);
			return;
		}
		
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockPistonRetractEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockRedstoneEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.BlockSpreadEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.LeavesDecayEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.NotePlayEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.block.SignChangeEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.enchantment.EnchantItemEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEnchanter().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.enchantment.PrepareItemEnchantEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEnchanter().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.CreatureSpawnEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getLocation().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.CreeperPowerEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.EntityChangeBlockEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.EntityCombustEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.EntityCreatePortalEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.EntityInteractEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.EntityPortalEnterEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.EntityPortalExitEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.EntityRegainHealthEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.EntityShootBowEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.EntityTameEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.EntityTeleportEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.ExpBottleEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.ExplosionPrimeEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.FoodLevelChangeEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.ItemDespawnEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.PigZapEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.PotionSplashEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.ProjectileHitEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.ProjectileLaunchEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.SheepDyeWoolEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.SheepRegrowWoolEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.entity.SlimeSplitEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getEntity().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.inventory.BrewEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.inventory.FurnaceBurnEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.inventory.FurnaceSmeltEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getBlock().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.inventory.InventoryCloseEvent event) throws EventException
	{
		Player player = (Player)event.getPlayer();
		if ( player != null )
			MenuManager.inventoryClosed(player);
		
		Game game = plugin.getGameForWorld(player.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.inventory.InventoryDragEvent event) throws EventException
	{
		Player player = (Player)event.getWhoClicked();
		if ( player != null && MenuManager.checkEvent(player, event) )
			return;
		
		Game game = plugin.getGameForWorld(event.getWhoClicked().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.inventory.InventoryEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getViewers().get(0).getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.inventory.InventoryMoveItemEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getInitiator().getViewers().get(0).getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.inventory.InventoryOpenEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.inventory.InventoryPickupItemEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getItem().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerAnimationEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerBedEnterEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerBedLeaveEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerBucketFillEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerChannelEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerChatTabCompleteEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerCommandPreprocessEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerEditBookEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerEggThrowEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerExpChangeEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerFishEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerGameModeChangeEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerItemBreakEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerItemConsumeEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerKickEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerLevelChangeEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerMoveEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerShearEntityEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerTeleportEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerToggleFlightEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerToggleSneakEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerToggleSprintEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.player.PlayerVelocityEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getPlayer().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.server.MapInitializeEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getMap().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.vehicle.VehicleBlockCollisionEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getVehicle().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.vehicle.VehicleCreateEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getVehicle().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.vehicle.VehicleDamageEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getVehicle().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.vehicle.VehicleDestroyEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getVehicle().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.vehicle.VehicleEnterEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getVehicle().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.vehicle.VehicleEntityCollisionEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getVehicle().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.vehicle.VehicleExitEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getVehicle().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.vehicle.VehicleMoveEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getVehicle().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.vehicle.VehicleUpdateEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getVehicle().getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.weather.LightningStrikeEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.weather.ThunderChangeEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.weather.WeatherChangeEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.world.ChunkLoadEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.world.ChunkPopulateEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.world.ChunkUnloadEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.world.PortalCreateEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.world.SpawnChangeEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.world.StructureGrowEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.world.WorldInitEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.world.WorldLoadEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.world.WorldSaveEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEvent(org.bukkit.event.world.WorldUnloadEvent event) throws EventException
	{
		Game game = plugin.getGameForWorld(event.getWorld());
		if ( game != null )
			fireGameEvent(event, game);
	}
}
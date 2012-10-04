package com.ftwinston.Killer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.server.RegionFile;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import com.ftwinston.Killer.Killer.GameState;

public class WorldManager
{
		public static WorldManager instance;
		
		private Killer plugin;
		public WorldManager(Killer killer)
		{
			plugin = killer;
			instance = this;
			
			seedGen = new Random();
			bindRegionFiles();

			// try to find the main world, based on the config-provided name
/*			mainWorld = getWorld(Settings.killerWorldName, Environment.NORMAL, true);
			netherWorld = getWorld(Settings.killerWorldName + "_nether", Environment.NETHER, true);
			endWorld = getWorld(Settings.killerWorldName + "_the_end", Environment.THE_END, false);*/
		}
		
		public void onDisable()
		{
			regionfiles = null;
			rafField = null;
		}
		
		Random seedGen;
		
		@SuppressWarnings("rawtypes")
		private static HashMap regionfiles;
		private static Field rafField;
		
		public World mainWorld;
		public World stagingWorld;
		public World netherWorld;
		
		static final int plinthPeakHeight = 76, spaceBetweenPlinthAndGlowstone = 4;
		static final int plinthSpawnOffsetX = 20, plinthSpawnOffsetZ = 0;
		public Location createPlinth(World world)
		{
			Location spawnPoint = world.getSpawnLocation();
			int x = spawnPoint.getBlockX() + plinthSpawnOffsetX;
			int z = spawnPoint.getBlockZ() + plinthSpawnOffsetZ;
			
			// a 3x3 column from bedrock to the plinth height
			for ( int y = 0; y < plinthPeakHeight; y++ )
				for ( int ix = x - 1; ix < x + 2; ix++ )
					for ( int iz = z - 1; iz < z + 2; iz++ )
					{
						Block b = world.getBlockAt(ix, y, iz);
						b.setType(Material.BEDROCK);
					}
			
			// with one block sticking up from it
			int y = plinthPeakHeight;
			for ( int ix = x - 1; ix < x + 2; ix++ )
					for ( int iz = z - 1; iz < z + 2; iz++ )
					{
						Block b = world.getBlockAt(ix, y, iz);
						b.setType(ix == x && iz == z ? Material.BEDROCK : Material.AIR);
					}
			
			// that has a pressure plate on it
			y = plinthPeakHeight + 1;
			Location pressurePlateLocation = new Location(world, x, y, z);
			for ( int ix = x - 1; ix < x + 2; ix++ )
					for ( int iz = z - 1; iz < z + 2; iz++ )
					{
						Block b = world.getBlockAt(ix, y, iz);
						b.setType(ix == x && iz == z ? Material.STONE_PLATE : Material.AIR);
					}
					
			// then a space
			for ( y = plinthPeakHeight + 2; y <= plinthPeakHeight + spaceBetweenPlinthAndGlowstone; y++ )
				for ( int ix = x - 1; ix < x + 2; ix++ )
					for ( int iz = z - 1; iz < z + 2; iz++ )
					{
						Block b = world.getBlockAt(ix, y, iz);
						b.setType(Material.AIR);
					}
			
			// and then a 1x1 pillar of glowstone, up to max height
			for ( y = plinthPeakHeight + spaceBetweenPlinthAndGlowstone + 1; y < world.getMaxHeight(); y++ )
				for ( int ix = x - 1; ix < x + 2; ix++ )
					for ( int iz = z - 1; iz < z + 2; iz++ )
					{
						Block b = world.getBlockAt(ix, y, iz);
						b.setType(ix == x && iz == z ? Material.GLOWSTONE : Material.AIR);
					}
			
			return pressurePlateLocation;
		}
/*
		private void moveWorldToFront(World thisWorld)
		{
			try
			{
				Field f = plugin.getServer().getClass().getDeclaredField("worlds");
				f.setAccessible(true);
				@SuppressWarnings("unchecked")
				Map<String, World> worlds = (Map<String, World>)f.get(plugin.getServer());
				f.setAccessible(false);
				
				// remove & re-add all worlds except the thisWorld
				String[] worldNames = new String[0];
				worldNames = worlds.keySet().toArray(worldNames);
				for ( String name : worldNames )
				{
					if ( name.equals(thisWorld.getName()) )
						continue;
					
					World tmp = worlds.get(name);
					worlds.remove(name);
					worlds.put(name, tmp);
				}
			}
			catch ( IllegalAccessException ex )
			{
				plugin.log.warning("Error removing world from bukkit master list: " + ex.getMessage());
			}
			catch  ( NoSuchFieldException ex )
			{
				plugin.log.warning("Error removing world from bukkit master list: " + ex.getMessage());
			}
			
			WorldServer worldServer = ((CraftWorld)thisWorld).getHandle();
			
			MinecraftServer ms = plugin.getMinecraftServer();
			ms.worlds.remove(ms.worlds.indexOf(worldServer));
			ms.worlds.add(0, worldServer);
		}
		
		private void moveWorldToEnd(World thisWorld)
		{
			try
			{
				Field f = plugin.getServer().getClass().getDeclaredField("worlds");
				f.setAccessible(true);
				@SuppressWarnings("unchecked")
				Map<String, World> worlds = (Map<String, World>)f.get(plugin.getServer());
				f.setAccessible(false);
				
				worlds.remove(thisWorld.getName());
				worlds.put(thisWorld.getName(), thisWorld);
			}
			catch ( IllegalAccessException ex )
			{
				plugin.log.warning("Error removing world from bukkit master list: " + ex.getMessage());
			}
			catch  ( NoSuchFieldException ex )
			{
				plugin.log.warning("Error removing world from bukkit master list: " + ex.getMessage());
			}
			
			WorldServer worldServer = ((CraftWorld)thisWorld).getHandle();
			
			MinecraftServer ms = plugin.getMinecraftServer();
			ms.worlds.remove(ms.worlds.indexOf(worldServer));
			ms.worlds.add(worldServer);
		}*/
		
		public void hijackDefaultWorldAsStagingWorld(String name)
		{
			// as the config may have changed, delete the existing staging world 
    		try
			{
    			delete(new File(plugin.getServer().getWorldContainer() + File.separator + name));
			}
			catch ( Exception e )
			{
			}
    		
			// in the already-loaded server configuration, create/update an entry specifying the generator to be used for the default world, which is the staging world
			YamlConfiguration configuration = plugin.getBukkitConfiguration();
			
			ConfigurationSection section = configuration.getConfigurationSection("worlds");
			if ( section == null )
				section = configuration.createSection("worlds");
			
			ConfigurationSection worldSection = section.getConfigurationSection(name);
			if ( worldSection == null )
				worldSection = section.createSection(name);
			
			worldSection.set("generator", "Killer");
			
			// disable the end and the nether, for the staging world. We'll re-enable once this has generated.
			final String prevAllowNether = plugin.getMinecraftServer().getPropertyManager().properties.getProperty("allow-nether", "true");
			final boolean prevAllowEnd = configuration.getBoolean("settings.allow-end", true);
			plugin.getMinecraftServer().getPropertyManager().properties.put("allow-nether", "false");			
			configuration.set("settings.allow-end", false);
			
			// restore server settings, once it's finished generating
	        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					plugin.getMinecraftServer().getPropertyManager().properties.put("allow-nether", prevAllowNether);
					plugin.getBukkitConfiguration().set("settings.allow-end", prevAllowEnd);
					
					plugin.getMinecraftServer().getPropertyManager().savePropertiesFile();
					try
					{
						plugin.getBukkitConfiguration().save((File)plugin.getMinecraftServer().options.valueOf("bukkit-settings"));
					}
					catch ( IOException ex )
					{
					}
				}
	        });
		}
		
		
		public void createStagingWorld(final String name) 
		{
	        World world = plugin.getServer().getWorld(name);
	        if ( world != null )
	        {// staging world already existed; delete it, because we might want to change it to correspond to config changes
	        	
	        	plugin.log.info("Deleting staging world, cos it already exists...");
	        	
	        	
	        	forceUnloadWorld(world, null);
	        	world = null;
	        	try
	        	{
	        		Thread.sleep(200);
	        	}
	        	catch ( InterruptedException ex )
	        	{
	        	}
	        	
	        	clearWorldReference(name);
				
	    		try
				{
	    			delete(new File(plugin.getServer().getWorldContainer() + File.separator + name));
				}
				catch ( Exception e )
				{
				}
	        }
	        
	        // delay this by 1 tick, so that some other worlds have already been created, so that the getDefaultGameMode call in CraftServer.createWorld doesn't crash
	        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
			        WorldCreator wc = new WorldCreator(name);
		    		StagingWorldGenerator gen = new StagingWorldGenerator();
			        wc.generator(gen);
					wc.environment(Environment.NORMAL);
					World newWorld = CreateWorld(wc, true);
					wc.generateStructures(true);
					
					stagingWorldCreated(newWorld);
				}
			}, 1);
		}
		
		public void stagingWorldCreated(World world)
		{
			world.setSpawnLocation(8, 2, StagingWorldGenerator.startButtonZ);
			world.setPVP(false);
	        world.setAutoSave(false); // don't save changes to the staging world
	        
	        stagingWorld = world;
		}
		
		public Location getStagingWorldSpawnPoint()
		{
			return new Location(stagingWorld, 8.5, 2, StagingWorldGenerator.startButtonZ + 0.5);
		}
		
		
		public World CreateWorld(WorldCreator wc, boolean loadChunks)
		{
			World world = plugin.getServer().createWorld(wc);
			
			if (world != null)
			{
				if ( loadChunks )
				{
					final int keepdimension = 7;
					int spawnx = world.getSpawnLocation().getBlockX() >> 4;
				    int spawnz = world.getSpawnLocation().getBlockZ() >> 4;
					for (int x = -keepdimension; x < keepdimension; x++)
						for (int z = -keepdimension; z < keepdimension; z++)
							world.loadChunk(spawnx + x, spawnz + z);
				}
				world.setKeepSpawnInMemory(loadChunks);
				plugin.log.info("World '" + world.getName() + "' created successfully!");
			}
			else
				plugin.log.info("World creation failed!");
			
			return world;
		}
		
		public void removeAllItems(World world)
		{
			List<Entity> list = world.getEntities();
			Iterator<Entity> entities = list.iterator();
			while (entities.hasNext())
			{
				Entity entity = entities.next();
				if (entity instanceof Item)
					entity.remove();
			}
		}
		
		@SuppressWarnings("rawtypes")
		private void bindRegionFiles()
		{
	        try
	        {
	        	Field a = net.minecraft.server.RegionFileCache.class.getDeclaredField("a");
	        	a.setAccessible(true);
				regionfiles = (HashMap) a.get(null);
				rafField = net.minecraft.server.RegionFile.class.getDeclaredField("c");
				rafField.setAccessible(true);
	        	plugin.log.info("Successfully bound variable to region file cache.");
	        	plugin.log.info("File references to unloaded worlds will be cleared!");
			}
	        catch (Throwable t)
	        {
	        	plugin.log.warning("Failed to bind to region file cache.");
	        	plugin.log.warning("Files will stay referenced after being unloaded!");
				t.printStackTrace();
			}
		}
		
		@SuppressWarnings("rawtypes")
		private boolean clearWorldReference(String worldName)
		{
			if (regionfiles == null) return false;
			if (rafField == null) return false;
			
			ArrayList<Object> removedKeys = new ArrayList<Object>();
			try
			{
				for (Object o : regionfiles.entrySet())
				{
					Map.Entry e = (Map.Entry) o;
					File f = (File) e.getKey();
					
					if (f.toString().startsWith("." + File.separator + worldName))
					{
						SoftReference ref = (SoftReference) e.getValue();
						try
						{
							RegionFile file = (RegionFile) ref.get();
							if (file != null)
							{
								RandomAccessFile raf = (RandomAccessFile) rafField.get(file);
								raf.close();
								removedKeys.add(f);
							}
						}
						catch (Exception ex)
						{
							ex.printStackTrace();
						}
					}
				}
			}
			catch (Exception ex)
			{
				plugin.log.warning("Exception while removing world reference for '" + worldName + "'!");
				ex.printStackTrace();
			}
			for (Object key : removedKeys)
				regionfiles.remove(key);
			
			return true;
		}
		
		private void forceUnloadWorld(World world, World movePlayersTo)
		{
			world.setAutoSave(false);
			if ( movePlayersTo == null )
				for ( Player player : world.getPlayers() )
					player.kickPlayer("World is being regenerated... and you were in it!");
			else
			{
				for ( Player player : world.getPlayers() )
					plugin.playerManager.putPlayerInWorld(player,  movePlayersTo);
			}
			
			String worldName = world.getName();
			if ( !plugin.getServer().unloadWorld(world, false) )
				plugin.log.warning("Error unloading world: " + worldName);
		}

		public void deleteWorlds()
		{
			plugin.log.info("Clearing out old worlds...");
			
			
			if ( mainWorld != null )
				forceUnloadWorld(mainWorld, stagingWorld);
			
			if ( netherWorld != null )
				forceUnloadWorld(netherWorld, stagingWorld);
			
			mainWorld = netherWorld = null;
			
			// now we want to try to delete the world folders
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new WorldDeleter(Settings.killerWorldName, Settings.killerWorldName + "_nether"), 80);
		}
		
		private class WorldDeleter implements Runnable
	    {
	    	String world1, world2;
	    	
	    	static final long retryDelay = 30;
	    	static final int maxRetries = 5;
	    	int attempt;
	    	
	    	public WorldDeleter(String name1, String name2)
	    	{
	    		attempt = 0;
	    		world1 = name1;
	    		world2 = name2;
    		}
	    	
	    	public void run()
	    	{
	    		boolean allGood = deleteWorld(world1) && deleteWorld(world2);
	    			
	    		if ( !allGood )
		    		if ( attempt < maxRetries )
	    			{
	    				plugin.log.info("Retrying world data deletion...");
	    				attempt++;
						plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, this, retryDelay);
						return;
	    			}
		    		else
		    			plugin.log.warning("Failed to delete some world information!");
	    	}
	    	
	    	private boolean deleteWorld(String worldName)
	    	{
	    		clearWorldReference(worldName);
	    		boolean allGood = true;
				
	    		try
				{
	    			if ( !delete(new File(plugin.getServer().getWorldContainer() + File.separator + worldName)) )
	    				allGood = false;
				}
				catch ( Exception e )
				{
					plugin.log.info("An error occurred when deleting the " + worldName + " world: " + e.getMessage());
				}
	    		
	    		return allGood;
	    	}
	    }
		
		private boolean delete(File folder)
		{
			if ( !folder.exists() )
				return true;
			boolean retVal = true;
			if (folder.isDirectory())
				for (File f : folder.listFiles())
					if (!delete(f))
					{
						retVal = false;
						//plugin.log.warning("Failed to delete file: " + f.getName());
					}
			return folder.delete() && retVal;
		}
		
		public void setupButtonClicked(int x, int z)
		{
			if ( x == StagingWorldGenerator.startButtonX )
			{
				if ( z == StagingWorldGenerator.startButtonZ )
				{
					plugin.log.info("Clicked force start button");
					if ( plugin.getOnlinePlayers().size() >= plugin.getGameMode().absMinPlayers() )
						plugin.setGameState(GameState.worldGeneration);
					else
						plugin.setGameState(GameState.stagingWorldConfirm);
				}
				else if ( z == StagingWorldGenerator.overrideButtonZ )
				{
					plugin.log.info("Clicked override button");
					plugin.setGameState(GameState.worldGeneration);
				}
				else if ( z == StagingWorldGenerator.cancelButtonZ )
				{
					plugin.log.info("Clicked cancel button");
					plugin.setGameState(GameState.stagingWorldReady);
				}
				else
					plugin.log.info("Clicked unrecognised button at z=" + z + ". Start button is at z=" + StagingWorldGenerator.startButtonZ + ".");
			}
			else if ( x == StagingWorldGenerator.gameModeButtonX )
			{
				GameMode gameMode = GameMode.get((z-8)/3);
				if ( plugin.setGameMode(gameMode) )
				{
					if ( plugin.getGameState() != GameState.stagingWorldReady )
						plugin.setGameState(GameState.stagingWorldReady);
				}
				
				plugin.log.info("Clicked on game mode: " + gameMode.getName());
				// ... update game mode option buttons?
			}
			else if ( z == StagingWorldGenerator.worldOptionZ )
			{
				WorldOption world = WorldOption.get((x-7)/2);
				if ( plugin.setWorldOption(world) )	
				{
					if ( plugin.getGameState() != GameState.stagingWorldReady )
						plugin.setGameState(GameState.stagingWorldReady);
				}
				
				plugin.log.info("Clicked on world option:" + world.getName());
			}
			else if ( z == StagingWorldGenerator.gameModeOptionZ )
			{
				plugin.log.info("Clicked on game mode option #" + (x-7)/2);
			}
		}
		
		public void showStartButton(boolean show)
		{
			Block button = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, 2, StagingWorldGenerator.startButtonZ);
			Block sign = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, 3, StagingWorldGenerator.startButtonZ);
			
			if ( show )
			{
				button.setType(Material.STONE_BUTTON);
				button.setData((byte)0x2);
				
				sign.setType(Material.WALL_SIGN);
				sign.setData((byte)0x4);
				Sign s = (Sign)sign.getState();
				s.setLine(1, "Push to");
				s.setLine(2, "start the game");
				s.update();
			}
			else
			{
				button.setType(Material.AIR);
				sign.setType(Material.AIR);
			}
		}
		
		public void showConfirmButtons(boolean show)
		{
			Block bOverride = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, 2, StagingWorldGenerator.overrideButtonZ);
			Block bCancel = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, 2, StagingWorldGenerator.cancelButtonZ);
			
			Block sOverride = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, 3, StagingWorldGenerator.overrideButtonZ);
			Block sCancel = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, 3, StagingWorldGenerator.cancelButtonZ);
			
			if ( show )
			{
				bOverride.setType(Material.STONE_BUTTON);
				bOverride.setData((byte)0x2);
				
				bCancel.setType(Material.STONE_BUTTON);
				bCancel.setData((byte)0x2);
				
				sOverride.setType(Material.WALL_SIGN);
				sOverride.setData((byte)0x4);
				Sign s = (Sign)sOverride.getState();
				s.setLine(1, "Push to");
				s.setLine(2, "confirm the");
				s.setLine(3, "game mode");
				s.update();
				
				sCancel.setType(Material.WALL_SIGN);
				sCancel.setData((byte)0x4);
				s = (Sign)sCancel.getState();
				s.setLine(1, "Push to");
				s.setLine(2, "cancel this");
				s.setLine(3, "game mode");
				s.update();
			}
			else
			{
				bOverride.setType(Material.AIR);
				bCancel.setType(Material.AIR);
				sOverride.setType(Material.AIR);
				sCancel.setType(Material.AIR);
			}
		}
		
		public void generateWorlds(WorldOption generator, Runnable runWhenDone)
		{
			plugin.broadcastMessage("Preparing new worlds...");
			generator.create();
			
	        if ( plugin.getGameMode().usesPlinth() ) // create a plinth in the main world. Always done with the same offset, so if the world already has a plinth, it should just get overwritten.
	        	plugin.plinthPressurePlateLocation = createPlinth(mainWorld);
	        
			// run whatever task was passed in
			if ( runWhenDone != null )
				runWhenDone.run();
		}
}
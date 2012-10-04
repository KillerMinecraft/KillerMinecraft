package com.ftwinston.Killer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegionFile;
import net.minecraft.server.WorldType;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

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
		public World endWorld;
		
		private World getWorld(String name, Environment type, boolean getAnyOnFailure)
		{
			List<World> worlds = plugin.getServer().getWorlds();
			for ( World world : worlds )
				if ( world.getName().equals(name) )
					return world;
			
			if ( !getAnyOnFailure )
				return null;

			// couldn't find world name specified, so get the first world of the right type
			for ( World world : worlds )
				if ( world.getEnvironment() == type )
				{
					plugin.log.warning("Couldn't find \"" + name + "\" world, using " + world.getName() + " instead");
					return world;
				}
		
			// still couldn't find something suitable...
			plugin.log.warning("Couldn't find \"" + name + "\" world, using " + worlds.get(0).getName() + " instead");
			return worlds.get(0);
		}
		
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
			world.setSpawnLocation(8, 2, StagingWorldGenerator.forceStartButtonZ);
			world.setPVP(false);
	        world.setAutoSave(false); // don't save changes to the staging world
	        
	        stagingWorld = world;
		}
		
		public Location getStagingWorldSpawnPoint()
		{
			return new Location(stagingWorld, 8.5, 2, StagingWorldGenerator.forceStartButtonZ + 0.5);
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
			
			int i = 0;
			if ( mainWorld != null )
				i++;
			if ( netherWorld != null )
				i++;
			if ( endWorld != null )
				i++;
			
			String[] worldNames = new String[i];
			i=0;
			
			if ( mainWorld != null )
			{
				forceUnloadWorld(mainWorld, stagingWorld);
				worldNames[i++] = mainWorld.getName();
			}
				
			if ( netherWorld != null )
			{
				forceUnloadWorld(netherWorld, stagingWorld);
				worldNames[i++] = netherWorld.getName();
			}
				
			if ( endWorld != null )
			{
				forceUnloadWorld(endWorld, stagingWorld);
				worldNames[i++] = endWorld.getName();
			}
			
			// now we want to try to delete the world folders
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new WorldDeleter(worldNames), 80);
		}
		
		private class WorldDeleter implements Runnable
	    {
	    	String[] worlds;
	    	
	    	static final long retryDelay = 30;
	    	static final int maxRetries = 5;
	    	int attempt;
	    	
	    	public WorldDeleter(String[] worlds)
	    	{
	    		attempt = 0;
	    		this.worlds = worlds;
    		}
	    	
	    	public void run()
	    	{
	    		boolean allGood = true;
    			for ( String worldName : worlds )
    			{
    				clearWorldReference(worldName);
    				
		    		try
					{
		    			if ( !delete(new File(plugin.getServer().getWorldContainer() + File.separator + worldName)) )
		    				allGood = false;
					}
					catch ( Exception e )
					{
						plugin.log.info("An error occurred when deleting the " + worldName + " world: " + e.getMessage());
					}
    			}
	    			
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
				if ( z == StagingWorldGenerator.forceStartButtonZ )
					plugin.log.info("Clicked force start button");
				else if ( z == StagingWorldGenerator.overrideButtonZ )
					plugin.log.info("Clicked override button");
				if ( z == StagingWorldGenerator.cancelButtonZ )
					plugin.log.info("Clicked cancel button");
			}
			else if ( x == StagingWorldGenerator.gameModeButtonX )
			{
				plugin.log.info("Clicked on game mode #" + (z-8)/3);
			}
			else if ( z == StagingWorldGenerator.worldOptionZ )
			{
				plugin.log.info("Clicked on world option #" + (x-7)/2);
			}
			else if ( z == StagingWorldGenerator.gameModeOptionZ )
			{
				plugin.log.info("Clicked on game mode option #" + (x-7)/2);
			}
		}
		
		public void generateWorlds(Runnable runWhenDone)
		{
			// todo: this shouldn't overwrite the default worlds, and this should account for custom worlds, etc.
			// this MIIIGHT want to run asynchronously, at least for copying custom worlds
			
			plugin.log.info("Generating new worlds...");
			MinecraftServer ms = plugin.getMinecraftServer();
			
			Method a;
			try
			{
				a = MinecraftServer.class.getDeclaredMethod("a", String.class, String.class, long.class, WorldType.class);
			}
			catch ( NoSuchMethodException ex )
			{
				plugin.log.warning("Unable to trigger default world generation, shutting down");
				plugin.getServer().shutdown();
				return;
			}
			
			try
			{
				a.setAccessible(true);
				a.invoke(ms, Settings.killerWorldName, Settings.killerWorldName, seedGen.nextLong(), WorldType.NORMAL);
				a.setAccessible(false);
			}
			catch ( IllegalAccessException ex )
			{
				plugin.log.warning("Illegal access: " + ex.getMessage());
			}
			catch ( InvocationTargetException ex )
			{
				plugin.log.warning("Invocation target exception: " + ex.getMessage());
			}

			mainWorld = getWorld(Settings.killerWorldName, Environment.NORMAL, true);
			netherWorld = getWorld(Settings.killerWorldName + "_nether", Environment.NETHER, true);
			endWorld = getWorld(Settings.killerWorldName + "_the_end", Environment.THE_END, false);
			
	        if ( plugin.getGameMode().usesPlinth() ) // create a plinth in the main world. Always done with the same offset, so if the world already has a plinth, it should just get overwritten.
	        	plugin.plinthPressurePlateLocation = createPlinth(mainWorld);
	        
			// run whatever task was passed in
			if ( runWhenDone != null )
				runWhenDone.run();
		}
}
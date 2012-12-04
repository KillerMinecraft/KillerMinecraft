package com.ftwinston.Killer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.server.ChunkCoordinates;
import net.minecraft.server.ChunkPosition;
import net.minecraft.server.ChunkProviderHell;
import net.minecraft.server.ConvertProgressUpdater;
import net.minecraft.server.Convertable;
import net.minecraft.server.EntityEnderSignal;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityTracker;
import net.minecraft.server.EnumGamemode;
import net.minecraft.server.IChunkProvider;
import net.minecraft.server.IWorldAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegionFile;
import net.minecraft.server.ServerNBTManager;
import net.minecraft.server.WorldGenNether;
import net.minecraft.server.WorldLoaderServer;
import net.minecraft.server.WorldServer;
import net.minecraft.server.WorldSettings;
import net.minecraft.server.WorldType;

import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.generator.NetherChunkGenerator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import com.ftwinston.Killer.Game.GameState;

class WorldManager
{
	public static WorldManager instance;
	
	private Killer plugin;
	public WorldManager(Killer killer)
	{
		plugin = killer;
		instance = this;
		
		seedGen = new Random();
		bindRegionFiles();
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
	
	public void hijackDefaultWorld(String name)
	{
		// as the config may have changed, delete the existing world 
		try
		{
			delete(new File(plugin.getServer().getWorldContainer() + File.separator + name));
		}
		catch ( Exception e )
		{
		}
		
		// in the already-loaded server configuration, create/update an entry specifying the generator to be used for the default world
		YamlConfiguration configuration = plugin.getBukkitConfiguration();
		
		ConfigurationSection section = configuration.getConfigurationSection("worlds");
		if ( section == null )
			section = configuration.createSection("worlds");
		
		ConfigurationSection worldSection = section.getConfigurationSection(name);
		if ( worldSection == null )
			worldSection = section.createSection(name);
		
		worldSection.set("generator", "Killer");
		
		// disable the end and the nether. We'll re-enable once this has generated.
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
		stagingWorld = plugin.getServer().getWorld(name);
		if ( stagingWorld != null )
		{// staging world already existed; delete it, because we might want to reset it back to its default state
			
			plugin.log.info("Deleting staging world, cos it already exists...");
			
			
			forceUnloadWorld(stagingWorld);
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
		
		// staging world must not be null when the init event is called, so we don't call this again
		if ( plugin.stagingWorldIsServerDefault )
			stagingWorld = plugin.getServer().getWorlds().get(0);
		
		stagingWorld = new WorldCreator(name)
			.generator(new StagingWorldGenerator())
			.environment(Environment.THE_END)
			.createWorld();
		
		stagingWorld.setSpawnFlags(false, false);
		stagingWorld.setDifficulty(Difficulty.HARD);
		stagingWorld.setPVP(false);
		stagingWorld.setAutoSave(false); // don't save changes to the staging world

		plugin.stagingWorldManager = new StagingWorldManager(plugin, stagingWorld);
		plugin.log.info("Staging world generated");
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
	
	public boolean isProtectedLocation(Game game, Location loc)
	{
		if ( loc.getWorld() == plugin.stagingWorld )
			return loc.getBlockZ() < StagingWorldGenerator.spleefMinZ
				|| loc.getBlockZ() > StagingWorldGenerator.spleefMaxZ
				|| loc.getBlockX() < StagingWorldGenerator.spleefMinX
				|| loc.getBlockX() > StagingWorldGenerator.spleefMaxX;
		
		if ( game != null )
			return game.getGameMode().isLocationProtected(loc);
		else
			return false;
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
			plugin.log.info("Successfully bound to region file cache.");
		}
		catch (Throwable t)
		{
			plugin.log.warning("Error binding to region file cache.");
			t.printStackTrace();
		}
	}
	
	public void deleteWorldFolders(final String prefix)
	{
		File worldFolder = plugin.getServer().getWorldContainer();
		
		String[] killerFolders = worldFolder.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(prefix) && dir.isDirectory();
			}
		});
		
		for ( String worldName : killerFolders )
			deleteWorld(worldName);
	}
	
	public boolean deleteWorld(String worldName)
	{
		clearWorldReference(worldName);
		boolean allGood = true;
		
		File folder = new File(plugin.getServer().getWorldContainer() + File.separator + worldName);
		try
		{
			if ( folder.exists() && !delete(folder) )
				allGood = false;
		}
		catch ( Exception e )
		{
			plugin.log.info("An error occurred when deleting the " + worldName + " world: " + e.getMessage());
		}
		
		return allGood;
	}
		
	@SuppressWarnings("rawtypes")
	private synchronized boolean clearWorldReference(String worldName)
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
					RegionFile file = (RegionFile) e.getValue();
					try
					{
						RandomAccessFile raf = (RandomAccessFile) rafField.get(file);
						raf.close();
						removedKeys.add(f);
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
	
	private void forceUnloadWorld(World world)
	{
		world.setAutoSave(false);
		if ( world == stagingWorld )
			for ( Player player : world.getPlayers() )
				player.kickPlayer("Staging world is being regenerated... and you were in it!");
		else
		{
			for ( Player player : world.getPlayers() )
				plugin.playerManager.putPlayerInStagingWorld(player);
		}
		
		// formerly used server.unloadWorld at this point. But it was failing, even when i force-cleared the player list
		CraftServer server = (CraftServer)plugin.getServer();
		CraftWorld craftWorld = (CraftWorld)world;
		
		try
		{
			Field f = server.getClass().getDeclaredField("worlds");
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			Map<String, World> worlds = (Map<String, World>)f.get(server);
			worlds.remove(world.getName().toLowerCase());
			f.setAccessible(false);
		}
		catch ( IllegalAccessException ex )
		{
			plugin.log.warning("Error removing world from bukkit master list: " + ex.getMessage());
		}
		catch  ( NoSuchFieldException ex )
		{
			plugin.log.warning("Error removing world from bukkit master list: " + ex.getMessage());
		}
		
		MinecraftServer ms = plugin.getMinecraftServer();
		ms.worlds.remove(ms.worlds.indexOf(craftWorld.getHandle()));
	}

	public void deleteKillerWorlds(Game game, Runnable runWhenDone)
	{
		plugin.log.info("Clearing out old worlds...");
		deleteWorlds(runWhenDone, game.getWorlds().toArray(new World[0]));
		game.getWorlds().clear();
	}
	
	public void deleteWorlds(Runnable runWhenDone, World... worlds)
	{
		String[] worldNames = new String[worlds.length];
		for ( int i=0; i<worlds.length; i++ )
		{
			worldNames[i] = worlds[i].getName();
			forceUnloadWorld(worlds[i]);
		}
		
		// now we want to try to delete the world folders
		plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new WorldDeleter(runWhenDone, worldNames), 80);
	}
	
	private class WorldDeleter implements Runnable
	{
		Runnable runWhenDone;
		String[] worlds;
		
		static final long retryDelay = 30;
		static final int maxRetries = 5;
		int attempt;
		
		public WorldDeleter(Runnable runWhenDone, String... names)
		{
			attempt = 0;
			worlds = names;
			this.runWhenDone = runWhenDone;
		}
		
		public void run()
		{
			boolean allGood = true;
			for ( String world : worlds )
				allGood = allGood && deleteWorld(world);
			
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
			
			if ( runWhenDone != null )
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, runWhenDone);
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
	
	public boolean seekNearestNetherFortress(Player player)
	{
		if ( player.getWorld().getEnvironment() != Environment.NETHER )
			return false;
		
		WorldServer world = ((CraftWorld)player.getWorld()).getHandle();
		
		IChunkProvider chunkProvider;
		try
		{
			Field field = net.minecraft.server.ChunkProviderServer.class.getDeclaredField("chunkProvider");
			field.setAccessible(true);
			chunkProvider = (IChunkProvider)field.get(world.chunkProviderServer);
			field.setAccessible(false);
		}
		catch (Throwable t)
		{
			return false;
		}
		
		if ( chunkProvider == null )
			return false;
		
		NetherChunkGenerator ncg = (NetherChunkGenerator)chunkProvider;
		ChunkProviderHell hellCP;
		try
		{
			Field field = org.bukkit.craftbukkit.generator.NormalChunkGenerator.class.getDeclaredField("provider");
			field.setAccessible(true);
			hellCP = (ChunkProviderHell)field.get(ncg);
			field.setAccessible(false);
		}
		catch (Throwable t)
		{
			return false;
		}
		
		Location playerLoc = player.getLocation();
		
		WorldGenNether fortressGenerator = (WorldGenNether)hellCP.c;
		ChunkPosition pos = fortressGenerator.getNearestGeneratedFeature(world, playerLoc.getBlockX(), playerLoc.getBlockY(), playerLoc.getBlockZ());
		if ( pos == null )
			return false; // this just means there isn't one nearby
		
		EntityEnderSignal entityendersignal = new EntityEnderSignal(world, playerLoc.getX(), playerLoc.getY() + player.getEyeHeight() - 0.2, playerLoc.getZ());

		entityendersignal.a((double) pos.x, pos.y, (double) pos.z);
		world.addEntity(entityendersignal);
		world.makeSound(((CraftPlayer)player).getHandle(), "random.bow", 0.5F, 0.4F /*/ (d.nextFloat() * 0.4F + 0.8F)*/);
		world.a((EntityHuman) null, 1002, playerLoc.getBlockX(), playerLoc.getBlockY(), playerLoc.getBlockZ(), 0);
		
		//player.sendMessage("Nearest nether fortress is at " + pos.x + ", " + pos.y + ", " + pos.z);
		return true;
	}
	
	public void generateWorlds(final Game game, final WorldOption generator, final Runnable runWhenDone)
	{
		game.getGameMode().broadcastMessage(game.getGameMode().getNumWorlds() == 1 ? "Preparing new world..." : "Preparing new worlds...");
		
		Runnable generationComplete = new Runnable() {
			@Override
			public void run()
			{
				for ( World world : game.getWorlds() )
				{
					world.setDifficulty(Difficulty.HARD);
					
					switch ( game.monsterNumbers )
					{
					case 0:
						world.setMonsterSpawnLimit(0);
						world.setTicksPerMonsterSpawns(1);
						break;
					case 1:
						world.setMonsterSpawnLimit(35);
						world.setTicksPerMonsterSpawns(1);
						break;
					case 2: // MC defaults
						world.setMonsterSpawnLimit(70);
						world.setTicksPerMonsterSpawns(1);
						break;
					case 3:
						world.setMonsterSpawnLimit(110);
						world.setTicksPerMonsterSpawns(1);
						break;
					case 4:
						world.setMonsterSpawnLimit(180);
						world.setTicksPerMonsterSpawns(1);
						break;
					}
					
					if ( world.getEnvironment() == Environment.NORMAL )
						switch ( game.animalNumbers )
						{
						case 0:
							world.setAnimalSpawnLimit(0);
							world.setTicksPerAnimalSpawns(600);
							break;
						case 1:
							world.setAnimalSpawnLimit(8);
							world.setTicksPerAnimalSpawns(500);
							break;
						case 2: // mc defaults
							world.setAnimalSpawnLimit(15);
							world.setTicksPerAnimalSpawns(400);
							break;
						case 3:
							world.setAnimalSpawnLimit(25);
							world.setTicksPerAnimalSpawns(300);
							break;
						case 4:
							world.setAnimalSpawnLimit(40);
							world.setTicksPerAnimalSpawns(200);
							break;
						}
				}
				
				plugin.stagingWorldManager.removeWorldGenerationIndicator();
				
				// run whatever task was passed in
				if ( runWhenDone != null )
					runWhenDone.run();
			}
		};
	
		try
		{
			generator.createWorlds(game, generationComplete);
		}
		catch (ArrayIndexOutOfBoundsException ex)
		{
			plugin.log.warning("An crash occurred during world generation. Aborting...");
			game.getGameMode().broadcastMessage("An error occurred during world generation.\nPlease try again...");
			
			game.setGameState(GameState.worldDeletion);
		}
	}
	
	public static long getSeedFromString(String str)
	{// copied from how bukkit handles string seeds
		long k = (new Random()).nextLong();

		if ( str != null && str.length() > 0)
		{
			try
			{
				long l = Long.parseLong(str);

				if (l != 0L)
					k = l;
			} catch (NumberFormatException numberformatexception)
			{
				k = (long) str.hashCode();
			}
		}
		return k;
	}
	
	public void generateCustomWorld(String name, String seed)
	{
		// ensure world folder doesn't already exist (we checked before calling this that there isn't an active world with this name)
		deleteWorld(name); 
	
		long lSeed = getSeedFromString(seed);
		// create the world
		WorldCreator wc = new WorldCreator(name).environment(Environment.NORMAL);
		wc.seed(lSeed);
		World world = plugin.getServer().createWorld(wc);
		
		// unload the world - but don't delete
		forceUnloadWorld(world);
		clearWorldReference(name);
		
		// now add it to the config
		Settings.addCustomWorld(name);
		
		// ... and update the staging world? We'll have to restart, otherwise
	}

	// this is a clone of CraftServer.createWorld, amended to accept extra block populators
	// it also spreads chunk creation across multiple ticks, instead of locking up the server while it generates 
    public World createWorld(WorldHelper helper, final Runnable runWhenDone)
    {
        final CraftServer craftServer = (CraftServer)plugin.getServer();
        MinecraftServer console = craftServer.getServer();
        
        String name = helper.getName();
        File folder = new File(craftServer.getWorldContainer(), name);
        World world = craftServer.getWorld(name);

        if (world != null)
            return world;

        if ((folder.exists()) && (!folder.isDirectory()))
            throw new IllegalArgumentException("File exists with the name '" + name + "' and isn't a folder");

        ChunkGenerator generator = helper.getGenerator();
        
        if (generator == null)
        	generator = craftServer.getGenerator(name);

        WorldType type = WorldType.getType(helper.getWorldType().getName());
        
        Convertable converter = new WorldLoaderServer(craftServer.getWorldContainer());
        if (converter.isConvertable(name)) {
        	craftServer.getLogger().info("Converting world '" + name + "'");
            converter.convert(name, new ConvertProgressUpdater(console));
        }

        int dimension = 10 + console.worlds.size();
        boolean used = false;
        do {
            for (WorldServer server : console.worlds) {
                used = server.dimension == dimension;
                if (used) {
                    dimension++;
                    break;
                }
            }
        } while(used);
        boolean hardcore = false;

        final WorldServer worldServer = new WorldServer(console, new ServerNBTManager(craftServer.getWorldContainer(), name, true), name, dimension, new WorldSettings(helper.getSeed(), EnumGamemode.a(craftServer.getDefaultGameMode().getValue()), helper.getGenerateStructures(), hardcore, type), console.methodProfiler, helper.getEnvironment(), generator);

        if (craftServer.getWorld(name) == null)
            return null;

        worldServer.worldMaps = console.worlds.get(0).worldMaps;

        worldServer.tracker = new EntityTracker(worldServer);
        worldServer.addIWorldAccess((IWorldAccess) new net.minecraft.server.WorldManager(console, worldServer));
        worldServer.difficulty = 3;
        worldServer.setSpawnFlags(true, true);
        console.worlds.add(worldServer);

        if (generator != null)
        	worldServer.getWorld().getPopulators().addAll(generator.getDefaultPopulators(worldServer.getWorld()));
        
        for ( BlockPopulator populator : helper.getExtraPopulators() )
        	worldServer.getWorld().getPopulators().add(populator);

        craftServer.getPluginManager().callEvent(new WorldInitEvent(worldServer.getWorld()));
        System.out.print("Preparing start region for level " + (console.worlds.size() - 1) + " (Seed: " + worldServer.getSeed() + ")");
        
        int worldNumber = worlds.size(), numberOfWorlds = plugin.getGameMode().getWorldsToGenerate().length; 
        plugin.stagingWorldManager.showWorldGenerationIndicator((float)worldNumber / (float)numberOfWorlds);
        ChunkBuilder cb = new ChunkBuilder(12, craftServer, worldServer, worldNumber, numberOfWorlds, runWhenDone);
    	cb.taskID = craftServer.getScheduler().scheduleSyncRepeatingTask(plugin, cb, 1L, 1L);
    	return worldServer.getWorld();
    }
    
    class ChunkBuilder implements Runnable
    {
    	public ChunkBuilder(int numChunksFromSpawn, CraftServer craftServer, WorldServer worldServer, int worldNumber, int numberOfWorlds, Runnable runWhenDone)
    	{
    		this.numChunksFromSpawn = numChunksFromSpawn;
    		sideLength = numChunksFromSpawn * 2 + 1;
    		numSteps = sideLength * sideLength;
    		
    		this.worldNumber = worldNumber;
    		this.numberOfWorlds = numberOfWorlds;
    		this.craftServer = craftServer;
    		this.worldServer = worldServer;
    		this.runWhenDone = runWhenDone;
    		
    		ChunkCoordinates spawnPos = worldServer.getSpawn();
        	spawnX = worldServer.getChunkAtWorldCoords(spawnPos.x, spawnPos.z).x;
        	spawnZ = worldServer.getChunkAtWorldCoords(spawnPos.x, spawnPos.z).z;
    	}
    	
    	int numChunksFromSpawn, stepNum = 0, sideLength, numSteps, spawnX, spawnZ;
        long reportTime = System.currentTimeMillis();
        int worldNumber, numberOfWorlds;
        public int taskID;
        CraftServer craftServer;
        WorldServer worldServer;
        Runnable runWhenDone;
        static final int chunksPerTick = 3; // how many chunks to generate each tick? 
    	
    	public void run()
    	{
            long time = System.currentTimeMillis();

            if (time < reportTime) {
                reportTime = time;
            }

            if (time > reportTime + 1000L) {
            	System.out.println("Preparing spawn area for " + worldServer.getWorld().getName() + ", " + (stepNum * 100 / numSteps) + "%");
            	float fraction = (float)stepNum/numSteps;
            	if ( numberOfWorlds > 1 )
            	{
            		fraction /= (float)numberOfWorlds;
            		fraction += (float)worldNumber/(float)numberOfWorlds;
            	}
            	plugin.stagingWorldManager.showWorldGenerationIndicator(fraction);
                reportTime = time;
            }

            for ( int i=0; i<chunksPerTick; i++ )
            {
            	int offsetX = stepNum / sideLength - numChunksFromSpawn;
            	int offsetZ = stepNum % sideLength - numChunksFromSpawn;

            	stepNum++;
            	worldServer.chunkProviderServer.getChunkAt(spawnX + offsetX, spawnZ + offsetZ);

            	if ( stepNum >= numSteps )
            	{
            		if ( worldNumber >= numberOfWorlds - 1 )
            			plugin.stagingWorldManager.removeWorldGenerationIndicator();
            		craftServer.getPluginManager().callEvent(new WorldLoadEvent(worldServer.getWorld()));
            		craftServer.getScheduler().cancelTask(taskID);
            		craftServer.getScheduler().scheduleSyncDelayedTask(plugin, runWhenDone);
            		return;
            	}
            }
        }
    }
}
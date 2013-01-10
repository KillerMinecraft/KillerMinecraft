package com.ftwinston.Killer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.BlockPopulator;

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
		plugin.craftBukkit.bindRegionFiles();
	}
	
	public void onDisable()
	{
		plugin.craftBukkit.unbindRegionFiles();
	}
	
	Random seedGen;
	
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
		YamlConfiguration configuration = plugin.craftBukkit.getBukkitConfiguration();
		
		ConfigurationSection section = configuration.getConfigurationSection("worlds");
		if ( section == null )
			section = configuration.createSection("worlds");
		
		ConfigurationSection worldSection = section.getConfigurationSection(name);
		if ( worldSection == null )
			worldSection = section.createSection(name);
		
		worldSection.set("generator", "Killer");
		
		// disable the end and the nether. We'll re-enable once this has generated.
		final String prevAllowNether = plugin.craftBukkit.getServerProperty("allow-nether", "true");
		final boolean prevAllowEnd = configuration.getBoolean("settings.allow-end", true);
		plugin.craftBukkit.setServerProperty("allow-nether", "false");			
		configuration.set("settings.allow-end", false);
		
		// restore server settings, once it's finished generating
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				plugin.craftBukkit.setServerProperty("allow-nether", prevAllowNether);
				YamlConfiguration configuration = plugin.craftBukkit.getBukkitConfiguration();
				
				configuration.set("settings.allow-end", prevAllowEnd);
				
				plugin.craftBukkit.saveServerPropertiesFile();
				plugin.craftBukkit.saveBukkitConfiguration(configuration);
			}
		});
	}
	
	public void createStagingWorld(final String name) 
	{
		plugin.stagingWorld = plugin.getServer().getWorld(name);
		if ( plugin.stagingWorld != null )
		{// staging world already existed; delete it, because we might want to reset it back to its default state
			plugin.log.info("Deleting staging world, because it already exists...");
			
			plugin.craftBukkit.forceUnloadWorld(plugin.stagingWorld);
			try
			{
				Thread.sleep(200);
			}
			catch ( InterruptedException ex )
			{
			}
			
			plugin.craftBukkit.clearWorldReference(name);
			
			try
			{
				delete(new File(plugin.getServer().getWorldContainer() + File.separator + name));
			}
			catch ( Exception e )
			{
			}
		}
		
		// ensure the end wall leaves enough room for all the options
		int maxZ = StagingWorldGenerator.getWallMaxZ();
		maxZ = Math.max(maxZ, StagingWorldGenerator.getOptionButtonZ(GameMode.gameModes.size(), true));
		maxZ = Math.max(maxZ, StagingWorldGenerator.getOptionButtonZ(WorldOption.worldOptions.size(), false) + 1);
		for ( GameModePlugin mode : GameMode.gameModes )
			maxZ = Math.max(maxZ, StagingWorldGenerator.getOptionButtonZ(mode.createInstance().setupOptions().length, false));
		for ( WorldOptionPlugin world : WorldOption.worldOptions )
			maxZ = Math.max(maxZ, StagingWorldGenerator.getOptionButtonZ(world.createInstance().setupOptions().length, false));
		StagingWorldGenerator.setWallMaxZ(maxZ + 1);
		
		// staging world must not be null when the init event is called, so we don't call this again
		if ( plugin.stagingWorldIsServerDefault )
			plugin.stagingWorld = plugin.getServer().getWorlds().get(0);

		plugin.stagingWorld = new WorldCreator(name)
			.generator(new StagingWorldGenerator())
			.environment(Environment.THE_END)
			.createWorld();
		
		plugin.stagingWorld.setSpawnFlags(false, false);
		plugin.stagingWorld.setDifficulty(Difficulty.HARD);
		plugin.stagingWorld.setPVP(false);
		plugin.stagingWorld.setAutoSave(false); // don't save changes to the staging world

		plugin.stagingWorldManager = new StagingWorldManager(plugin, plugin.stagingWorld);
		plugin.arenaManager = new ArenaManager(plugin, plugin.stagingWorld);
		
		for ( Game game : plugin.games )
			plugin.stagingWorldManager.updateGameInfoSigns(game);
		
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
		plugin.craftBukkit.clearWorldReference(worldName);
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
			for ( Player player : worlds[i].getPlayers() )
				plugin.playerManager.putPlayerInStagingWorld(player);
			plugin.craftBukkit.forceUnloadWorld(worlds[i]);
		}
		
		// now we want to try to delete the world folders
		plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new WorldDeleter(runWhenDone, worldNames), 80);
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
				
				plugin.stagingWorldManager.removeWorldGenerationIndicator(game);
				
				// run whatever task was passed in
				if ( runWhenDone != null )
					runWhenDone.run();
			}
		};
	
		try
		{
			generator.createWorlds(game, generationComplete);
		}
		catch (Exception ex)
		{
			plugin.log.warning("A crash occurred during world generation. Aborting...");
			game.getGameMode().broadcastMessage("An error occurred during world generation.\nPlease try again...");
			
			game.setGameState(GameState.worldDeletion);
		}
	}

	// this is a clone of CraftServer.createWorld, amended to accept extra block populators
	// it also spreads chunk creation across multiple ticks, instead of locking up the server while it generates 
    public World createWorld(WorldConfig config, final Runnable runWhenDone)
    {
    	World world = plugin.craftBukkit.createWorld(config.getWorldType(), config.getEnvironment(), config.getName(), config.getSeed(), config.getGenerator(), config.getGeneratorSettings(), config.getGenerateStructures());
    	
        for ( BlockPopulator populator : config.getExtraPopulators() )
        	world.getPopulators().add(populator);

        Server server = plugin.getServer();
        		
        server.getPluginManager().callEvent(new WorldInitEvent(world));
        System.out.print("Preparing start region for world: " + world.getName() + " (Seed: " + config.getSeed() + ")");
        
        int worldNumber = config.getGame().getWorlds().size(), numberOfWorlds = config.getGame().getGameMode().getWorldsToGenerate().length; 
        plugin.stagingWorldManager.showWorldGenerationIndicator(config.getGame(), (float)worldNumber / (float)numberOfWorlds);
        ChunkBuilder cb = new ChunkBuilder(config.getGame(), 12, server, world, worldNumber, numberOfWorlds, runWhenDone);
    	cb.taskID = server.getScheduler().scheduleSyncRepeatingTask(plugin, cb, 1L, 1L);
    	return world;
    }
    
    class ChunkBuilder implements Runnable
    {
    	public ChunkBuilder(Game game, int numChunksFromSpawn, Server server, World world, int worldNumber, int numberOfWorlds, Runnable runWhenDone)
    	{
    		this.numChunksFromSpawn = numChunksFromSpawn;
    		sideLength = numChunksFromSpawn * 2 + 1;
    		numSteps = sideLength * sideLength;
    		
    		this.game = game;
    		this.worldNumber = worldNumber;
    		this.numberOfWorlds = numberOfWorlds;
    		this.server = server;
    		this.world = world;
    		this.runWhenDone = runWhenDone;
    		
    		Location spawnPos = world.getSpawnLocation();
        	spawnX = spawnPos.getBlockX() >> 4;
        	spawnZ = spawnPos.getBlockZ() >> 4;
    	}
    	
    	Game game;
    	int numChunksFromSpawn, stepNum = 0, sideLength, numSteps, spawnX, spawnZ;
        long reportTime = System.currentTimeMillis();
        int worldNumber, numberOfWorlds;
        public int taskID;
        Server server;
        World world;
        Runnable runWhenDone;
        static final int chunksPerTick = 3; // how many chunks to generate each tick? 
    	
    	public void run()
    	{
            long time = System.currentTimeMillis();

            if (time < reportTime) {
                reportTime = time;
            }

            if (time > reportTime + 500L)
            {
            	float fraction = (float)stepNum/numSteps;
            	if ( numberOfWorlds > 1 )
            	{
            		fraction /= (float)numberOfWorlds;
            		fraction += (float)worldNumber/(float)numberOfWorlds;
            	}
            	plugin.stagingWorldManager.showWorldGenerationIndicator(game, fraction);
                reportTime = time;
            }

            for ( int i=0; i<chunksPerTick; i++ )
            {
            	int offsetX = stepNum / sideLength - numChunksFromSpawn;
            	int offsetZ = stepNum % sideLength - numChunksFromSpawn;

            	stepNum++;
            	
            	world.loadChunk(spawnX + offsetX, spawnZ + offsetZ);

            	if ( stepNum >= numSteps )
            	{
            		if ( worldNumber >= numberOfWorlds - 1 )
            			plugin.stagingWorldManager.removeWorldGenerationIndicator(game);
            		server.getPluginManager().callEvent(new WorldLoadEvent(world));
            		server.getScheduler().cancelTask(taskID);
            		server.getScheduler().scheduleSyncDelayedTask(plugin, runWhenDone);
            		
            		System.out.println("Finished generating world: " + world.getName());
            		return;
            	}
            }
        }
    }
}
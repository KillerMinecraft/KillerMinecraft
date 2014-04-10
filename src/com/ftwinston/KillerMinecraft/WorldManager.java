package com.ftwinston.KillerMinecraft;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import com.ftwinston.KillerMinecraft.Game.GameState;

class WorldManager
{
	private KillerMinecraft plugin;
	public int chunkBuilderTaskID = -1;
	
	public WorldManager(KillerMinecraft killer)
	{
		plugin = killer;
		plugin.craftBukkit.bindRegionFiles();
	}

	public void onDisable()
	{
		plugin.craftBukkit.unbindRegionFiles();
	}

	public void hijackDefaultWorld()
	{
		// in the already-loaded server configuration, create/update an entry specifying the generator to be used for the default world
		YamlConfiguration configuration = plugin.craftBukkit.getBukkitConfiguration();
		
		ConfigurationSection section = configuration.getConfigurationSection("worlds");
		if ( section == null )
			section = configuration.createSection("worlds");
		
		final String stagingWorldName = "world";
		ConfigurationSection worldSection = section.getConfigurationSection(stagingWorldName);
		if ( worldSection == null )
			worldSection = section.createSection(stagingWorldName);
		
		worldSection.set("generator", "Killer Minecraft");
		
		// whatever the name of the staging world, it should be the only world on the server. So change the level-name to match that, temporarily, and don't let it get a nether/end.
		final String prevLevelName = plugin.craftBukkit.getServerProperty("level-name", "world");
		final String prevAllowNether = plugin.craftBukkit.getServerProperty("allow-nether", "true");
		final boolean prevAllowEnd = configuration.getBoolean("settings.allow-end", true);
		
		plugin.craftBukkit.setServerProperty("level-name", stagingWorldName);
		plugin.craftBukkit.setServerProperty("allow-nether", "false");			
		configuration.set("settings.allow-end", false);
		
		// restore server settings, once it's finished generating
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				plugin.craftBukkit.setServerProperty("level-name", prevLevelName);
				plugin.craftBukkit.setServerProperty("allow-nether", prevAllowNether);
				
				YamlConfiguration configuration = plugin.craftBukkit.getBukkitConfiguration();
				configuration.set("settings.allow-end", prevAllowEnd);
				configuration.set("worlds", null);
				
				//if we don't actually save, then the files themselves never get messed with
				//plugin.craftBukkit.saveServerPropertiesFile();
				//plugin.craftBukkit.saveBukkitConfiguration(configuration);
			}
		});
	}

	public boolean isProtectedLocation(Location loc, Player player)
	{
		return isProtectedLocation(plugin.getGameForWorld(loc.getWorld()), loc, player);
	}
	
	public boolean isProtectedLocation(Game game, Location loc, Player player)
	{
		if ( game == null )
		{
			if ( !Settings.nothingButKiller )
				return false;
			
			ChunkGenerator gen = loc.getWorld().getGenerator(); 
			if ( gen == null || gen.getClass() != StagingWorldGenerator.class )
				return false;
			
			// this is the floating island staging world. The center of that is protected.
			int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
			
			return x >= -4 && x <= 4
				&& y >= 64 && y <= 68
				&& z >= -4 && z <= 4;
		}
		
		return game.getGameMode().isLocationProtected(loc, player);
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
			deleteWorldFolder(worldName);
	}
	
	public boolean deleteWorldFolder(String worldName)
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
	
	public void deleteGameWorlds(Game game, Runnable runWhenDone)
	{
		plugin.log.info("Deleting worlds for " + game.getName());
		
		List<World> worlds = game.getWorlds();
		String[] worldNames = new String[worlds.size()];
		
		Location ejectTo = Bukkit.getServer().getWorlds().get(0).getSpawnLocation();
		
		for ( int i=0; i<worldNames.length; i++ )
		{
			World world = worlds.get(i);
			worldNames[i] = world.getName();
			
			plugin.gamesByWorld.remove(world.getName());
			
			for ( Player player : world.getPlayers() )
				Helper.teleport(player, ejectTo);

			plugin.craftBukkit.forceUnloadWorld(world);
		}
		
		worlds.clear();
		
		// now we want to try to delete the world folders
		plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new DelayedWorldDeleter(runWhenDone, worldNames), 80);
	}

	private class DelayedWorldDeleter implements Runnable
	{
		Runnable runWhenDone;
		String[] worlds;
		
		static final long retryDelay = 30;
		static final int maxRetries = 5;
		int attempt;
		
		public DelayedWorldDeleter(Runnable runWhenDone, String... names)
		{
			attempt = 0;
			worlds = names;
			this.runWhenDone = runWhenDone;
		}
		
		public void run()
		{
			boolean allGood = true;
			for ( String world : worlds )
				allGood = allGood && deleteWorldFolder(world);
			
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

	public void generateWorlds(final Game game, final WorldGenerator generator, final Runnable runWhenDone)
	{
		game.broadcastMessage(game.getGameMode().getNumWorlds() == 1 ? "Generating game world..." : "Generating game worlds...");
		
		Runnable generationComplete = new Runnable() {
			@Override
			public void run()
			{
				for ( World world : game.getWorlds() )
				{
					world.setDifficulty(game.getDifficulty());
					
					world.setTicksPerMonsterSpawns(1);
					world.setMonsterSpawnLimit(game.getGameMode().getMonsterSpawnLimit(game.monsterNumbers));
					
					if ( world.getEnvironment() == Environment.NORMAL )
					{
						world.setTicksPerAnimalSpawns(400);
						world.setMonsterSpawnLimit(game.getGameMode().getAnimalSpawnLimit(game.animalNumbers));
					}
				}
				
				
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
			game.broadcastMessage("An error occurred during world generation.\nPlease try again...");
			
			game.setGameState(GameState.WORLD_DELETION);
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
        ChunkBuilder cb = new ChunkBuilder(config.getGame(), 12, server, world, worldNumber, numberOfWorlds, runWhenDone);
        chunkBuilderTaskID = server.getScheduler().scheduleSyncRepeatingTask(plugin, cb, 1L, 1L);
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
        Server server;
        World world;
        Runnable runWhenDone;
    	
    	public void run()
    	{
    		long time = System.currentTimeMillis();
            if (time > reportTime + 3000L)
            {
            	float fraction = (float)stepNum/numSteps;
            	if ( numberOfWorlds > 1 )
            	{
            		fraction /= (float)numberOfWorlds;
            		fraction += (float)worldNumber/(float)numberOfWorlds;
            	}
            	game.broadcastMessage((int)(fraction*100f) + "%");
                reportTime = time;
            }

            for ( int i=0; i<Settings.generateChunksPerTick; i++ )
            {
            	int offsetX = stepNum / sideLength - numChunksFromSpawn;
            	int offsetZ = stepNum % sideLength - numChunksFromSpawn;

            	stepNum++;
            	
            	world.loadChunk(spawnX + offsetX, spawnZ + offsetZ);

            	if ( stepNum >= numSteps )
            	{
            		server.getPluginManager().callEvent(new WorldLoadEvent(world));
            		server.getScheduler().cancelTask(chunkBuilderTaskID);
            		chunkBuilderTaskID = -1;
            		server.getScheduler().scheduleSyncDelayedTask(plugin, runWhenDone);
            		
            		game.getWorlds().add(world);
            		plugin.gamesByWorld.put(world.getName(), game);
            		
            		System.out.println("Finished generating world: " + world.getName());
            		return;
            	}
            }
        }
    }
}
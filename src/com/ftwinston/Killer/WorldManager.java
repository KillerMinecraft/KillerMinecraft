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
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
//import org.bukkit.craftbukkit.CraftServer;
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
		stagingWorld.setDifficulty(Difficulty.PEACEFUL);
		stagingWorld.setPVP(false);
		stagingWorld.setAutoSave(false); // don't save changes to the staging world
		plugin.log.info("Staging world generated");
	}
	
	Random spawnOffset = new Random();
	public Location getStagingWorldSpawnPoint()
	{
		return new Location(stagingWorld, -13.5f + spawnOffset.nextDouble() * 4 - 2, StagingWorldGenerator.floorY + 1, 26.5f - spawnOffset.nextDouble(), 180, 0);
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
			plugin.log.info("Successfully bound to region file cache.");
		}
		catch (Throwable t)
		{
			plugin.log.warning("Error binding to region file cache.");
			t.printStackTrace();
		}
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
	
	private void forceUnloadWorld(World world)
	{
		world.setAutoSave(false);
		if ( world == stagingWorld )
			for ( Player player : world.getPlayers() )
				player.kickPlayer("Staging world is being regenerated... and you were in it!");
		else
		{
			for ( Player player : world.getPlayers() )
				plugin.playerManager.teleport(player, getStagingWorldSpawnPoint());
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

	public void deleteKillerWorlds(Runnable runWhenDone)
	{
		plugin.log.info("Clearing out old worlds...");
		if ( mainWorld == null )
			if ( netherWorld == null )
				deleteWorlds(runWhenDone);
			else
				deleteWorlds(runWhenDone, netherWorld);
		else if ( netherWorld == null )
			deleteWorlds(runWhenDone, mainWorld);
		else
			deleteWorlds(runWhenDone, mainWorld, netherWorld);
		
		mainWorld = null;
		netherWorld = null;
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
	
	public enum StagingWorldOption
	{
		NONE,
		GAME_MODE,
		GAME_MODE_OPTION,
		WORLD_OPTION,
		GLOBAL_OPTION,
		MONSTERS,
		ANIMALS,
	}
	
	private StagingWorldOption currentOption = StagingWorldOption.NONE;
	public void setCurrentOption(StagingWorldOption option)
	{
		if ( option == currentOption )
			return;
		
		// disable whatever's currently on
		switch ( currentOption )
		{
		case GAME_MODE:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinCorridorX, StagingWorldGenerator.buttonY, StagingWorldGenerator.gameModeButtonZ).setData(StagingWorldGenerator.colorOptionOff);
			hideSetupOptionButtons();
			break;
		case GAME_MODE_OPTION:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinCorridorX, StagingWorldGenerator.buttonY, StagingWorldGenerator.gameOptionButtonZ).setData(StagingWorldGenerator.colorOptionOff);
			hideSetupOptionButtons();
			break;
		case WORLD_OPTION:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinCorridorX, StagingWorldGenerator.buttonY, StagingWorldGenerator.worldOptionButtonZ).setData(StagingWorldGenerator.colorOptionOff);
			hideSetupOptionButtons();
			break;
		case GLOBAL_OPTION:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinCorridorX, StagingWorldGenerator.buttonY, StagingWorldGenerator.globalOptionButtonZ).setData(StagingWorldGenerator.colorOptionOff);
			hideSetupOptionButtons();
			break;
		case MONSTERS:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinCorridorX, StagingWorldGenerator.buttonY, StagingWorldGenerator.monstersButtonZ).setData(StagingWorldGenerator.colorOptionOff);
			hideSetupOptionButtons();
			break;
		case ANIMALS:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinCorridorX, StagingWorldGenerator.buttonY, StagingWorldGenerator.animalsButtonZ).setData(StagingWorldGenerator.colorOptionOff);
			hideSetupOptionButtons();
			break;
		}
		
		currentOption = option;
		String[] labels;
		boolean[] values;
		
		// now set up the new option
		switch ( currentOption )
		{
		case GAME_MODE:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinCorridorX, StagingWorldGenerator.buttonY, StagingWorldGenerator.gameModeButtonZ).setData(StagingWorldGenerator.colorOptionOn);
			
			labels = new String[GameMode.gameModes.size()];
			values = new boolean[labels.length];
			for ( int i=0; i<labels.length; i++ )
			{
				GameMode mode = GameMode.gameModes.get(i); 
				labels[i] = mode.getName();
				values[i] = mode == plugin.getGameMode();
			}
			showSetupOptionButtons("Game mode:", labels, values);
			break;
		case GAME_MODE_OPTION:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinCorridorX, StagingWorldGenerator.buttonY, StagingWorldGenerator.gameOptionButtonZ).setData(StagingWorldGenerator.colorOptionOn);
			
			List<GameMode.Option> options = plugin.getGameMode().getOptions();
			labels = new String[options.size()];
			values = new boolean[labels.length];
			for ( int i=0; i<options.size(); i++ )
			{
				labels[i] = options.get(i).getName();
				values[i] = options.get(i).isEnabled();
			}
			showSetupOptionButtons("Mode option:", labels, values);
			break;
		case WORLD_OPTION:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinCorridorX, StagingWorldGenerator.buttonY, StagingWorldGenerator.worldOptionButtonZ).setData(StagingWorldGenerator.colorOptionOn);
			
			labels = new String[WorldOption.options.size()];
			values = new boolean[labels.length];
			for ( int i=0; i<labels.length; i++ )
			{
				WorldOption worldOption = WorldOption.options.get(i); 
				labels[i] = worldOption.getName();
				values[i] = worldOption == plugin.getWorldOption();
			}
			showSetupOptionButtons("World option:", labels, values);
			break;
		case GLOBAL_OPTION:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinCorridorX, StagingWorldGenerator.buttonY, StagingWorldGenerator.globalOptionButtonZ).setData(StagingWorldGenerator.colorOptionOn);
			labels = new String[] { "Craftable monster eggs", "Easier dispenser recipe", "Eyes of ender find nether fortresses" };
			values = new boolean[] { true, true, true, true };
			showSetupOptionButtons("Global option:", labels, values);
			break;
		case MONSTERS:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinCorridorX, StagingWorldGenerator.buttonY, StagingWorldGenerator.monstersButtonZ).setData(StagingWorldGenerator.colorOptionOn);
			labels = new String[5];
			values = new boolean[5];
			for ( int i=0; i<5; i++ )
			{
				labels[i] = StagingWorldGenerator.getQuantityText(i);
				values[i] = i == plugin.monsterNumbers;
			}
			showSetupOptionButtons("Monsters:", labels, values);
			break;
		case ANIMALS:
			stagingWorld.getBlockAt(StagingWorldGenerator.wallMinCorridorX, StagingWorldGenerator.buttonY, StagingWorldGenerator.animalsButtonZ).setData(StagingWorldGenerator.colorOptionOn);
			labels = new String[5];
			values = new boolean[5];
			for ( int i=0; i<5; i++ )
			{
				labels[i] = StagingWorldGenerator.getQuantityText(i);
				values[i] = i == plugin.animalNumbers;
			}
			showSetupOptionButtons("Animals:", labels, values);
		}
	}
	
	private void showSetupOptionButtons(String heading, String[] labels, boolean[] values)
	{
		Block b;
		for ( int i=0; i<labels.length; i++ )
		{
			int z = StagingWorldGenerator.getOptionButtonZ(i);
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.wallMaxX, StagingWorldGenerator.buttonY-1, z);
			b.setType(Material.WOOL);
			b.setData(StagingWorldGenerator.signBackColor);
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.wallMaxX, StagingWorldGenerator.buttonY, z);
			b.setType(Material.WOOL);
			b.setData(values[i] ? StagingWorldGenerator.colorOptionOn : StagingWorldGenerator.colorOptionOff);
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.wallMaxX, StagingWorldGenerator.buttonY+1, z);
			b.setType(Material.WOOL);
			b.setData(StagingWorldGenerator.signBackColor);
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.optionButtonX, StagingWorldGenerator.buttonY, z);
			b.setType(Material.STONE_BUTTON);
			b.setData((byte)0x2);
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.optionButtonX, StagingWorldGenerator.buttonY+1, z);
			b.setType(Material.WALL_SIGN);
			b.setData((byte)0x4);
			Sign s = (Sign)b.getState();
			s.setLine(0, heading);
			
			StagingWorldGenerator.fitTextOnSign(s, labels[i]);
			s.update();
		}
	}

	private void hideSetupOptionButtons()
	{
		Block b;
		for ( int i=0; i<12; i++ ) // there's only space for 12 options, without extending the world further
		{
			int z = StagingWorldGenerator.getOptionButtonZ(i);
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.optionButtonX, StagingWorldGenerator.buttonY, z);
			b.setType(Material.AIR);
			b.setData((byte)0x2);
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.optionButtonX, StagingWorldGenerator.buttonY+1, z);
			b.setType(Material.AIR);
			
			for ( int y=StagingWorldGenerator.floorY+1; y<StagingWorldGenerator.buttonY+2; y++)
			{
				b = stagingWorld.getBlockAt(StagingWorldGenerator.wallMaxX, y, z);
				b.setType(Material.SMOOTH_BRICK);
			}
		}
	}
	
	private void updateSetupOptionButtons(boolean[] values)
	{
		Block b;
		for ( int i=0; i<values.length; i++ )
		{
			int z = StagingWorldGenerator.getOptionButtonZ(i);
			
			b = stagingWorld.getBlockAt(StagingWorldGenerator.wallMaxX, StagingWorldGenerator.buttonY, z);
			b.setData(values[i] ? StagingWorldGenerator.colorOptionOn : StagingWorldGenerator.colorOptionOff);
		}
	}
	
	public void setupButtonClicked(int x, int z)
	{
		if ( x == StagingWorldGenerator.mainButtonX )
		{
			if ( z == StagingWorldGenerator.gameModeButtonZ )
				setCurrentOption(currentOption == StagingWorldOption.GAME_MODE ? StagingWorldOption.NONE : StagingWorldOption.GAME_MODE);
			else if ( z == StagingWorldGenerator.gameOptionButtonZ )
				setCurrentOption(currentOption == StagingWorldOption.GAME_MODE_OPTION ? StagingWorldOption.NONE : StagingWorldOption.GAME_MODE_OPTION);
			else if ( z == StagingWorldGenerator.worldOptionButtonZ )
				setCurrentOption(currentOption == StagingWorldOption.WORLD_OPTION ? StagingWorldOption.NONE : StagingWorldOption.WORLD_OPTION);
			else if ( z == StagingWorldGenerator.globalOptionButtonZ )
				setCurrentOption(currentOption == StagingWorldOption.GLOBAL_OPTION ? StagingWorldOption.NONE : StagingWorldOption.GLOBAL_OPTION);
			else if ( z == StagingWorldGenerator.monstersButtonZ )
				setCurrentOption(currentOption == StagingWorldOption.MONSTERS ? StagingWorldOption.NONE : StagingWorldOption.MONSTERS);
			else if ( z == StagingWorldGenerator.animalsButtonZ )
				setCurrentOption(currentOption == StagingWorldOption.ANIMALS ? StagingWorldOption.NONE : StagingWorldOption.ANIMALS);
		}
		else if ( x == StagingWorldGenerator.optionButtonX )
		{
			int num = StagingWorldGenerator.getOptionNumFromZ(z);
			
			boolean[] newValues; Block b; Sign s;
			switch ( currentOption )
			{
			case GAME_MODE:
				// change mode
				GameMode gameMode = GameMode.get(num);
				if ( plugin.getGameMode() == gameMode )
					return;
				plugin.setGameMode(gameMode);
				
				// update block colors
				newValues = new boolean[GameMode.gameModes.size()];
				for ( int i=0; i<newValues.length; i++ )
					newValues[i] = i == num;
				updateSetupOptionButtons(newValues);
				
				// update game mode sign
				b = stagingWorld.getBlockAt(StagingWorldGenerator.mainButtonX, StagingWorldGenerator.buttonY+1, StagingWorldGenerator.gameModeButtonZ-1);
				s = (Sign)b.getState();
				StagingWorldGenerator.fitTextOnSign(s, gameMode.getName());
				s.update();
				break;
			case GAME_MODE_OPTION:
				// toggle this option
				plugin.getGameMode().toggleOption(num);
				List<GameMode.Option> options = plugin.getGameMode().getOptions();
				
				// update block colors
				newValues = new boolean[options.size()];
				for ( int i=0; i<newValues.length; i++ )
					newValues[i] = options.get(i).isEnabled();
				updateSetupOptionButtons(newValues);
				break;
			case WORLD_OPTION:
				// change option
				WorldOption option = WorldOption.get(num);
				if ( plugin.getWorldOption() == option )
					return;
				plugin.setWorldOption(option);
				
				// update block colors
				newValues = new boolean[WorldOption.options.size()];
				for ( int i=0; i<newValues.length; i++ )
					newValues[i] = i == num;
				updateSetupOptionButtons(newValues);
				
				// update world option sign
				b = stagingWorld.getBlockAt(StagingWorldGenerator.mainButtonX, StagingWorldGenerator.buttonY+1, StagingWorldGenerator.worldOptionButtonZ-1);
				s = (Sign)b.getState();
				StagingWorldGenerator.fitTextOnSign(s, option.getName());
				s.update();
				break;
			case GLOBAL_OPTION:
				if ( num == 0 )
					plugin.toggleMonsterEggRecipes();
				else if ( num == 1 )
					plugin.toggleDispenserRecipe();
				else if ( num == 2 )
					plugin.toggleEnderEyeRecipe();

				newValues = new boolean[] { plugin.monsterEggsEnabled, plugin.dispenserRecipeEnabled, plugin.enderEyeRecipeEnabled };
				updateSetupOptionButtons(newValues);
				break;
			case MONSTERS:
				plugin.monsterNumbers = num;
				
				// update block colors
				newValues = new boolean[5];
				for ( int i=0; i<newValues.length; i++ )
					newValues[i] = i == num;
				updateSetupOptionButtons(newValues);
				
				// update main sign
				b = stagingWorld.getBlockAt(StagingWorldGenerator.mainButtonX, StagingWorldGenerator.buttonY+1, StagingWorldGenerator.monstersButtonZ-1);
				s = (Sign)b.getState();
				s.setLine(1, StagingWorldGenerator.padSignLeft(StagingWorldGenerator.getQuantityText(num)));
				s.update();
				break;
			case ANIMALS:
				plugin.animalNumbers = num;
				
				// update block colors
				newValues = new boolean[5];
				for ( int i=0; i<newValues.length; i++ )
					newValues[i] = i == num;
				updateSetupOptionButtons(newValues);
				
				// update main sign
				b = stagingWorld.getBlockAt(StagingWorldGenerator.mainButtonX, StagingWorldGenerator.buttonY+1, StagingWorldGenerator.monstersButtonZ-1);
				s = (Sign)b.getState();
				s.setLine(3, StagingWorldGenerator.padSignLeft(StagingWorldGenerator.getQuantityText(num)));
				s.update();
				break;
			}
		}
		else if ( z == StagingWorldGenerator.startButtonZ )
		{
			if ( x == StagingWorldGenerator.startButtonX )
			{
				if ( plugin.getOnlinePlayers().size() >= plugin.getGameMode().getMinPlayers() )
				{
					setCurrentOption(StagingWorldOption.NONE);
					plugin.setGameState(GameState.worldGeneration);
				}
				else
					plugin.setGameState(GameState.stagingWorldConfirm);
			}
			else if ( x == StagingWorldGenerator.overrideButtonX )
			{
				setCurrentOption(StagingWorldOption.NONE);
				plugin.setGameState(GameState.worldGeneration);
			}
			else if ( x == StagingWorldGenerator.cancelButtonX )
				plugin.setGameState(GameState.stagingWorldReady);
		}
	}
	
	public void showStartButtons(boolean confirm)
	{
		Block bStart = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, StagingWorldGenerator.buttonY, StagingWorldGenerator.startButtonZ);
		Block sStart = bStart.getRelative(BlockFace.UP);
		Block backStart = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, StagingWorldGenerator.buttonY, StagingWorldGenerator.wallMinCorridorZ);
		
		Block bOverride = stagingWorld.getBlockAt(StagingWorldGenerator.overrideButtonX, StagingWorldGenerator.buttonY, StagingWorldGenerator.startButtonZ);
		Block sOverride = bOverride.getRelative(BlockFace.UP);
		Block backOverride = stagingWorld.getBlockAt(StagingWorldGenerator.overrideButtonX, StagingWorldGenerator.buttonY, StagingWorldGenerator.wallMinCorridorZ);
		
		Block bCancel = stagingWorld.getBlockAt(StagingWorldGenerator.cancelButtonX, StagingWorldGenerator.buttonY, StagingWorldGenerator.startButtonZ);
		Block sCancel = bCancel.getRelative(BlockFace.UP);
		Block backCancel = stagingWorld.getBlockAt(StagingWorldGenerator.cancelButtonX, StagingWorldGenerator.buttonY, StagingWorldGenerator.wallMinCorridorZ);
		
		Block sHighInfo = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, StagingWorldGenerator.buttonY + 2, StagingWorldGenerator.startButtonZ);
		
		if ( confirm )
		{
			bStart.setType(Material.AIR);
			sStart.setType(Material.AIR);
			backStart.setData(StagingWorldGenerator.colorOptionOff);
			
			bOverride.setType(Material.STONE_BUTTON);
			bOverride.setData((byte)0x3);
			
			bCancel.setType(Material.STONE_BUTTON);
			bCancel.setData((byte)0x3);
			
			sOverride.setType(Material.WALL_SIGN);
			sOverride.setData((byte)0x3);
			Sign s = (Sign)sOverride.getState();
			s.setLine(1, "Push to start");
			s.setLine(2, "the game anyway");
			s.update();

			//sCancel.setData((byte)0x3); // because it still has the "data" value from the start button, which is different 
			sCancel.setType(Material.WALL_SIGN);
			sCancel.setData((byte)0x3);
			s = (Sign)sCancel.getState();
			s.setLine(1, "Push to cancel");
			s.setLine(2, "and choose");
			s.setLine(3, "something else");
			s.update();
			
			sHighInfo.setType(Material.WALL_SIGN);
			sHighInfo.setData((byte)0x3);
			s = (Sign)sHighInfo.getState();
			s.setLine(0, "This mode needs");
			s.setLine(1, "at least " + plugin.getGameMode().getMinPlayers());
			s.setLine(2, "players. You");
			s.setLine(3, "only have " + plugin.getOnlinePlayers().size() + ".");
			s.update();
			
			backOverride.setData(StagingWorldGenerator.colorOverrideButton);
			backCancel.setData(StagingWorldGenerator.colorCancelButton);
		}
		else
		{
			bStart.setType(Material.STONE_BUTTON);
			bStart.setData((byte)0x3);
			
			sStart.setType(Material.WALL_SIGN);
			sStart.setData((byte)0x3);
			Sign s = (Sign)sStart.getState();
			s.setLine(1, "Push to");
			s.setLine(2, "start the game");
			s.update();
			
			backStart.setData(StagingWorldGenerator.colorStartButton);
			backOverride.setData(StagingWorldGenerator.colorOptionOff);
			backCancel.setData(StagingWorldGenerator.colorOptionOff);
			
			bOverride.setType(Material.AIR);
			bCancel.setType(Material.AIR);
			sOverride.setType(Material.AIR);
			sCancel.setType(Material.AIR);
			sHighInfo.setType(Material.AIR);
		}
	}
	
	public void showWaitForDeletion()
	{
		Block sign = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, StagingWorldGenerator.buttonY + 1, StagingWorldGenerator.startButtonZ);
			
		sign.setType(Material.WALL_SIGN);
		sign.setData((byte)0x3);
		Sign s = (Sign)sign.getState();
		s.setLine(0, "Please wait for");
		s.setLine(1, "the last game's");
		s.setLine(2, "worlds to be");
		s.setLine(3, "deleted...");
		s.update();
		
		// hide all the start buttons' stuff
		Block bStart = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, StagingWorldGenerator.buttonY, StagingWorldGenerator.startButtonZ);
		Block backStart = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, StagingWorldGenerator.buttonY, StagingWorldGenerator.wallMinCorridorZ);
		
		Block bOverride = stagingWorld.getBlockAt(StagingWorldGenerator.overrideButtonX, StagingWorldGenerator.buttonY, StagingWorldGenerator.startButtonZ);
		Block sOverride = bOverride.getRelative(BlockFace.UP);
		Block backOverride = stagingWorld.getBlockAt(StagingWorldGenerator.overrideButtonX, StagingWorldGenerator.buttonY, StagingWorldGenerator.wallMinCorridorZ);
		
		Block bCancel = stagingWorld.getBlockAt(StagingWorldGenerator.cancelButtonX, StagingWorldGenerator.buttonY, StagingWorldGenerator.startButtonZ);
		Block sCancel = bCancel.getRelative(BlockFace.UP);
		Block backCancel = stagingWorld.getBlockAt(StagingWorldGenerator.cancelButtonX, StagingWorldGenerator.buttonY, StagingWorldGenerator.wallMinCorridorZ);

		Block sHighInfo = stagingWorld.getBlockAt(StagingWorldGenerator.startButtonX, StagingWorldGenerator.buttonY + 2, StagingWorldGenerator.startButtonZ);
		
		bStart.setType(Material.AIR);
		bOverride.setType(Material.AIR);
		bCancel.setType(Material.AIR);
		sOverride.setType(Material.AIR);
		sCancel.setType(Material.AIR);
		sHighInfo.setType(Material.AIR);
		
		backStart.setData(StagingWorldGenerator.colorOptionOff);
		backOverride.setData(StagingWorldGenerator.colorOptionOff);
		backCancel.setData(StagingWorldGenerator.colorOptionOff);
	}
	
	private void showWorldGenerationIndicator(float completion)
	{
		/*
		int maxCompleteZ = (int)((StagingWorldGenerator.wallMinZ - StagingWorldGenerator.wallMaxZ) * completion) + StagingWorldGenerator.wallMaxZ;
		if ( completion != 0f )
			maxCompleteZ --;
		
		for ( int x=StagingWorldGenerator.wallMinX+1; x<StagingWorldGenerator.wallMaxX; x++ )
			for ( int z=StagingWorldGenerator.wallMinZ; z<StagingWorldGenerator.wallMaxZ; z++ )
			{				
				Block b = stagingWorld.getBlockAt(x, StagingWorldGenerator.floorY-2, z);
				if ( b != null )
				{
					if ( z < maxCompleteZ )
					{
						b.setType(Material.REDSTONE_TORCH_ON);
						b.setData((byte)5);
					}
					else
						b.setType(Material.AIR);
				}
			}*/
	}
	
	public void removeWorldGenerationIndicator()
	{
		showWorldGenerationIndicator(0f);
	}
	
	public boolean seekNearestNetherFortress(Player player)
	{
		if ( netherWorld == null )
			return false;
		
		WorldServer world = ((CraftWorld)netherWorld).getHandle();
		
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
	
	public void generateWorlds(final WorldOption generator, final Runnable runWhenDone)
	{
		plugin.getGameMode().broadcastMessage("Preparing new worlds...");
		
		Runnable generationComplete = new Runnable() {
			@Override
			public void run()
			{
				// ensure those worlds are set to Hard difficulty
				plugin.worldManager.mainWorld.setDifficulty(Difficulty.HARD);
				plugin.worldManager.netherWorld.setDifficulty(Difficulty.HARD);
				
				switch ( plugin.animalNumbers )
				{
				case 0:
					mainWorld.setAnimalSpawnLimit(0);
					mainWorld.setTicksPerAnimalSpawns(600);
					break;
				case 1:
					mainWorld.setAnimalSpawnLimit(8);
					mainWorld.setTicksPerAnimalSpawns(500);
					break;
				case 2: // mc defaults
					mainWorld.setAnimalSpawnLimit(15);
					mainWorld.setTicksPerAnimalSpawns(400);
					break;
				case 3:
					mainWorld.setAnimalSpawnLimit(25);
					mainWorld.setTicksPerAnimalSpawns(300);
					break;
				case 4:
					mainWorld.setAnimalSpawnLimit(40);
					mainWorld.setTicksPerAnimalSpawns(200);
					break;
				}
				
				World[] worlds = netherWorld == null ? new World[] { mainWorld } : new World[] { mainWorld, netherWorld };
				for ( World world : worlds )
					switch ( plugin.monsterNumbers )
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
				
				removeWorldGenerationIndicator();
				
				// run whatever task was passed in
				if ( runWhenDone != null )
					runWhenDone.run();
			}
		};
	
		try
		{
			generator.createWorlds(generationComplete);
		}
		catch (ArrayIndexOutOfBoundsException ex)
		{
			plugin.log.warning("World generation crashed: see BUKKIT-2601!");
			plugin.getGameMode().broadcastMessage("An error occurred during world generation.\nThis is a bukkit bug we're waiting on a fix on.\nIt only happens the first time you try and generate.\nPlease try again...");
			
			plugin.setGameState(GameState.worldDeletion);
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
	// it also creates only one chunk per tick, instead of locking up the server while it generates 
    public World createWorld(WorldCreator creator, final Runnable runWhenDone, BlockPopulator... extraPopulators)
    {
        if (creator == null) {
            throw new IllegalArgumentException("Creator may not be null");
        }

        final CraftServer craftServer = (CraftServer)plugin.getServer();
        MinecraftServer console = craftServer.getServer();
        
        final String name = creator.name();
        ChunkGenerator generator = creator.generator();
        File folder = new File(craftServer.getWorldContainer(), name);
        World world = craftServer.getWorld(name);
        WorldType type = WorldType.getType(creator.type().getName());
        boolean generateStructures = creator.generateStructures();

        if (world != null) {
            return world;
        }

        if ((folder.exists()) && (!folder.isDirectory())) {
            throw new IllegalArgumentException("File exists with the name '" + name + "' and isn't a folder");
        }

        if (generator == null) {
            generator = craftServer.getGenerator(name);
        }

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

        final WorldServer worldServer = new WorldServer(console, new ServerNBTManager(craftServer.getWorldContainer(), name, true), name, dimension, new WorldSettings(creator.seed(), EnumGamemode.a(craftServer.getDefaultGameMode().getValue()), generateStructures, hardcore, type), console.methodProfiler, creator.environment(), generator);

        if (craftServer.getWorld(name) == null) {
            return null;
        }

        worldServer.worldMaps = console.worlds.get(0).worldMaps;

        worldServer.tracker = new EntityTracker(worldServer);
        worldServer.addIWorldAccess((IWorldAccess) new net.minecraft.server.WorldManager(console, worldServer));
        worldServer.difficulty = 3;
        worldServer.setSpawnFlags(true, true);
        console.worlds.add(worldServer);

        if (generator != null) {
        	worldServer.getWorld().getPopulators().addAll(generator.getDefaultPopulators(worldServer.getWorld()));
        }
        
        // use the extra block populators!
        if ( extraPopulators != null )
        	for ( int i=0; i<extraPopulators.length; i++ )
        		worldServer.getWorld().getPopulators().add(extraPopulators[i]);

        craftServer.getPluginManager().callEvent(new WorldInitEvent(worldServer.getWorld()));
        System.out.print("Preparing start region for level " + (console.worlds.size() - 1) + " (Seed: " + worldServer.getSeed() + ")");
        
        if ( mainWorld == null )
        	showWorldGenerationIndicator(0f);
        ChunkBuilder cb = new ChunkBuilder(12, craftServer, worldServer, plugin.getGameMode().usesNether(), mainWorld != null, runWhenDone);
    	cb.taskID = craftServer.getScheduler().scheduleSyncRepeatingTask(plugin, cb, 0L, 1L);
    	return worldServer.getWorld();
    }
    
    class ChunkBuilder implements Runnable
    {
    	public ChunkBuilder(int numChunksFromSpawn, CraftServer craftServer, WorldServer worldServer, boolean usesSecondaryWorld, boolean isSecondaryWorld, Runnable runWhenDone)
    	{
    		this.numChunksFromSpawn = numChunksFromSpawn;
    		sideLength = numChunksFromSpawn * 2 + 1;
    		numSteps = sideLength * sideLength;
    		
    		this.usesSecondaryWorld = usesSecondaryWorld;
    		this.isSecondaryWorld = isSecondaryWorld;
    		this.craftServer = craftServer;
    		this.worldServer = worldServer;
    		this.runWhenDone = runWhenDone;
    		
    		ChunkCoordinates spawnPos = worldServer.getSpawn();
        	spawnX = worldServer.getChunkAtWorldCoords(spawnPos.x, spawnPos.z).x;
        	spawnZ = worldServer.getChunkAtWorldCoords(spawnPos.x, spawnPos.z).z;
    	}
    	
    	int numChunksFromSpawn, stepNum = 0, sideLength, numSteps, spawnX, spawnZ;
        long reportTime = System.currentTimeMillis();
        boolean usesSecondaryWorld, isSecondaryWorld;
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
            	if ( usesSecondaryWorld )
            	{
            		fraction *= 0.5f;
            		if ( isSecondaryWorld )
            			fraction += 0.5f;
            	}
                showWorldGenerationIndicator(fraction);
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
            		if ( isSecondaryWorld || !usesSecondaryWorld )
            			showWorldGenerationIndicator(1f);
            		craftServer.getPluginManager().callEvent(new WorldLoadEvent(worldServer.getWorld()));
            		craftServer.getScheduler().cancelTask(taskID);
            		craftServer.getScheduler().scheduleSyncDelayedTask(plugin, runWhenDone);
            		return;
            	}
            }
        }
    }
}
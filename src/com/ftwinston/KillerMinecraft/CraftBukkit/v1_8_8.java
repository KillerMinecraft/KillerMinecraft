package com.ftwinston.KillerMinecraft.CraftBukkit;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import net.minecraft.server.v1_8_R3.ChunkProviderServer;
import net.minecraft.server.v1_8_R3.IChunkProvider;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.ChunkProviderHell;
import net.minecraft.server.v1_8_R3.EntityEnderSignal;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntityTracker;
import net.minecraft.server.v1_8_R3.EnumDifficulty;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IWorldAccess;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayInClientCommand;
import net.minecraft.server.v1_8_R3.RegionFile;
import net.minecraft.server.v1_8_R3.RegionFileCache;
import net.minecraft.server.v1_8_R3.ServerNBTManager;
import net.minecraft.server.v1_8_R3.StructureBoundingBox;
import net.minecraft.server.v1_8_R3.StructureStart;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.TileEntityCommand;
import net.minecraft.server.v1_8_R3.World;
import net.minecraft.server.v1_8_R3.WorldData;
import net.minecraft.server.v1_8_R3.WorldGenNether;
import net.minecraft.server.v1_8_R3.WorldGenVillage.WorldGenVillageStart;
import net.minecraft.server.v1_8_R3.WorldManager;
import net.minecraft.server.v1_8_R3.WorldServer;
import net.minecraft.server.v1_8_R3.WorldSettings;
import net.minecraft.server.v1_8_R3.WorldType;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World.Environment;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.generator.NormalChunkGenerator;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

import com.ftwinston.KillerMinecraft.KillerMinecraft;

public class v1_8_8 extends CraftBukkitAccess
{
	public v1_8_8(Plugin plugin)
	{
		super(plugin);
	}
	
	protected MinecraftServer getMinecraftServer()
	{
		CraftServer server = (CraftServer)plugin.getServer();
		return server.getServer();
	}

	public String getDefaultLevelName()
	{
		return getMinecraftServer().getPropertyManager().getString("level-name", "world");
	}

	public YamlConfiguration getBukkitConfiguration()
	{
		YamlConfiguration config = getField(CraftServer.class, plugin.getServer(), "configuration");
		return config;
	}

	public void saveBukkitConfiguration(YamlConfiguration configuration)
	{
		try
		{
			configuration.save((File)getMinecraftServer().options.valueOf("bukkit-settings"));
		}
		catch ( IOException ex )
		{
		}
	}
	
	public String getServerProperty(String name, String defaultVal)
	{
		return getMinecraftServer().getPropertyManager().properties.getProperty(name, defaultVal);
	}
	
	public void setServerProperty(String name, String value)
	{
		getMinecraftServer().getPropertyManager().properties.put(name, value);
	}
	
	public void saveServerPropertiesFile()
	{
		getMinecraftServer().getPropertyManager().savePropertiesFile();
	}
	/*
	public void sendForScoreboard(Player viewer, String name, boolean show)
	{
		PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
		((CraftPlayer)viewer).getHandle().playerConnection.sendPacket(packet);
	}
	*/
	public void sendForScoreboard(Player viewer, Player other, boolean show)
	{
		PacketPlayOutPlayerInfo.EnumPlayerInfoAction action = show ? PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER :PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER;
		EntityPlayer subject = ((CraftPlayer)other).getHandle();

		((CraftPlayer)viewer).getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(action, subject));
	}
	
	public void forceRespawn(Player player)
	{
    	PacketPlayInClientCommand packet = new PacketPlayInClientCommand(PacketPlayInClientCommand.EnumClientCommand.PERFORM_RESPAWN);
        ((CraftPlayer) player).getHandle().playerConnection.a(packet); // obfuscated;
	}
	
	public void bindRegionFiles()
	{
		regionfiles = getField(RegionFileCache.class, null, "a"); // obfuscated
		
		try
		{
			rafField = RegionFile.class.getDeclaredField("c"); // obfuscated
			rafField.setAccessible(true);
		}
		catch ( NoSuchFieldException e )
		{
			plugin.getLogger().warning("Error binding to region file cache.");
			e.printStackTrace();
			rafField = null;
		}
	}
	
	public void unbindRegionFiles()
	{
		regionfiles = null;
		rafField = null;
	}
	
	@SuppressWarnings("rawtypes")
	public synchronized boolean clearWorldReference(String worldName)
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
			plugin.getLogger().warning("Exception while removing world reference for '" + worldName + "'!");
			ex.printStackTrace();
		}
		for (Object key : removedKeys)
			regionfiles.remove(key);
		
		return true;
	}
	
	public void accountForDefaultWorldDeletion(org.bukkit.World newDefault)
	{
		// playerFileData in the player list must point to that of the new default world, instead of the deleted original world
		getMinecraftServer().getPlayerList().playerFileData = ((CraftWorld)newDefault).getHandle().getDataManager().getPlayerFileData();
	}
	
	public org.bukkit.World createWorld(org.bukkit.WorldType type, Environment env, String name, long seed, ChunkGenerator generator, String generatorSettings, boolean generateStructures)
    {
        final Server server = plugin.getServer();
        MinecraftServer console = getMinecraftServer();
        
        File folder = new File(server.getWorldContainer(), name);
        org.bukkit.World world = server.getWorld(name);

        if (world != null)
            return world;

        if ((folder.exists()) && (!folder.isDirectory()))
            throw new IllegalArgumentException("File exists with the name '" + name + "' and isn't a folder");

        
        WorldType worldType = WorldType.getType(type.getName());
        
        int dimension = 10 + console.worlds.size();
        boolean used = false;
        do {
            for (WorldServer ws : console.worlds) {
                used = ws.dimension == dimension;
                if (used) {
                    dimension++;
                    break;
                }
            }
        } while(used);
        boolean hardcore = false;

		@SuppressWarnings("deprecation")
		WorldSettings worldSettings = new WorldSettings(seed, WorldSettings.EnumGamemode.getById(server.getDefaultGameMode().getValue()), generateStructures, hardcore, worldType);
		worldSettings.setGeneratorSettings(generatorSettings);
		
        final WorldServer worldServer = new WorldServer(console, new ServerNBTManager(server.getWorldContainer(), name, true), new WorldData(worldSettings, name), dimension, console.methodProfiler, env, generator);

        if (server.getWorld(name) == null)
            return null;

        worldServer.b(); // obfuscated - set villages object that will otherwise cause a crash
        worldServer.a(worldSettings); // obfuscated - choose a sensible spawn point
        worldServer.worldMaps = console.worlds.get(0).worldMaps;

        worldServer.tracker = new EntityTracker(worldServer);
        worldServer.addIWorldAccess((IWorldAccess) new WorldManager(console, worldServer));
        worldServer.getWorldData().setDifficulty(EnumDifficulty.HARD);
        worldServer.setSpawnFlags(true, true);

        console.worlds.add(worldServer);

        world = worldServer.getWorld();
        return world;
    }

	public void finishCreateWorld(org.bukkit.World world, org.bukkit.WorldType type, Environment env, String name, long seed, ChunkGenerator generator, String generatorSettings, boolean generateStructures)
	{
		Server server = plugin.getServer();
        boolean hardcore = false;
        WorldType worldType = WorldType.getType(type.getName());

		@SuppressWarnings("deprecation")
		WorldSettings worldSettings = new WorldSettings(seed, WorldSettings.EnumGamemode.getById(server.getDefaultGameMode().getValue()), generateStructures, hardcore, worldType);
		worldSettings.setGeneratorSettings(generatorSettings);
	}
	
	public boolean isChunkGenerated(Chunk chunk)
	{
		return ((CraftChunk)chunk).getHandle().isDone();
	}
	
	public Location findNearestNetherFortress(Location loc)
	{
		if ( loc.getWorld().getEnvironment() != Environment.NETHER )
			return null;
		
		WorldServer world = ((CraftWorld)loc.getWorld()).getHandle();
		
		IChunkProvider chunkProvider = getField(ChunkProviderServer.class, world.chunkProviderServer, "chunkProvider");
		if ( chunkProvider == null )
		{
			KillerMinecraft.instance.log.info("chunkProvider null");
			return null;
		}
		
		ChunkProviderHell hellCP = getField(NormalChunkGenerator.class, chunkProvider, "provider");
		if ( hellCP == null )
			return null;
		
		WorldGenNether fortressGenerator = getField(ChunkProviderHell.class, hellCP, "B"); // obfuscated
		BlockPosition pos = fortressGenerator.getNearestGeneratedFeature(world, new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
		if ( pos == null )
			return null; // this just means there isn't one nearby
		
		return new Location(loc.getWorld(), pos.getX(), pos.getY(), pos.getZ());
	}
	
	public boolean createFlyingEnderEye(Player player, Location target)
	{
		WorldServer world = ((CraftWorld)target.getWorld()).getHandle();
		
		Location playerLoc = player.getLocation();
		EntityEnderSignal entityendersignal = new EntityEnderSignal(world, playerLoc.getX(), playerLoc.getY() + player.getEyeHeight() - 0.2, playerLoc.getZ());

		entityendersignal.a(new BlockPosition(target.getX(), target.getBlockY(), target.getZ())); // obfuscated
		world.addEntity(entityendersignal);
		world.makeSound(((CraftPlayer)player).getHandle(), "random.bow", 0.5F, 0.4F);
		world.a((EntityHuman) null, 1002, new BlockPosition(playerLoc.getBlockX(), playerLoc.getBlockY(), playerLoc.getBlockZ()), 0); // obfuscated
		return true;
	}

	@Override
	public void pushButton(org.bukkit.block.Block b)
	{
		Blocks.STONE_BUTTON.interact(((CraftWorld)b.getWorld()).getHandle(), new BlockPosition(b.getX(), b.getY(), b.getZ()), null, null, EnumDirection.DOWN, 0f, 0f, 0f);
	}
	
	@Override
	public void setCommandBlockCommand(org.bukkit.block.Block b, String command)
	{
		World world = ((CraftWorld)b.getWorld()).getHandle();
		TileEntity tileEntity = world.getTileEntity(new BlockPosition(b.getX(), b.getY(), b.getZ()));
		if ( tileEntity == null || !(tileEntity instanceof TileEntityCommand) )
			return;
		
		TileEntityCommand tec = (TileEntityCommand)tileEntity;
		tec.getCommandBlock().setCommand(command);
	}
	
	@Override
	public org.bukkit.inventory.ItemStack setEnchantmentGlow(org.bukkit.inventory.ItemStack item)
	{
		CraftItemStack output = CraftItemStack.asCraftCopy(item);
		
		ItemStack nmsStack = getField(CraftItemStack.class, output, "handle");
		if ( nmsStack == null )
			return item;
        NBTTagCompound compound = nmsStack.getTag();
 
        // Initialize the compound if we need to
        if (compound == null) {
            compound = new NBTTagCompound();
            nmsStack.setTag(compound);
        }
 
        // Empty enchanting compound
        compound.set("ench", new NBTTagList());
        return output;
	}

	@Override
	public void generateVillage(org.bukkit.Location loc, Random random, int radius)
	{
		Chunk chunk = loc.getChunk();
		CraftWorld craftworld = (CraftWorld)loc.getWorld();
		
		StructureStart start = new WorldGenVillageStart(craftworld.getHandle(), random, chunk.getZ(), chunk.getZ(), 0);
		start.a(craftworld.getHandle(), random, new StructureBoundingBox(loc.getBlockX() - radius, loc.getBlockZ() - radius, loc.getBlockX() + radius, loc.getBlockZ() + radius));
	}
}
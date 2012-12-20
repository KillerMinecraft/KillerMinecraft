package com.ftwinston.Killer;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

class ArenaManager
{
	public ArenaManager(Killer plugin, World world)
	{
		this.plugin = plugin;
		stagingWorld = world;
	}
	Killer plugin;
	World stagingWorld;
	
	Random random = new Random();
	
	public void pressurePlatePressed(Player player)
	{
		PlayerInventory inv = player.getInventory();
		plugin.playerManager.removeInventoryItems(inv, Material.IRON_SWORD, Material.DIAMOND_SPADE);
		inv.addItem(new ItemStack(plugin.arenaManager.monsterArenaModeEnabled ? Material.IRON_SWORD : Material.DIAMOND_SPADE));
		
		if ( monsterWaveNumber == 0 && countPlayersInArena() == 0 )
		{
			rebuildArena();
			endMonsterArena();

			// clear the world writing
			for ( int x=arenaScoreX - 25; x<arenaScoreX + 25; x++ )
				for ( int y=StagingWorldGenerator.floorY+3; y<StagingWorldGenerator.floorY+8; y++ )
					stagingWorld.getBlockAt(x, y, arenaScoreZ).setType(Material.AIR);
			
			if ( monsterArenaModeEnabled )
			{
				prepareNextMonsterWave();

				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					@Override
					public void run() {
						spawnMonsterWave();					
					}
				}, 50);
			}
		}
	}
	
	boolean monsterArenaModeEnabled = false;
	int monsterWaveNumber = 0, numMonstersAlive = 0;
	public void monsterKilled()	
	{
		if ( monsterWaveNumber == 0 )
			return;
		
		numMonstersAlive--;
		
		if ( numMonstersAlive <= 0 )
		{
			prepareNextMonsterWave();
			
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					spawnMonsterWave();					
				}
			}, 50);
		}
	}
	
	public void playerKilled()
	{
		if ( !monsterArenaModeEnabled )
			return;
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				if ( countPlayersInArena() == 0 )
					endMonsterArena();
			}
		}, 40);
	}
	
	public void endMonsterArena()
	{
		for ( Entity entity : stagingWorld.getEntities() )
			if ( entity instanceof Monster )
				entity.remove();
		monsterWaveNumber = 0; numMonstersAlive = 0;
		stagingWorld.setMonsterSpawnLimit(0);
	}
	
	private int countPlayersInArena()
	{
		int numPlayersInArena = 0;
		for ( Player player : stagingWorld.getPlayers() )
		{
			Location loc = player.getLocation();
			if ( loc.getBlockX() < StagingWorldGenerator.spleefMinX - 1 || loc.getBlockX() > StagingWorldGenerator.spleefMaxX + 1
			  || loc.getBlockZ() < StagingWorldGenerator.spleefMinZ - 1 || loc.getBlockZ() > StagingWorldGenerator.spleefMaxZ + 1)
				continue;
			numPlayersInArena ++;
		}
		return numPlayersInArena;
	}
	
	private static final int arenaScoreZ = StagingWorldGenerator.spleefMaxZ + 8, arenaScoreX = StagingWorldGenerator.waitingMonsterButtonX + 2;
	public void prepareNextMonsterWave()
	{
		monsterWaveNumber++;
		
		// write the wave number into the world
		boolean[][] text = StagingWorldGenerator.writeBlockText("WAVE " + monsterWaveNumber);
		int xMin = arenaScoreX + text.length/2, yMin = StagingWorldGenerator.floorY + 3;
		for ( int i=0; i<text.length; i++ )
			for ( int j=0; j<text[i].length; j++ )
				stagingWorld.getBlockAt(xMin-i, yMin + j, arenaScoreZ).setType(text[i][j] ? Material.SNOW_BLOCK : Material.AIR);
	}
	
	public void spawnMonsterWave()
	{
		if ( countPlayersInArena() == 0 )
		{
			endMonsterArena();
			return;
		}
		
		stagingWorld.setMonsterSpawnLimit(monsterWaveNumber);
		
		// wither + normal skeleton
		if ( monsterWaveNumber == 5 )
		{
			numMonstersAlive = 2;
			
			Skeleton skelly = (Skeleton)stagingWorld.spawnEntity(getMonsterSpawnLocation(), EntityType.SKELETON);
			skelly.setSkeletonType(SkeletonType.NORMAL);
			skelly.getEquipment().setItemInHand(new ItemStack(Material.BOW));
			
			skelly = (Skeleton)stagingWorld.spawnEntity(getMonsterSpawnLocation(), EntityType.SKELETON);
			skelly.setSkeletonType(SkeletonType.WITHER);
			skelly.getEquipment().setItemInHand(new ItemStack(Material.STONE_SWORD));
			return;
		}
		
		numMonstersAlive = monsterWaveNumber;
		
		if ( monsterWaveNumber % 10 == 0 )
		{
			// add a witch into the mix every 10th wave
			stagingWorld.spawnEntity(getMonsterSpawnLocation(), EntityType.WITCH);
		}
		
		for ( int i=0; i<monsterWaveNumber; i++ )
		{
			switch ( random.nextInt(4) )
			{
			case 0:
				stagingWorld.spawnEntity(getMonsterSpawnLocation(), EntityType.SPIDER);
				break;
			case 1:
				Zombie zomb = (Zombie)stagingWorld.spawnEntity(getMonsterSpawnLocation(), EntityType.ZOMBIE);
				if ( random.nextInt(16) == 0 )
					zomb.setBaby(true);
				if ( random.nextInt(6) == 0 )
					zomb.setVillager(true);
				break;
			case 2:
				Skeleton skelly = (Skeleton)stagingWorld.spawnEntity(getMonsterSpawnLocation(), EntityType.SKELETON);
				skelly.setSkeletonType(SkeletonType.NORMAL);
				skelly.getEquipment().setItemInHand(new ItemStack(Material.BOW));
				break;
			case 3:
				stagingWorld.spawnEntity(getMonsterSpawnLocation(), EntityType.CREEPER);
				break;
			}
		}
	}
	
	private Location getMonsterSpawnLocation()
	{
		Location loc;
		int triesLeft = 3, highestY;
		do
		{
			loc = new Location(stagingWorld, StagingWorldGenerator.spleefMinX + random.nextDouble() * 16, StagingWorldGenerator.spleefY+1, StagingWorldGenerator.spleefMinZ + random.nextDouble() * 16);
			highestY = stagingWorld.getHighestBlockYAt(loc);
			triesLeft --;
		}
		while ( triesLeft > 0 && highestY < StagingWorldGenerator.spleefY );
		
		if ( highestY >= StagingWorldGenerator.spleefY )
			loc.setY(highestY);
		
		return loc;
	}
	
	private void rebuildArena()
	{
		for ( int x=StagingWorldGenerator.spleefMinX; x<=StagingWorldGenerator.spleefMaxX; x++ )
			for ( int z=StagingWorldGenerator.spleefMinZ; z<=StagingWorldGenerator.spleefMaxZ; z++ )
			{
				stagingWorld.getBlockAt(x, StagingWorldGenerator.spleefY, z).setType(Material.DIRT);
				for ( int y=StagingWorldGenerator.spleefY+1; y<StagingWorldGenerator.spleefY+3; y++ )
					stagingWorld.getBlockAt(x, y, z).setType(Material.AIR);
			}
		
		if ( monsterArenaModeEnabled )
		{
			int centerZ = (StagingWorldGenerator.spleefMinZ + StagingWorldGenerator.spleefMaxZ) / 2;
			for ( int x=StagingWorldGenerator.spleefMinX + 3; x<=StagingWorldGenerator.spleefMaxX - 3; x++ )
				for ( int y=StagingWorldGenerator.spleefY + 1; y < StagingWorldGenerator.spleefY + 3; y++ )
				{
					stagingWorld.getBlockAt(x, y, centerZ).setType(Material.DIRT);
					stagingWorld.getBlockAt(x, y, centerZ + 1).setType(Material.DIRT);
				}
		}
	}
}

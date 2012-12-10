package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.World.Environment;
import org.bukkit.WorldType;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

// similar to WorldCreator, but has list of extra block populators, and no pesky createWorld method
public class WorldConfig
{
	public WorldConfig(String name, Environment env)
	{
		this.name = name;
		this.environment = env;
		this.type = WorldType.NORMAL;
		this.seed = seedGen.nextLong();
		this.generator = null;
		this.extraPopulators = new ArrayList<BlockPopulator>();
		this.generateStructures = true;
		this.chunkGeneratorLocked = false;
		this.generatorSettings = "";
	}
	
	public static long getSeedFromString(String str)
	{// copied from how bukkit handles string seeds
		long k = seedGen.nextLong();

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
	
	private static Random seedGen = new Random(); 
	
	private String name;
	private Environment environment;
	private WorldType type;
	private long seed;
	private ChunkGenerator generator;
	private List<BlockPopulator> extraPopulators;
	private boolean generateStructures;
	private boolean chunkGeneratorLocked;
	private String generatorSettings;
	
	public String getName() {
		return name;
	}
	
	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public WorldType getWorldType() {
		return type;
	}

	public void setWorldType(WorldType type) {
		this.type = type;
	}
	
	public long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	public ChunkGenerator getGenerator() {
		return generator;
	}

	public void setGenerator(ChunkGenerator generator) {
		if ( !chunkGeneratorLocked )
			this.generator = generator;
	}

	void lockChunkGenerator() {
		chunkGeneratorLocked = true;
	}
	
	public List<BlockPopulator> getExtraPopulators() {
		return extraPopulators;
	}
	public void setExtraPopulators(List<BlockPopulator> extraPopulators) {
		this.extraPopulators = extraPopulators;
	}

	public boolean getGenerateStructures() {
		return generateStructures;
	}

	public void setGenerateStructures(boolean generateStructures) {
		this.generateStructures = generateStructures;
	}
	
	public String getGeneratorSettings() {
		return generatorSettings;
	}

	public void setGeneratorSettings(String settings) {
		this.generatorSettings = settings;
	}

}

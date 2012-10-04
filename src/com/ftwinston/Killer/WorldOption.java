package com.ftwinston.Killer;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.generator.ChunkGenerator;

public class WorldOption
{
	public static List<WorldOption> options = new ArrayList<WorldOption>();
	public static WorldOption get(int num) { return options.get(num); }
	
	public static void setup(Killer killer)
	{
		
	
	}

	public WorldOption(String name, ChunkGenerator generator)
	{
		this.name = name;
	}
	
	public WorldOption(String name)
	{
		this.name = name;
	}
	
	private String name;
	public String getName()
	{
		return name;
	}
	
	private ChunkGenerator generator;
	public ChunkGenerator getGenerator()
	{
		return generator;
	}
}
package com.ftwinston.Killer.WorldOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;

import com.ftwinston.Killer.Settings;

public class CopyExistingWorld extends com.ftwinston.Killer.WorldOption
{
	public CopyExistingWorld(String name)
	{
		super(name);
	}
	
	public boolean isFixedWorld() { return true; }
	
	public void create()
	{		
		File sourceDir = new File(plugin.getServer().getWorldContainer() + File.separator + name);
		File targetDir = new File(plugin.getServer().getWorldContainer() + File.separator + Settings.killerWorldName);
		
		try
		{
			copyFolder(sourceDir, targetDir);
		}
		catch (IOException ex)
		{
			plugin.log.warning("An error occurred copying the " + name + " world");
		}
		
		sourceDir = new File(plugin.getServer().getWorldContainer() + File.separator + name + "_nether");
		targetDir = new File(plugin.getServer().getWorldContainer() + File.separator + Settings.killerWorldName + "_nether");
		
		if ( sourceDir.exists() && sourceDir.isDirectory() )
			try
			{
				copyFolder(sourceDir, targetDir);
			}
			catch (IOException ex)
			{
				plugin.log.warning("An error occurred copying the " + name + "_nether world");
			}
		
		plugin.worldManager.mainWorld = plugin.getServer().createWorld(new WorldCreator(Settings.killerWorldName).environment(Environment.NORMAL));
		plugin.worldManager.netherWorld = plugin.getServer().createWorld(new WorldCreator(Settings.killerWorldName + "_nether").environment(Environment.NETHER));
	}
	
	private void copyFolder(File source, File dest) throws IOException
	{
		if(source.isDirectory())
		{	 
    		//if directory doesn't exist, create it
    		if(!dest.exists())
    		   dest.mkdir();
 
    		//list all the directory contents
    		String files[] = source.list();
    		if ( files == null )
    			return;
    		
    		//recursively copy everything in the directory
    		for (String file : files)
    		{
    		   File srcFile = new File(source, file);
    		   File destFile = new File(dest, file);
    		   
    		   copyFolder(srcFile,destFile);
    		}
    	}
		else
		{
			InputStream in = null;
			OutputStream out = null;
			try
			{
	    		in = new FileInputStream(source);
		        out = new FileOutputStream(dest); 
				byte[] buffer = new byte[1024];
				
				int length;
				while ((length = in.read(buffer)) > 0)
				{
					out.write(buffer, 0, length);
				}
			}
			finally
			{
				if ( in != null )
					in.close();
				if ( out != null )
					out.close();
			}
    	}
	}
}

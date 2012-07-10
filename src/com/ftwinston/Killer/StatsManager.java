package com.ftwinston.Killer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class StatsManager
{
	Killer plugin;
	public StatsManager(Killer plugin)
	{
		this.plugin = plugin;
		isTracking = false;
	}

	private static final int version = 1;
	boolean isTracking; 
	private Date startedOn;
	private int numPlayersStart, numPlayersLateJoin, numPlayersQuit, numKillers, numKillersAdminAdded;
	
	public void gameStarted(int numPlayers)
	{
		isTracking = true;
		numPlayersStart = numPlayers; numPlayersLateJoin = 0; numPlayersQuit = 0; numKillers = 0; numKillersAdminAdded = 0;
		startedOn = new Date();
	}
	
	public void gameFinished(int numPlayers, int outcome, int winningItemID)
	{
		if ( !isTracking || !plugin.reportStats)
			return;
	
		isTracking = false;
		
		Date finishedOn = new Date();
		int duration = (int)(finishedOn.getTime() - startedOn.getTime()) / 1000;	
		
		plugin.log.info("Sending stats...");
		
		final URL statsPage;
		try
		{
			statsPage = new URL("http://killer.ftwinston.com/?d=" + duration + "&v=" + version + "&o=" + outcome + "&i=" + winningItemID + "&ns=" + numPlayersStart + "&ne=" + numPlayers + "&nl=" + numPlayersLateJoin + "&nq=" + numPlayersQuit + "&nk=" + numKillers + "&na=" + numKillersAdminAdded);
		}
		catch ( MalformedURLException ex )
		{
			plugin.log.info("Error generating stats URL, unable to send stats");
			return;
		}
		
		plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				try
				{
					BufferedReader in = new BufferedReader(new InputStreamReader(statsPage.openStream()));
					String inputLine;
			        while ((inputLine = in.readLine()) != null)
			        	if ( Killer.DEBUG )
			        		plugin.log.info("Stats server says: " + inputLine);
			        in.close();
				}
				catch ( IOException ex )
				{
					plugin.log.info("Error sending stats: " + ex.getMessage());
				}
			}
		});
	}
	
	public void playerJoinedLate()
	{
		numPlayersLateJoin++;
	}
	
	public void playerQuit()
	{
		numPlayersQuit++;
	}
	
	public void killerAdded()
	{
		numKillers++;
	}
	
	public void killerAddedByAdmin()
	{
		numKillersAdminAdded++;
	}
}
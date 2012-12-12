package com.ftwinston.Killer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

class StatsManager
{
	Killer plugin;
	public StatsManager(Killer plugin)
	{
		this.plugin = plugin;
		isTracking = false;
	}

	private static final int version = 3;
	public boolean isTracking;
	private Date startedOn;
	private int numPlayersStart, numPlayersLateJoin, numPlayersQuit;
	
	public void gameStarted(int numPlayers)
	{
		isTracking = true;
		numPlayersStart = numPlayers; numPlayersLateJoin = 0; numPlayersQuit = 0;
		startedOn = new Date();
	}
	
	public void gameFinished(GameMode mode, int numPlayersEnd, boolean abandoned)
	{
		if ( !isTracking || !Settings.reportStats)
			return;
	
		isTracking = false;
		
		Date finishedOn = new Date();
		int duration = (int)(finishedOn.getTime() - startedOn.getTime()) / 1000;	
		
		plugin.log.info("Sending stats...");
		
		final URL statsPage;
		try
		{
			statsPage = new URL("http://killer.ftwinston.com/report/?m=" + URLEncoder.encode(mode.getName(), "UTF-8") + "&d=" + duration + "&v=" + version + "&ns=" + numPlayersStart + "&ne=" + numPlayersEnd + "&nl=" + numPlayersLateJoin + "&nq=" + numPlayersQuit + "&a=" + (abandoned ? "1" : "0"));
		}
		catch ( Exception ex )
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
			        while (in.readLine() != null)
			        	;
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
}
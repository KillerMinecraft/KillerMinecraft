package com.ftwinston.KillerMinecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;


class StatsManager
{
	KillerMinecraft plugin;
	public StatsManager(KillerMinecraft plugin, int numGames)
	{
		this.plugin = plugin;
		games = new GameInfo[numGames];
		for ( int i=0; i<numGames; i++ )
			games[i] = new GameInfo();
	}
	
	class GameInfo
	{
		public GameInfo()
		{
			isTracking = false;
		}
		
		public void started(int numPlayers)
		{
			isTracking = true;
			numPlayersStart = numPlayers; numPlayersLateJoin = 0; numPlayersQuit = 0;
			startedOn = new Date();
		}
		
		public void finished()
		{
			isTracking = false;
		}
		
		public void playerJoinedLate()
		{
			numPlayersLateJoin++;
		}
		
		public void playerQuit()
		{
			numPlayersQuit++;
		}
		
		private boolean isTracking;
		private Date startedOn;
		private int numPlayersStart, numPlayersLateJoin, numPlayersQuit;		
	}

	GameInfo[] games;
	private static final int version = 3;
	
	public boolean isTracking(int gameNum)
	{
		return games[gameNum].isTracking;
	}
	
	public void gameStarted(int gameNum, int numPlayers)
	{
		games[gameNum].started(numPlayers);
	}
	
	public void gameFinished(int gameNum, GameMode mode, WorldGenerator world, int numPlayersEnd, boolean abandoned)
	{
		GameInfo game = games[gameNum];
		if ( !game.isTracking || !Settings.reportStats)
			return;
	
		game.finished();
		
		Date finishedOn = new Date();
		int duration = (int)(finishedOn.getTime() - game.startedOn.getTime()) / 1000;	
		
		plugin.log.info("Sending stats...");
		
		String modeOptions = "";
		for ( Option option : mode.options )
			modeOptions += option.getName().replace(":"," - ").replace(";"," - ") + ":" + option.getValueString() + ";";
		
		String worldOptions = "";
		for ( Option option : world.options )
			worldOptions += option.getName().replace(":"," - ").replace(";"," - ") + ":" + option.getValueString() + ";";
		
		final URL statsPage;
		try
		{
			statsPage = new URL("http://killer.ftwinston.com/report/?m=" + URLEncoder.encode(mode.getName(), "UTF-8") + "&w=" + URLEncoder.encode(world.getName(), "UTF-8") + "&d=" + duration + "&v=" + version + "&ns=" + game.numPlayersStart + "&ne=" + numPlayersEnd + "&nl=" + game.numPlayersLateJoin + "&nq=" + game.numPlayersQuit + "&a=" + (abandoned ? "1" : "0") + "&mo=" + URLEncoder.encode(modeOptions, "UTF-8") + "&wo=" + URLEncoder.encode(worldOptions, "UTF-8"));
		}
		catch ( Exception ex )
		{
			plugin.log.info("Error generating stats URL, unable to send stats");
			return;
		}
		
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
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
	
	public void playerJoinedLate(int gameNum)
	{
		games[gameNum].playerJoinedLate();
	}
	
	public void playerQuit(int gameNum)
	{
		games[gameNum].playerQuit();
	}
}
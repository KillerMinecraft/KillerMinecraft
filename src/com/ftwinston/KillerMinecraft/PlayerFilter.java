package com.ftwinston.KillerMinecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import com.ftwinston.KillerMinecraft.PlayerManager.Info;
import com.ftwinston.KillerMinecraft.Configuration.TeamInfo;

public class PlayerFilter
{
	public PlayerFilter()
	{
	
	}
	
	private enum Setting
	{
		Ignored,
		Required,
		RequiredNot,
	}
	
	private Setting aliveState = Setting.Ignored;
	private Setting onlineState = Setting.Ignored;
	
	private TeamInfo team;
	private Setting teamState = Setting.Ignored;
	
	private World world;
	private Setting worldState = Setting.Ignored;
	
	private ArrayList<String> excludedPlayers = new ArrayList<String>();
	
	private Game game = null;

	PlayerFilter setGame(Game game)
	{
		this.game = game;
		return this;
	}
	
	Game getGame() { return game; }
	
	public PlayerFilter alive()
	{
		aliveState = Setting.Required;
		return this;
	}
	
	public PlayerFilter notAlive()
	{
		aliveState = Setting.RequiredNot;
		return this;
	}

	public PlayerFilter online()
	{
		onlineState = Setting.Required;
		return this;
	}
	
	public PlayerFilter offline()
	{
		onlineState = Setting.RequiredNot;
		return this;
	}
	
	public PlayerFilter team(TeamInfo team)
	{
		this.team = team;
		teamState = Setting.Required;
		return this;
	}
	
	public PlayerFilter notTeam(TeamInfo team)
	{
		this.team = team;
		teamState = Setting.RequiredNot;
		return this;
	}
	
	public PlayerFilter world(World w)
	{
		world = w;
		worldState = Setting.Required;
		return this;
	}
	
	public PlayerFilter notWorld(World w)
	{
		world = w;
		worldState = Setting.RequiredNot;
		return this;
	}
	
	public PlayerFilter exclude(String... playerNames)
	{
		for ( String player : playerNames )
			excludedPlayers.add(player);
		return this;
	}
	
	public PlayerFilter exclude(OfflinePlayer... players)
	{
		for ( OfflinePlayer player : players )
			excludedPlayers.add(player.getName());
		return this;
	}
	
	private boolean matchesAny(String val, List<String> any)
	{
		for ( String test : any )
			if ( val.equals(test) )
				return true;
		
		return false;
	}
	
	// not to be called directly by game modes (so that Game can be set by the GameMode), but to be returned by an equivalent method in GameMode
	List<Player> getOnlinePlayers()
	{
		ArrayList<Player> players = new ArrayList<Player>();
		
		if ( onlineState == Setting.RequiredNot )
			return players;
		
		for ( Map.Entry<String, Info> info : game.getPlayerInfo().entrySet() )
		{
			Info infoVal = info.getValue();
			
			switch ( aliveState )
			{
			case Required:
				if ( !infoVal.isAlive() )
					continue;
				break;
			case RequiredNot:
				if ( infoVal.isAlive() )
					continue;
				break;
			case Ignored:
				break;
			}
			
			switch ( teamState )
			{
			case Required:
				if ( infoVal.getTeam() != team )
					continue;
				break;
			case RequiredNot:
				if ( infoVal.getTeam() == team )
					continue;
				break;
			case Ignored:
				break;
			}

			Player p = KillerMinecraft.instance.getServer().getPlayerExact(info.getKey());
			if ( p == null )
				continue;
			
			switch ( worldState )
			{
			case Required:
				if ( p.getWorld() != world )
					continue;
				break;
			case RequiredNot:
				if ( p.getWorld() == world )
					continue;
			case Ignored:
				break;
			}
			
			if ( matchesAny(info.getKey(), excludedPlayers) )
				continue;
			
			players.add(p);
		}
		return players;
	}
	
	List<OfflinePlayer> getPlayers()
	{
		ArrayList<OfflinePlayer> players = new ArrayList<OfflinePlayer>();
		
		for ( Map.Entry<String, Info> info : game.getPlayerInfo().entrySet() )
		{
			Info infoVal = info.getValue();
			
			switch ( aliveState )
			{
			case Required:
				if ( !infoVal.isAlive() )
					continue;
				break;
			case RequiredNot:
				if ( infoVal.isAlive() )
					continue;
				break;
			case Ignored:
				break;
			}
			
			switch ( teamState )
			{
			case Required:
				if ( infoVal.getTeam() != team )
					continue;
				break;
			case RequiredNot:
				if ( infoVal.getTeam() == team )
					continue;
				break;
			case Ignored:
				break;
			}

			OfflinePlayer op = KillerMinecraft.instance.getServer().getOfflinePlayer(info.getKey());
			if ( op == null )
				continue;
			
			switch ( onlineState )
			{
			case Required:
				if ( !op.isOnline() )
					continue;
				break;
			case RequiredNot:
				if ( op.isOnline() )
					continue;
				break;
			case Ignored:
				break;
			}
			
			Player p = op.getPlayer();
			switch ( worldState )
			{
			case Required:
				if ( p == null || p.getWorld() != world )
					continue;
				break;
			case RequiredNot:
				if ( p == null ||p.getWorld() == world )
					continue;
				break;
			case Ignored:
				break;
			}
			
			if ( matchesAny(info.getKey(), excludedPlayers) )
				continue;
			
			players.add(op);
		}
		return players;
	}
}

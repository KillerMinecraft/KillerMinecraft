package com.ftwinston.Killer;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import com.ftwinston.Killer.PlayerManager;

import com.ftwinston.Killer.GameModes.InvisibleKiller;
import com.ftwinston.Killer.GameModes.MysteryKiller;

public abstract class GameMode
{
	public static Map<String, GameMode> gameModes = new LinkedHashMap<String, GameMode>();

	public static void setupGameModes()
	{
		GameMode g = new MysteryKiller();
		gameModes.put(g.getName(), g);
		
		g = new InvisibleKiller();
		gameModes.put(g.getName(), g);
	}
	
	public static GameMode getByName(String name) { return gameModes.get(name); }
	public static GameMode getDefault() { return gameModes.get("Mystery Killer"); }
	
	public abstract String getName();
	public abstract int absMinPlayers();
	public abstract boolean killersCompassPointsAtFriendlies();
	public abstract boolean friendliesCompassPointsAtKiller();
	public abstract boolean discreteDeathMessages();
	public abstract boolean usesPlinth();
	public abstract int determineNumberOfKillersToAdd(int numAlive, int numKillers, int numAliveKillers);
	
	public abstract String describePlayer(boolean killer);
	public abstract boolean informOfKillerAssignment(PlayerManager pm);
	public abstract boolean informOfKillerIdentity(PlayerManager pm);
	
	public abstract boolean playerJoined(Player player, PlayerManager pm, boolean isNewPlayer, boolean isKiller, int numKillersAssigned);
	public abstract void prepareKiller(Player player, PlayerManager pm);
	public abstract void prepareFriendly(Player player, PlayerManager pm);
	
	public abstract void checkForEndOfGame(PlayerManager pm, Player playerOnPlinth, Material itemOnPlinth);
	
	public void playerDamaged(EntityDamageEvent event) { }
}

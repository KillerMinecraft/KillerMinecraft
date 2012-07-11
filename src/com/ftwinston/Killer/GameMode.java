package com.ftwinston.Killer;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.entity.Player;

import com.ftwinston.Killer.PlayerManager;

import com.ftwinston.Killer.GameModes.MysteryKiller;

public abstract class GameMode
{
	public static Map<String, GameMode> gameModes = new LinkedHashMap<String, GameMode>();

	public static void setupGameModes()
	{
		GameMode g = new MysteryKiller();
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
	
	public abstract boolean playerJoined(Player player, boolean isNewPlayer, boolean isKiller, int numKillersAssigned);
	public abstract void giveItemsToKiller(Player player, int numKillers, int numFriendlies);
	public abstract void giveItemsToFriendly(Player player, int numKillers, int numFriendlies);
	
	public abstract void checkForEndOfGame(PlayerManager playerManager, Player playerOnPlinth, Material itemOnPlinth);
}

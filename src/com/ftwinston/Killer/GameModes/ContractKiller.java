package com.ftwinston.Killer.GameModes;

import java.util.Map;
import java.util.Random;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.PlayerManager;
import com.ftwinston.Killer.PlayerManager.Info;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.Material;

public class ContractKiller extends GameMode
{
	Random r = new Random();
	
	@Override
	public String getName() { return "Contract Killer"; }
	
	@Override
	public int getModeNumber() { return 5; }

	@Override
	public int absMinPlayers() { return 4; }

	@Override
	public boolean killersCompassPointsAtFriendlies() { return false; }

	@Override
	public boolean friendliesCompassPointsAtKiller() { return false; }

	@Override
	public boolean discreteDeathMessages() { return false; }

	@Override
	public boolean usesPlinth() { return false; }

	@Override
	public int determineNumberOfKillersToAdd(int numAlive, int numKillers, int numAliveKillers)
	{
		return numAlive - numAliveKillers;
	}
	
	@Override
	public String describePlayer(boolean killer, boolean plural)
	{
		return plural ? "players" : "player";
	}
	
	@Override
	public boolean informOfKillerAssignment(PlayerManager pm) { return false; }
	
	@Override
	public boolean informOfKillerIdentity() { return false; }
	
	@Override
	public boolean immediateKillerAssignment() { return true; }
	
	@Override
	public void explainGameMode(Player player, PlayerManager pm)
	{
		boolean isKiller = pm.isKiller(player.getName());
		String message = getName() + "\n";
		message += "Everyone " + (pm.numKillersAssigned() > 0 ? "has been" : "will soon be") + " assigned a target, and they must try and kill this player without being seen doing so by anybody else. Your compass will point towards your victim, and if anyone sees you kill them, you will die instead of them. Remember that someone else is hunting you! If you kill anyone other than your target or your hunter, you will die instead of them.\nWhen you kill your target, you are assigned their target, and the game continues until only one player remains alive.";
		player.sendMessage(message);
	}
	
	@Override
	public void playerJoined(Player player, PlayerManager pm, boolean isNewPlayer, PlayerManager.Info info)
	{
		if ( isNewPlayer ) // this is a new player, and the game's started ... so fit them into the victim list
		{
			player.sendMessage("Welcome to Killer Minecraft!");
			
			if ( pm.numKillersAssigned() == 0 )
				return;
		
			PlayerManager.Info info = pm.getInfo(player.getName());
		
			// pick a player to be this player's hunter. This player's victim will be the hunter's victim.
			int numCandidates = 0;
			for ( Map.Entry<String, Info> entry : pm.getPlayerInfo() )
				if ( entry.isAlive() && entry.isKiller() )
					numCandidates ++;
			
			int hunterIndex = r.nextInt(numCandidates), int i = 0;
			for ( Map.Entry<String, Info> entry : pm.getPlayerInfo() )
				if ( entry.isAlive() && entry.isKiller() )
					if ( i == hunterIndex )
					{
						info.target = entry.getValue().target;
						entry.getValue().target = player.getName();
						
						Player hunterPlayer = plugin.getServer().getPlayerExact(entry.getKey());
						if ( hunterPlayer != null && hunterPlayer.isOnline() )
							hunterPlayer.sendMessage("Your target has changed, and is now: " +  ChatColor.YELLOW + info.target + ChatColor.RESET + "!");
						break;
					}
					else
						i++;
			
			// tell them who their target is
			player.sendMessage("Your target is: " +  ChatColor.YELLOW + info.target + ChatColor.RESET + "!");
			info.setKiller(true); // everyone's a killer in their own way
		}
		else
		{
			String message = "Welcome back.";
			
			PlayerManager.Info info = pm.getInfo(player.getName());
			if ( info != null && info.target != null )
				message += " Your target is: " +  ChatColor.YELLOW + info.target + ChatColor.RESET + "!";
				
			player.sendMessage(message);
		}
	}
	
	@Override
	public void playerKilled(Player player, PlayerManager pm, PlayerManager.Info info)
	{
		if ( numSurvivors() > 1 ) 
			// find this player's hunter ... change their target to this player's target
			for ( Map.Entry<String, Info> entry : pm.getPlayerInfo() )
				if ( player.getName().equals(entry.getValue().target)
				{
					entry.getValue().target = info.target;
					Player hunterPlayer = plugin.getServer().getPlayerExact(entry.getKey());
					if ( hunterPlayer != null && hunterPlayer.isOnline() )
						hunterPlayer.sendMessage("Your target has changed, and is now: " +  ChatColor.YELLOW + info.target + ChatColor.RESET + "!");
					break;
				}
			
		info.target = null;
	}
	
	private final int teamSeparationOffset = 25;
	
	@Override
	public void prepareKiller(Player player, PlayerManager pm, boolean isNewPlayer)
	{
		if ( !isNewPlayer )
			return; // don't give items when rejoining
		
		PlayerInventory inv = player.getInventory();
		inv.addItem(new ItemStack(Material.COMPASS, 1));
	}
	
	@Override
	public void prepareFriendly(Player player, PlayerManager pm, boolean isNewPlayer)
	{
	}
	
	@Override
	public void checkForEndOfGame(PlayerManager pm, Player playerOnPlinth, Material itemOnPlinth)
	{
		// if there's no one alive at all, game was drawn
		if (pm.numSurvivors() < 2 )
		{
			pm.gameFinished(pm.numSurvivors() == 1, false, null, null);
			return;
		}
	}
}
